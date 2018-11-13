package jiaoni.daigou.lib.wx;

import jiaoni.daigou.lib.wx.model.LoginAnswer;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.SyncCheck;

import java.io.OutputStream;
import java.util.List;

public interface WxWebClient {

    /**
     * Fetches an UUID for login use.
     */
    String fetchLoginUuid();

    /**
     * Generates QR code to given output stream. Client is responsible to close the stream.
     */
    void outputQrCode(final String loginUuid, final OutputStream outputStream);

    /**
     * Login with given uuid.
     */
    LoginAnswer askLogin(final String loginUuid);

    /**
     * Initialize WX. Download basic information.
     */
    void initialize(final Session session);

    /**
     * Turn op WX status notification.
     */
    void statusNotify(final Session session);

    /**
     * Check if need to sync with WX.
     */
    SyncCheck syncCheck(final Session session);

    /**
     * Sync with WX.
     */
    List<Message> sync(final Session session);

    /**
     * Get image type message content.
     */
    byte[] getMessageImage(final Session session, final String messageId);

    /**
     * Get video type message content.
     */
    byte[] getMessageVideo(final Session session, final String messageId);

    /**
     * Get voice type message content.
     */
    byte[] getMessageVoice(final Session session, final String messageId);

    /**
     * Send WX reply.
     */
    void sendReply(final Session session, WxReply reply);

    /**
     * Get full contacts.
     */
    void syncContacts(final Session session);

    /**
     * Get group contacts.
     */
    public void batchGetContacts(final Session session, final List<String> groupChatUserNames);
}
