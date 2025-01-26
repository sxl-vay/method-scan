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

import java.io.FileOutputStream;
import java.io.IOException;

public class ExportMethodReferencesAction手动输入 extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 弹出对话框，输入类名和方法名
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

        // 查找类
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(qualifiedClassName, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            Messages.showErrorDialog("找不到类：" + qualifiedClassName, "错误");
            return;
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
            Messages.showErrorDialog("找不到方法：" + methodName, "错误");
            return;
        }

        // 查找引用并导出到 Excel
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("References");
        int rowIndex = 0;

        for (PsiReference reference : ReferencesSearch.search(targetMethod).findAll()) {
            PsiElement element = reference.getElement();
            String location = element.getContainingFile().getVirtualFile().getPath();
            String codeSnippet = element.getText();

            // 写入 Excel
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(location);
            row.createCell(1).setCellValue(codeSnippet);
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
