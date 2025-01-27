package top.boking.methodscan.domain;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class ExcelModel {
    @ExcelProperty(value = "方法签名")
    @ColumnWidth(100)
    private String signature;

    @ExcelProperty("引用方")
    @ColumnWidth(100)
    private String reference;

    @ExcelProperty("作者")
    @ColumnWidth(20)
    private String author;

//    @ExcelProperty("代码片段")
    @ExcelIgnore
    private String codeSnippet;

}
