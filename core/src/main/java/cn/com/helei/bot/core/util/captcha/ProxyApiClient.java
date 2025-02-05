package cn.com.helei.bot.core.util.captcha;

import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.util.RestApiClient;
import cn.com.helei.bot.core.util.RestApiClientFactory;
import com.twocaptcha.ApiClient;
import com.twocaptcha.exceptions.ApiException;
import com.twocaptcha.exceptions.NetworkException;
import okhttp3.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class ProxyApiClient extends ApiClient {

    private final OkHttpClient proxyOkHttpClient;

    public ProxyApiClient(ProxyInfo proxy) {
        RestApiClient client = RestApiClientFactory.getClient(proxy);
        this.proxyOkHttpClient = client == null ? new OkHttpClient() : client.getOkHttpClient();
    }


    public String in(Map<String, String> params, Map<String, File> files) throws Exception {
        HttpUrl.Builder url = (new HttpUrl.Builder()).scheme("https").host(this.host).addPathSegment("in.php");
        Object body;
        if (files.size() == 0) {
            FormBody.Builder form = new FormBody.Builder();
            Objects.requireNonNull(form);
            params.forEach(form::add);
            body = form.build();
        } else {
            MultipartBody.Builder form = new MultipartBody.Builder();
            form.setType(MultipartBody.FORM);
            Objects.requireNonNull(form);
            params.forEach(form::addFormDataPart);
            Iterator var6 = files.entrySet().iterator();

            while(var6.hasNext()) {
                Map.Entry<String, File> entry = (Map.Entry)var6.next();
                byte[] fileBytes = Files.readAllBytes(((File)entry.getValue()).toPath());
                form.addFormDataPart((String)entry.getKey(), ((File)entry.getValue()).getName(), RequestBody.create(fileBytes));
            }

            body = form.build();
        }

        Request request = (new Request.Builder()).url(url.build()).post((RequestBody)body).build();
        return this.proxyExecute(request);
    }

    public String res(Map<String, String> params) throws Exception {
        HttpUrl.Builder url = (new HttpUrl.Builder()).scheme("https").host(this.host).addPathSegment("res.php");
        Objects.requireNonNull(url);
        params.forEach(url::addQueryParameter);
        Request request = (new Request.Builder()).url(url.build()).build();
        return this.proxyExecute(request);
    }


    private String proxyExecute(Request request) throws Exception {
        Response response = this.proxyOkHttpClient.newCall(request).execute();

        String var4;
        try {
            if (!response.isSuccessful()) {
                throw new NetworkException("Unexpected code " + response);
            }

            String body = response.body().string();
            if (body.startsWith("ERROR_")) {
                throw new ApiException(body);
            }

            var4 = body;
        } catch (Throwable var6) {
            if (response != null) {
                try {
                    response.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }

            throw var6;
        }

        if (response != null) {
            response.close();
        }

        return var4;
    }
}
