package com.github.alvinli1991.metadata.toolkit.dag.service.impl;

import com.github.alvinli1991.metadata.toolkit.dag.domain.common.LogicDag;
import com.github.alvinli1991.metadata.toolkit.dag.domain.common.Node;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.MsKey;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.MsNodeType;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.xml.DagGraph;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.xml.Depend;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.xml.Stage;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.xml.Unit;
import com.github.alvinli1991.metadata.toolkit.dag.domain.plantuml.State;
import com.github.alvinli1991.metadata.toolkit.dag.domain.plantuml.StateRelation;
import com.github.alvinli1991.metadata.toolkit.dag.domain.plantuml.StateUml;
import com.github.alvinli1991.metadata.toolkit.dag.service.DagPlantumlStateService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Date: 2023/9/12
 * Time: 23:44
 */
public class MsDagPlantumlStateService implements DagPlantumlStateService {
    private final Project project;

    public MsDagPlantumlStateService(Project project) {
        this.project = project;
    }


    @Override
    public LogicDag parse(PsiFile psiFile) {
        XmlFile xmlFile = (XmlFile) psiFile;
        DomManager domManager = DomManager.getDomManager(project);
        DomFileElement<DagGraph> dagFile = domManager.getFileElement(xmlFile, DagGraph.class);
        if (Objects.isNull(dagFile)) {
            return null;
        }
        DagGraph dagGraph = dagFile.getRootElement();
        List<Unit> units = dagGraph.getUnits().getUnits();
        List<Stage> stages = dagGraph.getStages().getStages();
        if (CollectionUtils.isEmpty(units) || CollectionUtils.isEmpty(stages)) {
            return null;
        }

        //解析node&edge
        LogicDag logicDag = new LogicDag(dagGraph.getDagId().getValue());
        //add node
        units.stream()
                .map(unit -> {
                    //创建action node
                    Node actionNode = new Node(unit.getId().getValue(), MsNodeType.Action.name(), unit.getDescription().getValue());
                    actionNode.putData(MsKey.clz.name(), unit.getClz().getClzName());
                    return actionNode;
                })
                .forEach(logicDag::addNode);
        stages.stream()
                .map(stage -> {
                    //创建stage node
                    Node stageNode = new Node(stage.getStageId(), MsNodeType.Stage.name(), stage.getStageDesc().getValue());
                    //设置node所属的stage
                    stage.getFlows().getFlows().forEach(flow -> {
                        logicDag.getNodeById(flow.getFrom().getValue())
                                .ifPresent(node -> node.putData(MsKey.stage.name(), stage.getStageId()));
                        logicDag.getNodeById(flow.getTo().getValue())
                                .ifPresent(node -> node.putData(MsKey.stage.name(), stage.getStageId()));
                    });
                    return stageNode;
                })
                .forEach(logicDag::addNode);

        //add edge
        stages.forEach(stage -> {
            //add stage edge
            List<Depend> depends = stage.getDepends().getDepends();
            if (CollectionUtils.isNotEmpty(depends)) {
                logicDag.getNodeById(stage.getStageId())
                        .ifPresent(to -> depends.forEach(depend -> {
                            Node from = logicDag.getNodeById(depend.getStageId()).orElse(null);
                            logicDag.addEdge(from, to);
                        }));
            } else {
                logicDag.getNodeById(stage.getStageId())
                        .ifPresent(from -> logicDag.addEdge(from, null));
            }
            //add node edge
            stage.getFlows().getFlows()
                    .forEach(flow -> logicDag.getNodeById(flow.getFrom().getValue())
                            .ifPresent(from -> {
                                        Node to = logicDag.getNodeById(flow.getTo().getValue()).orElse(null);
                                        logicDag.addEdge(from, to);
                                    }
                            ));
        });

        return logicDag;
    }

    @Override
    public StateUml buildPlantumlState(LogicDag logicDag) {
        //create  state
        Map<String, List<Node>> stageIdWithNodes = logicDag.getNodes()
                .stream()
                .filter(node -> StringUtils.equals(MsNodeType.Action.name(), node.getType()))
                .filter(node -> StringUtils.isNotBlank(node.getDataValue(MsKey.stage.name(), "")))
                .collect(Collectors.groupingBy(node -> node.getDataValue(MsKey.stage.name(), ""), Collectors.toList()));

        List<State> states = stageIdWithNodes.entrySet()
                .stream()
                .map(entry -> {
                    String stageId = entry.getKey();
                    List<Node> stageChildren = entry.getValue();
                    return logicDag.getNodeById(stageId)
                            .map(stageNode -> {
                                        State.StateBuilder builder = State.builder();
                                        builder.name(stageNode.getId())
                                                .description(MsKey.desc.name(), stageNode.getDesc());
                                        stageChildren.forEach(child -> {
                                            State.StateBuilder childBuilder = State.builder();
                                            childBuilder.name(child.getId())
                                                    .description(MsKey.desc.name(), child.getDesc())
                                                    .description(MsKey.clz.name(), child.getDataValue(MsKey.clz.name(), ""));
                                            builder.child(childBuilder.build());
                                        });
                                        return builder.build();
                                    }
                            ).orElse(null);

                })
                .filter(Objects::nonNull)
                .toList();

        //create relation
        List<StateRelation> stateRelations = logicDag.getEdges()
                .stream()
                .map(edge -> {
                    if (null == edge.getSource() || null == edge.getTarget()) {
                        return null;
                    }
                    return StateRelation.builder()
                            .from(Optional.ofNullable(edge.getSource())
                                    .map(source -> State.builder().name(source.getId()).build())
                                    .orElse(null))
                            .to(Optional.ofNullable(edge.getTarget())
                                    .map(source -> State.builder().name(source.getId()).build())
                                    .orElse(null))
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        return StateUml.builder()
                .states(states)
                .stateRelations(stateRelations)
                .build();
    }
}
