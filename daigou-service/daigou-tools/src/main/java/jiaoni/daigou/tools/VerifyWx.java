package jiaoni.daigou.tools;

import com.google.common.util.concurrent.Uninterruptibles;
import jiaoni.common.appengine.access.ocr.GoogleVisionOcrClient;
import jiaoni.common.httpclient.BrowserClient;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.common.utils.Envs;
import jiaoni.daigou.contentparser.CnAddressParser;
import jiaoni.daigou.contentparser.CnCustomerContactParser;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxReply;
import jiaoni.daigou.lib.wx.WxSyncer;
import jiaoni.daigou.lib.wx.WxWebClient;
import jiaoni.daigou.lib.wx.WxWebClientImpl;
import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.LoginAnswer;
import jiaoni.daigou.lib.wx.model.LoginStatus;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;
import jiaoni.daigou.service.appengine.impls.parser.DbEnhancedCustomerParser;
import jiaoni.daigou.service.appengine.impls.parser.ParserFacade;
import jiaoni.daigou.wiremodel.api.ParseRequest;
import jiaoni.daigou.wiremodel.api.ParseResponse;
import jiaoni.daigou.wiremodel.api.ParsedObject;
import jiaoni.daigou.wiremodel.entity.Customer;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VerifyWx {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            CustomerDbClient customerDbClient = new CustomerDbClient(Env.PROD, remoteApi.getDatastoreService(), remoteApi.getMemcacheService());
            ParserFacade parserFacade = new ParserFacade(
                    new CnAddressParser(),
                    new DbEnhancedCustomerParser(new CnCustomerContactParser(), customerDbClient),
                    new GoogleVisionOcrClient()
            );

            //
            // Login
            //
            String qrFile = Envs.getLocalTmpDir() + "wxqr.png";
            WxWebClient client = new WxWebClientImpl(new BrowserClient());
            String uuid = client.fetchLoginUuid();
            try (FileOutputStream outputStream = new FileOutputStream(new File(qrFile))) {
                client.outputQrCode(uuid, outputStream);
            }
            Runtime.getRuntime().exec("open " + qrFile);

            Session session;
            while (true) {
                LoginAnswer loginAnswer = client.askLogin(uuid);
                if (LoginStatus.SUCCESS == loginAnswer.getStatus()) {
                    session = loginAnswer.getSession();
                    break;
                }
                System.out.println("waiting...");
                Thread.sleep(2000L);
            }

            System.out.println("login success");

            client.initialize(session);
            client.syncContacts(session);

            //
            // Start syncing
            //
            boolean startSync = true;
            if (startSync) {
                LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
                WxSyncer syncer = new WxSyncer(session, client, messages);
                new Thread(syncer).start();

                // Main thread wait
                while (true) {
                    try {

                        while (!messages.isEmpty()) {
                            Message message = messages.poll();
                            String fromUserName = message.getFromUserName();

                            Contact contact = null;
                            if (session.getMyself().getUserName().equals(fromUserName)) {
                                contact = session.getMyself();
                            }
                            if (contact == null) {
                                contact = session.getPersonalAccounts().get(message.getFromUserName());
                            }
                            if (contact == null) {
                                contact = session.getGroupChatAccounts().get(message.getFromUserName());
                            }
                            String senderName = contact == null ? "unknown" : contact.getNickName();

                            System.out.println("get message: from :" + senderName + ":\n" + message.getContent());

                            if (!senderName.contains("代购奔小康") && !senderName.equals("Tutu")) {
                                continue;
                            }

                            if (StringUtils.isNotBlank(message.getContent())) {
                                ParseResponse parseResponse = parserFacade.parse(ParseRequest.newBuilder()
                                        .addTexts(message.getContent())
                                        .setDomain(ParseRequest.Domain.CUSTOMER)
                                        .build());
                                System.out.println(ObjectMapperProvider.prettyToJson(parseResponse));
                                if (parseResponse.getResultsCount() > 0) {
                                    ParsedObject res = parseResponse.getResults(0);
                                    if (res.hasCustomer()) {
                                        Customer customer = res.getCustomer();
                                        if (customer.getAddressesCount() > 0 && StringUtils.isNoneBlank(
                                                customer.getName(),
                                                customer.getPhone().getPhone(),
                                                customer.getAddresses(0).getRegion(),
                                                customer.getAddresses(0).getCity(),
                                                customer.getAddresses(0).getZone(),
                                                customer.getAddresses(0).getAddress()
                                        )) {
                                            String key = customerDbClient.computeKey(customer.getPhone(), customer.getName());
//                                            if (customerDbClient.getById(key) == null) {
//                                                customerDbClient.put(customer);
//                                            }
                                            client.sendReply(session, WxReply.builder()
                                                    .text(message.getFromUserName(), "我发现了一个新的客户。我记录在案了." + ObjectMapperProvider.compactToJson(customer))
                                                    .build());
                                        }
                                    }
                                }

                            }
                        }
                    } catch (Exception e) {
                    }
                    Uninterruptibles.sleepUninterruptibly(2000L, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}


