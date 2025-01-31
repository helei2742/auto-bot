package cn.com.helei.application.keitokun;

import lombok.Data;

@Data
public class KeitokunUidAndTokenDto {

    private Integer id;

    private String type;

    private String uid;

    private String token;
}
