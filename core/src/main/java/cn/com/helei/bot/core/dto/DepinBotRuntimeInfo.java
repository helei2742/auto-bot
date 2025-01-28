package cn.com.helei.bot.core.dto;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class DepinBotRuntimeInfo {

    private final ConcurrentHashMap<String, Object> keyValueInfoMap = new ConcurrentHashMap<>();

}
