package jiaonidaigou.appengine.api.tasks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.api.access.email.EmailClient;
import jiaonidaigou.appengine.api.access.sms.SmsClient;
import jiaonidaigou.appengine.api.utils.ShippingOrderUtils;
import jiaonidaigou.appengine.api.utils.TeddyUtils;
import jiaoni.common.utils.Environments;
import jiaonidaigou.appengine.contenttemplate.TemplateData;
import jiaonidaigou.appengine.contenttemplate.TemplatesFactory;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.wiremodel.entity.Price;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.api.utils.TeddyUtils.getKnownShippingCarriers;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.CN_POSTMAN_ASSIGNED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.DELIVERED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.EXTERNAL_SHIPPING_CREATED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.EXTERNAL_SHPPING_PENDING;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.INIT;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.PACKED;

@Singleton
public class SyncJiaoniShippingOrdersTaskRunner implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncJiaoniShippingOrdersTaskRunner.class);

    private static final String JIAONI_SMS_TEMPLATE_ID_WITH_POSTMAN = "176834";
    private static final String JIAONI_SMS_TEMPLATE_ID_WITHOUT_POSTMAN = "176833";
    private static final int DISPLAY_SHIPPING_HISTORY_ITEMS = 7;
    private static final int NO_UPDATES_WARN_DAYS = 20;
    private static final int MAX_ORDER_PREVIEWS_TO_CHECK = 4;

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
        Status status = shippingOrder.getStatus();
        return (status == CN_TRACKING_NUMBER_ASSIGNED || status == CN_POSTMAN_ASSIGNED)
                || (status == DELIVERED && shippingOrder.getShippingEnding() == ShippingOrder.ShippingEnding.PICK_UP_BOX);
    }

    private static List<Map<String, Object>> shippingOrdersInTemplate(final Collection<ShippingOrder> shippingOrders,
                                                                      final List<String> toNotifyCustomerOrderIds) {
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
                .add("teddyOrderId", shippingOrder.getTeddyOrderId())
                .add("teddyFormattedId", String.valueOf(shippingOrder.getTeddyFormattedId()))
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
            return shippingOrder.getCreationTime();
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

    private void syncFromPreview() {
        List<ShippingOrder> toSave = new ArrayList<>();

        // Load preview first.
        for (int i = 1; i <= MAX_ORDER_PREVIEWS_TO_CHECK; i++) {
            List<ShippingOrder> toSaveInThisPage = new ArrayList<>();

            LOGGER.info("teddy preview page {}", i);
            Map<Long, ShippingOrder> fromPreview = jiaoniTeddyClient.getOrderPreviews(i).entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            t -> TeddyUtils.convertToShippingOrderFromOrderPreview(t.getValue())
                    ));

            if (MapUtils.isEmpty(fromPreview)) {
                LOGGER.info("Load 0 order previews from page {}. break!", i);
                break;
            }

            long minTeddyOrderId = fromPreview.keySet().stream().min(Long::compare).get();
            long maxTeddyOrderId = fromPreview.keySet().stream().max(Long::compare).get();

            Map<Long, ShippingOrder> fromDb = shippingOrderDbClient.queryByTeddyOrderIdRange(minTeddyOrderId, maxTeddyOrderId)
                    .stream()
                    .collect(Collectors.toMap(
                            t -> Long.parseLong(t.getTeddyOrderId()),
                            t -> t
                    ));
            LOGGER.info("Load {} orders from DB. minTeddyId={}, maxTeddyId={}.", fromDb.size(), minTeddyOrderId, maxTeddyOrderId);

            for (Map.Entry<Long, ShippingOrder> entry : fromPreview.entrySet()) {
                ShippingOrder previewOrder = entry.getValue();
                ShippingOrder dbOrder = fromDb.get(entry.getKey());
                if (dbOrder == null) { // A new order
                    toSaveInThisPage.add(previewOrder);
                    continue;
                }
                boolean canUpdate = dbOrder.getStatus() == EXTERNAL_SHPPING_PENDING || StringUtils.isBlank(dbOrder.getTrackingNumber());
                if (canUpdate && StringUtils.isNotBlank(previewOrder.getTrackingNumber())) {
                    ShippingOrder toUpdate = dbOrder.toBuilder()
                            .setTrackingNumber(previewOrder.getTrackingNumber())
                            .setShippingCarrier(previewOrder.getShippingCarrier())
                            .setStatus(CN_TRACKING_NUMBER_ASSIGNED)
                            .build();
                    toSaveInThisPage.add(toUpdate);
                }
            }

            if (toSaveInThisPage.isEmpty()) {
                LOGGER.info("All {} orders in preview page {} has been tracked in DB. break!", fromPreview.size(), i);
                break;
            }

            toSave.addAll(toSaveInThisPage);
        }

        LOGGER.info("Totally need to track {} added/updated orders.", toSave.size());
        shippingOrderDbClient.put(toSave);
    }

    private void syncFromDb(final Report report) {
        List<ShippingOrder> toCheck = shippingOrderDbClient.queryNonDeliveredOrders();
        List<ShippingOrder> toSave = new ArrayList<>();
        List<ShippingOrder> toNotifyCustomer = new ArrayList<>();

        for (ShippingOrder order : toCheck) {
            if (StringUtils.isBlank(order.getTeddyOrderId())) {
                report.orderByStatus.put(order.getStatus(), order);
                continue;
            }

            long teddyOrderId = Long.parseLong(order.getTeddyOrderId());
            boolean includeShippingHistory = StringUtils.isNotBlank(order.getTrackingNumber());
            ShippingOrder syncedOrder = TeddyUtils.convertToShippingOrder(
                    hackTeddyClient.getOrderDetails(teddyOrderId, includeShippingHistory));
            order = ShippingOrderUtils.mergeSyncedOrder(order, syncedOrder);

            report.orderByStatus.put(order.getStatus(), order);

            boolean notifyCustomer = determineNotifyCustomer(order);
            if (notifyCustomer) {
                toNotifyCustomer.add(order);
            }

            toSave.add(order);
        }
        shippingOrderDbClient.put(toSave);

        for (ShippingOrder order : toNotifyCustomer) {
            notifyCustomer(order);
            report.customerNotifiedIds.add(order.getId());
        }
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        Report report = new Report();
        syncFromPreview();
        syncFromDb(report);
        notifyAdmin(report);
    }

    private void notifyAdmin(final Report report) {
        long noUpdatesLine = DateTime.now().minusDays(NO_UPDATES_WARN_DAYS).getMillis();

        Multimap<Status, ShippingOrder> byStatus = report.orderByStatus;
        List<ShippingOrder> noUpdates = byStatus.values()
                .stream()
                .filter(t -> lastUpdateTime(t) < noUpdatesLine)
                .collect(Collectors.toList());

        TemplateData overview = new TemplateData()
                .add("a.新建快递单 待包货",
                        byStatus.get(INIT).size() + byStatus.get(PACKED).size())
                .add("b.等待小熊付款",
                        byStatus.get(EXTERNAL_SHIPPING_CREATED).size())
                .add("c.小熊处理 - 总数",
                        byStatus.get(EXTERNAL_SHPPING_PENDING).size())
                .add("d.国内快递可追踪",
                        byStatus.get(CN_TRACKING_NUMBER_ASSIGNED).size() + byStatus.get(CN_POSTMAN_ASSIGNED).size())
                .add("e.已送达",
                        byStatus.get(DELIVERED).size())
                .add("f.发送短信通知",
                        report.customerNotifiedIds.size())
                .add("g." + NO_UPDATES_WARN_DAYS + "天小熊未更新 - 总数",
                        noUpdates.size());

        String date = DateTime.now().toString("yyyy-MM-dd");
        String subject = "小熊发货单状态 更新 " + date;

        TemplateData templateData = new TemplateData()
                .addAsDateTime("queryTime", DateTime.now())
                .add("overview", overview.build())
                .add("noUpdatesOverDays", shippingOrdersInTemplate(noUpdates, ImmutableList.of()));

        Map<Status, String> statusNameMap = ImmutableMap
                .<Status, String>builder()
                .put(INIT, "新建 待包货")
                .put(PACKED, "已包货 待发货")
                .put(EXTERNAL_SHIPPING_CREATED, "小熊已发货 待付款")
                .put(EXTERNAL_SHPPING_PENDING, "小熊处理")
                .put(CN_TRACKING_NUMBER_ASSIGNED, "到达国内 已知快递单号")
                .put(CN_POSTMAN_ASSIGNED, "最后一里 已知邮递员")
                .put(DELIVERED, "已送达")
                .build();
        List<Map<String, Object>> statusObjs = new ArrayList<>();
        for (Map.Entry<Status, Collection<ShippingOrder>> entry : byStatus.asMap().entrySet()) {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("name", statusNameMap.get(entry.getKey()));
            statusMap.put("dat", shippingOrdersInTemplate(entry.getValue(), report.customerNotifiedIds));
            statusObjs.add(statusMap);
        }
        templateData.add("statusMaps", statusObjs);

        String html = TemplatesFactory.instance()
                .getTemplate("jiaoni_shippingorders_summary_zh_cn.ftl")
                .toContent(templateData.build());

        if (StringUtils.isNotBlank(subject) && StringUtils.isNotBlank(html)) {
            emailClient.sendHtml(String.join(",", Environments.ADMIN_EMAILS), subject, html);
        }
    }

    private void notifyCustomer(final ShippingOrder shippingOrder) {
        if (StringUtils.isAnyBlank(
                shippingOrder.getReceiver().getPhone().getPhone(),
                shippingOrder.getTrackingNumber()
        )) {
            return;
        }

        String shippingCarrier = StringUtils.trimToEmpty(shippingOrder.getShippingCarrier());
        if (!getKnownShippingCarriers().contains(shippingCarrier)) {
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
                    shippingCarrier,
                    shippingOrder.getTrackingNumber(),
                    shippingOrder.getPostman().getName(),
                    shippingOrder.getPostman().getPhone().getPhone()
            };
        } else {
            templateId = JIAONI_SMS_TEMPLATE_ID_WITHOUT_POSTMAN;
            params = new String[]{
                    shippingCarrier,
                    shippingOrder.getTrackingNumber()
            };
        }

        smsClient.sendTextWithTemplate("cn",
                shippingOrder.getReceiver().getPhone().getPhone(),
                templateId,
                params);

        shippingOrderDbClient.put(shippingOrder.toBuilder().setCustomerNotified(true).build());
    }

    private static class Report {
        Multimap<Status, ShippingOrder> orderByStatus = ArrayListMultimap.create();
        List<String> customerNotifiedIds = new ArrayList<>();
    }
}
