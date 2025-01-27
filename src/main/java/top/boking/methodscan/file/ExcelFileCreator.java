package top.boking.methodscan.file;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
@Slf4j
public class ExcelFileCreator {

    public static File createExcelFileInSelectedDirectory(Project project) {
        // Create and configure the dialog
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        // Create the input field for the directory path (editable)
        JBTextField directoryField = new JBTextField();

        directoryField.setEditable(true);  // Allow manual input
        directoryField.setPreferredSize(new java.awt.Dimension(300, directoryField.getPreferredSize().height));  // Fix height
        // Initialize with the default directory (for example, the project directory)
        directoryField.setText(getDefaultDirectoryPath(project));

        // Create a button to open the file chooser
        JButton chooseButton = new JButton("选择目录");
        chooseButton.addActionListener(e -> {
            VirtualFile chosenDir = FileChooser.chooseFile(new FileChooserDescriptor(false, true, false, false, false, false), project, null);
            if (chosenDir != null) {
                directoryField.setText(chosenDir.getPath());
            }
        });
        // Add components to the panel
        panel.add(directoryField);
        panel.add(chooseButton);
        // Create and display the dialog
        DialogWrapper dialog = new DialogWrapper(project) {
            {
                init();
                setTitle("Select Folder to Save Excel File");
            }

            @Override
            protected JComponent createCenterPanel() {
                return panel;
            }
            @Override
            protected void doOKAction() {
                super.doOKAction();
            }

            @Override
            public void doCancelAction() {
                directoryField.setText(null);
                super.doCancelAction();
            }
        };

        dialog.show();
        // Get the chosen path (from either the manual input or the file chooser)
        String directoryPath = directoryField.getText();
        if (directoryPath == null) {
            return null;
        }
        File file = new File(directoryPath, "ExportedData.xlsx");

        if (file.exists()) {
            AtomicReference<String> qualifiedClassName = new AtomicReference<>();
            qualifiedClassName.set(Messages.showInputDialog(
                    project,
                    "ExportedData.xlsx file already exists, please enter a custom file name:",
                    "Input File Name",
                    Messages.getQuestionIcon()
            ));
            file = new File(directoryPath, qualifiedClassName + ".xlsx");
        }

        return file;
    }

    private static String getDefaultDirectoryPath(Project project) {
        // You can customize this method to return a default directory, e.g., the project directory
        return project.getBasePath();  // Or any other default directory
    }

}
