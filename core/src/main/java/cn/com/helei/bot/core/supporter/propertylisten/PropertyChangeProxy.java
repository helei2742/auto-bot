package cn.com.helei.bot.core.supporter.propertylisten;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyChangeProxy implements MethodInterceptor {

    private final Object target;

    private final PropertyChangeListener listener;

    private final Map<String, Object> fieldValues = new HashMap<>();

    public PropertyChangeProxy(Object target, PropertyChangeListener listener) {
        this.target = target;
        this.listener = listener;
        Class<?> targetClass = target.getClass();

        if (targetClass.isAnnotationPresent(PropertyChangeListenClass.class) || target instanceof Map<?, ?>) {
            List<Field> fields = new ArrayList<>();
            fields.addAll(List.of(targetClass.getDeclaredFields()));
            fields.addAll(List.of(targetClass.getSuperclass().getDeclaredFields()));

            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(PropertyChangeListenField.class)) {
                    String name = getFieldName(field);
                    try {
                        Object fieldValue = field.get(target);

                        if (fieldValue instanceof Map<?, ?>) {
                            fieldValue = createMapProxy((Map<?, ?>) fieldValue, name);
                            field.set(target, fieldValue);
                        }

                        fieldValues.put(name, fieldValue);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("无法访问字段值", e);
                    }
                }
            }
        }
    }

    public static <T> T createProxy(T target, PropertyChangeListener listener) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(new PropertyChangeProxy(target, listener));

        return (T) enhancer.create();
    }


    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object invoke;
        String fieldName = getFieldNameFromMethod(method);

        if (isSetter(method) && fieldValues.containsKey(fieldName)) {

            // 是代理的目标
            invoke = methodProxy.invoke(target, args);


            Object oldValue = fieldValues.get(fieldName);
            Object newValue = args[0];

            if (newValue instanceof Map<?, ?>) {
                newValue = createMapProxy((Map<?, ?>) newValue, fieldName);
            }

            //属性值发生变化
            if ((oldValue == null && newValue != null) || (oldValue != null && !oldValue.equals(newValue))) {
                fieldValues.put(fieldName, newValue);
                listener.onPropertyChanged(new PropertyChangeInvocation(
                        target,
                        fieldName,
                        oldValue,
                        newValue,
                        System.currentTimeMillis())
                );
            }
        } else {
            invoke = method.invoke(target, args);
        }

        return invoke;
    }


    private Map<?, ?> createMapProxy(Map<?, ?> originalMap, String fieldName) {
        Object o = Enhancer.create(Map.class, (MethodInterceptor) (obj, method, args, methodProxy) -> {
            PropertyChangeInvocation invocation = null;

            if (method.getName().equals("put")) {
                Object key = args[0];
                Object value = args[1];

                invocation = new PropertyChangeInvocation(target,
                        fieldName + "[" + key + "]", originalMap.get(key), value, System.currentTimeMillis());
            }

            if (method.getName().equals("remove")) {
                Object key = args[0];

                invocation = new PropertyChangeInvocation(target,
                        fieldName + "[" + key + "]", originalMap.get(key), null, System.currentTimeMillis());
            }

            Object invoke = method.invoke(originalMap, args);

            if (invocation != null) {
                listener.onPropertyChanged(invocation);
            }

            return invoke;
        });

        return (Map<?, ?>) o;
    }

    private String getFieldNameFromMethod(Method method) {
        String name = method.getName().substring(3);

        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }


    private boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameters().length == 1;
    }

    private static @NotNull String getFieldName(Field field) {
        PropertyChangeListenField persistenceField = field.getAnnotation(PropertyChangeListenField.class);
        String name = persistenceField.name();
        if (name.isEmpty()) {
            name = field.getName();
        }
        return name;
    }

}
