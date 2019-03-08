package jiaoni.daigou.tools;

import com.google.common.util.concurrent.Uninterruptibles;
import jiaoni.common.appengine.access.ocr.GoogleVisionOcrClient;
import jiaoni.common.httpclient.BrowserClient;
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
import jiaoni.daigou.service.appengine.impls.customer.CustomerFacade;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;
import jiaoni.daigou.service.appengine.impls.parser.DbEnhancedCustomerParser;
import jiaoni.daigou.service.appengine.impls.parser.ParserFacade;
import jiaoni.daigou.service.appengine.impls.wx.RichMessage;
import jiaoni.daigou.service.appengine.impls.wx.WxMessageEnricher;
import jiaoni.daigou.wiremodel.api.ParseRequest;
import jiaoni.daigou.wiremodel.api.ParseResponse;
import jiaoni.daigou.wiremodel.entity.Customer;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//
// When speak to group, to contact is the group.
//

// unless its my speaking
// toContact is always me.
// from is group
// speaker is the speaker.

public class VerifyWx {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            CustomerDbClient customerDbClient = new CustomerDbClient(Env.PROD, remoteApi.getDatastoreService(), remoteApi.getMemcacheService());
            ParserFacade parserFacade = new ParserFacade(
                    new CnAddressParser(),
                    new DbEnhancedCustomerParser(new CnCustomerContactParser(), customerDbClient),
                    new GoogleVisionOcrClient()
            );
//            CustomerFacade customerFacade = new CustomerFacade(customerDbClient);

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

            WxMessageEnricher enricher = new WxMessageEnricher(client);

            //
            // Start syncing
            //
            boolean startSync = true;
            LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
            WxSyncer syncer = new WxSyncer(session, client, messages);
            new Thread(syncer).start();

            // Main thread wait
            while (true) {
                try {

                    while (!messages.isEmpty()) {
                        Message msg = messages.poll();
                        RichMessage message = enricher.enrich(session, msg);

                        String content = message.getTextContent();
                        if (StringUtils.isBlank(content)) {
                            continue;
                        }

                        if (message.isFromMyself() || !message.isGroupMessage()) {
                            continue;
                        }

                        Contact fromContact = message.getFromContact();
                        if (fromContact == null || !fromContact.getNickName().contains("代购奔小康")) {
                            continue;
                        }

                        ParseResponse parseResponse = parserFacade.parse(ParseRequest.newBuilder()
                                .setDomain(ParseRequest.Domain.CUSTOMER)
                                .addTexts(content)
                                .build());
                        if (parseResponse.getResultsCount() == 0 || !parseResponse.getResults(0).hasCustomer()) {
                            continue;
                        }
                        Customer customer = parseResponse.getResults(0).getCustomer();
                        if (customer.getAddressesCount() == 0 || StringUtils.isAnyBlank(
                                customer.getName(),
                                customer.getPhone().getPhone(),
                                customer.getAddresses(0).getRegion(),
                                customer.getAddresses(0).getCity(),
                                customer.getAddresses(0).getZone(),
                                customer.getAddresses(0).getAddress())) {
                            continue;
                        }

//                        customer = customerFacade.createOrUpdateCustomer(customer);
                        String reply = String.format("我发现了客户。name:%s, phone:%s, %s|%s|%s|%s",
                                customer.getName(),
                                customer.getPhone().getPhone(),
                                customer.getAddresses(0).getRegion(),
                                customer.getAddresses(0).getCity(),
                                customer.getAddresses(0).getZone(),
                                customer.getAddresses(0).getAddress());
                        System.out.println(reply);
                        client.sendReply(session, WxReply.builder()
                                .text(message.getOriginalMessage().getFromUserName(), reply)
                                .build());
                    }
                } catch (Exception e) {
                }
                Uninterruptibles.sleepUninterruptibly(2000L, TimeUnit.MILLISECONDS);
            }
        }
    }
}


