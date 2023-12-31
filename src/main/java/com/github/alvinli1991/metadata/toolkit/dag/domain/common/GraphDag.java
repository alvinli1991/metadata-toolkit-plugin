package com.github.alvinli1991.metadata.toolkit.dag.domain.common;

import com.github.alvinli1991.metadata.toolkit.dag.utils.GraphUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedAcyclicGraph;

/**
 * Date: 2023/9/11
 * Time: 2:45 PM
 */
public class GraphDag {

    private final String id;
    /**
     *
     */
    private final Graph<Node, Edge> graphDag;


    public GraphDag(String id) {
        this.id = id;
        this.graphDag = new DirectedAcyclicGraph<>(Edge.class);
    }

    public String getId() {
        return StringUtils.trimToEmpty(id);
    }

    public void addDependency(Node from, Node to) {
        GraphUtils.addEdge(this.graphDag, from, to);
    }


}
