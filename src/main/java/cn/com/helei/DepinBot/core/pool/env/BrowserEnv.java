package cn.com.helei.DepinBot.core.pool.env;

import cn.com.helei.DepinBot.core.pool.AbstractYamlLineItem;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class BrowserEnv extends AbstractYamlLineItem {

    private Map<String, String> headers;

    public BrowserEnv(Object originLine) {
        Map<String, String> map = (Map<String, String>) originLine;
        if (map != null) {
            headers = new HashMap<>(map);
        }
    }

    public BrowserEnv() {
            this.headers = new HashMap<>();
    }
}
