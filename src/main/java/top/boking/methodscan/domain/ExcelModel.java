package top.boking.methodscan.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ExcelModel {
    @ExcelProperty("方法签名")
    private String signature;

    @ExcelProperty("引用方")
    private String reference;

    @ExcelProperty("作者")
    private String author;

    @ExcelProperty("代码片段")
    private String codeSnippet;

}
