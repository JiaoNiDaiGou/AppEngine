package jiaoni.daigou.tools;

import com.google.common.util.concurrent.Uninterruptibles;
import jiaoni.common.httpclient.BrowserClient;
import jiaoni.common.utils.Envs;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxClient;
import jiaoni.daigou.lib.wx.WxClientImpl;
import jiaoni.daigou.lib.wx.WxSyncer;
import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.LoginAnswer;
import jiaoni.daigou.lib.wx.model.LoginStatus;
import jiaoni.daigou.lib.wx.model.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VerifyWx {
    public static void main(String[] args) throws Exception {

        //
        // Login
        //
        String qrFile = Envs.getLocalTmpDir() + "wxqr.png";
        WxClient client = new WxClientImpl(new BrowserClient());
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
                }

                Uninterruptibles.sleepUninterruptibly(1000L, TimeUnit.MILLISECONDS);
            }
        }
    }
}


