package cn.com.helei.bot.core.pool;

import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
public abstract class AbstractYamlLineItem {

    @PropertyChangeListenField
    private Integer id;

    private Object originLine;

    public AbstractYamlLineItem(Object originLine){
        this.originLine = originLine;
    }
}
