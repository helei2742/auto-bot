package cn.com.helei.bot.core.config;

import cn.com.helei.bot.core.constants.ProxyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypedAccountConfig {

    private String type;

    private ProxyType proxyType = ProxyType.NO;

    private AccountMailConfig mail;

    private String accountFileUserDirPath;

}
