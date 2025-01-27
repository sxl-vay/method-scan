package top.boking.methodscan.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import top.boking.methodscan.ExportMethodReferencesAction;

import java.util.List;
public class UploadFileAction extends AnAction {
        @Override
        public void actionPerformed(AnActionEvent e) {
            Project project = e.getProject();
            if (DumbService.isDumb(project)) {
                Messages.showErrorDialog("工程索引尚未就绪请等待！", "错误");
                return;
            }
            List<String> methodList = ExportMethodReferencesAction.getMethodListFromFile(project);
            if (methodList == null || methodList.isEmpty()) {
                Messages.showErrorDialog("没有找到要搜索的方法！", "错误");
                return;
            }
            ExportMethodReferencesAction.searchMethodAndWrite(methodList, project);
        }
    }
