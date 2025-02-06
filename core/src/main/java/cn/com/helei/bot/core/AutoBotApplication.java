package cn.com.helei.bot.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "cn.com.helei.bot.core.mvc.mapper")
public class AutoBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoBotApplication.class, args);
    }
}
