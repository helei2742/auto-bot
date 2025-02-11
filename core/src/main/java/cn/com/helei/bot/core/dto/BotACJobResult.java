package cn.com.helei.bot.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BotACJobResult {

    private Integer botId;

    private String group;

    private String jobName;

    private Integer acId;

    private Boolean success;

    private String errorMsg;

    private Object data;

    public BotACJobResult(Integer botId, String group, String jobName, Integer acId) {
        this(botId, group, jobName, acId, true, null, null);
    }

    public static BotACJobResult ok(Integer botId, String group, String jobName, Integer acId) {
        return new BotACJobResult(botId, group, jobName, acId, true, null, null);
    }

    public BotACJobResult setResult(Result result) {
        this.success = result.getSuccess();
        this.errorMsg = result.getErrorMsg();
        this.data = result.getData();

        return this;
    }
}
