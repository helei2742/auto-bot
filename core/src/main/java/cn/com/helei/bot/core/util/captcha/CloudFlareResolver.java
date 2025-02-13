package cn.com.helei.bot.core.util.captcha;

import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.util.RestApiClientFactory;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CloudFlareResolver {

    private static final String CREATE_TASK_URL = "https://api.2captcha.com/createTask";

    private static final String GET_RESULT_URL = "https://api.2captcha.com/getTaskResult";

    private static final int GET_RESULT_TIMES = 10;

    private static final int GET_RESULT_INTERVAL_SECONDS = 3;

    public static CompletableFuture<JSONObject> cloudFlareResolve(
            ProxyInfo proxy,
            String websiteUrl,
            String websiteKey,
            String twoCaptchaApiKey
    ) {
        JSONObject body = new JSONObject();

        body.put("clientKey", twoCaptchaApiKey);
        JSONObject task = getTaskInfo(proxy, websiteUrl, websiteKey);
        body.put("task", task);

        log.info("url[{}] start CloudFlare resolve，create task id...", websiteUrl);
        return RestApiClientFactory.getClient(proxy).request(
                CREATE_TASK_URL,
                HttpMethod.POST,
                null,
                null,
                body
        ).thenApplyAsync(resultStr -> {
            JSONObject result = JSONObject.parseObject(resultStr);

            String taskId = result.getString("taskId");
            if (StrUtil.isNotBlank(taskId)) {
                try {
                    return getTaskSolution(proxy, twoCaptchaApiKey, taskId);
                } catch (Exception e) {
                    throw new RuntimeException(websiteUrl + " CloudFlare resolve error", e);
                }
            }

            throw new RuntimeException(websiteUrl + " CloudFlare resolve error, task create error," + resultStr);
        });
    }


    private static JSONObject getTaskSolution(ProxyInfo proxy, String apiKey, String taskId) throws CaptchaResolveException {
        for (int i = 0; i < GET_RESULT_TIMES; i++) {
            JSONObject body = new JSONObject();
            body.put("taskId", taskId);
            body.put("clientKey", apiKey);

            try {
                String resultStr = RestApiClientFactory.getClient(proxy).request(
                        GET_RESULT_URL,
                        HttpMethod.POST,
                        null,
                        null,
                        body
                ).get();

                JSONObject result = JSONObject.parseObject(resultStr);

                Integer errorId = result.getInteger("errorId");

                boolean isBreak = true;
                if (errorId == 0) {
                    String status = result.getString("status");

                    if ("processing".equals(status)) {
                        log.warn("tart[{}] in processing, [{}/{}] ", taskId, i + 1, GET_RESULT_TIMES);
                        isBreak = false;
                    } else if ("ready".equals(status)) {
                        log.info("tart[{}] is ready", taskId);
                        return result.getJSONObject("solution");
                    }
                } else if (errorId == 12) {
                    log.error("Workers could not solve the Captcha");
                    throw new CaptchaResolveException("Workers could not solve the Captcha");
                }

                if (isBreak) {
                    String message = String.format("tart[%s] receive unknown response, %s",
                            taskId, resultStr);
                    log.error(message);
                    throw new CaptchaResolveException(message);
                }

                TimeUnit.SECONDS.sleep(GET_RESULT_INTERVAL_SECONDS);
            } catch (Exception e) {
                throw new CaptchaResolveException("task " + taskId + " resolve occur an error",e);
            }
        }

        throw new CaptchaResolveException("task " + taskId + " result request times out of limit " + GET_RESULT_TIMES);
    }

    @NotNull
    private static JSONObject getTaskInfo(
            ProxyInfo proxy,
            String websiteUrl,
            String websiteKey
    ) {
        JSONObject task = new JSONObject();
        task.put("type", "TurnstileTask");
        task.put("websiteURL", websiteUrl);
        task.put("websiteKey", websiteKey);

        if (proxy != null) {
            task.put("proxyType", proxy.getHost().toLowerCase());
            task.put("proxyAddress", proxy.getHost());
            task.put("proxyPort", String.valueOf(proxy.getPort()));
            task.put("proxyLogin", proxy.getUsername());
            task.put("proxyPassword", proxy.getPassword());
        }
        return task;
    }
}
