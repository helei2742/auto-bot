package cn.com.helei.bot.core.util.llm;

import cn.com.helei.bot.core.util.RestApiClientFactory;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QwenLLMAgent {

    private static final String BASE_URL = "";

    private final String apiKey;

    public QwenLLMAgent(String apiKey) {
        this.apiKey = apiKey;
    }

    public CompletableFuture<OpenAIPrompt> request(OpenAIPrompt prompt) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);

        return RestApiClientFactory
                .getClient(null)
                .request(
                        BASE_URL,
                        HttpMethod.POST,
                        headers,
                        null,
                        prompt.build()
                )
                .thenApplyAsync(response -> {
                    JSONObject result = JSONObject.parseObject(response);
                    String answer = result.getJSONArray("choices").getJSONObject(0).getString("content");
                    prompt.addAnswer(answer);
                    return prompt;
                });
    }

}
