package cn.com.helei.bot.app.kaleido_finance;

import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.util.llm.OpenAIPrompt;
import cn.com.helei.bot.core.util.llm.QwenLLMAgent;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class KaLeiDoFinanceBot extends AnnoDriveAutoBot<KaLeiDoFinanceBot> {

    private static final String REGISTER_API = "https://kaleidofinance.xyz/api/testnet/register";

    private static final String QUESTION_GENERATE_PROMPT = """
            帮我生成[%s]个简单的问题, 有一下几点要求：
            1.问题不能够重复, 需保证多样化。
            2.问题需涵盖各个领域，包括数学、常识、自然科学、历史、文化等。
            3.输出以JSON格式输出, 如： ["question1", "question2", "question3"...]。
            强调, 我需要[%s]个问题
            """;

    public KaLeiDoFinanceBot(AutoBotConfig autoBotConfig, BotApi botApi) {
        super(autoBotConfig, botApi);

        OpenAIPrompt prompt = OpenAIPrompt
                .builder()
                .system(String.format(QUESTION_GENERATE_PROMPT, 24, 24))
                .model("text-davinci-002")
                .topP(0.7)
                .temperature(1)
                .build();

        CompletableFuture<List<String>> questionFuture = new QwenLLMAgent(autoBotConfig.getConfig("openai.api.key"))
                .request(prompt)
                .thenApplyAsync(response -> {

                    String lastAnswer = response.getLastAnswer();
                    if (StrUtil.isNotBlank(lastAnswer)) {
                        return JSONArray.parseArray(lastAnswer).stream().map(Object::toString).toList();
                    }

                    throw new RuntimeException("LLM 请求获取问题失败, " + response);
                });
    }

    @Override
    protected KaLeiDoFinanceBot getInstance() {
        return this;
    }

}
