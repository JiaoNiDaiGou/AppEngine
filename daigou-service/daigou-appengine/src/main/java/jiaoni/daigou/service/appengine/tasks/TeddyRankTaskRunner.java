package jiaoni.daigou.service.appengine.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import jiaoni.common.appengine.access.email.EmailClient;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.common.utils.CollectionUtils2;
import jiaoni.common.utils.Envs;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TeddyRankTaskRunner implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeddyRankTaskRunner.class);

    private static final int SENDER_RANK_LIMIT = 10;

    private final StorageClient storageClient;
    private final EmailClient emailClient;

    @Inject
    public TeddyRankTaskRunner(final StorageClient storageClient,
                               final EmailClient emailClient) {
        this.storageClient = storageClient;
        this.emailClient = emailClient;
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        LOGGER.info("Building Teddy shipping order ranks");

        List<ShippingOrder> shippingOrders = loadAllShippingOrders();

        DateTime now = DateTime.now();
        List<Map.Entry<String, Long>> senderRankLast30d = senderRank(shippingOrders, now.minusDays(30));
        List<Map.Entry<String, Long>> senderRankLast180d = senderRank(shippingOrders, now.minusDays(180));

        StringBuilder html = new StringBuilder();
        html.append("report time: ").append(now).append("\n");
        html.append("last 30d\n");
        for (Map.Entry<String, Long> entry : senderRankLast30d) {
            html.append("   ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        html.append("\nlast 180d\n");
        for (Map.Entry<String, Long> entry : senderRankLast30d) {
            html.append("   ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        for (String emailTo : Envs.getAdminEmails()) {
            emailClient.sendText(emailTo, "Teddy Sender Rank", html.toString());
        }
    }

    private List<ShippingOrder> loadAllShippingOrders() {
        List<String> paths = storageClient.listAll(AppEnvs.Dir.SHIPPING_ORDERS_DUMP);
        if (paths.isEmpty()) {
            return ImmutableList.of();
        }
        LOGGER.info("Ready to archive following dump files: {}", paths);

        List<ShippingOrder> toReturn = new ArrayList<>();
        for (String path : paths) {
            LOGGER.info("Load dump {}", path);
            byte[] bytes = storageClient.read(path);
            List<ShippingOrder> shippingOrdersInPage;
            try {
                shippingOrdersInPage = ObjectMapperProvider.get().readValue(bytes, new TypeReference<List<ShippingOrder>>() {
                });
            } catch (IOException e) {
                throw new InternalIOException(e);
            }
            toReturn.addAll(shippingOrdersInPage);
        }
        return toReturn;
    }

    private List<Map.Entry<String, Long>> senderRank(List<ShippingOrder> shippingOrders, DateTime timeline) {
        Map<String, Long> cnt = new HashMap<>();
        for (ShippingOrder shippingOrder : shippingOrders) {
            if (timeline.isBefore(shippingOrder.getCreationTime())) {
                CollectionUtils2.incCnt(cnt, shippingOrder.getSenderName());
            }
        }
        return CollectionUtils2.topDesc(cnt, SENDER_RANK_LIMIT);
    }
}
