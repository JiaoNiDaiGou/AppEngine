package jiaoni.daigou.lib.wx;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.SyncKey;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store current session credentials.
 */
public class Session {
    @JsonProperty
    private String sessionId;

    @JsonProperty
    private DateTime startTimestamp;

    @JsonProperty
    private String webUrl;

    @JsonProperty
    private String webpushUrl;

    @JsonProperty
    private String deviceId;

    @JsonProperty
    private String skey;

    @JsonProperty
    private String wxsid;

    @JsonProperty
    private String wxuin;

    @JsonProperty
    private String passTicket;

    @JsonProperty
    private SyncKey syncKey;

    @JsonProperty
    private SyncKey syncCheckKey;

    @JsonProperty
    private Contact myself;

    @JsonProperty
    private Map<String, Contact> personalAccounts = new ConcurrentHashMap<>();

    @JsonProperty
    private Map<String, Contact> groupChatAccounts = new ConcurrentHashMap<>();

    @JsonProperty
    private boolean loggedIn;

    @JsonProperty
    private DateTime lastSyncCheckTimestamp;

    @JsonProperty
    private DateTime lastReplyTimestamp;

    // For JSON
    private Session() {
    }

    Session(final String sessionId, final DateTime startTimestamp) {
        this.sessionId = sessionId;
        this.startTimestamp = startTimestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public DateTime getStartTimestamp() {
        return startTimestamp;
    }

    String getSkey() {
        return skey;
    }

    synchronized void setSkey(String skey) {
        this.skey = skey;
    }

    String getWxsid() {
        return wxsid;
    }

    synchronized void setWxsid(String wxsid) {
        this.wxsid = wxsid;
    }

    String getWxuin() {
        return wxuin;
    }

    synchronized void setWxuin(String wxuin) {
        this.wxuin = wxuin;
    }

    String getPassTicket() {
        return passTicket;
    }

    synchronized void setPassTicket(String passTicket) {
        this.passTicket = passTicket;
    }

    String getDeviceId() {
        return deviceId;
    }

    synchronized void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    String getWebUrl() {
        return webUrl;
    }

    synchronized void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    SyncKey getSyncKey() {
        return syncKey;
    }

    synchronized void setSyncKey(SyncKey syncKey) {
        this.syncKey = syncKey;
    }

    Contact getMyself() {
        return myself;
    }

    synchronized void setMyself(Contact myself) {
        this.myself = myself;
    }

    boolean isLoggedIn() {
        return loggedIn;
    }

    synchronized void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    String getWebpushUrl() {
        return webpushUrl;
    }

    synchronized void setWebpushUrl(String webpushUrl) {
        this.webpushUrl = webpushUrl;
    }

    SyncKey getSyncCheckKey() {
        return syncCheckKey;
    }

    synchronized void setSyncCheckKey(SyncKey syncCheckKey) {
        this.syncCheckKey = syncCheckKey;
    }

    DateTime getLastSyncCheckTimestamp() {
        return lastSyncCheckTimestamp;
    }

    synchronized void setLastSyncCheckTimestamp(DateTime lastSyncCheckTimestamp) {
        this.lastSyncCheckTimestamp = lastSyncCheckTimestamp;
    }

    synchronized void updateContacts(final List<Contact> contacts) {
        if (CollectionUtils.isEmpty(contacts)) {
            return;
        }
        for (Contact contact : contacts) {
            switch (contact.getType()) {
                case PERSONAL_ACCOUNT:
                    this.personalAccounts.put(contact.getUserName(), contact);
                    break;
                case GROUP_CHAT_ACCOUNT:
                    this.groupChatAccounts.put(contact.getUserName(), contact);
                    break;
                default:
                    break;
            }
        }
    }

    public Map<String, Contact> getPersonalAccounts() {
        return Collections.unmodifiableMap(personalAccounts);
    }

    public Map<String, Contact> getGroupChatAccounts() {
        return Collections.unmodifiableMap(groupChatAccounts);
    }

    public DateTime getLastReplyTimestamp() {
        return lastReplyTimestamp;
    }

    @JsonIgnore
    public synchronized void setLastReplyTimestampNow() {
        this.lastReplyTimestamp = DateTime.now();
    }
}
