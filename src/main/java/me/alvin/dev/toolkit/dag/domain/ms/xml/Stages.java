package me.alvin.dev.toolkit.dag.domain.ms.xml;

import java.util.List;

/**
 * Date: 2023/9/11
 * Time: 2:07 PM
 */
public interface Stages extends com.intellij.util.xml.DomElement {
    List<Stage> getStages();
}
