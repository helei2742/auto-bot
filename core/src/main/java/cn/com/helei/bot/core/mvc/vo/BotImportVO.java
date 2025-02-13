package cn.com.helei.bot.core.mvc.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BotImportVO {

    private Integer botId;

    private String botKey;

    private List<Map<String, Object>> rawLines;
}
