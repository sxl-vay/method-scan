package top.boking.methodscan.parsemethod;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

public class ReferenceResolver {

    /**
     * 根据 IDEA Copy Reference 字符串解析并定位唯一方法
     *
     * @param project 当前项目
     * @param reference Copy Reference 字符串（例如 "tok.boking.collectionanmap.MapTest#get(tok.boking.enumdemo.TestEnum)"）
     * @return 定位到的 PsiMethod（如果存在）
     */
    public PsiMethod resolveMethodFromReference(Project project, String reference) {
        if (reference == null || !reference.contains("#")) {
            throw new IllegalArgumentException("引用字符串格式无效：" + reference);
        }

        // 拆分类名和方法部分
        String[] parts = reference.split("#");
        if (parts.length != 2) {
            throw new IllegalArgumentException("引用字符串格式无效：" + reference);
        }

        String className = parts[0];
        String methodWithParams = parts[1];

        // 查找类
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            throw new IllegalArgumentException("未找到类：" + className);
        }

        // 利用 PsiResolveHelper 查找方法
        PsiResolveHelper resolveHelper = JavaPsiFacade.getInstance(project).getResolveHelper();
        for (PsiMethod method : psiClass.getMethods()) {
            if (matchesMethod(method, methodWithParams)) {
                return method;
            }
        }

        throw new IllegalArgumentException("未找到匹配的方法：" + reference);
    }

    /**
     * 检查方法是否与字符串中的方法签名匹配
     *
     * @param method PsiMethod
     * @param methodWithParams 方法签名部分（例如 "get(tok.boking.enumdemo.TestEnum)"）
     * @return 是否匹配
     */
    private boolean matchesMethod(PsiMethod method, String methodWithParams) {
        // 提取方法名和参数
        int paramStartIndex = methodWithParams.indexOf('(');
        int paramEndIndex = methodWithParams.indexOf(')');
        if (paramStartIndex == -1 || paramEndIndex == -1) {
            return false;
        }

        String methodName = methodWithParams.substring(0, paramStartIndex);
        String paramString = methodWithParams.substring(paramStartIndex + 1, paramEndIndex);

        if (!method.getName().equals(methodName)) {
            return false; // 方法名不匹配
        }

        // 解析参数列表
        PsiParameter[] parameters = method.getParameterList().getParameters();
        String[] paramTypes = paramString.isEmpty() ? new String[0] : paramString.split(",");
        if (parameters.length != paramTypes.length) {
            return false; // 参数数量不匹配
        }

        // 比较参数类型
        for (int i = 0; i < parameters.length; i++) {
            String expectedType = paramTypes[i].trim();
            String actualType = parameters[i].getType().getCanonicalText();
            if (!actualType.equals(expectedType)) {
                return false; // 参数类型不匹配
            }
        }

        return true; // 完全匹配
    }
}
