package jiaonidaigou.appengine.api.tasks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.api.access.email.EmailClient;
import jiaonidaigou.appengine.api.access.sms.SmsClient;
import jiaonidaigou.appengine.api.utils.TeddyConversions;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.contenttemplate.TemplateData;
import jiaonidaigou.appengine.contenttemplate.TemplatesFactory;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.model.OrderPreview;
import jiaonidaigou.appengine.wiremodel.entity.Price;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.CREATED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.DELIVERED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.PENDING;

@Singleton
public class SyncJiaoniShippingOrdersTaskRunner implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncJiaoniShippingOrdersTaskRunner.class);

    private static final String JIAONI_SMS_TEMPLATE_ID_WITH_POSTMAN = "176834";
    private static final String JIAONI_SMS_TEMPLATE_ID_WITHOUT_POSTMAN = "176833";
    private static final Set<String> KNOWN_RAW_SHIPPING_STATUS = Sets.newHashSet("邮政平邮", "圆通速递", "邮政包裹");
    private static final int DISPLAY_SHIPPING_HISTORY_ITEMS = 7;
    private static final int NO_UPDATES_WARN_DAYS = 20;
    private static final int MAX_ORDER_PREVIEWS_TO_CHECK = 5;

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


    private static boolean determineNotifyCustomer(final ShippingOrder shippingOrder) {
        if (shippingOrder.getCustomerNotified()) {
            return false;
        }
        ShippingOrder.Status status = shippingOrder.getStatus();
        return (status == CN_TRACKING_NUMBER_ASSIGNED || status == CN_POSTMAN_ASSIGNED)
                || (status == DELIVERED && shippingOrder.getShippingEnding() == ShippingOrder.ShippingEnding.PICK_UP_BOX);
    }

    private static List<Map<String, Object>> shippingOrdersInTemplate(final Collection<ShippingOrder> shippingOrders,
                                                                      final List<ShippingOrder> toNotifyCustomer) {
        Set<String> toNotifyCustomerOrderIds = toNotifyCustomer.stream()
                .map(ShippingOrder::getId)
                .collect(Collectors.toSet());
        return shippingOrders.stream()
                .map(t -> shippingOrderInTemplate(t, toNotifyCustomerOrderIds.contains(t.getId())))
                .collect(Collectors.toList());
    }

    private static Map<String, Object> shippingOrderInTemplate(final ShippingOrder shippingOrder,
                                                               final boolean notifyCustomer) {
        String productSummary = String.join("<br />",
                Arrays.stream(StringUtils.split(shippingOrder.getProductSummary(), ";"))
                        .filter(StringUtils::isNotBlank).collect(Collectors.toList()));

        List<ShippingOrder.ShippingHistoryEntry> shippingTracks = shippingOrder.getShippingHistoryList();
        if (shippingTracks.size() > DISPLAY_SHIPPING_HISTORY_ITEMS) {
            shippingTracks = shippingTracks.subList(0, DISPLAY_SHIPPING_HISTORY_ITEMS);
        }

        String latestStatus = shippingTracks.stream()
                .map(t -> new DateTime(t.getTimestamp()).toString("yyyy-MM-dd HH:mm:ss") + " " + t.getStatus())
                .reduce((a, b) -> a + "<br />" + b)
                .orElse("");
        String postmanInfo = "";
        if (shippingOrder.hasPostman()) {
            postmanInfo = shippingOrder.getPostman().getName() + shippingOrder.getPostman().getPhone();
        }

        return new TemplateData()
                .add("id", shippingOrder.getTeddyOrderId())
                .add("formattedId", String.valueOf(shippingOrder.getTeddyFormattedId()))
                .addAsDateTime("creationTime", shippingOrder.getCreationTime())
                .addAsDateTime("lastUpdateTime", lastUpdateTime(shippingOrder))
                .add("receiverName", shippingOrder.getReceiver().getName(), "")
                .add("receiverPhone", shippingOrder.getReceiver().getPhone().getPhone(), "")
                .add("productSummary", productSummary, "")
                .add("price", formatPrice(shippingOrder.getTotalPrice()), "")
                .add("shippingCarrier", shippingOrder.getShippingCarrier(), "")
                .add("trackingNumber", shippingOrder.getTrackingNumber(), "")
                .add("latestStatus", latestStatus)
                .add("postmanInfo", postmanInfo)
                .add("smsCustomerNotificationSend", notifyCustomer)
                .build();
    }

    private static long lastUpdateTime(final ShippingOrder shippingOrder) {
        if (shippingOrder.getShippingHistoryCount() == 0) {
            return 0;
        }
        return shippingOrder.getShippingHistory(shippingOrder.getShippingHistoryCount() - 1).getTimestamp();
    }

    private static String formatPrice(final Price price) {
        String unitStr;
        switch (price.getUnit()) {
            case USD:
                unitStr = "$";
                break;
            case RMB:
                unitStr = "¥";
                break;
            default:
                unitStr = price.getUnit().name();
                break;
        }
        String valStr = String.valueOf(price.getValue());
        valStr = valStr.substring(0, Math.min(4, valStr.length()));
        return unitStr + valStr;
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        // First track new orders.
        for (int page = 1; page <= MAX_ORDER_PREVIEWS_TO_CHECK; page++) {
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

        List<ShippingOrder> curShippingOrders = shippingOrderDbClient.queryNonDeliveredOrders();
        LOGGER.info("Need to sync {} non-delivered orders.", curShippingOrders.size());

        List<ShippingOrder> newShippingOrders = new ArrayList<>();
        List<ShippingOrder> toSave = new ArrayList<>();
        List<ShippingOrder> toNotifyCustomer = new ArrayList<>();

        for (ShippingOrder curShippingOrder : curShippingOrders) {
            ShippingOrder newShippingOrder = TeddyConversions.convertShippingOrder(
                    hackTeddyClient.getOrderDetails(Long.parseLong(curShippingOrder.getTeddyOrderId()), true)
            );

            // Carry over other information:
            newShippingOrder = newShippingOrder.toBuilder()
                    .setCustomerNotified(curShippingOrder.getCustomerNotified())
                    .build();
            if (determineNotifyCustomer(newShippingOrder)) {
                toNotifyCustomer.add(newShippingOrder);
            } else {
                newShippingOrder = newShippingOrder.toBuilder()
                        .setCustomerNotified(true)
                        .build();
            }

            newShippingOrders.add(newShippingOrder);
            if (!curShippingOrder.equals(newShippingOrder)) {
                toSave.add(newShippingOrder);
            }
        }
        shippingOrderDbClient.put(toSave);

        for (ShippingOrder shippingOrder : toNotifyCustomer) {
            notifyCustomer(shippingOrder);
            shippingOrderDbClient.put(shippingOrder.toBuilder().setCustomerNotified(true).build());
        }
        notifyAdmin(curShippingOrders, newShippingOrders, toNotifyCustomer);
    }

    private void notifyAdmin(
            final List<ShippingOrder> curShippingOrders,
            final List<ShippingOrder> newShippingOrders,
            final List<ShippingOrder> notifyCustomer) {
        Multimap<ShippingOrder.Status, ShippingOrder> oldOrdersByStatus = ArrayListMultimap.create();
        curShippingOrders.forEach(t -> oldOrdersByStatus.put(t.getStatus(), t));
        Multimap<ShippingOrder.Status, ShippingOrder> newOrdersByStatus = ArrayListMultimap.create();
        newShippingOrders.forEach(t -> newOrdersByStatus.put(t.getStatus(), t));
        DateTime now = DateTime.now();
        List<ShippingOrder> noUpdatesOverDays = newShippingOrders
                .stream()
                .filter(t -> new DateTime(lastUpdateTime(t)).isBefore(now.minusDays(NO_UPDATES_WARN_DAYS)))
                .collect(Collectors.toList());


        TemplateData overview = new TemplateData()
                .add("a.下单成功 待付款 - 总数",
                        newOrdersByStatus.get(CREATED).size())
                .add("a.下单成功 待付款 - 新增数",
                        newOrdersByStatus.get(CREATED).size() - oldOrdersByStatus.get(CREATED).size())
                .add("b.小熊处理 - 总数",
                        newOrdersByStatus.get(PENDING).size())
                .add("b.小熊处理 - 新增数",
                        newOrdersByStatus.get(PENDING).size() - newOrdersByStatus.get(PENDING).size())
                .add("d.国内快递追踪号生成 - 新增数",
                        newOrdersByStatus.get(CN_TRACKING_NUMBER_ASSIGNED).size() - oldOrdersByStatus.get(CN_TRACKING_NUMBER_ASSIGNED).size())
                .add("e.国内配送快递员信息获知 - 新增数",
                        newOrdersByStatus.get(CN_POSTMAN_ASSIGNED).size() - oldOrdersByStatus.get(CN_POSTMAN_ASSIGNED).size())
                .add("f.发送短信通知", notifyCustomer.size())
                .add("f." + NO_UPDATES_WARN_DAYS + "天小熊未更新 - 总数", noUpdatesOverDays.size());
//        for (Map.Entry<Integer, Integer> entry : stats.getOrderCntBySenderJiaoRank().entrySet()) {
//            overview.add("h.过去" + entry.getKey() + "天" + UserMode.JIAO.getSenderName() + "小熊发货排名",
//                    entry.getValue() + 1);

        String date = DateTime.now().toString("yyyy-MM-dd");
        String subject = "小熊发货单状态 更新 " + date;

        Map<String, Object> data = new TemplateData()
                .addAsDateTime("queryTime", DateTime.now())
                .add("overview", overview.build())
                .add("newlyCreated", shippingOrdersInTemplate(newOrdersByStatus.get(CREATED), notifyCustomer))
                .add("newlyPending", shippingOrdersInTemplate(newOrdersByStatus.get(PENDING), notifyCustomer))
                .add("newlyTrackingNumberAssigned", shippingOrdersInTemplate(newOrdersByStatus.get(CN_TRACKING_NUMBER_ASSIGNED), notifyCustomer))
                .add("newlyPostmanAssigned", shippingOrdersInTemplate(newOrdersByStatus.get(CN_POSTMAN_ASSIGNED), notifyCustomer))
                .add("noUpdatesOverDays", noUpdatesOverDays)
                // TODO
                .add("last30DaysSenderReports", new ArrayList<>())
                .build();

        LOGGER.info("Data in template: {}", data);

        String html = TemplatesFactory.instance()
                .getTemplate("jiaoni_shippingorders_summary_zh_cn.ftl")
                .toContent(data);

        if (StringUtils.isNotBlank(subject) && StringUtils.isNotBlank(html)) {
            emailClient.sendHtml(String.join(",", Environments.ADMIN_EMAILS), subject, html);
        }

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
}
