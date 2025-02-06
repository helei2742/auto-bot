package cn.com.helei.bot.core.supporter.persistence;

import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeInvocation;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeProxy;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public abstract class AbstractPersistenceManager implements AccountPersistenceManager {


    /**
     * 监听的对象 -> 该对象的root
     */
    private final ConcurrentMap<Object, Object> listenedObjRootMap = new ConcurrentHashMap<>();

    /**
     * root target -> root proxy
     */
    private final ConcurrentMap<Object, Object> originRoot2ProxyMap = new ConcurrentHashMap<>();


    /**
     * 注册持久化监听
     *
     * @param type            type
     * @param accountContexts accountContexts
     */
    @Override
    public void registerPersistenceListener(String type, List<AccountContext> accountContexts) {
        if (accountContexts == null || accountContexts.isEmpty()) return;
        accountContexts.replaceAll(accountContext -> bindPersistenceAnnoListener(type, accountContext));
    }


    /**
     * 对象属性添加变化监听
     *
     * @param type   type
     * @param target target 目标对象
     * @param <T>    T
     * @return 添加监听后被动态代理的对象
     */
    public <T> T bindPersistenceAnnoListener(String type, T target) {
        return doBindPersistenceAnnoListener(target, target);
    }


    /**
     * 对象属性添加变化监听
     *
     * @param target target 目标对象
     * @param <T>    T
     * @return 添加监听后被动态代理的对象
     */
    private <T> T doBindPersistenceAnnoListener(T target, Object rootObj) {
        if (target == null) return null;

        Class<?> targetClass = target.getClass();

        PropertyChangeListenClass propertyChangeListenClass = targetClass.getAnnotation(PropertyChangeListenClass.class);

        // 类上带有PersistenceClass注解，表示可以的类
        if (propertyChangeListenClass == null) {
            return target;
        }

        T proxy = PropertyChangeProxy.createProxy(target, this::propertyChangeHandler);

        // 深度监听，还要给监听的字段对象内的属性监听
        if (propertyChangeListenClass.isDeep()) {

            for (Field field : targetClass.getDeclaredFields()) {
                field.setAccessible(true);
                // 字段上带有PersistenceField注解，表示可以的字段， 字段类型上带有PersistenceClass，还要监听字段对象的属性
                if (field.isAnnotationPresent(PropertyChangeListenField.class)
                        && field.getType().isAnnotationPresent(PropertyChangeListenClass.class)) {
                    try {
                        Object fieldValue = field.get(target);
                        Object filedProxy = doBindPersistenceAnnoListener(fieldValue, rootObj);

                        field.set(target, filedProxy);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("访问字段失败", e);
                    }
                }
            }
        }

        listenedObjRootMap.put(target, rootObj);

        if (target.equals(rootObj)) {
            originRoot2ProxyMap.put(rootObj, proxy);
        }

        return proxy;
    }


    /**
     * 对象属性发生改变的回调
     *
     * @param invocation invocation
     */
    protected abstract void propertyChangeHandler(PropertyChangeInvocation invocation);
}
