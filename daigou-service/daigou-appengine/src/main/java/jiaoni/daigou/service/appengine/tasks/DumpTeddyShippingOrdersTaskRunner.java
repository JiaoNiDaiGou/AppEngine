package jiaoni.daigou.service.appengine.tasks;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Uninterruptibles;
import jiaoni.common.appengine.access.email.EmailClient;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.appengine.access.taskqueue.PubSubClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.appengine.access.taskqueue.TaskQueueClient;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.model.Order;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.teddy.TeddyUtils;
import jiaoni.daigou.service.appengine.utils.RegistryFactory;
import jiaoni.daigou.service.appengine.utils.ShippingOrderUtils;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * What other people send.
 */
public class DumpTeddyShippingOrdersTaskRunner implements Consumer<TaskMessage> {
    private static final String DUMP_DIR = AppEnvs.Dir.SHIPPING_ORDERS_DUMP;
    private static final String ARCHIEVE_DIR = AppEnvs.Dir.SHIPPING_ORDERS_ARCHIVE;
    private static final Logger LOGGER = LoggerFactory.getLogger(DumpTeddyShippingOrdersTaskRunner.class);
    private static final long KNOWN_START_ID = 134009;
    /**
     * Register key to store last forward dumped ShippingOrder ID.
     */
    private static final String REGISTRY_KEY_LAST_FORWARD_DUMP_ID = "dumpShippingOrder.lastDumpId.forward";
    /**
     * Register key to store last backward dumped ShippingOrder ID.
     */
    private static final String REGISTRY_KEY_LAST_BACKWARD_DUMP_ID = "dumpShippingOrder.lastDumpId.backward";
    /**
     * If we find this number of continuous null orders, break it.
     */
    private static final int MAX_CONTINUOUS_NULL = 100;
    private static final int MAX_CONTINUOUS_NULL_FOR_ONE_DAY = 10;

    /**
     * How long the task can run
     */
    private static final int TASK_ALIVE_TIME_SECONDS = 9 * 60;

    private final EmailClient emailClient;
    private final StorageClient storageClient;
    private final PubSubClient pubSubClient;
    private final TeddyClient teddyClient;

    @Inject
    public DumpTeddyShippingOrdersTaskRunner(final EmailClient emailClient,
                                             final StorageClient storageClient,
                                             final PubSubClient pubSubClient,
                                             @Named(TeddyAdmins.HACK) TeddyClient teddyClient) {
        this.emailClient = emailClient;
        this.storageClient = storageClient;
        this.pubSubClient = pubSubClient;
        this.teddyClient = teddyClient;
    }

    private boolean handleBackward(final Message message,
                                   final TaskMessage taskMessage,
                                   final DateTime startTime) {
        final DateTime endTime = startTime.plusSeconds(TASK_ALIVE_TIME_SECONDS);
        final long startId = determineStartId(message, REGISTRY_KEY_LAST_BACKWARD_DUMP_ID);

        List<ShippingOrder> shippingOrders = new ArrayList<>();

        long curId = startId;
        while (DateTime.now().isBefore(endTime)) {
            curId--;
            ShippingOrder shippingOrder = loadShippingOrder(curId);
            boolean isNull = shippingOrder == null || shippingOrder.getCreationTime() == 0;
            if (!isNull) {
                shippingOrders.add(shippingOrder);
            }

            if (curId <= KNOWN_START_ID) {
                LOGGER.info("Reach known start ID {}", KNOWN_START_ID);
                break;
            }
        }
        boolean hasNextTask = (curId > KNOWN_START_ID) && (message.limit == 0 || taskMessage.getReachCount() < message.limit);

        LOGGER.info("Load {} orders and save. startId={}, endId={}. hasNextTask={}. newStartId={}",
                shippingOrders.size(), startId, curId, hasNextTask, curId);
        saveLastDumpId(REGISTRY_KEY_LAST_BACKWARD_DUMP_ID, curId);
        saveShippingOrders(shippingOrders);

        message.id = curId;
        return hasNextTask;
    }

