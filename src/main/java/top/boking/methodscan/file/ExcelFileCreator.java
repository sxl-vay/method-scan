package top.boking.methodscan.file;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class ExcelFileCreator {

    /**
     * 打开目录选择器，获取用户指定的目录，并在其中创建 Excel 文件。
     *
     * @param project 当前项目
     */
    public static File createExcelFileInSelectedDirectory(Project project) {
        // 显示目录选择器
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                false, // 不允许选择文件
                true,  // 允许选择目录
                false, false, false, false
        );
        descriptor.setTitle("选择保存 Excel 文件的目录");
        descriptor.setDescription("请选择一个目录，Excel 文件将保存到该目录下。");
        VirtualFile chosenDir = FileChooser.chooseFile(descriptor, project, null);
        // 获取目录路径
        String directoryPath = chosenDir.getPath();
        File file = new File(directoryPath, "ExportedData.xlsx");
        if (file.exists()) {
            AtomicReference<String> qualifiedClassName = new AtomicReference<>();
            ApplicationManager.getApplication().invokeLater(() -> {
                // 手动输入类名和方法名
                qualifiedClassName.set(Messages.showInputDialog(
                        project,
                        "ExportedData.xlsx 文件已存在,请输入自定义文件名称",
                        "输入文件名称",
                        Messages.getQuestionIcon()
                ));
            });

            file  = new File(directoryPath, qualifiedClassName+".xlsx");
        }
        return file;
    }
}
