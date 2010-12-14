/*
 * Copyright  2010 Pieter van der Meer (pieter@pieni.nl)
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

package nl.pieni.maven.dependency_analyzer.neo4j.node.factory;

import nl.pieni.maven.dependency_analyzer.database.DependencyDatabase;
import nl.pieni.maven.dependency_analyzer.enums.ArtifactRelations;
import nl.pieni.maven.dependency_analyzer.neo4j.node.ArtifactNodeDecorator;
import nl.pieni.maven.dependency_analyzer.neo4j.node.GroupNodeDecorator;
import nl.pieni.maven.dependency_analyzer.node.ArtifactNode;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import static nl.pieni.maven.dependency_analyzer.neo4j.enums.NodeProperties.ARTIFACT_ID;


/**
 * An artifact Node
 */
public class ArtifactNodeFactory extends AbstractNodeFactory<ArtifactNode> {

    /**
     * {@inheritDoc}
     */
    public ArtifactNodeFactory(DependencyDatabase<GraphDatabaseService, Node> database, final Log logger) {
        super(database, logger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ArtifactNode create(@NotNull final Dependency dependency) {
        getDatabase().startTransaction();
        Node node = getDatabase().createNode();
        ArtifactNode artifactNode = new ArtifactNodeDecorator(node, dependency);
        getDatabase().indexOnProperty(node, ARTIFACT_ID);
        LOGGER.info("Create ArtifactNode: " + artifactNode);
        getDatabase().stopTransaction();
        return artifactNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insert(@NotNull final Dependency dependency) {
        int nodeCount = 0;
        GroupNodeDecorator groupNode = (GroupNodeDecorator)getDatabase().findGroupNode(dependency);
        ArtifactNodeDecorator artifactNode = (ArtifactNodeDecorator)getDatabase().findArtifactNode(dependency);
        if (artifactNode == null) {
            artifactNode = (ArtifactNodeDecorator)create(dependency);
            nodeCount++;
            getDatabase().startTransaction();
            groupNode.createRelationshipTo(artifactNode, ArtifactRelations.has);
            LOGGER.info("Created relation " + ArtifactRelations.has + "between " + groupNode + " and " + artifactNode);
            getDatabase().stopTransaction();
        }
        return nodeCount;
    }
}