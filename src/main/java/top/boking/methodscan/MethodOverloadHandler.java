package top.boking.methodscan;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.List;

public class MethodOverloadHandler {

    /**
     * 查找类中的指定方法，并根据用户选择过滤重载方法。
     *
     * @param project 当前项目
     * @param qualifiedClassName 类的全限定名
     * @param methodName 方法名
     * @return 用户选择的 PsiMethod（如果有）
     */
    public static PsiMethod findSpecificMethod(Project project, String qualifiedClassName, String methodName) {
        // 查找类
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(qualifiedClassName, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            Messages.showErrorDialog("找不到类：" + qualifiedClassName, "错误");
            return null;
        }

        // 获取所有同名方法（包括重载）
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        if (methods.length == 0) {
            Messages.showErrorDialog("找不到方法：" + methodName, "错误");
            return null;
        }

        // 如果只有一个方法，直接返回
        if (methods.length == 1) {
            return methods[0];
        }

        // 如果有多个重载，列出供用户选择
        List<String> methodSignatures = new ArrayList<>();
        for (PsiMethod method : methods) {
            StringBuilder signature = new StringBuilder(method.getName());
            signature.append("(");

            // 拼接参数类型
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (int i = 0; i < parameters.length; i++) {
                PsiType type = parameters[i].getType();
                if (type instanceof PsiPrimitiveType) {

                } else if (type instanceof PsiClassType) {
                    PsiClassType psiClassType = (PsiClassType) type;
                    PsiClass resolve = psiClassType.resolve();
                    String qualifiedName = resolve.getQualifiedName();

                }
                signature.append(type.getPresentableText());
                if (i < parameters.length - 1) {
                    signature.append(", ");
                }
            }
            signature.append(")");
            methodSignatures.add(signature.toString());
        }

        // 弹出选择框供用户选择重载方法
        String selectedSignature = Messages.showEditableChooseDialog(
                "请选择具体的重载方法：",
                "选择方法重载",
                Messages.getQuestionIcon(),
                methodSignatures.toArray(new String[0]),
                methodSignatures.get(0), // 默认选择第一个
                null
        );

        if (selectedSignature == null) {
            Messages.showInfoMessage("操作已取消。", "取消");
            return null;
        }

        // 根据选择找到对应的 PsiMethod
        for (PsiMethod method : methods) {
            if (getMethodSignature(method).equals(selectedSignature)) {
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
    private static String getMethodSignature(PsiMethod method) {
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
}
