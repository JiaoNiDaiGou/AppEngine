package jiaonidaigou.appengine.api.tasks;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Uninterruptibles;
import jiaonidaigou.appengine.api.access.email.EmailClient;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.api.access.taskqueue.PubSubClient;
import jiaonidaigou.appengine.api.access.taskqueue.TaskQueueClient;
import jiaonidaigou.appengine.api.registry.Registry;
import jiaonidaigou.appengine.api.utils.TeddyConversions;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;

import static jiaonidaigou.appengine.common.utils.Environments.SERVICE_NAME_JIAONIDAIGOU;

/**
 * What other people send.
 * <p>
 * Start from 119520, and run backward.
 */
public class DumpTeddyShippingOrdersTaskRunner implements Consumer<TaskMessage> {
    private static final String ORDER_ARCHIEVE_DIR = Environments.GCS_ROOT_ENDSLASH + "xiaoxiong_shipping_orders/";
    private static final Logger LOGGER = LoggerFactory.getLogger(DumpTeddyShippingOrdersTaskRunner.class);
    private static final long KNOWN_START_ID = 134009;
    /**
     * Register key to store last dumped ShippingOrder ID.
     */
    public static final String REGISTRY_KEY_LAST_DUMP_ID = "dumpShippingOrder.lastDumpId";
    /**
     * If we find this number of continuous null orders, break it.
     */
    private static final int MIN_NUM_CONTINUOUS_NULL_ORDER = 100;
    /**
     * How long the task can run
     */
    private static final int TASK_ALIVE_TIME_SECONDS = 9 * 60;
    private final EmailClient emailClient;
    private final StorageClient storageClient;
    private final PubSubClient pubSubClient;
    private final TeddyClient teddyClient;
    private final Registry registry;

    @Inject
    public DumpTeddyShippingOrdersTaskRunner(final EmailClient emailClient,
                                             final StorageClient storageClient,
                                             final PubSubClient pubSubClient,
                                             @Named(TeddyAdmins.HACK) final TeddyClient teddyClient,
                                             final Registry registry) {
        this.emailClient = emailClient;
        this.storageClient = storageClient;
        this.pubSubClient = pubSubClient;
        this.teddyClient = teddyClient;
        this.registry = registry;
    }

    private static String decideDumpFileName(final DateTime dateTime) {
        return ORDER_ARCHIEVE_DIR + dateTime.toString("yyyy-MM-dd") + ".json";
    }

    private Message handleBackward(final Message message,
                                   final TaskMessage taskMessage,
                                   final DateTime startTime,
                                   final List<ShippingOrder> shippingOrders) {
        int count = 0;
        long id = determineStartId(message);

        while (DateTime.now().isBefore(startTime.plusSeconds(TASK_ALIVE_TIME_SECONDS))) {
            id--;
            Order order = loadOrder(id);
            boolean orderNull = order == null || order.getCreationTime() == null;
            LOGGER.info("Dump backward. task.reachCount={}, dumpCount={}, order.id={}, order is null? {}",
                    taskMessage.getReachCount(), count, id, orderNull);
            if (orderNull) {
                continue;
            }
            shippingOrders.add(TeddyConversions.convertShippingOrder(order));
            if (id <= KNOWN_START_ID) {
                break;
            }
        }

        LOGGER.info("Load {} orders. Save new start order id {}", shippingOrders, id);
        registry.setRegistry(SERVICE_NAME_JIAONIDAIGOU, REGISTRY_KEY_LAST_DUMP_ID, String.valueOf(id));

        boolean hasNextTask = (id > KNOWN_START_ID) && (message.limit == 0 || message.limit > taskMessage.getReachCount());
        if (!hasNextTask) {
            return null;
        }
        return new Message(id, message.limit, true);
    }

    private Message handleForward(final Message message,
                                  final TaskMessage taskMessage,
                                  final DateTime startTime,
                                  final List<ShippingOrder> shippingOrders) {
        int count = 0;
        int continuousNull = 0;
        long id = determineStartId(message);
        long lastNonNullId = message.id;
        boolean hasNextTask = true;

        while (DateTime.now().isBefore(startTime.plusSeconds(TASK_ALIVE_TIME_SECONDS))) {
            id++;
            Order order = loadOrder(id);
            boolean orderNull = order == null || order.getCreationTime() == null;
            LOGGER.info("Dump forward. task.reachCount={}, dumpCount={}, order.id={}, order is null? {}",
                    taskMessage.getReachCount(), count, id, orderNull);
            if (orderNull) {
                continuousNull++;
                if (continuousNull > MIN_NUM_CONTINUOUS_NULL_ORDER) {
                    LOGGER.info("Found {} continuous null order. I think no newer orders.", continuousNull);
                    hasNextTask = false;
                    break;
                }
            } else {
                continuousNull = 0;
                lastNonNullId = id;
                shippingOrders.add(TeddyConversions.convertShippingOrder(order));
            }
        }

        LOGGER.info("Load {} orders. Save new start order id {}", shippingOrders.size(), lastNonNullId);
        registry.setRegistry(SERVICE_NAME_JIAONIDAIGOU, REGISTRY_KEY_LAST_DUMP_ID, String.valueOf(lastNonNullId));

        hasNextTask &= message.limit == 0 || message.limit > taskMessage.getReachCount();
        if (!hasNextTask) {
            return null;
        }
        return new Message(lastNonNullId, message.limit, false);
    }

