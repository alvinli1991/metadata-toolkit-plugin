package com.github.alvinli1991.metadata.toolkit.dag.service;

import com.github.alvinli1991.metadata.toolkit.dag.domain.common.LogicDag;
import com.github.alvinli1991.metadata.toolkit.dag.domain.common.Node;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.MsKey;
import com.github.alvinli1991.metadata.toolkit.dag.domain.ms.MsNodeType;
import com.github.alvinli1991.metadata.toolkit.dag.domain.plantuml.State;
import com.github.alvinli1991.metadata.toolkit.dag.domain.plantuml.StateRelation;
import com.github.alvinli1991.metadata.toolkit.dag.domain.plantuml.StateUml;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Date: 2023/9/12
 * Time: 23:41
 */
public interface DagPlantumlStateService {


    /**
     * parse file to DAG
     *
     * @param psiFile
     * @return
     */
    LogicDag parse(PsiFile psiFile);

    default StateUml buildPlantumlState(LogicDag logicDag) {
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
