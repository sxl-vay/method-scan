package top.boking.methodscan;

import com.alibaba.excel.EasyExcel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import top.boking.methodscan.domain.ExcelModel;
import top.boking.methodscan.domain.MethodReference;
import top.boking.methodscan.file.CustomLoopMergeStrategy;
import top.boking.methodscan.file.ExcelFileCreator;
import top.boking.methodscan.parsemethod.GitCommitInfo;
import top.boking.methodscan.parsemethod.ReferenceLineInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class ExportMethodReferencesAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        /*if (!DumbService.isDumb(project)) {
            Messages.showErrorDialog("工程索引尚未就绪请等待！", "错误");
            return;
        }*/
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

    private void searchMethodAndWrite(List<String> methodList, Project project) {
        Map<String, List<ExcelModel>> dataMap = new LinkedHashMap<>();
        DumbService.getInstance(project).runWhenSmart(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                dataMap.putAll(processDataMap(methodList, project));
            });
        });
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            saveFile(project, dataMap);
        });
    }

    private Map<String, List<ExcelModel>> processDataMap(List<String> methodList, Project project) {
        Map<String, List<ExcelModel>> dataMap = new LinkedHashMap<>();
        for (String methodInfo : methodList) {
            List<ExcelModel> datas = new ArrayList<>();
            MethodReference methodReference = new MethodReference(methodInfo);
            String methodName = methodReference.getMethodName();
            String className = methodReference.getClassName();
            // 查找类
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(className, GlobalSearchScope.allScope(project));

            PsiMethod targetMethod = methodReference.findSpecificMethod(project);
            if (psiClass == null || targetMethod == null) {
                ExcelModel excelModel = new ExcelModel();
                excelModel.setSignature(methodInfo);
                excelModel.setSignature(methodInfo);
                excelModel.setReference(psiClass == null?"找不到类":"找不到方法");
                datas.add(excelModel);
                dataMap.put(methodInfo, datas);
                continue;
            }
            // 查找引用
            for (PsiReference reference : ReferencesSearch.search(targetMethod).findAll()) {
                ExcelModel excelModel = new ExcelModel();
                excelModel.setSignature(methodInfo);
                PsiElement element = reference.getElement();
                PsiMethod referencingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
                String refMethodName = referencingMethod.getName();
                PsiClass referenceClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                String qualifiedName = referenceClass.getQualifiedName();
                // 获取引用所在的文件
                PsiFile psiFile = element.getContainingFile();
                // 获取 VirtualFile 和 Document
                VirtualFile virtualFile = psiFile.getVirtualFile();
                int location = ReferenceLineInfo.getLineNumber(element);
                String commitAuthor = GitCommitInfo.getCommitAuthor(project, virtualFile, location);
                // 写入 Excel
                String referenceStr = qualifiedName + "." + refMethodName + ":" + location;
                excelModel.setReference(referenceStr);
                excelModel.setAuthor(commitAuthor);
                String codeSnippet = element.getText();
                excelModel.setCodeSnippet(codeSnippet);
                datas.add(excelModel);
            }
            dataMap.put(methodInfo, datas);
        }
        return dataMap;
    }

    private static void saveFile(Project project, Map<String, List<ExcelModel>> datas) {
        // 保存 Excel 文件
        try {
            Collection<?> dataList = parseData(datas);
            File file = ExcelFileCreator.createExcelFileInSelectedDirectory(project);
            EasyExcel.write(file, ExcelModel.class)
                    .sheet("模板")
                    .registerWriteHandler(new CustomLoopMergeStrategy(dataList.size()))
                    .doWrite(dataList);
            Messages.showInfoMessage("方法引用导出成功！文件已保存为：methodReferences.xlsx", "成功");
        } catch (Exception ex) {
            ex.printStackTrace();
//            Messages.showErrorDialog("导出 Excel 文件失败：" + ex.getMessage(), "错误");
        }
    }

    private static Collection<?> parseData(Map<String, List<ExcelModel>> datas) {
        return datas.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static @Nullable List<String> getMethodList(int choice, Project project) {
        List<String> methodList = new ArrayList<>();
        if (choice == 0) {
            // 手动输入类名和方法名
            String qualifiedClassNames = Messages.showInputDialog(
                    project,
                    "请输入方法签名的全限定名（例如：com.example.MyClass）：",
                    "输入方法签名",
                    Messages.getQuestionIcon()
            );
            if (qualifiedClassNames == null || qualifiedClassNames.trim().isEmpty()) {
                Messages.showErrorDialog("类名不能为空！", "错误");
                return null;
            }
            methodList.addAll(Arrays.stream(qualifiedClassNames.split(";")).toList());
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