    private void writeToStorage(final List<ShippingOrder> orders) {
        Multimap<String, ShippingOrder> ordersByPath = ArrayListMultimap.create();
        for (ShippingOrder order : orders) {
            String path = decideDumpFileName(new DateTime(order.getCreationTime()));
            ordersByPath.put(path, order);
        }
        for (Map.Entry<String, Collection<ShippingOrder>> entry : ordersByPath.asMap().entrySet()) {
            String path = entry.getKey();
            List<ShippingOrder> allOrders = loadShippingOrders(path);
            allOrders.addAll(entry.getValue());
            LOGGER.info("Save {} more orders (totally {}) into {}", entry.getValue().size(), allOrders.size(), path);
            saveShippingOrders(path, allOrders);
        }
    }

    @Override
    public void accept(final TaskMessage taskMessage) {
        LOGGER.info("Start running task {}", taskMessage);

        final DateTime startTime = DateTime.now();
        final Message message;
        try {
            message = ObjectMapperProvider.get().readValue(taskMessage.getPayload(), Message.class);
        } catch (IOException e) {
            LOGGER.error("bad task message payload {}", taskMessage, e);
            return;
        }
        List<ShippingOrder> shippingOrders = new ArrayList<>();
        Message nextMessage;
        if (message.backward) {
            nextMessage = handleBackward(message, taskMessage, startTime, shippingOrders);
        } else {
            nextMessage = handleForward(message, taskMessage, startTime, shippingOrders);
        }

        writeToStorage(shippingOrders);

        if (nextMessage != null) {
            LOGGER.info("Arrange next task with order id {}", message.id);
            pubSubClient.submit(
                    TaskQueueClient.QueueName.HIGH_FREQUENCY,
                    taskMessage.toBuilder()
                            .withPayloadJson(nextMessage)
                            .increaseReachCount()
                            .build());
        } else {
            LOGGER.info("No more next task. end ticket {}.", message);
            for (String adminEmal : Environments.ADMIN_EMAILS) {
                emailClient.sendText(adminEmal,
                        "Teddy OrderDump " + taskMessage.getReachCount(),
                        String.format("End RID %s, Total order %s.", message.id, shippingOrders.size()));
            }
        }
    }

    private Order loadOrder(final long id) {
        Order order = teddyClient.getOrderDetails(id, false);
        Uninterruptibles.sleepUninterruptibly((long) (1000L + Math.random() * 1000L),
                TimeUnit.MILLISECONDS);
        return order;
    }

    private long determineStartId(final Message message) {
        long id = message.id;
        if (id == 0) {
            id = Long.parseLong(registry.getRegistry(SERVICE_NAME_JIAONIDAIGOU, REGISTRY_KEY_LAST_DUMP_ID));
        }
        LOGGER.info("determine start ID {}", id);
        return id;
    }

    private List<ShippingOrder> loadShippingOrders(final String path) {
        if (!storageClient.exists(path)) {
            return new ArrayList<>();
        }
        byte[] bytes = storageClient.read(path);
        try {
            return ObjectMapperProvider.get().readValue(bytes, new TypeReference<List<ShippingOrder>>() {
            });
        } catch (IOException e) {
            LOGGER.error("Failed to load orders from {}.", path, e);
            return new ArrayList<>();
        }
    }

    private void saveShippingOrders(final String path, final Collection<ShippingOrder> shippingOrders) {
        List<ShippingOrder> toSave = new ArrayList<>(shippingOrders);
        toSave.sort(Comparator.comparing(ShippingOrder::getTeddyOrderId));
        byte[] bytes;
        try {
            bytes = ObjectMapperProvider.get().writeValueAsBytes(toSave);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to save orders to {}.", path, e);
            return;
        }
        storageClient.write(path, MediaType.JSON_UTF_8.toString(), bytes);
    }

    public static class Message {
        @JsonProperty
        private long id;
        @JsonProperty
        private int limit;
        @JsonProperty
        private boolean backward;

        public Message(long id, int limit, boolean backward) {
            this.id = id;
            this.limit = limit;
            this.backward = backward;
        }

        // For json
        Message() {
        }
    }
}
