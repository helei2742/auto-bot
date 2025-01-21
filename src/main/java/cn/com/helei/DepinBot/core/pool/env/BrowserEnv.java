package cn.com.helei.DepinBot.core.pool.env;

import cn.com.helei.DepinBot.core.pool.AbstractYamlLineItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class BrowserEnv extends AbstractYamlLineItem {

    private Map<String, String> headers;

}
