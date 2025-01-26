package top.boking.methodscan.parsemethod;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

public class MethodLocator {

    /**
     * 根据 IDEA Copy Reference 字符串定位唯一方法
     *
     * @param project 当前项目
     * @param reference IDEA Copy Reference 字符串
     * @return 定位到的 PsiMethod（如果存在）
     */
    public PsiMethod locateMethodFromReference(Project project, String reference) {
        if (reference == null || reference.isEmpty()) {
            Messages.showErrorDialog("引用字符串不能为空！", "错误");
            return null;
        }

        try {
            // 拆分字符串：类名和方法+参数部分
            int hashIndex = reference.indexOf('#');
            if (hashIndex == -1 || hashIndex == reference.length() - 1) {
                Messages.showErrorDialog("引用字符串格式无效：" + reference, "错误");
                return null;
            }

            String className = reference.substring(0, hashIndex);
            String methodAndParams = reference.substring(hashIndex + 1);

            // 提取方法名和参数部分
            int paramStartIndex = methodAndParams.indexOf('(');
            int paramEndIndex = methodAndParams.indexOf(')');
            if (paramStartIndex == -1 || paramEndIndex == -1 || paramEndIndex <= paramStartIndex) {
                Messages.showErrorDialog("引用字符串格式无效：" + methodAndParams, "错误");
                return null;
            }

            String methodName = methodAndParams.substring(0, paramStartIndex);
            String paramString = methodAndParams.substring(paramStartIndex + 1, paramEndIndex);

            // 解析参数类型列表
            String[] paramTypes = paramString.isEmpty() ? new String[0] : paramString.split(",");

            // 查找类
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(className, GlobalSearchScope.allScope(project));
            if (psiClass == null) {
                Messages.showErrorDialog("未找到类：" + className, "错误");
                return null;
            }

            // 查找方法
            PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
            for (PsiMethod method : methods) {
                if (isMethodMatch(method, paramTypes)) {
                    return method;
                }
            }

            Messages.showErrorDialog("未找到匹配的方法：" + reference, "错误");
            return null;

        } catch (Exception e) {
            Messages.showErrorDialog("解析引用字符串时出错：" + e.getMessage(), "错误");
            return null;
        }
    }

    /**
     * 检查方法是否与参数类型列表匹配
     *
     * @param method PsiMethod
     * @param paramTypes 参数类型列表（全限定名）
     * @return 是否匹配
     */
    private boolean isMethodMatch(PsiMethod method, String[] paramTypes) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length != paramTypes.length) {
            return false; // 参数数量不同
        }

        for (int i = 0; i < parameters.length; i++) {
            PsiType paramType = parameters[i].getType();
            if (!paramType.getCanonicalText().equals(paramTypes[i].trim())) {
                return false; // 参数类型不同
            }
        }

        return true; // 参数完全匹配
    }
}
