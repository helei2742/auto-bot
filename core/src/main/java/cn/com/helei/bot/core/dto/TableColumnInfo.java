package cn.com.helei.bot.core.dto;

import lombok.Data;

@Data
public class TableColumnInfo {
    private Integer cid;

    private String name;

    private String type;

    private Integer notnull;

    private Object dflt_value;

    private Integer pk;
}
