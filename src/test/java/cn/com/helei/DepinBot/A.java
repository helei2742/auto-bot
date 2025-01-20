package cn.com.helei.DepinBot;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


OkHttpClient client = new OkHttpClient();

Request request = new Request.Builder()
    .url("https://testnet.humanity.org/api/claim/dailyRewards")
    .post()
    .header("accept", "application/json, text/plain, */*")
    .header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8")
    .header("authorization", "")
    .header("cache-control", "no-cache")
    .header("content-length", "0")
    .header("cookie", "QueueITAccepted-SDFrts345E-V3_humanityprotocol=EventId%3Dhumanityprotocol%26QueueId%3Dbb276c2a-ca20-4ee4-ad3a-963d140a4f5b%26RedirectType%3Dsafetynet%26IssueTime%3D1737243043%26Hash%3Df3f51d3ce1a141afbc9dee9e7eaf649e0b9c07fd552e7270458935ecea0a2337")
    .header("origin", "https://testnet.humanity.org")
    .header("pragma", "no-cache")
    .header("priority", "u=1, i")
    .header("referer", "https://testnet.humanity.org/dashboard")
    .header("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
    .header("sec-ch-ua-mobile", "?0")
    .header("sec-ch-ua-platform", "\"macOS\"")
    .header("sec-fetch-dest", "empty")
    .header("sec-fetch-mode", "cors")
    .header("sec-fetch-site", "same-origin")

    .header("token", "AjBOmW2JBjgDyPz35D7/YciYN7KErx60xzerWuXAHOvIvRWOghrnXa7NEzmOHtMqpe0ZFUUS8uD/5B3yEz8Hui4xXfRHwjoOqnmmcY4uWQpGBuvMSZIA7UjHKqL0AxW1y/3zIbx3D4R/hN2BGdphYYOyE5pHQOlha09QToNqVtDwuZsuxuzO5cAV8HrOQnz0oNWapw9sBZis/Ya/hc8niPBeIfKx7YxqFPevHQn72kd6mQ2JLpRUGPITdAiWEsBr")
    .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
    .build();

try (Response response = client.newCall(request).execute()) {
    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
    response.body().string();
}
"AjBOmW2JBjgDyPz35D7/YciYN7KErx60xzerWuXAHOvIvRWOghrnXa7NEzmOHtMq52kcDWKbQdz5+SC4XFE6StjbjxKE4dN+ePfrvZvJB1jvlvnZRf28oFH+bbGahVyfGGI7cUHKnhuWhlYEXEOxycIHwrs0YEyfT9qislN83oRkFLWk0krE3hRIVWn+wtN2VxGrxPPby4YdTFok1VIi2j9u+X6J+AevjZTUJOpbZVM0IJ0qzN/mSCDrBCyxbGMa"
"AjBOmW2JBjgDyPz35D7/YciYN7KErx60xzerWuXAHOvIvRWOghrnXa7NEzmOHtMqpe0ZFUUS8uD/5B3yEz8Hui4xXfRHwjoOqnmmcY4uWQpGBuvMSZIA7UjHKqL0AxW1y/3zIbx3D4R/hN2BGdphYYOyE5pHQOlha09QToNqVtDwuZsuxuzO5cAV8HrOQnz0oNWapw9sBZis/Ya/hc8niPBeIfKx7YxqFPevHQn72kd6mQ2JLpRUGPITdAiWEsBr"
