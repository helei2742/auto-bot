package cn.com.helei.bot.core.util.excel;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

public class IntegerStringConverter implements Converter<Integer> {
    @Override
    public Class<?> supportJavaTypeKey() {
        return Integer.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }


    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        if (cellData.getType() == CellDataTypeEnum.NUMBER) {
            return Integer.valueOf(String.valueOf(cellData.getNumberValue()));
        }

        String stringValue = cellData.getStringValue();
        if (StrUtil.isBlank(stringValue)) {
            return null;
        }
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
