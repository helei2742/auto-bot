package cn.com.helei.bot.core.supporter.netty.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HandlerEntity<T> {
    private long expireTime;

    private Consumer<T> callback;
}
