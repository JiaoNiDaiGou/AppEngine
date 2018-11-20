package jiaoni.daigou.lib.douyin;

import com.fasterxml.jackson.databind.JsonNode;
import jiaoni.common.httpclient.BrowserClient;

public class DouYinClient {
    private static final String BASE_URL = "https://aweme.snssdk.com/aweme/v1";
    private static final String FEED_URL = BASE_URL + "/feed/";

    private static String url = "https://aweme.snssdk.com/aweme/v1/feed/?iid=32142611788&" +
            "ac=4G&os_api=18&app_name=aweme&channel=App%20Store&idfa=67642C64-6404-403A-8B0D-31A059C3A2BD&device_platform=iphone&" +
            "build_number=17909&vid=9D61EDED-6680-471A-A134-D1C96399BB83" +
            "&openudid=9a661cd28951ab44f0870508f7af64dfb9b5dc36&device_type=iPhone8,2&app_version=1.7.9&device_id=50862505508&" +
            "version_code=1.7.9&os_version=10.2.1&screen_width=1125&aid=1128&count=6&feed_style=0&max_cursor=0&min_cursor=0&" +
            "pull_type=0&type=0&user_id=96840867747&volume=0.00&mas=000171d64eb699219ac45f410bcc83d1accd3ee629e6ec51f8ceb1&" +
            "as=a1859114c0ed4b50731900&ts=1531121872";

    //https://aweme.snssdk.com/v1/feed/??pull_type=0&app_version=1.7.9&iid=32142611788&channel=App+Store&device_type=iPhone8%2C2&type=0&openudid=9a661cd28951ab44f0870508f7af64dfb9b5dc36&vid=9D61EDED-6680-471A-A134-D1C96399BB83&os_api=18&max_cursor=0&mas=000171d64eb699219ac45f410bcc83d1accd3ee629e6ec51f8ceb1&feed_style=0&ac=4&screen_width=1125&device_id=50862505508&idfa=67642C64-6404-403A-8B0D-31A059C3A2BD&os_version=10.2.1&version_code=1.7.9&count=6&volume=0.00&app_name=aweme&as=a1859114c0ed4b50731900&user_id=96840867747&device_platform=iphone&build_number=17909&aid=1128&min_cursor=0&ts=1542603929490

    private final BrowserClient client;

    public DouYinClient(final BrowserClient client) {
        this.client = client;
    }

    public JsonNode feed() {
        return client.doGet()
                .url(FEED_URL)
                .header("Accept", "*/*")
                .header("User-Agent", "Aweme/1.7.9 (iPhone; iOS 10.2.1; Scale/3.00)")
                .pathParam("iid", "32142611788")
                .pathParam("ac", 4)
                .pathParam("os_api", 18)
                .pathParam("app_name", "aweme")
                .pathParam("channel", "App Store")
                .pathParam("idfa", "67642C64-6404-403A-8B0D-31A059C3A2BD")
                .pathParam("device_platform", "iphone")
                .pathParam("build_number", 17909)
                .pathParam("vid", "9D61EDED-6680-471A-A134-D1C96399BB83")
                .pathParam("openudid", "9a661cd28951ab44f0870508f7af64dfb9b5dc36")
                .pathParam("device_type", "iPhone8,2")
                .pathParam("app_version", "1.7.9")
                .pathParam("device_id", "50862505508")
                .pathParam("version_code", "1.7.9")
                .pathParam("os_version", "10.2.1")
                .pathParam("screen_width", 1125)
                .pathParam("aid", 1128)
                .pathParam("count", 6)
                .pathParam("feed_style", 0)
                .pathParam("max_cursor", 0)
                .pathParam("min_cursor", 0)
                .pathParam("pull_type", 0)
                .pathParam("type", 0)
                .pathParam("user_id", "96840867747")
                .pathParam("volume", "0.00")
                .pathParam("mas", "000171d64eb699219ac45f410bcc83d1accd3ee629e6ec51f8ceb1")
                .pathParam("as", "a1859114c0ed4b50731900")
                .pathParam("ts", System.currentTimeMillis())
                .request()
                .callToJsonNode();
    }

    public static void main(String[] args) throws Exception {
        DouYinClient client = new DouYinClient(new BrowserClient());
        JsonNode jsonNode = client.feed();
        System.out.println(jsonNode);
    }
}
