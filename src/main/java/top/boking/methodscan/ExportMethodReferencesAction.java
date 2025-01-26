package top.boking.methodscan;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.Nullable;
import top.boking.methodscan.domain.MethodReference;
import top.boking.methodscan.file.ExcelFileCreator;
import top.boking.methodscan.parsemethod.GitCommitInfo;
import top.boking.methodscan.parsemethod.ReferenceLineInfo;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExportMethodReferencesAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {



        Project project = e.getProject();
        if (project == null) return;

        // 显示选择框，提供两种直观的选项
        String[] options = {"手动输入方法名称", "上传文件"};
        int choice = Messages.showDialog(
                project,
                "请选择一种方式：\n1. 手动输入方法名称\n2. 上传文件解析方法名",
                "选择方法来源",
                options,
                0, // 默认选中第一个选项
                Messages.getQuestionIcon()
        );

        List<String> methodList = getMethodList(choice, project);
        if (methodList == null) return;
        if (methodList.isEmpty()) {
            Messages.showErrorDialog("没有找到要搜索的方法！", "错误");
            return;
        }

        searchMethodAndWrite(methodList, project);
    }

    private static void searchMethodAndWrite(List<String> methodList, Project project) {
        // 创建 Excel 工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("References");
        sheet.setDefaultColumnWidth(15);
        sheet.setColumnWidth(0, 150);
        Row headRow = sheet.createRow(0);
        headRow.createCell(0).setCellValue("");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            int rowIndex = 1;
            for (String methodInfo : methodList) {
                MethodReference methodReference = new MethodReference(methodInfo);
                String methodName = methodReference.getMethodName();
                String className = methodReference.getClassName();
                // 查找类
                PsiClass psiClass = JavaPsiFacade.getInstance(project)
                        .findClass(className, GlobalSearchScope.allScope(project));
                // 查找方法
                PsiMethod targetMethod = methodReference.findSpecificMethod(project);
                if (targetMethod == null) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue("找不到方法：" + className + "." + methodName);
                    continue;
                }
                if (psiClass == null) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue("找不到类：" + className);
                    continue;
                }
                // 查找引用
                for (PsiReference reference : ReferencesSearch.search(targetMethod).findAll()) {
                    PsiElement element = reference.getElement();
                    // 获取引用所在的文件
                    PsiFile psiFile = element.getContainingFile();
                    // 获取 VirtualFile 和 Document
                    VirtualFile virtualFile = psiFile.getVirtualFile();
                    int location = ReferenceLineInfo.getLineNumber(element);
                    String codeSnippet = element.getText();
                    String commitAuthor = GitCommitInfo.getCommitAuthor(project, virtualFile, location);
                    // 写入 Excel
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(className + "." + methodName);
                    row.createCell(1).setCellValue(location);
                    row.createCell(2).setCellValue(codeSnippet);
                }
            }

        });
        // 后台任务完成后，回到 EDT 执行保存操作
        ApplicationManager.getApplication().invokeLater(() -> {
            saveFile(project, workbook); // 确保在任务完成后执行保存操作
        });
    }

    private static void saveFile(Project project, Workbook workbook) {
        // 保存 Excel 文件
        try (FileOutputStream out = new FileOutputStream(ExcelFileCreator.createExcelFileInSelectedDirectory(project))) {
            workbook.write(out);
            Messages.showInfoMessage("方法引用导出成功！文件已保存为：MethodReferences.xlsx", "成功");
        } catch (IOException ex) {
            Messages.showErrorDialog("导出 Excel 文件失败：" + ex.getMessage(), "错误");
        }
    }

    private static @Nullable List<String> getMethodList(int choice, Project project) {
        List<String> methodList = new ArrayList<>();
        if (choice == 0) {
            // 手动输入类名和方法名
            String qualifiedClassName = Messages.showInputDialog(
                    project,
                    "请输入方法签名的全限定名（例如：com.example.MyClass）：",
                    "输入方法签名",
                    Messages.getQuestionIcon()
            );

            if (qualifiedClassName == null || qualifiedClassName.trim().isEmpty()) {
                Messages.showErrorDialog("类名不能为空！", "错误");
                return null;
            }
            methodList.add(qualifiedClassName);
        } else if (choice == 1) {
            // 使用 IDEA 的 FileChooser 显示文件选择器
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                    true, // 仅支持选择文件
                    false, // 不允许选择文件夹
                    false, false, false, false
            );
            fileChooserDescriptor.setTitle("选择包含方法名的文件");
            fileChooserDescriptor.setDescription("文件格式为：类全限定名 方法名，每行一个");

            VirtualFile file = FileChooser.chooseFile(fileChooserDescriptor, project, null);
            if (file == null) {
                Messages.showErrorDialog("未选择文件！", "错误");
                return null;
            }
            // 解析文件内容
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    methodList.add(line);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("文件解析失败：" + ex.getMessage(), "错误");
                return null;
            }
        } else {
            Messages.showInfoMessage("操作已取消。", "取消");
            return null;
        }
        return methodList;
    }
}
