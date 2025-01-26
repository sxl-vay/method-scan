package top.boking.methodscan.bak;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.openapi.ui.Messages;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExportMethodReferencesActionYesNo选择 extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 提供选择方式
        int choice = Messages.showYesNoDialog(
                project,
                "请选择一种方式：\n1. 手动输入方法名称\n2. 上传文件解析方法名",
                "选择方式",
                Messages.getQuestionIcon()
        );

        List<String[]> methodList = new ArrayList<>();
        if (choice == Messages.YES) {
            // 手动输入类名和方法名
            String qualifiedClassName = Messages.showInputDialog(
                    project,
                    "请输入类的全限定名（例如：com.example.MyClass）：",
                    "输入类名",
                    Messages.getQuestionIcon()
            );

            if (qualifiedClassName == null || qualifiedClassName.trim().isEmpty()) {
                Messages.showErrorDialog("类名不能为空！", "错误");
                return;
            }

            String methodName = Messages.showInputDialog(
                    project,
                    "请输入方法名称：",
                    "输入方法名",
                    Messages.getQuestionIcon()
            );

            if (methodName == null || methodName.trim().isEmpty()) {
                Messages.showErrorDialog("方法名不能为空！", "错误");
                return;
            }

            methodList.add(new String[]{qualifiedClassName, methodName});

        } else if (choice == Messages.NO) {
            // 文件选择器
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择包含方法名的文件");
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\\s+");
                        if (parts.length == 2) {
                            methodList.add(parts); // 假设文件格式为 "类名 方法名"
                        }
                    }
                } catch (IOException ex) {
                    Messages.showErrorDialog("文件解析失败：" + ex.getMessage(), "错误");
                    return;
                }
            } else {
                Messages.showErrorDialog("未选择文件！", "错误");
                return;
            }
        }

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
            if (psiClass == null) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("找不到类：" + qualifiedClassName);
                continue;
            }

            // 查找方法
            PsiMethod targetMethod = null;
            for (PsiMethod method : psiClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }

            if (targetMethod == null) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("找不到方法：" + qualifiedClassName + "." + methodName);
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
}
