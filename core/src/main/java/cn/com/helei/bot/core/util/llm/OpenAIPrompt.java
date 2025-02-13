package cn.com.helei.bot.core.util.llm;

import com.alibaba.fastjson.JSONObject;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class OpenAIPrompt {

    private String model;

    private final List<JSONObject> messages = new ArrayList<>();

    private String system;

    private final JSONObject systemJB = new JSONObject();

    private boolean stream = false;

    private double temperature;

    private double topP = 0.7;

    private double presencePenalty = 0;

    private final JSONObject responseForMatObj = new JSONObject();

    public JSONObject build() {
        JSONObject jb = new JSONObject();
        jb.put("model", model);
        jb.put("messages", messages);
        jb.put("system", system);
        jb.put("stream", stream);
        jb.put("temperature", temperature);
        jb.put("top_p", topP);
        jb.put("presence_penalty", presencePenalty);
        jb.put("response_format", responseForMatObj);

        return jb;
    }

    public void setSystem(String content) {
        this.system = content;

        if (messages.isEmpty() || "system".equals(messages.getFirst().getString("role"))) {
            if (messages.isEmpty()) messages.addFirst(systemJB);
            systemJB.put("role", "system");
            systemJB.put("content", content);
        }
    }

    public void addQuestion(String content) {

        if (!messages.isEmpty() && "user".equals(messages.getLast().getString("role"))) {
            messages.getLast().put("content", content);
        } else {
            JSONObject question = new JSONObject();
            question.put("role", "user");
            question.put("content", content);

            this.messages.add(question);
        }
    }

    public void addAnswer(String content) {

        if (!messages.isEmpty() && "assistant".equals(messages.getLast().getString("role"))) {
            messages.getLast().put("content", content);
        } else {
            JSONObject question = new JSONObject();
            question.put("role", "assistant");
            question.put("content", content);

            this.messages.add(question);
        }
    }

    public void setResponseFormat(String type) {
        this.responseForMatObj.put("type", type);
    }

    public String getLastAnswer() {

        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getString("role"))) return messages.get(i).getString("content");
        }

        return null;
    }
}
