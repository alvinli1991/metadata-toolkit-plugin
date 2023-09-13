package me.alvin.dev.toolkit.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.xml.XmlFile;
import me.alvin.dev.toolkit.dag.domain.common.LogicDag;
import me.alvin.dev.toolkit.dag.domain.plantuml.StateUml;
import me.alvin.dev.toolkit.dag.service.DagPlantumlStateService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author: Li Xiang
 * Date: 2023/9/8
 * Time: 2:38 PM
 */
public class LppXmlToPlantumlState2Action extends AnAction {
    private static final Logger LOG = Logger.getInstance(LppXmlToPlantumlState2Action.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        //读取选中的xml文件
        Project project = event.getProject();
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        XmlFile xmlFile = (XmlFile) psiFile;
        DagPlantumlStateService dagPlantumlStateService = project.getService(DagPlantumlStateService.class);
        LogicDag logicDag = dagPlantumlStateService.parse(psiFile);
        if (null == logicDag) {
            return;
        }

        StateUml stateUml = dagPlantumlStateService.buildPlantumlState(logicDag);
        if (null == stateUml) {
            return;
        }

        //save to file
        String statePlantUml = stateUml.toPlantuml();
        ApplicationManager.getApplication().runWriteAction(() -> {
            PsiDirectory xmlFileDirectory = xmlFile.getContainingDirectory();
            String fileName = logicDag.getId() + ".puml";
            VirtualFile virtualFile = VfsUtil.findRelativeFile(xmlFileDirectory.getVirtualFile(), fileName);
            if (Objects.isNull(virtualFile)) {
                PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
                PsiFile statePlantUmlFile = psiFileFactory.createFileFromText(fileName
                        , PlainTextFileType.INSTANCE, statePlantUml);

                xmlFileDirectory.add(statePlantUmlFile);
            } else {
                Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                document.setText(statePlantUml);
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        Project project = event.getProject();
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        boolean canProcess = project.getService(DagPlantumlStateService.class).canProcess(psiFile);
        event.getPresentation().setEnabledAndVisible(canProcess);
    }
}
