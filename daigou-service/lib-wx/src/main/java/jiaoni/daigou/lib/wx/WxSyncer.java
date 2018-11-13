package jiaoni.daigou.lib.wx;

import com.google.common.util.concurrent.Uninterruptibles;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.SyncCheck;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class WxSyncer implements Runnable {
    private final Session session;
    private final WxWebClient client;
    private final BlockingQueue<Message> messages;
    private int count = 0;
    private volatile boolean keepLive;

    public WxSyncer(final Session session,
                    final WxWebClient client,
                    final BlockingQueue<Message> messages) {
        this.session = session;
        this.client = client;
        this.messages = messages;
        keepLive = true;
    }

    public void stop() {
        keepLive = false;
    }

    @Override
    public void run() {
        while (keepLive) {
            System.out.println(String.format("run[%s] syncTaks", count));

            SyncCheck syncCheck = client.syncCheck(session);
            if (syncCheck.isLoggedOut()) {
                System.out.println("LOGGED OUT!");
                return;
            }

            if (syncCheck.needSync()) {
                client.sync(session).forEach(messages::offer);
            }

            count++;
            Uninterruptibles.sleepUninterruptibly(2000L, TimeUnit.MILLISECONDS);
        }
    }
}
