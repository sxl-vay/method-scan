package top.boking.methodscan.file;

import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.context.RowWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class CustomLoopMergeStrategy implements RowWriteHandler {

    private String lastHolder;
    private Integer lastIndex = 1;
    private final Integer count;

    public CustomLoopMergeStrategy(Integer count) {
        this.count = count;
    }

    @Override
    public void beforeRowCreate(RowWriteHandlerContext context) {
        RowWriteHandler.super.beforeRowCreate(context);
    }

    @Override
    public void beforeRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Integer rowIndex, Integer relativeRowIndex, Boolean isHead) {
        RowWriteHandler.super.beforeRowCreate(writeSheetHolder, writeTableHolder, rowIndex, relativeRowIndex, isHead);
    }

    @Override
    public void afterRowCreate(RowWriteHandlerContext context) {
    }

    @Override
    public void afterRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, Boolean isHead) {
        RowWriteHandler.super.afterRowCreate(writeSheetHolder, writeTableHolder, row, relativeRowIndex, isHead);
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        Integer rowIndex = context.getRowIndex();
        // 获取当前行的第一列的单元格内容
        if (rowIndex == 0) {
            return; // 第一行不合并
        }
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        Cell currentCell = sheet.getRow(rowIndex).getCell(0); // 当前行第一列
        String currentValue = currentCell.getStringCellValue();
        if (rowIndex == 1) {
            lastHolder = currentValue;
            return; // 第一数据行不合并
        }
        Cell previousCell = sheet.getRow(rowIndex - 1).getCell(0); // 上一行第一列
        String previousValue = previousCell.getStringCellValue();
        if (!currentValue.equals(previousValue)) {
            // 合并单元格
            sheet.addMergedRegion(new CellRangeAddress(lastIndex, rowIndex-1, 0, 0));
            lastIndex = rowIndex;
            lastHolder = currentValue;
        }
        if (rowIndex == count ) {
            sheet.addMergedRegion(new CellRangeAddress(lastIndex, rowIndex, 0, 0));
        }
    }

    @Override
    public void afterRowDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, Boolean isHead) {
        RowWriteHandler.super.afterRowDispose(writeSheetHolder, writeTableHolder, row, relativeRowIndex, isHead);
    }
}
