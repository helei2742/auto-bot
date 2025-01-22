package cn.com.helei.DepinBot.core.pool.env;

import cn.com.helei.DepinBot.core.pool.AbstractYamlLineItem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BrowserEnv extends AbstractYamlLineItem {

    private Map<String, String> headers;

    public BrowserEnv(Object originLine) {
        headers = (Map<String, String>) originLine;
    }
}
