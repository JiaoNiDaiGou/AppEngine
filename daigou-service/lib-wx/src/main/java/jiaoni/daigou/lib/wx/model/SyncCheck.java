package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SyncCheck {
    /**
     * Normal: 0
     * Fail or Logged out: 1101.
     * Log message out: 1100:
     * Log message quit on phone: 1102
     */
    @JsonProperty("retcode")
    private int retcode;

    /**
     * Normal: 0
     * New message: 2
     * Find or delete friend: 4 通過時發現,刪除好友
     * Other guy pass friend verification: 6 刪除時發現和對方通過好友驗證
     * enter or leave chatroom: 7
     */
    @JsonProperty("selector")
    private int selector;

    @JsonProperty("timestamp")
    private DateTime timestamp;

    // For json use only
    public SyncCheck() {
    }

    private SyncCheck(int retcode, int selector) {
        this.retcode = retcode;
        this.selector = selector;
        this.timestamp = DateTime.now(DateTimeZone.UTC);
    }

    /**
     * Sample response:
     * window.synccheck={retcode:"xxx",selector:"xxx"}
     */
    public static SyncCheck responseOf(final String raw) {
        StringResponse stringResponse = StringResponse.responseOf(raw);
        String syncCheckStr = stringResponse.getRaw("window.synccheck");
        syncCheckStr = StringUtils.substringBetween(syncCheckStr, "{", "}");
        if (StringUtils.isBlank(syncCheckStr)) {
            return new SyncCheck(-1, -1);
        }

        int retcode = -1;
        int selector = -1;
        String[] parts = StringUtils.split(syncCheckStr, ",");
        for (String part : parts) {
            if (part.startsWith("retcode:")) {
                retcode = Integer.parseInt(part.substring("retcode:".length() + 1,
                        part.length() - 1));
            } else if (part.startsWith("selector:")) {
                selector = Integer.parseInt(part.substring("selector:".length() + 1,
                        part.length() - 1));
            }
        }
        return new SyncCheck(retcode, selector);
    }

    public int getRetcode() {
        return retcode;
    }

    public int getSelector() {
        return selector;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    @JsonIgnore
    public boolean needSync() {
        return retcode == 0 || selector != 0;
    }

    @JsonIgnore
    public boolean isLoggedOut() {
        return retcode >= 1100;
    }
}
