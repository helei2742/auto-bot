package cn.com.helei.application.teneo;

import cn.com.helei.bot.core.config.WSDepinBotConfig;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeneoDepinConfig extends WSDepinBotConfig {

    private String apiKey;

}
