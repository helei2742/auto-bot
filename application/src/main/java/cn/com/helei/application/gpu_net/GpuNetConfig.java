package cn.com.helei.application.gpu_net;

import cn.com.helei.bot.core.BaseDepinBotConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class GpuNetConfig extends BaseDepinBotConfig {

    public List<String> solAddress;

}
