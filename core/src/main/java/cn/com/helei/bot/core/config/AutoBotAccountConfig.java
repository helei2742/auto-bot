package cn.com.helei.bot.core.config;

import cn.com.helei.bot.core.constants.ProxyType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AutoBotAccountConfig {

    private String configFilePath;

    private ProxyType proxyType;

    private Boolean proxyRepeat;
}
