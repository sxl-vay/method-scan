package top.boking.methodscan.file;

import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.RowWriteHandlerContext;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Row;

public class CustomLoopMergeStrategy implements RowWriteHandler {
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
        RowWriteHandler.super.afterRowCreate(context);
    }

    @Override
    public void afterRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, Boolean isHead) {
        RowWriteHandler.super.afterRowCreate(writeSheetHolder, writeTableHolder, row, relativeRowIndex, isHead);
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        RowWriteHandler.super.afterRowDispose(context);
    }

    @Override
    public void afterRowDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, Boolean isHead) {
        RowWriteHandler.super.afterRowDispose(writeSheetHolder, writeTableHolder, row, relativeRowIndex, isHead);
    }
}
