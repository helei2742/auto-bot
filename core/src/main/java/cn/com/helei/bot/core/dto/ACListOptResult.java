package cn.com.helei.bot.core.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ACListOptResult {

    private Integer botId;

    private String botName;

    private String jobName;

    private Boolean success;

    private String errorMsg;

    private List<BotACJobResult> results;

    private Integer successCount;

    public static ACListOptResult fail(
            Integer botId,
            String botName,
            String jobName,
            String errorMsg
    ) {
        return new ACListOptResult(botId, botName, jobName, false, errorMsg, null, 0);
    }
}
