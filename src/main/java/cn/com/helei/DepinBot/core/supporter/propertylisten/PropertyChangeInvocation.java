package cn.com.helei.DepinBot.core.supporter.propertylisten;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyChangeInvocation {

    private Object target;

    private String propertyName;

    private Object oldValue;

    private Object newValue;

    private Long timestamp;
}
