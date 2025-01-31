package cn.com.helei.application.pipe;

import cn.com.helei.bot.core.config.BaseDepinBotConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PipeConfig extends BaseDepinBotConfig {

    private String getBaseUrlQueryUrl = "https://pipe-network-backend.pipecanary.workers.dev/api/getBaseUrl";
}
