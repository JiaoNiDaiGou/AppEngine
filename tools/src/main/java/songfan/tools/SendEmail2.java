package songfan.tools;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import jiaonidaigou.appengine.api.access.email.LocalGmailSender;
import jiaonidaigou.appengine.api.access.gcp.GoogleApisClientFactory;
import jiaonidaigou.appengine.api.access.sheets.SheetsUtils;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.contenttemplate.TemplateData;
import jiaonidaigou.appengine.contenttemplate.Templates;
import jiaonidaigou.appengine.contenttemplate.TemplatesFactory;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.Delivery;
import jiaonidaigou.appengine.wiremodel.entity.Order;
import jiaonidaigou.appengine.wiremodel.entity.OrderEntry;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import jiaonidaigou.appengine.wiremodel.entity.Price;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class SendEmail2 {
    // CONFIG THIS
    //
    // If run local, using furuijie@gmail.com
    private static final String SHEET_PATH =
            "https://docs.google.com/spreadsheets/d/1y31CkS2JvLQahTpUq-CEtOlv3h7nXG2uXxE7tVKoQOg/edit#gid=611704717";
    private static final boolean SEND_TO_FU = true;

    //
    //
    //
    private static final String FU_EMAIL = "furuijie@gmail.com";
    //    private static final String[] CHEN_EMAILS = { "hsiting@yahoo.com" };
//    private static final String[] CHEN_EMAILS = { "furuijie@gmail.com" };

    private static final String EMAIL_SUBJECT = "感謝訂購 楊媽媽家常菜";
    private static final String ADMIN_EMAIL_SUBJECT_TEMPLATE = "楊媽媽家常菜 %s 总览";

    public static void main(String[] args) throws Exception {
        Sheets client = GoogleApisClientFactory.sheets();

        String spreadsheetId = SheetsUtils.extractSpreadsheetId(SHEET_PATH);
        int sheetId = SheetsUtils.extractSheetId(SHEET_PATH);
        System.out.println("SpreadsheetID: " + spreadsheetId + ", SheetID: " + sheetId);

        Spreadsheet spreadsheet = client.spreadsheets()
                .get(spreadsheetId)
                .execute();
        checkNotNull(spreadsheet);
        Sheet sheet = spreadsheet.getSheets()
                .stream()
                .filter(t -> t.getProperties().getSheetId() == sheetId)
                .findFirst()
                .orElse(null);
        checkNotNull(sheet);

        Schema schema = parseSchema(client, spreadsheetId, sheet);
        System.out.println("Parsing order schema finished");

        List<Order> orders = parseOrders(client, spreadsheetId, sheet, schema);

        System.out.println("\n\n");
        for (Order order : orders) {
            System.out.println(String.format("%s : %s : %s : %s :  %s",
                    order.getCustomer().getName(),
                    order.getCustomer().getEmails(0),
                    order.getDelivery().getDeliveryTimeRaw(),
                    order.getDelivery().getDeliveryAddressRaw(),
                    order.getEntiresList().stream().map(t -> t.getProduct().getName() + "[" + t.getQuantity() + "]").reduce((a, b) -> a + ", " + b).get()));
        }

        LocalGmailSender sender = new LocalGmailSender("songfan.rfu@gmail.com", "yangmama");

        if (SEND_TO_FU) {
            // Let's send to FU randomly
            Collections.shuffle(orders);
            orders = orders.subList(0, 1);
        }

        sendOrderConfirmations(spreadsheet, sheetId, orders, sender, !SEND_TO_FU);
    }

    private static Schema parseSchema(final Sheets client,
                                      final String spreadsheetId,
                                      final Sheet sheet)
            throws Exception {
        Schema schema = new Schema();

        schema.maxRowCount = sheet.getProperties().getGridProperties().getRowCount();
        schema.maxColCount = sheet.getProperties().getGridProperties().getColumnCount();

        // Get first row as title.
        String range = SheetsUtils.rowRange(sheet, 1);
        System.out.println("Header range : " + range);

        List<String> headers = SheetsUtils.toStringMatrix(
                client.spreadsheets()
                        .values()
                        .get(spreadsheetId, range)
                        .execute())
                .get(0);
        System.out.println("Find " + headers.size() + " headers:");
        headers.forEach(t -> System.out.println("\t" + t));

        for (int i = 0; i < headers.size(); i++) {
            String raw = StringUtils.trimToEmpty(headers.get(i));
            if (StringUtils.isNotBlank(raw)) {
                if (raw.equals("時間戳記")) {
                    System.out.println("Parse " + raw + " as Timestamp");
                    schema.datetimeIndex = i;
                } else if (raw.contains("電子郵件")) {
                    System.out.println("Parse " + raw + " as email.");
                    schema.emailIndex = i;
                } else if (raw.contains("聯絡名稱")) {
                    System.out.println("Parse " + raw + " as name.");
                    schema.nameIndex = i;
                } else if (raw.contains("電話")) {
                    System.out.println("Parse " + raw + "as phone.");
                    schema.phoneIndex = i;
                } else if ((raw.contains("取貨") && raw.contains("時間")) || (raw.equals("取貨時間"))) {
                    System.out.println("Parse " + raw + " as DeliveryTime.");
                    schema.deliveryTimeIndex = i;
                } else if (raw.contains("取貨") && raw.contains("地點")) {
                    System.out.println("Parse " + raw + " as DeliveryLocation.");
                    schema.deliveryLocationIndex.put(i, raw);
                } else if (!raw.equals("$") && raw.contains("$")) { // '$' may used as total price.
                    String productPriceStr = parsePrice(raw);
                    double productPrice = Double.parseDouble(productPriceStr);
                    productPriceStr = "$" + productPriceStr;
                    String productName = (StringUtils.substringBeforeLast(raw, productPriceStr).trim()
                            + " "
                            + StringUtils.substringAfterLast(raw, productPriceStr).trim()).trim();

                    Product product = Product.newBuilder()
                            .setName(productName)
                            .setSuggestedUnitPrice(Price.newBuilder().setValue(productPrice))
                            .build();
                    System.out.println("Parse (" + i + ") '" + raw + "' as Product:\n  name:[" + productName + "]\n  price:$[ " + productPrice + " ]");
                    schema.productsIndex.put(i, product);
                }
            }
        }

        checkArgument(schema.maxRowCount > 0, "The spreadsheet has no row");
        checkArgument(schema.datetimeIndex >= 0, "Timestamp not found");
        checkArgument(schema.emailIndex >= 0, "Customer email not found");
        checkArgument(schema.nameIndex >= 0, "Customer name not found");
        checkArgument(schema.phoneIndex >= 0, "Customer phone not found");
        checkArgument(schema.deliveryTimeIndex >= 0, "Delivery time not found");
        checkArgument(schema.deliveryLocationIndex.size() > 0, "Delivery location not found.");
        checkArgument(schema.productsIndex.size() > 0, "Products not found");
        return schema;
    }

    private static String parsePrice(String str) {
        StringBuilder text = new StringBuilder();
        boolean add = false;
        for (char c : str.toCharArray()) {
            if (c == '$') {
                add = true;
            } else if (c == ' ') {
            } else if ((c >= '0' && c <= '9') || c == '.') {
                if (add) {
                    text.append(c);
                }
            } else {
                add = false;
            }
        }
        return text.toString();
    }

    private static <T> List<T> appendFullList(final List<T> list, final int fullSize) {
        if (list.size() >= fullSize) {
            return list;
        }
        List<T> toReturn = new ArrayList<>(list);
        for (int i = 0; i < fullSize - list.size(); i++) {
            toReturn.add(null);
        }
        return toReturn;
    }

    private static List<Order> parseOrders(final Sheets client,
                                           final String spreadsheetId,
                                           final Sheet sheet,
                                           final Schema schema)
            throws Exception {
        String range = SheetsUtils.rowRange(sheet, 2, schema.maxRowCount);
        System.out.println("Value range:" + range);

        List<List<String>> data = SheetsUtils.toStringMatrix(
                client.spreadsheets()
                        .values()
                        .get(spreadsheetId, range)
                        .execute());

        List<Order> orders = new ArrayList<>();

        for (List<String> r : data) {
            List<String> row = appendFullList(r, schema.maxColCount);

            // Creation timestamp.
            DateTime creationTime = parseCreationTime(row.get(schema.datetimeIndex));
            if (creationTime == null) {
                continue;
            }

            // Customer name
            String customerName = row.get(schema.nameIndex);

            // Customer email
            String customerEmail = row.get(schema.emailIndex);
            if (SEND_TO_FU) {
                customerEmail = FU_EMAIL;
            }

            // Customer phone
            String customerPhone = row.get(schema.phoneIndex);

            // Delivery time raw
            String deliveryTimeRaw = row.get(schema.deliveryTimeIndex);

            // Delivery location raw
            String deliveryAddressRaw = null;
            for (Map.Entry<Integer, String> entry : schema.deliveryLocationIndex.entrySet()) {
                deliveryAddressRaw = row.get(entry.getKey());
                if (StringUtils.isNotBlank(deliveryAddressRaw)) {
                    break;
                }
            }

            // Orders
            List<OrderEntry> orderEntries = new ArrayList<>();
            for (Map.Entry<Integer, Product> entry : schema.productsIndex.entrySet()) {
                String quantityStr = row.get(entry.getKey());
                Product product = entry.getValue();
                int quantity = StringUtils.isBlank(quantityStr) ? 0 : Integer.parseInt(quantityStr);
                if (quantity > 0) {
                    orderEntries.add(OrderEntry.newBuilder()
                            .setProduct(Product.newBuilder()
                                    .setName(product.getName()))
                            .setUnitSellingPrice(product.getSuggestedUnitPrice())
                            .setQuantity(quantity)
                            .build());
                }
            }
            if (orderEntries.isEmpty()) {
                continue;
            }

            // Validation
            checkState(StringUtils.isNotBlank(customerName), "customer name is null");
            checkState(StringUtils.isNotBlank(customerEmail), "email is null");
            checkState(StringUtils.isNotBlank(deliveryAddressRaw), "delivery address is null.");
            checkState(StringUtils.isNotBlank(deliveryTimeRaw), "delivery time is null.");

            Customer customer = Customer.newBuilder()
                    .setName(customerName)
                    .setPhone(PhoneNumber.newBuilder().setCountryCode("1").setPhone(customerPhone))
                    .addEmails(customerEmail)
                    .build();

            Order order = Order.newBuilder()
                    .setDelivery(Delivery.newBuilder()
                            .setDeliveryAddressRaw(deliveryAddressRaw)
                            .setDeliveryTimeRaw(deliveryTimeRaw))
                    .setCustomer(customer)
                    .addAllEntires(orderEntries)
                    .build();

            orders.add(order);
        }

        return orders;
    }

    private static DateTime parseCreationTime(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        str = str.trim();
        String[] parts = str.split(" ");
        if (parts.length != 3) {
            return null;
        }
        String[] dateParts = StringUtils.split(parts[0], "/");
        if (dateParts.length != 3) {
            return null;
        }
        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int day = Integer.parseInt(dateParts[2]);
        boolean pm = parts[1].equals("下午");
        String[] timeParts = StringUtils.split(parts[2], ":");
        int hour = Integer.parseInt(timeParts[0]);
        if (pm && hour != 12) {
            hour += 12;
        }
        int min = Integer.parseInt(timeParts[1]);
        int sec = Integer.parseInt(timeParts[2]);
        return new DateTime().withDate(year, month, day).withTime(hour, min, sec, 0);
    }

    private static void sendOrderConfirmations(final Spreadsheet spreadsheet,
                                               final int sheetId,
                                               final List<Order> orders,
                                               final LocalGmailSender sender,
                                               final boolean track)
            throws Exception {
        File localTrackFile = new File(Environments.LOCAL_TEMP_DIR_ENDSLASH
                + "send_email_2_" + spreadsheet.getSpreadsheetId() + "_" + sheetId + ".log");
        Set<String> emailSent;
        if (!track || !localTrackFile.exists()) {
            emailSent = new HashSet<>();
        } else {
            try (Reader reader = new InputStreamReader(new FileInputStream(localTrackFile), Charsets.UTF_8)) {
                emailSent = new HashSet<>(CharStreams.readLines(reader));
            }
        }

        Map<Order, Map<String, Object>> emailProps = new HashMap<>();
        for (Order order : orders) {
            emailProps.put(order, buildOrderConfirmationProps(order));
        }

        try {
            Templates templates = TemplatesFactory.instance().getTemplate("order_confirmation_body.zh_tw.ftl");
            for (int i = 0; i < orders.size(); i++) {
                Order order = orders.get(i);
                String email = order.getCustomer().getEmails(0);

                if (StringUtils.isBlank(email)) {
                    System.err.println(order.getCustomer().getName() + " has no email!");
                    continue;
                }

                if (emailSent.contains(email)) {
                    System.out.println(String.format("[%s/%s]Already sent to %s", i + 1, orders.size(), email));
                    continue;
                }

                System.out.println(String.format("[%s/%s]Send to %s", i + 1, orders.size(), email));
                String html = templates.toContent(emailProps.get(order));

                if (StringUtils.isNotBlank(html)) {
                    sender.sendHtml(email, EMAIL_SUBJECT, html);
                    emailSent.add(email);
                    Thread.sleep(2000L);
                }
            }

            if (track) {
                templates = TemplatesFactory.instance().getTemplate("order_group_list_body.zh_tw.ftl");
                Multimap<Pair<String, String>, Map<String, Object>> orderPropsByDelivery = ArrayListMultimap.create();
                for (Order order : orders) {
                    Pair<String, String> key = Pair.of(
                            order.getDelivery().getDeliveryAddressRaw(),
                            order.getDelivery().getDeliveryTimeRaw());
                    orderPropsByDelivery.put(key, buildOrderConfirmationProps(order));
                }

                List<Map<String, Object>> allOrderProps = new ArrayList<>();
                for (Map.Entry<Pair<String, String>, Collection<Map<String, Object>>> entry : orderPropsByDelivery.asMap().entrySet()) {
                    List<Map<String, Object>> ordersByThisDelivery = new ArrayList<>(entry.getValue());
                    Map<String, Object> props = new TemplateData()
                            .add("rawAddress", entry.getKey().getLeft())
                            .add("rawTime", entry.getKey().getRight())
                            .add("orders", ordersByThisDelivery)
                            .build();
                    allOrderProps.add(props);
                }
                Map<String, Object> allProps = new TemplateData()
                        .add("orderGroupTag", spreadsheet.getProperties().getTitle())
                        .add("orderGroupByDelivery", allOrderProps)
                        .build();

//                for (String email : CHEN_EMAILS) {
//                    if (!emailSent.contains("admin::" + email)) {
//                    String html = templates.toContent(allProps);
//                    sender.sendHtml(
//                            email,
//                            String.format(ADMIN_EMAIL_SUBJECT_TEMPLATE, spreadsheet.getProperties().getTitle()),
//                            html);
//                    emailSent.add("admin::" + email);
//                    System.out.println("Send summary to admin " + email);
//                    }
//                }
            }

        } finally {
            if (track) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(localTrackFile), Charsets.UTF_8))) {
                    for (String email : emailSent) {
                        writer.write(email + "\n");
                    }
                }
            }
        }
    }

    private static Map<String, Object> buildOrderConfirmationProps(final Order order) {
        List<Map<String, Object>> orders = order.getEntiresList()
                .stream()
                .map(t -> new TemplateData()
                        .add("name", t.getProduct().getName())
                        .add("quantity", t.getQuantity())
                        .add("unitPrice", t.getUnitSellingPrice().getValue())
                        .build())
                .collect(Collectors.toList());

        double totalPrice = order.getEntiresList()
                .stream()
                .map(t -> t.getQuantity() * t.getUnitSellingPrice().getValue())
                .reduce((a, b) -> a + b)
                .orElse(0d);

        return new TemplateData()
                .add("customerName", order.getCustomer().getName())
                .add("customerPhone", order.getCustomer().getPhone().getPhone())
                .add("orders", orders)
                .add("totalPrice", totalPrice)
                .add("deliveryAddress", order.getDelivery().getDeliveryAddressRaw())
                .add("deliveryTime", order.getDelivery().getDeliveryTimeRaw())
                .build();
    }

    private static class Schema {
        int datetimeIndex = -1;
        int emailIndex = -1;
        int nameIndex = -1;
        int phoneIndex = -1;
        int deliveryTimeIndex = -1;
        Map<Integer, String> deliveryLocationIndex = new HashMap<>();
        Map<Integer, Product> productsIndex = new HashMap<>();
        int maxRowCount = -1;
        int maxColCount = -1;
    }
}
