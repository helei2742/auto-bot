package cn.com.helei.DepinBot.core.pool;

import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenField;
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
