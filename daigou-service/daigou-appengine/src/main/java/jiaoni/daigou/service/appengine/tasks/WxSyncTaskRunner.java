package jiaoni.daigou.service.appengine.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.Uninterruptibles;
import jiaoni.common.appengine.access.email.EmailClient;
import jiaoni.common.appengine.access.taskqueue.PubSubClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.appengine.registry.Registry;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.utils.Envs;
import jiaoni.common.utils.TimestampUtils;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxWebClient;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.SyncCheck;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.db.WxWebSessionDbClient;
import jiaoni.daigou.service.appengine.impls.wx.RichMessage;
import jiaoni.daigou.service.appengine.impls.wx.WxAggregateMessageHandler;
import jiaoni.daigou.service.appengine.impls.wx.WxMessageEnricher;
import jiaoni.daigou.service.appengine.utils.RegistryFactory;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaoni.common.appengine.access.taskqueue.PubSubClient.QueueName.DEV_QUEUE;
import static jiaoni.common.appengine.access.taskqueue.PubSubClient.QueueName.PROD_QUEUE;
import static jiaoni.daigou.service.appengine.utils.RegistryFactory.Keys.WxSyncTaskRunner_ALLOW_NEXT_TASK;
import static jiaoni.daigou.service.appengine.utils.RegistryFactory.Keys.WxSyncTaskRunner_RUN_FOREVER;

@Singleton
public class WxSyncTaskRunner implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WxSyncTaskRunner.class);

    private final WxWebClient wxClient;
    private final PubSubClient pubSubClient;
    private final EmailClient emailClient;
    private final WxWebSessionDbClient dbClient;
    private final Registry registry;
    private final WxAggregateMessageHandler handler;
    private final WxMessageEnricher messageEnricher;

    @Inject
    public WxSyncTaskRunner(final WxWebClient wxClient,
                            final PubSubClient pubSubClient,
                            final EmailClient emailClient,
                            final WxWebSessionDbClient dbClient,
                            final WxAggregateMessageHandler handler,
                            final WxMessageEnricher messageEnricher) {
        this.wxClient = wxClient;
        this.pubSubClient = pubSubClient;
        this.emailClient = emailClient;
        this.dbClient = dbClient;
        this.handler = handler;
        this.registry = RegistryFactory.get();
        this.messageEnricher = messageEnricher;

    }

    private Duration timeDiff(final DateTime startTime) {
        return Duration.millis(DateTime.now().getMillis() - startTime.getMillis());
    }

    private long decideSleepSec(Session session) {
        DateTime lastReplyTs = session.getLastReplyTimestamp();
        if (lastReplyTs == null || lastReplyTs.plusMinutes(5).isBeforeNow()) {
            return 10;
        } else if (lastReplyTs.plusMinutes(3).isBeforeNow()) {
            return 5;
        } else {
            return 2;
        }
    }

    /**
     * Send email when WX logged out.
     */
    private void sendEmailWxLoggedOut(final Session session, final TaskMessage task) {
        emailClient.sendText(Envs.getAdminEmails(),
                "WxSyncTaskRunner logged out",
                String.format("WxSyncTaskRunner reach logged out. Active minutes: %s. reachCount: %s",
                        timeDiff(session.getStartTimestamp()).getStandardMinutes(), task.getReachCount()));
    }

    /**
     * Send email for WX health check.
     */
    private WxSyncTicket sendEmailWxHealthCheck(final Session session, final WxSyncTicket ticket) {
        long curHealthCheckHour = TimestampUtils.diff(DateTime.now(), session.getStartTimestamp()).getStandardHours();
        if (curHealthCheckHour > ticket.lastHealthCheckedHour) {
            emailClient.sendText(Envs.getAdminEmails(),
                    "WxSyncTaskRunner active hours " + curHealthCheckHour,
                    String.format("WxSyncTaskRunner has active for %s hours. SessionId=%s. StartTime=%s.",
                            curHealthCheckHour, ticket.sessionId, session.getStartTimestamp()));
            return new WxSyncTicket(session.getSessionId(), curHealthCheckHour);
        }
        return ticket;
    }

    @Override
    public void accept(TaskMessage task) {
        WxSyncTicket ticket;
        try {
            ticket = ObjectMapperProvider.get().readValue(task.getPayload(), WxSyncTicket.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Session session = dbClient.getById(ticket.sessionId);
        if (session == null) {
            throw new RuntimeException("failed to find wx session by " + ticket.sessionId);
        }

        final DateTime startTime = DateTime.now();
        while (true) {
            try {
                SyncCheck syncCheck = wxClient.syncCheck(session);

                if (syncCheck.isLoggedOut()) {
                    LOGGER.warn("Wx logged out!");
                    sendEmailWxLoggedOut(session, task);
                    return;
                }

                if (syncCheck.needSync()) {
                    LOGGER.info("Need to sync");

                    // TODO:
                    // Currently we just handle message one by one
                    // Ideally, we should reply based on context

                    List<Message> messages = wxClient.sync(session);
                    for (Message message : messages) {
                        RichMessage richMessage = messageEnricher.enrich(session, message);
                        handler.handle(session, richMessage);
                        session.setLastReplyTimestampNow();
                    }
                }

            } catch (Exception e) {
                LOGGER.error("Wx ERROR!", e);
            }

            // TODO
            // Report status to WX periodically

            if (startTime.plusMinutes(9).isBeforeNow()) {
                LOGGER.info("WxSyncRunnable {} ready to end.");
                dbClient.put(session);

                // NOTE:
                // We should stop handle messages: stop call sync()
                // let next task runner run it.

                DateTime lastReplyTs = session.getLastReplyTimestamp();
                boolean scheduleNext = registry.getRegistryAsBoolean(AppEnvs.getServiceName(), WxSyncTaskRunner_RUN_FOREVER, false);
                scheduleNext |= (lastReplyTs == null || lastReplyTs.plusHours(2).isAfterNow())
                        && registry.getRegistryAsBoolean(AppEnvs.getServiceName(), WxSyncTaskRunner_ALLOW_NEXT_TASK, true);
                if (!scheduleNext) {
                    LOGGER.info("No activation after {}. Won't schedule next WxSyncRunnable.", session.getLastReplyTimestamp());
                }

                dbClient.put(session);

                LOGGER.info("Schedule next WxSyncTask");
                pubSubClient.submit(AppEnvs.isProd() ? PROD_QUEUE : DEV_QUEUE,
                        task.toBuilder().increaseReachCount().build()
                );
                return;
            }

            ticket = sendEmailWxHealthCheck(session, ticket);
            Uninterruptibles.sleepUninterruptibly(decideSleepSec(session), TimeUnit.SECONDS);
        }
    }

    public static class WxSyncTicket {
        @JsonProperty("sessionId")
        private String sessionId;

        @JsonProperty("lastHealthCheckedHour")
        private long lastHealthCheckedHour;

        public WxSyncTicket(final String sessionId,
                            final long lastHealthCheckedHour) {
            this.sessionId = sessionId;
            this.lastHealthCheckedHour = lastHealthCheckedHour;
        }

        // For Json Only
        private WxSyncTicket() {
        }
    }
}