    /**
     * Handle forward. Stop criteria:
     * - task.limit reached.
     * - the creation date of last non-null item is within 1 day, and continues 10 null.
     * - continues 100 nulls
     * <p>
     * Returns if schedule next task.
     */
    private boolean handleForward(final Message message,
                                  final TaskMessage taskMessage,
                                  final DateTime startTime) {

        final DateTime endTime = startTime.plusSeconds(TASK_ALIVE_TIME_SECONDS);
        final long startId = determineStartId(message, REGISTRY_KEY_LAST_FORWARD_DUMP_ID);

        List<ShippingOrder> shippingOrders = new ArrayList<>();

        int continuousNullCount = 0;
        long curId = startId;
        long lastNonNullId = startId;
        boolean hasNextTask = true;
        DateTime lastNonNullTimestamp = message.latestNonNullTimestamp;

        while (DateTime.now().isBefore(endTime)) {
            ShippingOrder shippingOrder = loadShippingOrder(curId);
            boolean isNull = shippingOrder == null || shippingOrder.getCreationTime() == 0;
            if (isNull) {
                continuousNullCount++;

                if (continuousNullCount > MAX_CONTINUOUS_NULL_FOR_ONE_DAY
                        && lastNonNullTimestamp != null
                        && DateTime.now().minusDays(1).isBefore(lastNonNullTimestamp)) {
                    LOGGER.info("Found {} continues null order when last non-null order is within 1 day. BREAK!", continuousNullCount);
                    hasNextTask = false;
                    break;
                }

                if (continuousNullCount > MAX_CONTINUOUS_NULL) {
                    LOGGER.info("Found {} continuous null order. I think no newer orders.", continuousNullCount);
                    hasNextTask = false;
                    break;
                }

                // For others, just keep going.
            } else {
                continuousNullCount = 0;
                lastNonNullId = curId;
                lastNonNullTimestamp = new DateTime(shippingOrder.getCreationTime());
                shippingOrders.add(shippingOrder);
            }
            curId++;
        }

        hasNextTask &= message.limit == 0 || taskMessage.getReachCount() < message.limit;

        LOGGER.info("Load {} orders and save. startId={}, endId={}. hasNextTask={}. newStartId={}",
                shippingOrders.size(), startId, curId, hasNextTask, lastNonNullId);
        saveLastDumpId(REGISTRY_KEY_LAST_FORWARD_DUMP_ID, lastNonNullId);
        saveShippingOrders(shippingOrders);

        // If we continue task, just start from curId. Otherwise, tomorrow, the task will start from lastNonNullId.
        message.id = curId - 1;
        message.latestNonNullTimestamp = lastNonNullTimestamp;

        return hasNextTask;
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

        boolean hasNext = message.backward
                ? handleBackward(message, taskMessage, startTime)
                : handleForward(message, taskMessage, startTime);

        if (hasNext) {
            LOGGER.info("Arrange next task with order id {}", message.id);
            pubSubClient.submit(
                    TaskQueueClient.QueueName.HIGH_FREQUENCY,
                    taskMessage.toBuilder()
                            .withPayloadJson(message)
                            .increaseReachCount()
                            .build());
        } else {
            archiveShippingOrders();

            LOGGER.info("No more next task. end ticket {}.", message);

            String emailContent = String.format("Teddy OrderDump. Message.reachCount=%s, Start RID=%s, End RID=%s, End RID.ts=%s",
                    taskMessage.getReachCount(),
                    message.startId,
                    message.id,
                    message.latestNonNullTimestamp == null ? "null" : message.latestNonNullTimestamp.toDateTime(DateTimeZone.forOffsetHours(-8)));

            for (String adminEmal : AppEnvs.getAdminEmails()) {
                emailClient.sendText(adminEmal, "teddy order dump", emailContent);
            }

            // In the end, kick off rank task
            LOGGER.info("Schedule Teddy Rank task");
            pubSubClient.submit(PubSubClient.QueueName.HIGH_FREQUENCY,
                    TaskMessage.newEmptyMessage(TeddyRankTaskRunner.class));
        }
    }

