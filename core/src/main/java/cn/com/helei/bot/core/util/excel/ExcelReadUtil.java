package cn.com.helei.bot.core.util.excel;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import io.micrometer.common.lang.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Slf4j
public class ExcelReadUtil {

    public static <T> Map<String, List<T>> readExcelAsMap(
            String filePath,
            BiFunction<String, Map<String, Object>, T> converter,
            BiConsumer<String, List<T>> sheetReadHandler
    ) throws IOException {

        Map<String, List<T>> sheetDataMap = new HashMap<>();

        try (ExcelReader reader = EasyExcel.read(filePath).build()) {
            List<ReadSheet> sheets = reader.excelExecutor().sheetList();

            for (ReadSheet sheet : sheets) {
                String sheetName = sheet.getSheetName();
                List<T> list = readExcelToMap(filePath, sheetName, converter, sheetReadHandler);
                sheetDataMap.put(sheetName, list);
            }

            return sheetDataMap;
        }
    }

    public static <T> Map<String, List<T>> readExcelAsMap(
            String filePath,
            Class<T> tClass,
            BiFunction<String, T, Boolean> filter,
            BiConsumer<String, List<T>> sheetReadHandler
    ) throws IOException {

        Map<String, List<T>> sheetDataMap = new HashMap<>();

        try (ExcelReader reader = EasyExcel.read(filePath).build()) {
            List<ReadSheet> sheets = reader.excelExecutor().sheetList();

            for (ReadSheet sheet : sheets) {
                String sheetName = sheet.getSheetName();
                List<T> list = readSingleSheet(filePath, sheetName, tClass, filter, sheetReadHandler);
                sheetDataMap.put(sheetName, list);
            }

            return sheetDataMap;
        }
    }

    private static <T> List<T> readSingleSheet(String filePath, String sheetName, Class<T> tClass,
                                               BiFunction<String, T, Boolean> filter, BiConsumer<String, List<T>> sheetReadHandler) {
        List<T> list = new ArrayList<>();

        EasyExcel.read(filePath, tClass, new AnalysisEventListener<T>() {

                    @Override
                    public void invoke(T t, AnalysisContext analysisContext) {
                        if (filter.apply(sheetName, t)) {
                            list.add(t);
                        }
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        sheetReadHandler.accept(sheetName, list);
                    }
                })
                .registerConverter(new StringConverter())
                .sheet(sheetName).doRead();

        return list;
    }


    /**
     * 读取excel,放入List<Map<String, String>>
     *
     * @param fileName 读取excel的文件名称
     * @return datalist
     */
    public static List<Map<String, Object>> readExcelToMap(String fileName) {
        return readExcelToMap(fileName, null);
    }

    public static List<Map<String, Object>> readExcelToMap(
            String fileName,
            String sheetName
    ) {
        return readExcelToMap(
                fileName,
                sheetName,
                (name, map)-> map,
                null
        );
    }

    /**
     * 读取excel,放入List<Map<String, String>>
     *
     * @param fileName  读取excel的文件名称
     * @param sheetName sheetName
     * @return datalist
     */
    public static <T> List<T> readExcelToMap(
            String fileName,
            String sheetName,
            BiFunction<String, Map<String, Object>, T> converter,
            BiConsumer<String, List<T>> sheetReadHandler
    ) {
        List<T> dataList = new ArrayList<>();

        ExcelReaderBuilder builder = EasyExcel.read(fileName, new AnalysisEventListener<Map<String, Object>>() {
            //用于存储表头的信息
            private Map<Integer, String> headMap;

            //读取excel表头信息
            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                this.headMap = headMap;
            }

            //直接使用Map来保存数据
            @Override
            public void invoke(Map<String, Object> valueData, AnalysisContext context) {
                //把表头和值放入Map
                HashMap<String, Object> paramsMap = new HashMap<>();
                for (int i = 0; i < valueData.size(); i++) {
                    String key = headMap.get(i);
                    Object value = valueData.get(i);
                    //将表头作为map的key，每行每个单元格的数据作为map的value
                    paramsMap.put(key, value);
                }

                if (converter != null) {
                    dataList.add(converter.apply(sheetName, paramsMap));
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.debug("Excel读取完成,文件名:" + fileName + ",sheet:" + sheetName + ",行数：" + dataList.size());

                if (sheetReadHandler != null) {
                    sheetReadHandler.accept(sheetName, dataList);
                }
            }
        });

        if (StrUtil.isBlank(sheetName)) {
            builder.sheet().doRead();
        } else {
            builder.sheet(sheetName).doRead();
        }

        return dataList;
    }

