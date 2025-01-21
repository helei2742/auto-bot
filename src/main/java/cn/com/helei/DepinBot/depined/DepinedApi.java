package cn.com.helei.DepinBot.depined;

import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.pool.account.DepinClientAccount;
import cn.com.helei.DepinBot.core.pool.env.BrowserEnv;
import cn.com.helei.DepinBot.core.exception.RegisterException;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import cn.com.helei.DepinBot.core.util.RestApiClient;
import cn.com.helei.DepinBot.core.util.RestApiClientFactory;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DepinedApi {

//    private DConfi

    public CompletableFuture<Void> register(NetworkProxy proxy, AccountContext accountContext) {
        DepinClientAccount clientAccount = accountContext.getClientAccount();
        JSONObject body = new JSONObject();
        body.put("email", clientAccount.getEmail());
        body.put("password", clientAccount.getPassword());

        BrowserEnv browserEnv = accountContext.getBrowserEnv();


        HttpHeaders headers = new DefaultHttpHeaders();
        browserEnv.getHeaders().forEach((headers::set));
        headers.add("content-type", "application/json");
        headers.set("origin", "https://app.depined.org");
        headers.set("referer", "https://app.depined.org/");
        headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");


        return getRestApiClient(proxy)
                .request("https://api.depined.org/api/user/register", "POST", headers, null, body)
                .thenAcceptAsync(res -> {
                    // 注册获得token
                    JSONObject result = JSONObject.parseObject(res);

                    if (result.getBoolean("status")) {
                        JSONObject data = result.getJSONObject("data");
                        accountContext.setParam("token", data.getString("token"));
                        accountContext.setParam("user_id", data.getString("user_id"));

                    } else {
                        throw new RegisterException(String.format("账户[%s]注册失败，[%s]", accountContext.getName(), res));
                    }
                }).thenRunAsync(() -> {
                    //设置用户名
                    try {
                        setUsername(proxy, accountContext).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RegisterException(String.format("账户[%s]注册失败，[%s]", accountContext.getName(), e.getMessage()));
                    }
                }).thenRunAsync(()->{
                    //设置Rendering


                });

    }


    public CompletableFuture<Void> setUsername(NetworkProxy proxy, AccountContext accountContext) {
        String token = accountContext.getParam("token");
        String username = accountContext.getClientAccount().getEmail().split("@")[0];

        JSONObject body = new JSONObject();

        body.put("username", username);
        body.put("step", "username");

        BrowserEnv browserEnv = accountContext.getBrowserEnv();
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        browserEnv.getHeaders().forEach((headers::set));
        headers.set("content-type", "application/json");
        headers.set("origin", "https://app.depined.org");
        headers.set("referer", "https://app.depined.org/");
        headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        headers.set("authorization", "Bearer " + token);

        return getRestApiClient(proxy)
                .request("https://api.depined.org/api/user/profile-creation", "POST", headers, null, body)
                .thenAcceptAsync(res -> {
                    // 注册获得token
                    JSONObject result = JSONObject.parseObject(res);

                    if (!result.getBoolean("status")) {
                        throw new RegisterException(String.format("账户[%s]设置用户名失败，[%s]", accountContext.getName(), res));
                    }
                });
    }

    public CompletableFuture<Void> setRendering(NetworkProxy proxy, AccountContext accountContext) {
        String token = accountContext.getParam("token");
        JSONObject body = new JSONObject();

        body.put("step", "description");
        body.put("description", "Rendering");

        BrowserEnv browserEnv = accountContext.getBrowserEnv();
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        browserEnv.getHeaders().forEach((headers::set));
        headers.set("content-type", "application/json");
        headers.set("origin", "https://app.depined.org");
        headers.set("referer", "https://app.depined.org/");
        headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        headers.set("authorization", "Bearer " + token);

        return getRestApiClient(proxy)
                .request("https://api.depined.org/api/user/profile-creation", "POST", headers, null, body)
                .thenAcceptAsync(res -> {
                    // 注册获得token
                    JSONObject result = JSONObject.parseObject(res);

                    if (!result.getBoolean("status")) {
                        throw new RegisterException(String.format("账户[%s]设置Rendering失败，[%s]", accountContext.getName(), res));
                    }
                });
    }

    public CompletableFuture<Void> setReferral(NetworkProxy proxy, AccountContext accountContext) {
        String token = accountContext.getParam("token");
        JSONObject body = new JSONObject();

//        body.put("referral_code", );

        BrowserEnv browserEnv = accountContext.getBrowserEnv();
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        browserEnv.getHeaders().forEach((headers::set));
        headers.set("content-type", "application/json");
        headers.set("origin", "https://app.depined.org");
        headers.set("referer", "https://app.depined.org/");
        headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        headers.set("authorization", "Bearer " + token);

        return getRestApiClient(proxy)
                .request("https://api.depined.org/api/user/profile-creation", "POST", headers, null, body)
                .thenAcceptAsync(res -> {
                    // 注册获得token
                    JSONObject result = JSONObject.parseObject(res);

                    if (!result.getBoolean("status")) {
                        throw new RegisterException(String.format("账户[%s]设置Rendering失败，[%s]", accountContext.getName(), res));
                    }
                });
    }
    private RestApiClient getRestApiClient(NetworkProxy networkProxy) {
        return RestApiClientFactory.getClient(networkProxy);
    }

}
