package cn.com.helei.application.teneo;

import cn.com.helei.bot.core.WSDepinBotConfig;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeneoDepinConfig extends WSDepinBotConfig {

    private String apiKey;

}
