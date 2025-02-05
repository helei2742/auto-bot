package cn.com.helei.bot.core.config;

import java.util.List;

public class SystemConfig {

    public static final List<String> CONFIG_DIR_BOT_PATH = List.of("config", "bot");

    public static final List<String> CONFIG_DIR_APP_PATH = List.of("config", "app");

    public static final String SQL_LITE_DRIVER = "org.sqllite.JDBC";

    public static final String SQL_LITE_JDBC_URL = "jdbc:sqllite:botData/db/auto_bot.db";
}
