package jiaoni.daigou.lib.wx;

import com.google.common.base.Ticker;
import jiaoni.daigou.lib.wx.model.SyncKey;
import org.joda.time.DateTime;

import java.security.SecureRandom;
import java.util.Random;

class WxUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a random 15 digit as DeviceId.
     */
    static String generateRandomDeviceId() {
        return "e" + String.valueOf(SECURE_RANDOM.nextLong()).substring(1, 16);
    }

    // 時間戳左移4位隨後補上4位隨機數
    static String generateClientMsgId() {
        String toReturn = String.valueOf(System.currentTimeMillis());
        return toReturn.substring(0, toReturn.length() - 4)
                + String.format("%04d", (new Random()).nextInt(10000));
    }

    static String nowMillisToString() {
        return String.valueOf(DateTime.now().getMillis());
    }

    static String nowMillisNegateToString() {
        return String.valueOf((int) ~System.currentTimeMillis());
    }

    static String syncKeyToString(final SyncKey key) {
        return key.getList().stream()
                .map(t -> t.getKey() + "_" + t.getVal())
                .reduce((a, b) -> a + "|" + b)
                .orElse(null);
    }

    private static long nanosToMillis(long nano) {
        return nano / 1000000L;
    }
}
