package top.boking.methodscan.parsemethod;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.annotate.GitAnnotationProvider;
import git4idea.repo.GitRepository;


public class GitCommitInfo {

    /**
     * 获取指定文件某行代码的提交者信息。
     *
     * @param project    当前项目
     * @param virtualFile 文件的虚拟路径
     * @param lineNumber 行号（从 1 开始）
     * @return 提交者的名称
     */
    public static String getCommitAuthor(Project project, VirtualFile virtualFile, int lineNumber) {
        try {
            // 获取 Git 注释（Blame）信息
            GitAnnotationProvider annotationProvider = new GitAnnotationProvider(project);
            FileAnnotation annotation = null;
            try {
                annotation = annotationProvider.annotate(virtualFile);
            } catch (VcsException e) {
                throw new RuntimeException(e);
            }
            // 确保行号有效
            if (annotation != null && lineNumber > 0 && lineNumber <= annotation.getLineCount()) {
                VcsRevisionNumber revisionNumber = annotation.getLineRevisionNumber(lineNumber - 1); // 行号从 0 开始
                String htmlToolTip = annotation.getHtmlToolTip(lineNumber - 1);
                int i = htmlToolTip.indexOf("Author:");
                String substring = htmlToolTip.substring(i);
                int i1 = substring.indexOf("&lt;");
                String authorName = substring.substring("Author: ".length(), i1);
                return authorName;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "错误：" + e.getMessage();
        }
        return "未找到提交者";
    }
}
