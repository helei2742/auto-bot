package cn.com.helei.DepinBot.core.util.table;

import cn.hutool.core.util.StrUtil;
import com.jakewharton.fliptables.FlipTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CommandLineTablePrintHelper {

    /**
     * 生成表格字符串
     *
     * @param list   list
     * @param tClass tClass
     * @return 表格字符串
     */
    public static  String generateTableString(List<?> list, Class<?> tClass) {
        List<String> tableHeader = new ArrayList<>();
        List<Field> tableFields = new ArrayList<>();

        for (Field field : tClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(CommandTableField.class)) {
                String name = field.getAnnotation(CommandTableField.class).name();
                if (StrUtil.isBlank(name)) {
                    name = field.getName();
                }

                tableHeader.add(name);
                tableFields.add(field);
            }
        }

        if (tableHeader.isEmpty()) {
            List<Field> fieldList = List.of(tClass.getDeclaredFields());
            tableFields.addAll(fieldList);
            tableHeader.addAll(fieldList.stream().map(Field::getName).toList());
        }

        tableHeader.addFirst("row");
        String[][] table = new String[list.size()][tableHeader.size()];

        for (int i = 0; i < list.size(); i++) {
            Object obj =  list.get(i);

            table[i][0] = String.valueOf(i);
            for (int i1 = 1; i1 <= tableFields.size(); i1++) {
                Field field = tableFields.get(i1 - 1);
                field.setAccessible(true);
                Object o = null;
                try {
                    o = field.get(obj);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                table[i][i1] = String.valueOf(o == null ? "NO_DATA" : o);
            }
        }

        return FlipTable.of(tableHeader.toArray(new String[0]), table);
    }

}
