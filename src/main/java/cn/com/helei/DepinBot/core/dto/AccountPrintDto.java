package cn.com.helei.DepinBot.core.dto;

import cn.com.helei.DepinBot.core.constants.ConnectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountPrintDto {


    private String name;

    private String proxyInfo;

    private String browserEnvInfo;

    private Boolean usable;

    private LocalDateTime startDateTime;

    private LocalDateTime updateDateTime;

    private Integer heartBeatCount;

    private ConnectStatus connectStatus;
}
