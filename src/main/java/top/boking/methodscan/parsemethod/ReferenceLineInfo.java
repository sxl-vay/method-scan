package top.boking.methodscan.parsemethod;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.fileEditor.FileDocumentManager;

public class ReferenceLineInfo {

    public static int getLineNumber(PsiElement element) {
        // 获取引用所在的文件
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) return -1;
        // 获取 VirtualFile 和 Document
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) return -1;
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) return -1;
        // 获取引用的文本偏移量并计算行号
        int offset = element.getTextOffset();
        return document.getLineNumber(offset) + 1; // 行号从 0 开始，+1 转为人类可读
    }
}
