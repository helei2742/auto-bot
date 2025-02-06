package cn.com.helei.bot.core.config;

import cn.com.helei.bot.core.util.typehandler.JsonTypeHandler;
import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.helei.bot.core.util.typehandler.MapTextTypeHandler;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisConfig implements MetaObjectHandler {
    /**
     * 使用mp做添加操作时候，这个方法执行
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        //设置属性值
        this.setFieldValByName("insertDatetime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateDatetime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("isValid", 1, metaObject);
    }

    /**
     * 使用mp做修改操作时候，这个方法执行
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateDatetime", LocalDateTime.now(), metaObject);
    }


    @Bean
    public JsonTypeHandler jsonTypeHandler() {
        return new JsonTypeHandler();
    }

    @Bean
    public MapTextTypeHandler mapTextTypeHandler() {
        return new MapTextTypeHandler();
    }

    @Bean
    public LocalDateTimeTypeHandler localDateTimeTypeHandler() {
        return new LocalDateTimeTypeHandler();
    }
}
