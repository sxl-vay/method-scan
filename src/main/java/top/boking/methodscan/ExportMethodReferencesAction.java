package top.boking.methodscan;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.Nullable;

import java.io.*;
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

        List<String[]> methodList = getMethodList(choice, project);
        if (methodList == null) return;
        if (methodList.isEmpty()) {
            Messages.showErrorDialog("没有找到要搜索的方法！", "错误");
            return;
        }

        // 创建 Excel 工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("References");
        int rowIndex = 0;

        for (String[] methodInfo : methodList) {
            String qualifiedClassName = methodInfo[0];
            String methodName = methodInfo[1];

            // 查找类
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(qualifiedClassName, GlobalSearchScope.allScope(project));


            // 查找方法
            PsiMethod targetMethod = MethodOverloadHandler.findSpecificMethod(project, methodInfo[0], methodInfo[1]);
            /*for (PsiMethod method : psiClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }*/

            if (targetMethod == null) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("找不到方法：" + qualifiedClassName + "." + methodName);
                continue;
            }


            if (psiClass == null) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("找不到类：" + qualifiedClassName);
                continue;
            }
            // 查找引用
            for (PsiReference reference : ReferencesSearch.search(targetMethod).findAll()) {
                PsiElement element = reference.getElement();
                String location = element.getContainingFile().getVirtualFile().getPath();
                String codeSnippet = element.getText();

                // 写入 Excel
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(qualifiedClassName + "." + methodName);
                row.createCell(1).setCellValue(location);
                row.createCell(2).setCellValue(codeSnippet);
            }
        }

        // 保存 Excel 文件
        try (FileOutputStream out = new FileOutputStream("MethodReferences.xlsx")) {
            workbook.write(out);
            Messages.showInfoMessage("方法引用导出成功！文件已保存为：MethodReferences.xlsx", "成功");
        } catch (IOException ex) {
            Messages.showErrorDialog("导出 Excel 文件失败：" + ex.getMessage(), "错误");
        }
    }

    private static @Nullable List<String[]> getMethodList(int choice, Project project) {
        List<String[]> methodList = new ArrayList<>();
        if (choice == 0) {
            // 手动输入类名和方法名
            String qualifiedClassName = Messages.showInputDialog(
                    project,
                    "请输入类的全限定名（例如：com.example.MyClass）：",
                    "输入类名",
                    Messages.getQuestionIcon()
            );

            if (qualifiedClassName == null || qualifiedClassName.trim().isEmpty()) {
                Messages.showErrorDialog("类名不能为空！", "错误");
                return null;
            }

            String methodName = Messages.showInputDialog(
                    project,
                    "请输入方法名称：",
                    "输入方法名",
                    Messages.getQuestionIcon()
            );

            if (methodName == null || methodName.trim().isEmpty()) {
                Messages.showErrorDialog("方法名不能为空！", "错误");
                return null;
            }

            methodList.add(new String[]{qualifiedClassName, methodName});

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
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2) {
                        methodList.add(parts); // 假设文件格式为 "类名 方法名"
                    }
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
