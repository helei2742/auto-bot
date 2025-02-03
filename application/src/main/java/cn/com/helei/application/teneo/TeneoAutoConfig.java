package cn.com.helei.application.teneo;

import cn.com.helei.bot.core.config.WSAutoBotConfig;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeneoAutoConfig extends WSAutoBotConfig {

    private String apiKey;

}
