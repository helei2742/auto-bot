package cn.com.helei.DepinBot.core.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountPrintDto {

    private Integer id;

    private String name;

    private String proxyInfo;

    private String browserEnvInfo;

    private Boolean signUp;

}
