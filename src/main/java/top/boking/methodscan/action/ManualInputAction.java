package top.boking.methodscan.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;
import top.boking.methodscan.ExportMethodReferencesAction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ManualInputAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (DumbService.isDumb(project)) {
            Messages.showErrorDialog("工程索引尚未就绪请等待！", "错误");
            return;
        }
        List<String> methodList = getMethodListFromManualInput(project);
        if (methodList == null || methodList.isEmpty()) {
            Messages.showErrorDialog("没有找到要搜索的方法！", "错误");
            return;
        }
        ExportMethodReferencesAction.searchMethodAndWrite(methodList, project);
    }

    public @Nullable List<String> getMethodListFromManualInput(Project project) {
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
        return Arrays.stream(qualifiedClassNames.split(";")).collect(Collectors.toList());
    }

}