    private void archiveShippingOrders() {
        LOGGER.info("Archiving shipping orders");
        List<String> paths = storageClient.listAll(DUMP_DIR);
        if (paths.isEmpty()) {
            return;
        }
        LOGGER.info("Ready to archive following dump files: {}", paths);

        List<ShippingOrder> shippingOrders = new ArrayList<>();
        for (String path : paths) {
            LOGGER.info("Load dump {}", path);
            shippingOrders.addAll(loadShippingOrders(path));
        }

        Multimap<String, ShippingOrder> map = ArrayListMultimap.create();
        for (ShippingOrder shippingOrder : shippingOrders) {
            DateTime creationTime = new DateTime(shippingOrder.getCreationTime());
            String month = creationTime.toString("yyyy_MM");
            map.put(month, shippingOrder);
        }
        LOGGER.info("Totally load {} shipping orders from dump", map.size());

        for (Map.Entry<String, Collection<ShippingOrder>> entry : map.asMap().entrySet()) {
            // archive_dir/2018_04.json
            String path = ARCHIEVE_DIR + entry.getKey() + ".json";

            List<ShippingOrder> toSave = storageClient.exists(path) ? new ArrayList<>(loadShippingOrders(path)) : new ArrayList<>();
            toSave.addAll(entry.getValue());
            toSave.sort(ShippingOrderUtils.comparatorByTeddyOrderIdAsc());

            byte[] bytes;
            try {
                bytes = ObjectMapperProvider.get().writeValueAsBytes(toSave);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to save orders to {}.", path, e);
                return;
            }
            LOGGER.info("Write archive {} orders to archive: {}", toSave.size(), path);
            storageClient.write(path, MediaType.JSON_UTF_8.toString(), bytes);
        }

        for (String path : paths) {
            LOGGER.info("Delete dump file {}", path);
            storageClient.delete(path);
        }
    }

    private List<ShippingOrder> loadShippingOrders(final String path) {
        byte[] bytes = storageClient.read(path);
        try {
            return ObjectMapperProvider.get().readValue(bytes, new TypeReference<List<ShippingOrder>>() {
            });
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    private void saveLastDumpId(final String key, final long id) {
        RegistryFactory.get().setRegistry(AppEnvs.getServiceName(), key, String.valueOf(id));
    }

    private long determineStartId(final Message message, final String key) {
        long id = message.id;
        if (id == 0) {
            id = Long.parseLong(RegistryFactory.get().getRegistry(AppEnvs.getServiceName(), key));
        }
        LOGGER.info("determine start ID {}", id);
        if (message.startId == 0) {
            message.startId = id;
        }
        return id;
    }

    private ShippingOrder loadShippingOrder(final long id) {
        Order order = teddyClient.getOrderDetails(id, false);
        long waitTime = (long) (1000L + 1000 * Math.random());
        Uninterruptibles.sleepUninterruptibly(waitTime, TimeUnit.MILLISECONDS);
        if (order == null) {
            LOGGER.info("Order id {} is null", id);
            return null;
        }
        return TeddyUtils.convertToShippingOrder(order);
    }

    private void saveShippingOrders(final Collection<ShippingOrder> shippingOrders) {
        if (CollectionUtils.isEmpty(shippingOrders)) {
            LOGGER.warn("Nothing to save");
            return;
        }

        List<ShippingOrder> toSave = new ArrayList<>(shippingOrders);
        toSave.sort(ShippingOrderUtils.comparatorByTeddyOrderIdAsc());
        String minTeddyId = toSave.get(0).getTeddyOrderId();
        String maxTeddyId = toSave.get(toSave.size() - 1).getTeddyOrderId();

        String path = DUMP_DIR + DateTime.now().toString("yyyy_MM_dd") + "_" + minTeddyId + "_" + maxTeddyId + ".json";

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
        // The first start ID.
        @JsonProperty
        long startId;
        // Last stopped ID.
        @JsonProperty
        long id;
        @JsonProperty
        int limit;
        @JsonProperty
        boolean backward;
        @JsonProperty
        DateTime latestNonNullTimestamp;

        // For json
        Message() {
        }

        public Message(long id, int limit, boolean backward) {
            this.id = id;
            this.limit = limit;
            this.backward = backward;
        }
    }
}
