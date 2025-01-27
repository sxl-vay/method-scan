package top.boking.methodscan.file;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
@Slf4j
public class ExcelFileCreator {



    public static File createExcelFileInSelectedDirectory(Project project) {
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(
                new FileSaverDescriptor("Save Excel File", "Choose where to save the Excel file"), project);

        String defaultPath = getDefaultDirectoryPath(project);
        VirtualFile baseDir = project.getBaseDir();
        if (defaultPath != null) {
            baseDir = baseDir.findFileByRelativePath(defaultPath);
        }
        String defaultFileName = "ExportedData.xlsx";   
        VirtualFileWrapper wrapper = dialog.save(baseDir, defaultFileName);
        if (wrapper != null) {
            return wrapper.getFile();
        }
        return null;
    }

    private static String getDefaultDirectoryPath(Project project) {
        // You can customize this method to return a default directory, e.g., the project directory
        return project.getBasePath();  // Or any other default directory
    }

}
