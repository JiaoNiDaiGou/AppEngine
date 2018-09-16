package jiaonidaigou.appengine.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.location.CnCity;
import jiaonidaigou.appengine.common.location.CnLocations;
import jiaonidaigou.appengine.common.location.CnRegion;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;
import jiaonidaigou.appengine.lib.teddy.model.Receiver;
import jiaonidaigou.appengine.tools.remote.RemoteApi;
import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public class SyncReceivers {
    public static void main(String[] args) throws Exception {
        File file = new File(Environments.LOCAL_TEMP_DIR_ENDSLASH + "my_receivers.json");

//        Multimap<String, Receiver> receivers = loadReceiversFromTeddy();
//        ObjectMapperProvider.get().writerWithDefaultPrettyPrinter().writeValue(file, receivers);

        Multimap<String, Receiver> receivers = loadReceiversFromFile(file);
        System.out.println(receivers.size());

        List<Customer> customers = new ArrayList<>();
        for (Map.Entry<String, Collection<Receiver>> entry : receivers.asMap().entrySet()) {
            customers.addAll(convertToCustomer(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        System.out.println("process " + customers.size());

//        writeToDb(customers);
    }

    private static void writeToDb(List<Customer> customers) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {

            CustomerDbClient customerDbClient = new CustomerDbClient(
                    remoteApi.getDatastoreService(),
                    Environments.SERVICE_NAME_JIAONIDAIGOU);

            customerDbClient.put(customers);
        }
    }

    private static Multimap<String, Receiver> loadReceiversFromFile(File file) throws Exception {
        return ObjectMapperProvider.get().readValue(file, new TypeReference<Multimap<String, Receiver>>() {
        });
    }

    private static Multimap<String, Receiver> loadReceiversFromTeddy() {
        TeddyClient teddyClient = new TeddyClientImpl("jiaoni", new MockBrowserClient("test"));
        Multimap<String, Receiver> toReturn = ArrayListMultimap.create();
        for (int i = 1; i <= 50; i++) {
            System.out.println("loading page " + i);
            List<Receiver> receiversInThisPage = teddyClient.getReceivers(i);
            if (receiversInThisPage.isEmpty()) {
                break;
            }
            for (Receiver receiver : receiversInThisPage) {
                toReturn.put(receiver.getPhone(), receiver);
            }
        }
        return toReturn;
    }

    private static List<Customer> convertToCustomer(final String phone, List<Receiver> receivers) {
        List<Customer> toReturn = new ArrayList<>();

        if (receivers.size() == 1) {
            Receiver receiver = receivers.get(0);
            Customer.Builder builder = buildPartial(receiver.getUserId(), receiver.getName(), receiver.getIdCardNumber());
            if (!addPhone(builder, phone)) {
                System.err.println("invalid phone: " + receiver);
                return toReturn;
            }
            if (!addAddress(builder, receiver.getAddressRegion(), receiver.getAddressCity(), receiver.getAddressZone(), receiver.getAddress())) {
                System.err.println("invalid address: " + receiver);
                return toReturn;
            }
            toReturn.add(builder.build());
        } else {
            boolean nameSame = receivers.stream().map(Receiver::getName).distinct().count() == 1;
            if (nameSame) {
                Receiver receiver = receivers.get(0);
                Customer.Builder builder = buildPartial(
                        receiver.getUserId(),
                        receiver.getName(),
                        receivers.stream().map(Receiver::getIdCardNumber).filter(StringUtils::isNotBlank).findFirst().orElse(null));
                if (!addPhone(builder, phone)) {
                    System.err.println("invalid phone: " + receiver);
                    return toReturn;
                }
                for (Receiver re : receivers) {
                    Customer.Builder newBuilder = builder.build().toBuilder();
                    if (!addAddress(newBuilder, re.getAddressRegion(), re.getAddressCity(), re.getAddressZone(), re.getAddress())) {
                        System.err.println("invalid address: " + re);
                        return toReturn;
                    }
                    toReturn.add(newBuilder.build());
                }
            } else {
                for (Receiver receiver : receivers) {
                    Customer.Builder builder = buildPartial(
                            receiver.getUserId(),
                            receiver.getName(),
                            receivers.stream().map(Receiver::getIdCardNumber).filter(StringUtils::isNotBlank).findFirst().orElse(null));
                    if (!addPhone(builder, phone)) {
                        System.err.println("invalid phone: " + receiver);
                        return toReturn;
                    }
                    if (!addAddress(builder, receiver.getAddressRegion(), receiver.getAddressCity(), receiver.getAddressZone(), receiver.getAddress())) {
                        System.err.println("invalid address: " + receiver);
                        return toReturn;
                    }
                    toReturn.add(builder.build());
                }
            }
        }

        return toReturn;
    }

    private static boolean addPhone(Customer.Builder builder, String phone) {
        boolean validPhone = phone != null && phone.startsWith("1") && phone.length() == 11;
        if (!validPhone) {
            return false;
        }
        builder.setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone(phone).build());
        builder.setId(CustomerDbClient.computeKey(builder.getPhone(), builder.getName()));
        return true;
    }

    private static boolean addAddress(Customer.Builder builder, String region, String city, String zone, String addr) {
        String message = String.format("region=%s, city=%s zone=%s addr=%s", region, city, zone, addr);
        List<CnRegion> cnRegions = CnLocations.getInstance().searchRegion(region);
        checkState(cnRegions.size() == 1, message);
        CnRegion cnRegion = cnRegions.get(0);

        List<CnCity> cnCities = CnLocations.getInstance().searchCity(cnRegion, city);
        checkState(cnCities.size() == 1, message + ": " + cnCities);
        CnCity cnCity = cnCities.get(0);

        builder.addAddresses(Address.newBuilder()
                .setRegion(cnRegion.getName())
                .setCity(cnCity.getName())
                .setZone(zone)
                .setAddress(addr)
                .build());
        return true;
    }


    private static Customer.Builder buildPartial(String userId, String name, String idCard) {
        Customer.Builder builder = Customer.newBuilder()
                .setSocialContacts(Customer.SocialContacts.newBuilder()
                        .setTeddyUserId(userId)
                        .build())
                .setName(name);
        if (StringUtils.isNotBlank(idCard)) {
            builder.setIdCard(idCard);
        }
        return builder;
    }
}
