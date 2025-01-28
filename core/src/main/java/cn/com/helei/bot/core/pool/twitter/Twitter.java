package cn.com.helei.bot.core.pool.twitter;

import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Twitter extends AbstractYamlLineItem {

    private String username;

    private String password;

    private String email;

    private String f2aToken;


    public Twitter(Object lineStr) {
        if (lineStr == null) {
            System.out.println(lineStr);
        }
        String[] split = ((String) lineStr).split(", ");
        if (split.length != 4) {
            throw new IllegalArgumentException("推特参数配置错误，应为 username, password, email, f2aToken");
        }

        this.username = split[0];
        this.password = split[1];
        this.email = split[2];
        this.f2aToken = split[3];
    }
}
