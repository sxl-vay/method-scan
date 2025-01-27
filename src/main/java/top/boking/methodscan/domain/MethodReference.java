package top.boking.methodscan.domain;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.Data;

@Data
public class MethodReference {
    /**
     * IDEA Copy Reference 路径
     */
    private String reference;

    private String className;

    private String methodName;

    private String[] paramTypes;

    private boolean hasOverloaded;

    private int paramStartIndex;
    private int paramEndIndex;

    public MethodReference(String reference) {
        this.reference = reference;
        // 拆分字符串：类名和方法+参数部分
        int hashIndex = reference.indexOf('#');
        if (hashIndex == -1 || hashIndex == reference.length() - 1) {
            Messages.showErrorDialog("引用字符串格式无效：" + reference, "错误");
        }
        className = reference.substring(0, hashIndex);
        String methodAndParams = reference.substring(hashIndex + 1);
        // 提取方法名和参数部分
        paramStartIndex = methodAndParams.indexOf('(');
        paramEndIndex = methodAndParams.indexOf(')');

        if (paramStartIndex != -1 && paramEndIndex != -1) {
            hasOverloaded = true;
            methodName = methodAndParams.substring(0, paramStartIndex);
            String paramString = methodAndParams.substring(paramStartIndex + 1, paramEndIndex);
            // 解析参数类型列表
            paramTypes = paramString.isEmpty() ? new String[0] : paramString.split(",");
        } else {
            methodName = methodAndParams;
        }
    }


    /**
     * 查找类中的指定方法，并根据用户选择过滤重载方法。
     * 如果有重载方法必须指定参数
     *
     * @param project 当前项目
     * @return 用户选择的 PsiMethod（如果有）
     */
    public PsiMethod findSpecificMethod(Project project) {
        // 查找类
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
//            Messages.showErrorDialog("找不到类：" + className, "错误");
            return null;
        }
        // 获取所有同名方法（包括重载）
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        // 根据选择找到对应的 PsiMethod
        for (PsiMethod method : methods) {
            if (isMethodMatch(method)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 获取方法的签名（方法名 + 参数类型）。
     *
     * @param method PsiMethod
     * @return 方法签名
     */
    private String getMethodSignature(PsiMethod method) {
        StringBuilder signature = new StringBuilder(method.getName());
        signature.append("(");

        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiType type = parameters[i].getType();
            signature.append(type.getPresentableText());
            if (i < parameters.length - 1) {
                signature.append(", ");
            }
        }
        signature.append(")");
        return signature.toString();
    }

    /**
     * 检查方法是否与参数类型列表匹配
     *
     * @param method     PsiMethod
     * @return 是否匹配
     */
    private boolean isMethodMatch(PsiMethod method) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        //todo 这里的判断应该是无意义的,如果 paramTypes == null 代表这个方法没有重载,那么前面就已经返回了
        if (paramTypes == null && parameters.length == 0) {
            return true;
        }
        if (paramTypes == null && !hasOverloaded) {
            return true;
        }
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
