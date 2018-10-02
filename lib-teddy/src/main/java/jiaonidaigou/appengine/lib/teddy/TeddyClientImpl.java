package jiaonidaigou.appengine.lib.teddy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Uninterruptibles;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.common.utils.JsoupUtils;
import jiaonidaigou.appengine.common.utils.Secrets;
import jiaonidaigou.appengine.common.utils.StringUtils2;
import jiaonidaigou.appengine.lib.teddy.model.Admin;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.lib.teddy.model.OrderPreview;
import jiaonidaigou.appengine.lib.teddy.model.Product;
import jiaonidaigou.appengine.lib.teddy.model.Receiver;
import jiaonidaigou.appengine.lib.teddy.model.ShippingHistoryEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static jiaonidaigou.appengine.common.utils.JsoupUtils.getChildText;
import static jiaonidaigou.appengine.common.utils.JsoupUtils.getElementTextById;
import static jiaonidaigou.appengine.common.utils.StringUtils2.replaceNonCharTypesWith;

public class TeddyClientImpl implements TeddyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeddyClientImpl.class);

    private static final String BASE_URL = "http://rnbex.us";
    private static final String INDEX_URL = BASE_URL + "/index.aspx";
    private static final String MEMBER_URL = BASE_URL + "/Member/";
    private static final String MEMBER_RECEIVER_LIST_URL = MEMBER_URL + "ReceiverList.aspx";
    private static final String MEMBER_ORDER_ADD_URL = MEMBER_URL + "OrderAdd.aspx";
    private static final String MEMBER_ORDER_LIST_ALL_URL = MEMBER_URL + "OrderListAll.aspx";
    private static final String MEMBER_ORDER_VIEW_URL = MEMBER_URL + "OrderView.aspx";
    private static final String ORDER_SHIPPING_TRACK = "http://rnbex.us/select/";
    private static final Pattern POSTMAN_TRACK_ENTRY_PATTERN = Pattern.compile("^.*(1\\d{10}).*$");

    private final Admin admin;
    private final MockBrowserClient client;
    private final State curState;

    public TeddyClientImpl(final String adminUsername,
                           final MockBrowserClient client) {
        this(Secrets.of("teddy." + adminUsername + ".json").getAsJson(Admin.class), client);
    }

    TeddyClientImpl(final Admin admin,
                    final MockBrowserClient client) {
        this.client = checkNotNull(client);
        this.admin = admin;
        this.curState = new State();
    }

    @Override
    public List<Receiver> getReceivers(int pageNum) {
        Document page = autoLogin(() -> client
                .doGet()
                .url(MEMBER_RECEIVER_LIST_URL)
                .pathParam("page", pageNum)
                .request()
                .callToHtml());

        List<Receiver> toReturn = new ArrayList<>();

        Elements allTables = page.select("table.tableList");
        if (allTables.size() == 0) {
            return toReturn;
        }

        Element table = allTables.get(0).child(0);
        for (int i = 1; i < table.childNodeSize(); i++) { // Skip first which is the header.
            // E.g.
            //  <tr>
            //   <td>1</td>
            //   <td>韩智力</td>
            //   <td>15754885566</td>
            //   <td>内蒙古自治区-呼和浩特市-新城区</td>
            //   <td>海拉尔东路街道 巨华世纪城 和谐园</td>
            //   <td></td>
            //   <td></td>
            //   <td></td>
            //   <td><a href="ReceiverEdit.aspx?ID=22140">编辑</a></td>
            //   <td><a class="del" href="ReceiverDel.aspx?ID=22140">删除</a></td>
            //  </tr>
            Element trElement = table.child(i);

            String[] fullAddressParts = StringUtils.split(getChildText(trElement, 3), "-");

            String editStr = trElement.child(8).getElementsByTag("a").attr("href");
            String userId = StringUtils.substringAfterLast(editStr, "=");

            Receiver receiver = Receiver.builder()
                    .withUserId(userId)
                    .withName(getChildText(trElement, 1))
                    .withPhone(getChildText(trElement, 2))
                    .withAddressRegion(fullAddressParts[0])
                    .withAddressCity(fullAddressParts[1])
                    .withAddressZone(fullAddressParts[2])
                    .withAddress(getChildText(trElement, 4))
                    .build();

            toReturn.add(receiver);
        }

        LOGGER.info("Load {} receivers in page {}.", toReturn.size(), pageNum);
        return toReturn;
    }

    @Override
    public List<Receiver> getReceivers(final Range<Integer> pageRange) {
        checkNotNull(pageRange);
        checkArgument(pageRange.hasLowerBound() && pageRange.hasUpperBound());

        List<Receiver> toReturn = new ArrayList<>();
        for (int pageNum = pageRange.lowerEndpoint(); pageNum <= pageRange.upperEndpoint(); pageNum++) {
            toReturn.addAll(getReceivers(pageNum));
        }
        return toReturn;
    }

    @Override
    public Order getOrderDetails(final long orderId) {
        return getOrderDetails(orderId, true);
    }

    @Override
    public Order makeOrder(Receiver receiver, List<Product> products, double totalWeight) {
        checkNotNull(receiver);

        // Get VIEWSTATE for addOrder page.
        Document document = autoLogin(() -> client
                .doGet()
                .url(MEMBER_ORDER_ADD_URL)
                .request()
                .callToHtml());
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        String orderViewState = document.select("#__VIEWSTATE").val();

        String productsStr = products.stream()
                .map(p -> String.format("%s—%s—%s—%s——%s—0—@",
                        p.getName(),
                        p.getBrand(),
                        p.getQuantity(),
                        p.getUnitPriceInDollers(),
                        p.getCategory().getVal()))
                .reduce((a, b) -> a + b)
                .get();

        document = autoLogin(() -> client
                .doPost()
                .url(MEMBER_ORDER_ADD_URL)
                .formBodyParam("__VIEWSTATE", orderViewState)
                .formBodyParam("hidUserID", admin.getUserId())
                .formBodyParam("dropDepotID", "10") // 10 is 西雅图仓库, 14 is 波特兰仓库
                .formBodyParam("dropRoute", "35") // 35 is 默认线路
                .formBodyParam("hidZhuanYun", "快递")
                .formBodyParam("txtDeliverName", admin.getSenderName())
                .formBodyParam("txtDeliverMobilePhone", admin.getSenderPhone())
                .formBodyParam("txtDeliverAddress", admin.getSenderAddress())
                .formBodyParam("checkIsSaveDeliver", "on")
                .formBodyParam("txtReceiverName", receiver.getName())
                .formBodyParam("txtReceiverMobilePhone", receiver.getPhone()) // E.g. 15006235507
                .formBodyParam("hidReceiverMobilePhone", receiver.getPhone())
                .formBodyParam("txtSheng", receiver.getAddressRegion()) // E.g. 江苏省
                .formBodyParam("txtShi", receiver.getAddressCity()) // E.g. 苏州市
                .formBodyParam("txtQu", receiver.getAddressZone()) // E.g. 常熟市
                .formBodyParam("txtReceiverAddress", receiver.getAddress()) // E.g. 高新技术产业开发区 澎湖路8号 新凯盛
                .formBodyParam("dropReceiverIDCardType", "身份证")
                .formBodyParam("txtReceiverIDCardNum", "")
                .formBodyParam("hidReceiverID", receiver.getUserId())
                .formBodyParam("checkIsSaveReceiver", "on")
                .formBodyParam("hidTax", "")
                .formBodyParam("hidTr", productsStr) // E.g. mingcheng—pingpai—3—2——包包—0—@mingcheng2—pingpai2—4—3——小电器类—0—@
                .formBodyParam("txtWeight", String.valueOf(totalWeight))
                .formBodyParam("txtSafeFreeOver", "")
                .formBodyParam("txtNote", "")
                .formBodyParam("dropScore", "")
                .formBodyParam("btnTJ", "创建")
                .request()
                .callToHtml());

        // E.g.
        // <h2>Object moved to <a href="/Member/Out.aspx?ID=109371">here</a>.</h2>
        //
        // <h2>Object moved to <a href="/sessionOut.aspx">here</a>.</h2> that is login expired
        String link = document.select("h2 a").attr("href");

        Document outPage = autoLogin(() -> client
                .doGet()
                .url(BASE_URL + link)
                .request()
                .callToHtml());
        // Response:
        // <div class="showContent showContentInfo" style="padding: 60px 200px; font-size: 18px;">
        // 运单号：<b style="color: #8FC320;">RB100113911</b>创建成功！...
        Element outEle = outPage.selectFirst("div.showContent");
        String formattedId = replaceNonCharTypesWith(
                outEle.text(),
                new StringUtils2.CharType[]{
                        StringUtils2.CharType.DIGIT,
                        StringUtils2.CharType.A2Z,
                        StringUtils2.CharType.CHINESE },
                " ");
        formattedId = StringUtils.substringBetween(formattedId, "运单号", "创建成功").trim();
        long id = Long.parseLong(StringUtils.substringAfterLast(link, "="));
        LOGGER.info("Make order success. Order# Id={}, formattedId={}", id, formattedId);

        return Order.builder()
                .withId(id)
                .withFormattedId(formattedId)
                .withStatus(Order.Status.CREATED)
                .withReceiver(receiver)
                .withProducts(products)
                .build();
    }

    @Override
    public Map<Long, OrderPreview> getOrderPreviews(int pageNum) {
        checkArgument(pageNum >= 1);

        Document page = autoLogin(() -> client
                .doGet()
                .url(MEMBER_ORDER_LIST_ALL_URL)
                .pathParam("page", pageNum)
                .request()
                .callToHtml());

        Map<Long, OrderPreview> toReturn = new HashMap<>();
        Element table = page.selectFirst("table.tableList");
        if (table == null) {
            return toReturn;
        }
        Elements trList = table.getElementsByTag("tr");
        for (int i = 1; i < trList.size(); i++) { // First <tr> is header
            Element tr = trList.get(i);
            OrderPreview orderPreview = parseOrderPreviewElement(tr);
            toReturn.put(orderPreview.getId(), orderPreview);
        }

        LOGGER.info("Load {} order previews in page {}.", toReturn.size(), pageNum);
        return toReturn;
    }

    @Override
    public Map<Long, OrderPreview> getOrderPreviews(final Range<Integer> pageRange) {
        checkNotNull(pageRange);
        checkArgument(pageRange.hasLowerBound() && pageRange.hasUpperBound());

        Map<Long, OrderPreview> toReturn = new HashMap<>();
        for (int pageNum = pageRange.lowerEndpoint(); pageNum <= pageRange.upperEndpoint(); pageNum++) {
            toReturn.putAll(getOrderPreviews(pageNum));
        }
        return toReturn;
    }

    @Override
    public Order getOrderDetails(final long orderId,
                                 final boolean includeShippingInfo) {
        Document orderViewPage = autoLogin(() -> client
                .doGet()
                .url(MEMBER_ORDER_VIEW_URL)
                .pathParam("ID", orderId)
                .request()
                .callToHtml());

        String formattedIdFromPage = getElementTextById(orderViewPage, "lblCNum");
        if (StringUtils.isBlank(formattedIdFromPage)) {
            // No such order
            return null;
        }

        Element element = orderViewPage.selectFirst("table.detailTable");
        List<Element> trs = element.getElementsByTag("tr");
        StringBuilder productSummary = new StringBuilder();
        List<Product> products = new ArrayList<>();
        double totalPrice = 0d;
        for (int i = 1; i < trs.size(); i++) {
            Element tr = trs.get(i);
            Product.Category category = Product.Category.nameOf(getChildText(tr, 2));
            String brand = getChildText(tr, 5);
            String name = getChildText(tr, 3);
            String unit = getChildText(tr, 4);
            int quantity = Optional.ofNullable(Ints.tryParse(getChildText(tr, 6))).orElse(0);
            double unitPrice = Optional.ofNullable(Doubles.tryParse(getChildText(tr, 7))).orElse(0d);
            Product product = Product.builder()
                    .withCategory(category)
                    .withBrand(brand)
                    .withName(name)
                    .withQuantity(quantity)
                    .withUnitPriceInDollers((int) unitPrice)
                    .build();
            products.add(product);
            productSummary.append(String.format("%s %s [%s] * %s", brand, name, unit, quantity));
            totalPrice += unitPrice * quantity;
            if (i != trs.size() - 1) {
                productSummary.append(";");
            }
        }

        Double totalWeight = Doubles.tryParse(StringUtils.trimToEmpty(
                JsoupUtils.getElementTextById(orderViewPage, "lblWeight")));
        Element moneyEle = orderViewPage.selectFirst("b.money");
        Double shippingFee = null;
        if (moneyEle != null) {
            shippingFee = Doubles.tryParse(StringUtils.trimToEmpty(moneyEle.text()));
        }

        String idCardNumber = getElementTextById(orderViewPage, "lblReceiverIDCardNum");

        Order.Builder builder = Order.builder()
                .withId(orderId)
                .withFormattedId(formattedIdFromPage)
                .withSenderName(getElementTextById(orderViewPage, "lblDeliverName")) // senderPhone is lblDeliverMobilePhone
                .withReceiver(Receiver.builder()
                        .withName(getElementTextById(orderViewPage, "lblReceiverName"))
                        .withPhone(getElementTextById(orderViewPage, "lblReceiverMobilePhone"))
                        .withIdCardNumber(idCardNumber)
                        .build())
                .withIdCardUploaded(StringUtils.isNotBlank(idCardNumber))
                .withCreationTime(parseTimestamp(getElementTextById(orderViewPage, "lblCreatime")))
                .withProducts(products)
                .withProductSummary(productSummary.toString())
                .withPrice(totalPrice)
                .withTotalWeight(totalWeight)
                .withShippingFee(shippingFee);

        Order.Status status = shippingFee != null ? Order.Status.PENDING : Order.Status.CREATED;
        if (includeShippingInfo) {
            Order.Status anotherStatus = syncOrderShippingHistory(formattedIdFromPage, builder);
            status = anotherStatus != null ? anotherStatus : status;
        }

        return builder.withStatus(status)
                .build();
    }

    @Nullable
    private Order.Status syncOrderShippingHistory(final String formattedId, final Order.Builder builder) {
        Document page = autoLogin(() -> client
                .doGet()
                .url(ORDER_SHIPPING_TRACK)
                .pathParam("num", formattedId)
                .request()
                .callToHtml());

        Element table = page.selectFirst("table.yundanTable");
        if (table == null) {
            return null;
        }

        List<ShippingHistoryEntry> shippingHistory = new ArrayList<>();
        String rawShippingStatus = null;
        String trackingNumber = null;
        Elements trList = table.getElementsByTag("tr");

        // Line[0] is: 运单编号：RB100110251
        // Line[1] is table header: 处理时间	运单状态
        for (int i = 2; i < trList.size(); i++) {
            Elements tdList = trList.get(i).getElementsByTag("td");
            if (tdList.size() == 2) {
                DateTime timestamp = parseTimestamp(tdList.get(0).text());
                String status = tdList.get(1).text();
                shippingHistory.add(ShippingHistoryEntry.builder()
                        .withStatus(status)
                        .withTimestamp(timestamp)
                        .build());
            } else if (tdList.size() == 1) {
                String text = tdList.get(0).text();
                if (StringUtils.isNotBlank(text)) {
                    if (text.startsWith("转运至：")) {
                        text = text.substring("转运至：".length());
                    }
                    Pair<String, String> trackingNumberPair = parseTrackingNumber(text);
                    if (trackingNumberPair != null && rawShippingStatus == null && trackingNumber == null) {
                        rawShippingStatus = trackingNumberPair.getLeft();
                        trackingNumber = trackingNumberPair.getRight();
                    }
                }
            }
        }

        shippingHistory.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        PostManInfo postManInfo = null;
        if (!shippingHistory.isEmpty()) {
            if (StringUtils.isBlank(rawShippingStatus)) {
                rawShippingStatus = shippingHistory.get(0).getStatus();
            }

            for (ShippingHistoryEntry entry : shippingHistory) {
                postManInfo = parsePostman(entry.getStatus());
                if (postManInfo != null) {
                    break;
                }
            }
        }

        builder.withRawShippingStatus(rawShippingStatus)
                .withTrackingNumber(trackingNumber)
                .withPostmanName(postManInfo == null ? null : postManInfo.postmanName)
                .withPostmanPhone(postManInfo == null ? null : postManInfo.postmanPhone)
                .withDelivered(postManInfo != null && postManInfo.delivered)
                .withShippingHistory(shippingHistory);

        //
        // Determine order status
        //

        Order.Status status = null;
        if (StringUtils.isNotBlank(trackingNumber)) {
            Order.DeliveryEnding deliveryEnding = determineDeliveryEnding(shippingHistory);
            if (deliveryEnding != null) {
                status = Order.Status.DELIVERED;
                builder.withDeliveryEnding(deliveryEnding);
            } else if (postManInfo != null && StringUtils.isNotBlank(postManInfo.postmanPhone)) {
                status = Order.Status.POSTMAN_ASSIGNED;
            } else {
                status = Order.Status.TRACKING_NUMBER_ASSIGNED;
            }
        } else if ("运单创建成功".equals(rawShippingStatus)) {
            status = Order.Status.CREATED;
        }
        return status;
    }

    private static Order.DeliveryEnding determineDeliveryEnding(final List<ShippingHistoryEntry> entries) {
        // Check last 3 items.
        List<String> latestStatus = new ArrayList<>();
        for (int i = 0; i < 3 && i < entries.size(); i++) {
            latestStatus.add(entries.get(i).getStatus());
        }

        for (String status : latestStatus) {
            if (status.contains("自提点")) {
                return Order.DeliveryEnding.PICK_UP_BOX;
            } else if (status.contains("投递到") && status.contains("包裹柜")) {
                return Order.DeliveryEnding.PICK_UP_BOX;
            } else if (status.contains("已签收")) {
                if (status.contains("他人代收")
                        || status.contains("协议信箱")
                        || status.contains("物业")
                        || status.contains("收发室")
                        || status.contains("其他")) {
                    return Order.DeliveryEnding.OTHERS_SIGNED;
                } else if (status.contains("本人签收")) {
                    return Order.DeliveryEnding.SELF_SIGNED;
                } else {
                    LOGGER.warn("Cannot determine deliveryEnding for signed for {}", latestStatus);
                    return Order.DeliveryEnding.UNKNOWN_SIGNED;
                }
            }
        }
        for (String status : latestStatus) {
            if (status.contains("已妥投")) {
                LOGGER.warn("Cannot determine deliveryEnding for {}", latestStatus);
                return Order.DeliveryEnding.UNKNOWN;
            }
        }
        return null;
    }

    /**
     * Make callAsString with auto login.
     */
    private Document autoLogin(final Callable<Document> call) {
        Document document;
        try {
            document = call.call();
            if (loginExpired(document)) {
                login();
                document = call.call();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (loginExpired(document)) {
            throw new RuntimeException("AutoLogin failed for " + admin.getLoginUsername());
        }
        return document;
    }

    /**
     * Return pair [shippingStatus, trackingNumber]
     */
    @VisibleForTesting
    static Pair<String, String> parseTrackingNumber(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        str = StringUtils.replace(str, "（", "(");
        str = StringUtils.replace(str, "）", ")");
        if (str.matches("^.*\\((\\d+)\\)$")) {
            String trackingNumber = StringUtils.substringBetween(str, "(", ")");
            String shippingStatus = StringUtils.substringBefore(str, "(" + trackingNumber).trim();
            return Pair.of(shippingStatus, trackingNumber);
        } else {
            return Pair.of(str, null);
        }
    }

    @VisibleForTesting
    static PostManInfo parsePostman(final String str) {
        String status = replaceNonCharTypesWith(
                str,
                new StringUtils2.CharType[]{
                        StringUtils2.CharType.DIGIT,
                        StringUtils2.CharType.A2Z,
                        StringUtils2.CharType.CHINESE },
                " ");
        if (status.contains("投递员")) {
            Matcher matcher = POSTMAN_TRACK_ENTRY_PATTERN.matcher(status);
            if (matcher.find()) {
                String phone = matcher.group(1);
                String name = StringUtils.substringBefore(status, phone).trim();
                name = StringUtils.substringAfterLast(name, " ");

                // Only received personally is guaranteed.
                boolean delivered = status.contains("本人签收");
                return new PostManInfo(name, phone, delivered);
            }
        }
        return null;
    }

    /**
     * If the response doc indicating login session expired.
     */
    @VisibleForTesting
    static boolean loginExpired(final Document doc) {
        if (StringUtils.startsWith(doc.select("html body h2 a").attr("href"), "/sessionOut.aspx")) {
            return true;
        }
        if (doc.getElementById("CheckCode") == null) {
            return false;
        }
        Element imageCheck = doc.getElementById("ImageCheck");
        return imageCheck != null && "看不清，换一个".equals(imageCheck.attr("title"));
    }

    private static DateTime parseTimestamp(final String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        DateTimeFormatter timestampFormat;
        if (text.contains("-")) {
            timestampFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        } else {
            timestampFormat = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");
        }
        return DateTime.parse(text, timestampFormat);
    }

    private static OrderPreview parseOrderPreviewElement(final Element tr) {
        // E.g.
        // <tr>
        //   <td><input class="check" rel="108602" type="checkbox"></td>
        //   <td>1</td>
        //   <td>快递</td>
        //   <td><a href="OrderView.aspx?ID=108602">RB100108602</a></td>
        //   <td>西雅图仓库<br/>默认线路</td>
        //   <td>已发货<br/>2018-06-05<br/>15:30:03</td>
        //   <td class="money">3.5</td>
        //   <td class="money">14</td>
        //   <td>娇妮</td>
        //   <td>黄桂<br/><b style="color:#049945;font-weight:normal;">有身份证照片</b></td>
        //   <td class="TableTdDetail"><b></b>nature made 叶酸0*1;schiff维骨力200粒*2;nature made钙0*1;</td>
        //   <td>邮政平邮(9975123725790)</td>
        //   <td><a href="OrderView.aspx?ID=108602">详情</a></td>
        //   <td><a target="_blank" href="/select/?num=RB100108602">追踪</a></td>
        //   <td><a class="linkWin" wintitle="标签" winhref="/adminkdUser/User/OrderViewLabel.aspx?IDS=108602," target="_blank">标签</a></td>
        // </tr>
        String formattedId = getChildText(tr, 3, 0);
        long id = Long.parseLong(StringUtils.substringAfterLast(tr.child(3).child(0).attr("href"), "="));

        String rawStatus = null;
        DateTime lastUpdateTime = null;
        String[] orderStatusStrs = StringUtils.split(getChildText(tr, 5));
        if (orderStatusStrs != null && orderStatusStrs.length > 0) {
            rawStatus = orderStatusStrs[0];
            if (orderStatusStrs.length > 2) {
                lastUpdateTime = parseTimestamp(orderStatusStrs[1] + " " + orderStatusStrs[2]);
            }
        }

        String shippingStatusStr = getChildText(tr, 11);
        Pair<String, String> trackingNumberResult = Pair.of(null, null);
        if (StringUtils.isNotBlank(shippingStatusStr)) {
            trackingNumberResult = parseTrackingNumber(shippingStatusStr);
        }

        return OrderPreview.builder()
                .withId(id)
                .withFormattedId(formattedId)
                .withRawStatus(rawStatus)
                .withLastUpdatedTime(lastUpdateTime)
                .withPrice(Doubles.tryParse(getChildText(tr, 7)))
                .withReceiverName(StringUtils.split(getChildText(tr, 9))[0])
                .withIdCardUploaded("有身份证照片".equals(getChildText(tr, 9, 1)))
                .withProductSummary(getChildText(tr, 10))
                .withRawShippingStatus(trackingNumberResult.getLeft())
                .withTrackingNumber(trackingNumberResult.getRight())
                .build();
    }

    @VisibleForTesting
    void login() {
        LOGGER.info("~~~ LOGIN [{}] ~~~", admin.getLoginUsername());

        curState.loggedIn = false;

        // Access home page to get __VIEWSTATE
        Document page = client.doGet()
                .url(INDEX_URL)
                .request()
                .callToHtml();
        String viewState = page.select("#__VIEWSTATE").val();

        // Login
        client.doPost()
                .url(INDEX_URL)
                .formBodyParam("__VIEWSTATE", viewState)
                .formBodyParam("txtEmail", admin.getLoginUsername())
                .formBodyParam("txtPassword", admin.getLoginPassword())
                .formBodyParam("btnLogin", "登录")
                .request()
                .callToString();

        curState.loggedIn = true;
        client.saveCookies();
    }

    @VisibleForTesting
    boolean isLoggedIn() {
        return curState.loggedIn;
    }

    private static class State {
        boolean loggedIn;
    }

    private static class PostManInfo {
        private String postmanName;
        private String postmanPhone;
        private boolean delivered;

        PostManInfo(String name, String phone, boolean delivered) {
            this.postmanName = name;
            this.postmanPhone = phone;
            this.delivered = delivered;
        }
    }
}
