package jiaonidaigou.appengine.api.tasks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.api.access.email.EmailClient;
import jiaonidaigou.appengine.api.access.sms.SmsClient;
import jiaonidaigou.appengine.api.utils.TeddyConversions;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.model.OrderPreview;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.CN_POSTMAN_ASSIGNED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.DELIVERED;

@Singleton
public class SyncJiaoniShippingOrdersTaskRunner implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncJiaoniShippingOrdersTaskRunner.class);

    private static final String JIAONI_SMS_TEMPLATE_ID_WITH_POSTMAN = "176834";
    private static final String JIAONI_SMS_TEMPLATE_ID_WITHOUT_POSTMAN = "176833";
    private static final Set<String> KNOWN_RAW_SHIPPING_STATUS = Sets.newHashSet("邮政平邮", "圆通速递", "邮政包裹");

    private final TeddyClient jiaoniTeddyClient;
    private final TeddyClient hackTeddyClient;
    private final ShippingOrderDbClient shippingOrderDbClient;
    private final EmailClient emailClient;
    private final SmsClient smsClient;

    @Inject
    public SyncJiaoniShippingOrdersTaskRunner(@Named(TeddyAdmins.JIAONI) final TeddyClient jiaoniTeddyClient,
                                              @Named(TeddyAdmins.HACK) final TeddyClient hackTeddyClient,
                                              final ShippingOrderDbClient shippingOrderDbClient,
                                              final EmailClient emailClient,
                                              final SmsClient smsClient) {
        this.jiaoniTeddyClient = jiaoniTeddyClient;
        this.hackTeddyClient = hackTeddyClient;
        this.shippingOrderDbClient = shippingOrderDbClient;
        this.emailClient = emailClient;
        this.smsClient = smsClient;
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        // First track new orders.
        for (int page = 1; page < 2; page++) {
            LOGGER.info("Handle order preview page {}", page);

            Map<Long, OrderPreview> orderPreviews = jiaoniTeddyClient.getOrderPreviews(page);
            if (MapUtils.isEmpty(orderPreviews)) {
                LOGGER.info("Load 0 order previews from page {}. break!", page);
                break;
            }

            LOGGER.info("Load {} order previews from page {}", orderPreviews.size(), page);

            long minOrderId = orderPreviews.keySet().stream().min(Long::compare).get();
            long maxOrderId = orderPreviews.keySet().stream().max(Long::compare).get();

            List<ShippingOrder> shippingOrders = shippingOrderDbClient.queryByTeddyOrderIdRange(minOrderId, maxOrderId);
            LOGGER.info("Load {} orders from DB. minTeddyId={}, maxTeddyId={}. page {} all tracked={}. ",
                    shippingOrders.size(), minOrderId, maxOrderId, page, orderPreviews.size() == shippingOrders.size());

            boolean allOrdersInThisPageTracked = shippingOrders.size() >= orderPreviews.size();
            if (allOrdersInThisPageTracked) {
                LOGGER.info("All {} orders in page {} has been tracked in DB. break!", orderPreviews.size(), page);
                break;
            }

            LOGGER.info("Found {} new orders in Teddy. Tracking them!", orderPreviews.size() - shippingOrders.size());
            Set<Long> trackedOrderIds = shippingOrders.stream()
                    .map(t -> Long.parseLong(t.getTeddyOrderId()))
                    .collect(Collectors.toSet());
            Collection<Long> orderIdsToTrack = CollectionUtils.subtract(orderPreviews.keySet(), trackedOrderIds);
            List<ShippingOrder> ordersToTrack = orderIdsToTrack
                    .stream()
                    .map(t -> hackTeddyClient.getOrderDetails(t, true))
                    .map(TeddyConversions::convertShippingOrder)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            shippingOrderDbClient.put(ordersToTrack);
        }

        Multimap<ShippingOrder.Status, ShippingOrder> updatedShippingOrders = ArrayListMultimap.create();
        List<ShippingOrder> toSave = new ArrayList<>();
        List<ShippingOrder> toNotifyCustomer = new ArrayList<>();

        // Then track orders by status.
        List<ShippingOrder> nonDeliveredShippingOrders = shippingOrderDbClient.queryNonDeliveredOrders();
        for (ShippingOrder shippingOrder : nonDeliveredShippingOrders) {

            ShippingOrder updatedShippingOrder = TeddyConversions.convertShippingOrder(
                    hackTeddyClient.getOrderDetails(Long.parseLong(shippingOrder.getTeddyOrderId()), true)
            );

            if (updatedShippingOrder.getStatus() != shippingOrder.getStatus()) {
                updatedShippingOrders.put(updatedShippingOrder.getStatus(), updatedShippingOrder);
            }
            if (notifyCustomer(shippingOrder.getStatus(), updatedShippingOrder.getStatus())) {
                toNotifyCustomer.add(updatedShippingOrder);
            }
            if (!updatedShippingOrder.equals(shippingOrder)) {
                toSave.add(updatedShippingOrder);
            }
        }
        shippingOrderDbClient.put(toSave);


        // Notify customer
        for (ShippingOrder shippingOrder : toNotifyCustomer) {
            notifyCustomer(shippingOrder);
        }

        // Send email
    }

    private void notifyCustomer(final ShippingOrder shippingOrder) {
        if (StringUtils.isNoneBlank(
                shippingOrder.getReceiver().getPhone().getPhone(),
                shippingOrder.getTrackingNumber()
        )) {
            String rawShippingStatus = StringUtils.trimToEmpty(shippingOrder.getShippingCarrier());
            if (!KNOWN_RAW_SHIPPING_STATUS.contains(rawShippingStatus)) {
                LOGGER.warn("Order {} has tracking number {}. But its shipping carrier is unknown {}.",
                        shippingOrder, shippingOrder.getTrackingNumber(), shippingOrder.getShippingCarrier());
            }

            final String[] params;
            final String templateId;
            if (shippingOrder.hasPostman() && StringUtils.isNoneBlank(
                    shippingOrder.getPostman().getName(),
                    shippingOrder.getPostman().getPhone().getPhone())) {
                templateId = JIAONI_SMS_TEMPLATE_ID_WITH_POSTMAN;
                params = new String[]{
                        rawShippingStatus,
                        shippingOrder.getTrackingNumber(),
                        shippingOrder.getPostman().getName(),
                        shippingOrder.getPostman().getPhone().getPhone()
                };
            } else {
                templateId = JIAONI_SMS_TEMPLATE_ID_WITHOUT_POSTMAN;
                params = new String[]{
                        rawShippingStatus,
                        shippingOrder.getTrackingNumber()
                };
            }
            smsClient.sendTextWithTemplate("cn",
                    shippingOrder.getReceiver().getPhone().getPhone(),
                    templateId,
                    params);
        }
    }

    private static boolean notifyCustomer(final ShippingOrder.Status oldStatus, final ShippingOrder.Status newStatus) {
        return (newStatus == CN_TRACKING_NUMBER_ASSIGNED || newStatus == CN_POSTMAN_ASSIGNED)
                && (oldStatus != CN_TRACKING_NUMBER_ASSIGNED && oldStatus != CN_POSTMAN_ASSIGNED && oldStatus != DELIVERED);
    }
}
