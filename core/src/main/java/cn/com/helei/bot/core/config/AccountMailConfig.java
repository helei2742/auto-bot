package cn.com.helei.bot.core.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountMailConfig {

    private String protocol;

    private String host;

    private int port;

    private boolean sslEnable;
}
