package jiaonidaigou.appengine.api.tasks;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.access.email.EmailClient;
import jiaonidaigou.appengine.api.guice.JiaoNiDaiGou;
import jiaonidaigou.appengine.common.location.CnCity;
import jiaonidaigou.appengine.common.location.CnLocations;
import jiaonidaigou.appengine.common.location.CnRegion;
import jiaonidaigou.appengine.common.location.CnZone;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.model.Receiver;
import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOn;

@Singleton
public class SyncJiaoniCustomersTaskRunner implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncJiaoniCustomersTaskRunner.class);

    private static final int MAX_PAGES_TO_LOAD_FROM_TEDDY = 10;

    static class Message {
    }

    private final CustomerDbClient customerDbClient;
    private final TeddyClient teddyClient;
    private final EmailClient emailClient;

    @Inject
    public SyncJiaoniCustomersTaskRunner(@JiaoNiDaiGou final CustomerDbClient customerDbClient,
                                         @Named(TeddyAdmins.JIAONI) final TeddyClient teddyClient,
                                         EmailClient emailClient) {
        this.customerDbClient = customerDbClient;
        this.teddyClient = teddyClient;
        this.emailClient = emailClient;
    }

    @Override
    public void accept(final TaskMessage taskMessage) {
        meterOn();

        LOGGER.info("task message: {}", taskMessage);

        // Pair[name, phone]
        Map<Pair<String, String>, Customer> existingCustomers = new HashMap<>();
        customerDbClient.scan().forEach(t ->
                existingCustomers.put(Pair.of(t.getName(), t.getPhone().getPhone()), t));

        // A list of customer to update to DB.
        List<Customer> toUpdate = new ArrayList<>();

        // Log to email
        StringBuilder logger = new StringBuilder()
                .append(DateTime.now().toString())
                .append("\n")
                .append("Sync receivers:")
                .append("\n");
        for (int i = 1; i <= MAX_PAGES_TO_LOAD_FROM_TEDDY; i++) {
            List<Receiver> receivers = teddyClient.getReceivers(i);
            // No receivers in this page, stop fetching more.
            if (receivers.isEmpty()) {
                break;
            }

            // All receivers in this page are the already synced, stop fetching more.
            List<Customer> toUpdateCurPage = getCustomersToUpdate(logger, existingCustomers, receivers);
            if (toUpdateCurPage.isEmpty()) {
                break;
            }

            toUpdate.addAll(toUpdateCurPage);
        }

        if (!toUpdate.isEmpty()) {
            LOGGER.info("Update {} customers into DB.", toUpdate.size());
            customerDbClient.put(toUpdate);
        }

        logger.append("\n")
                .append("totally sync " + toUpdate.size() + " new customers.");

        for (String adminEmail : Environments.ADMIN_EMAILS) {
            emailClient.sendText(adminEmail, "SyncReceiver report", logger.toString());
        }
    }

    /**
     * Get customers to update by checking existing customers.
     */
    private static List<Customer> getCustomersToUpdate(final StringBuilder logger,
                                                       final Map<Pair<String, String>, Customer> existingCustomers,
                                                       final List<Receiver> newReceivers) {
        List<Customer> toReturn = new ArrayList<>();

        for (Receiver receiver : newReceivers) {
            Customer toUpdate = getCustomerToUpdate(logger, existingCustomers, receiver);
            if (toUpdate != null) {
                toReturn.add(toUpdate);
            }
        }

        return toReturn;
    }

    private static Customer getCustomerToUpdate(final StringBuilder logger,
                                                final Map<Pair<String, String>, Customer> existingCustomers,
                                                final Receiver receiver) {
        String name = receiver.getName();
        String phone = receiver.getPhone();
        String userId = receiver.getUserId();
        Pair<String, String> key = Pair.of(name, phone);

        Customer existingCustomer = existingCustomers.get(key);
        Customer.Builder builder = existingCustomer == null ? newCustomer(userId, name) : existingCustomer.toBuilder();

        addIdCard(builder, receiver.getIdCardNumber());

        if (!addPhone(builder, phone)) {
            logger.append("Invalid phone. ").append(receiver).append("\n");
            return null;
        }

        if (!addAddress(builder,
                receiver.getAddressRegion(),
                receiver.getAddressCity(),
                receiver.getAddressZone(),
                receiver.getAddress())) {
            logger.append("Invalid address. ").append(receiver).append("\n");
            return null;
        }

        Customer newCustomer = builder.build();
        if (newCustomer.equals(existingCustomer)) {
            return null; // no need to change.
        }
        existingCustomers.put(key, newCustomer);
        return newCustomer;
    }

    private static boolean addPhone(Customer.Builder builder, String phone) {
        if (builder.hasPhone()) {
            return true;
        }
        boolean validPhone = phone != null && phone.startsWith("1") && phone.length() == 11;
        if (!validPhone) {
            return false;
        }
        builder.setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone(phone).build());
        builder.setId(CustomerDbClient.computeKey(builder.getPhone(), builder.getName()));
        return true;
    }

    private static boolean addAddress(Customer.Builder builder, String region, String city, String zone, String addr) {
        List<CnRegion> cnRegions = CnLocations.getInstance().searchRegion(region);
        if (cnRegions.size() != 1) {
            return false;
        }
        CnRegion cnRegion = cnRegions.get(0);

        List<CnCity> cnCities = CnLocations.getInstance().searchCity(cnRegion, city);
        if (cnCities.size() != 1) {
            return false;
        }
        CnCity cnCity = cnCities.get(0);

        for (CnZone knownZone : cnCity.getZones()) {
            if (knownZone.getAllPossibleNames().contains(zone)) {
                zone = knownZone.getName();
                break;
            }
        }

        Address newAddress = Address.newBuilder()
                .setRegion(cnRegion.getName())
                .setCity(cnCity.getName())
                .setZone(zone)
                .setAddress(addr)
                .build();

        if (!builder.getAddressesList().contains(newAddress)) {
            builder.addAddresses(newAddress);
        }
        return true;
    }

    private static void addIdCard(Customer.Builder builder, String idCard) {
        if (StringUtils.isNotBlank(idCard) && StringUtils.isBlank(idCard)) {
            builder.setIdCard(idCard);
        }
    }

    private static Customer.Builder newCustomer(String userId, String name) {
        return Customer.newBuilder()
                .setSocialContacts(Customer.SocialContacts.newBuilder()
                        .setTeddyUserId(userId))
                .setName(name);
    }
}
