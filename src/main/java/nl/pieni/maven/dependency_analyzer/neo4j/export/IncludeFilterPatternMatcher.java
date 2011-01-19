/*
 * Copyright (c) 2011 Pieter van der Meer (pieter@pieni.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.pieni.maven.dependency_analyzer.neo4j.export;

import nl.pieni.maven.dependency_analyzer.filter.GAVIncludeFilter;
import nl.pieni.maven.dependency_analyzer.neo4j.enums.ArtifactRelations;
import nl.pieni.maven.dependency_analyzer.neo4j.enums.NodeProperties;
import nl.pieni.maven.dependency_analyzer.neo4j.enums.NodeType;
import nl.pieni.maven.dependency_analyzer.neo4j.node.ArtifactNodeDecorator;
import nl.pieni.maven.dependency_analyzer.neo4j.node.GroupNodeDecorator;
import nl.pieni.maven.dependency_analyzer.neo4j.node.VersionNodeDecorator;
import nl.pieni.maven.dependency_analyzer.node.ArtifactNode;
import nl.pieni.maven.dependency_analyzer.node.GroupNode;
import nl.pieni.maven.dependency_analyzer.node.VersionNode;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: pieter
 * Date: 18-1-11
 * Time: 21:20
 * To change this template use File | Settings | File Templates.
 */
class IncludeFilterPatternMatcher {
    private final GAVIncludeFilter gavIncludeFilter;

    public IncludeFilterPatternMatcher(List<String> includeFilterPatterns) {
        this.gavIncludeFilter = new GAVIncludeFilter(includeFilterPatterns);
    }

    public boolean include(Node node) {
        if (!node.hasProperty(NodeProperties.NODE_TYPE)) {
            throw new IllegalArgumentException("Matcher can not be called with a non Dependency node");
        }

        NodeType type = NodeType.fromString((String) node.getProperty(NodeProperties.NODE_TYPE));

        switch (type) {
            case VersionNode:
                return matchVersionNode(node);
            case ArtifactNode:
                return matchArtifactNode(node);
            case GroupNode:
                return matchGroupNode(node);
            default:
                return false;
        }
    }

    private boolean matchGroupNode(Node node) {
        GroupNodeDecorator groupNode = new GroupNodeDecorator(node);
        String gav = groupNode.getGroupId();
        if (gavIncludeFilter.filter(gav)) {
            return true;
        }

        //Due to the structure of the DB "nl.pieni.maven" is stored as: nl -> nl.pieni -> nl.pieni.maven
        //So a false match needs some more work to be false
        return hasMatchFurtherDown(groupNode);

    }

    private boolean hasMatchFurtherDown(GroupNodeDecorator groupNodeDecorator) {

        Iterable<Relationship> relationships = groupNodeDecorator.getRelationships(ArtifactRelations.has, Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            Node otherNode = relationship.getOtherNode(groupNodeDecorator);
            if (NodeType.ArtifactNode == NodeType.fromString((String)otherNode.getProperty(NodeProperties.NODE_TYPE.toString()))) {
                return matchArtifactNode(otherNode);
            }

            return matchGroupNode(otherNode);
        }

        return false;
    }

    private boolean matchArtifactNode(Node node) {
        ArtifactNode artifactNode = new ArtifactNodeDecorator(node);
        GroupNode groupNode = artifactNode.getParent();
        String gav = groupNode.getGroupId() + ":" + artifactNode.getArtifactId();
        return gavIncludeFilter.filter(gav);
    }

    private boolean matchVersionNode(Node node) {
        VersionNode versionNode = new VersionNodeDecorator(node);
        ArtifactNode artifactNode = versionNode.getParent();
        GroupNode groupNode = artifactNode.getParent();
        String gav = groupNode.getGroupId() + ":" + artifactNode.getArtifactId() + ":" + versionNode.getVersion();
        return gavIncludeFilter.filter(gav);
    }

}
