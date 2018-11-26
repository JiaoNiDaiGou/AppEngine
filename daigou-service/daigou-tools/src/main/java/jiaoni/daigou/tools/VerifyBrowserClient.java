package jiaoni.daigou.tools;

import jiaoni.common.httpclient.BrowserClient;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie2;

public class VerifyBrowserClient {
    public static void main(String[] args) {
        BrowserClient client = new BrowserClient();

        Cookie cookie = new BasicClientCookie2("ASP.NET_SessionId", "oohs03itmpkwtv0z1ywcjhs3");


        String string = client.doGet()
                .url("https://www.google.com")
                .addCookie(cookie)
                .request()
                .callToString();
        System.out.println(string);
    }
}
