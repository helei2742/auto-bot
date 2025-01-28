package cn.com.helei.bot.core.pool.env;

import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
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

    public Map<String, String> getHeaders() {
        if (headers == null) return new HashMap<>();
        return new HashMap<>(headers);
    }
}
