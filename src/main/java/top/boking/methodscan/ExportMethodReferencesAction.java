package top.boking.methodscan;

import com.alibaba.excel.EasyExcel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ExportMethodReferencesAction {

    public static void searchMethodAndWrite(List<String> methodList, Project project) {
        File file = ExcelFileCreator.createExcelFileInSelectedDirectory(project);
        if (file == null) return;
        
        ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Backgroundable(project, "扫描方法引用", true) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("正在扫描方法引用...");
                Map<String, List<ExcelModel>> dataMap = new LinkedHashMap<>();
                int total = methodList.size();
                int currentOut = 0;
                for (String methodInfo : methodList) {
                    List<ExcelModel> datas = new ArrayList<>();
                    MethodReference methodReference = new MethodReference(methodInfo);
                    String className = methodReference.getClassName();
                    // 查找类
                    PsiClass psiClass = JavaPsiFacade.getInstance(project)
                            .findClass(className, GlobalSearchScope.allScope(project));
                    PsiMethod targetMethod = methodReference.findSpecificMethod(project);
                    if (psiClass == null || targetMethod == null) {
                        ExcelModel excelModel = new ExcelModel();
                        excelModel.setSignature(methodInfo);
                        excelModel.setSignature(methodInfo);
                        excelModel.setReference(psiClass == null ? "找不到类" : "找不到方法");
                        datas.add(excelModel);
                        dataMap.put(methodInfo, datas);
                        continue;
                    }
                    // 查找引用
                    Collection<PsiReference> refs = ReferencesSearch.search(targetMethod).findAll();
                    int currentInner = 0;

                    for (PsiReference reference : refs) {
                        double littleProcess = ++currentInner * ((double) 1 / (refs.size()*methodList.size()));
                        double out = ((double) currentOut / total);
                        double all = out + littleProcess;
                        updateProcess(indicator, methodInfo, all, currentOut, total);
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
                        String commitAuthor = null;
                        try {
                            //这里可能会阻塞EDT
                            commitAuthor = GitCommitInfo.getCommitAuthor(project, virtualFile, location);
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // 写入 Excel
                        String referenceStr = qualifiedName + "." + refMethodName;
                        excelModel.setCodeLineNumber(location);
                        excelModel.setReference(referenceStr);
                        excelModel.setAuthor(commitAuthor);
                        String codeSnippet = element.getText();
                        excelModel.setCodeSnippet(codeSnippet);
                        datas.add(excelModel);
                    }
                    dataMap.put(methodInfo, datas);
                    if (refs.isEmpty()) {
                        double all = (double) currentOut / total;
                        updateProcess(indicator, methodInfo, all, currentOut, total);
                    }
                    ++currentOut;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        saveFile(dataMap, file);
                    });
                });
            }

            private static void updateProcess(ProgressIndicator indicator, String methodInfo, double all, int currentOut, int total) {
                indicator.setFraction(all);
                indicator.setText2(String.format("处理进度: %d/%d - %s", currentOut, total, methodInfo));
            }
        });
    }

    private static void saveFile(Map<String, List<ExcelModel>> datas, File file) {
        // 保存 Excel 文件
        try {
            Collection<?> dataList = parseData(datas);
            EasyExcel.write(file, ExcelModel.class)
                    .sheet("模板")
                    .registerWriteHandler(new CustomLoopMergeStrategy(dataList.size()))
                    .doWrite(dataList);
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showInfoMessage("方法引用导出成功！文件已保存为：" + file.getName(), "成功");
            });
        } catch (Exception ex) {
            ex.printStackTrace();
//            Messages.showErrorDialog("导出 Excel 文件失败：" + ex.getMessage(), "错误");
        }
    }

    private static Collection<?> parseData(Map<String, List<ExcelModel>> datas) {
        return datas.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }


    public static @Nullable List<String> getMethodListFromFile(Project project) {
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

        List<String> methodList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                methodList.add(line);
            }
        } catch (IOException ex) {
            Messages.showErrorDialog("文件解析失败：" + ex.getMessage(), "错误");
            return null;
        }
        return methodList;
    }
}