    /**
     * 使用Class来读取Excel
     *
     * @param path path
     * @return 返回 ClassList 的列表
     */
    public static <T> List<T> readXLSXConvertObjectList(String path, Class<T> classT) throws IOException {
        try (InputStream inputStream = new FileInputStream(new File(path))) {
            return readExcelConvertObjectList(inputStream, ExcelTypeEnum.XLSX, classT);
        }
    }

    /**
     * 使用Class来读取Excel
     *
     * @param path path
     * @return 返回 ClassList 的列表
     */
    public static <T> List<T> readXLSXConvertObjectList(String path, String sheetName, Class<T> classT) throws IOException {
        try (InputStream inputStream = new FileInputStream(new File(path))) {
            return readExcelConvertObjectList(inputStream, ExcelTypeEnum.XLSX, sheetName, classT);
        }
    }

    /**
     * 使用Class来读取Excel
     *
     * @param inputStream Excel的输入流
     * @return 返回 ClassList 的列表
     */
    public static <T> List<T> readXLSXConvertObjectList(InputStream inputStream, Class<T> classT) {
        return readExcelConvertObjectList(inputStream, ExcelTypeEnum.XLSX, classT);
    }

    /**
     * 使用Class来读取Excel
     *
     * @param inputStream Excel的输入流
     * @return 返回 ClassList 的列表
     */
    public static <T> List<T> readXLSXConvertObjectList(InputStream inputStream, String sheetName, Class<T> classT) {
        return readExcelConvertObjectList(inputStream, ExcelTypeEnum.XLSX, sheetName, classT);
    }


    /**
     * 使用Class来读取Excel
     *
     * @param inputStream   Excel的输入流
     * @param excelTypeEnum Excel的格式(XLS或XLSX)
     * @return 返回 ClassList 的列表
     */
    public static <T> List<T> readExcelConvertObjectList(InputStream inputStream, ExcelTypeEnum excelTypeEnum, Class<T> classT) {
        return readExcelConvertObjectList(inputStream, excelTypeEnum, null, classT);
    }

    /**
     * 使用Class来读取Excel
     *
     * @param inputStream   Excel的输入流
     * @param excelTypeEnum Excel的格式(XLS或XLSX)
     * @return 返回 ClassList 的列表
     */
    public static <T> List<T> readExcelConvertObjectList(InputStream inputStream, ExcelTypeEnum excelTypeEnum, String sheetName, Class<T> classT) {
        return readExcelConvertObjectList(inputStream, excelTypeEnum, sheetName, 1, classT);
    }


    /**
     * 读取excel数据到数据对象
     *
     * @param inputStream   文件流
     * @param excelTypeEnum 文件类型Excel的格式(XLS或XLSX)
     * @param sheetName     sheetName
     * @param headLineNum   开始读取数据的行
     * @param classT        转为对象的CLASS
     * @param <T>           T
     * @return list
     */
    public static <T> List<T> readExcelConvertObjectList(
            InputStream inputStream,
            ExcelTypeEnum excelTypeEnum,
            String sheetName,
            @Nullable Integer headLineNum,
            Class<T> classT
    ) {
        if (headLineNum == null) {
            headLineNum = 1;
        }
        ExcelReaderBuilder builder = EasyExcel
                .read(inputStream)
                .registerConverter(new StringConverter())
                .excelType(excelTypeEnum)
                .head(classT).headRowNumber(headLineNum);
        if (StrUtil.isNotBlank(sheetName)) {
            return builder.sheet(sheetName).doReadSync();
        } else {
            return builder.sheet(0).doReadSync();
        }
    }
}
