package cn.com.helei.DepinBot.core.env;

import lombok.Data;

import java.util.Map;

@Data
public class BrowserEnv {

    private Integer id;

    private Map<String, String> headers;

}
