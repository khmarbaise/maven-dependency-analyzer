/*
 * Copyright (c) 2010 Pieter van der Meer (pieter@pieni.nl)
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

package nl.pieni.maven.dependency_analyzer.neo4j.database;

import nl.pieni.maven.dependency_analyzer.database.DependencyDatabase;
import nl.pieni.maven.dependency_analyzer.database.DependencyDatabaseSearcher;
import nl.pieni.maven.dependency_analyzer.database.DependencyNodeProcessor;
import nl.pieni.maven.dependency_analyzer.enums.DependencyScopeRelations;
import nl.pieni.maven.dependency_analyzer.neo4j.node.ArtifactNodeDecorator;
import nl.pieni.maven.dependency_analyzer.neo4j.node.GroupNodeDecorator;
import nl.pieni.maven.dependency_analyzer.neo4j.node.VersionNodeDecorator;
import nl.pieni.maven.dependency_analyzer.node.ArtifactNode;
import nl.pieni.maven.dependency_analyzer.node.VersionNode;
import org.apache.maven.model.Dependency;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: pieter
 * Date: 21-12-10
 * Time: 19:55
 * To change this template use File | Settings | File Templates.
 */
public class DependencyDatabaseSearcherImplNonBreakingTest extends AbstractDatabaseImplTest {

    private static DependencyDatabase<GraphDatabaseService, Node> database;
    private static DependencyDatabaseSearcher<Node> searcher;
    private static int dependencyCnt = 0;


    @BeforeClass
    public static void beforeClass() throws IOException {
        beforeBase();
        database = new DependencyDatabaseImpl(log, getDBDirectory());
        searcher = new DependencyDatabaseSearcherImpl(log, database);

//        //Create the dependencies used
//        dependencyA = new Dependency();
//        dependencyA.setArtifactId("artifactId_A");
//        dependencyA.setGroupId("groupId_A");
//        dependencyA.setVersion("1.0");
//        dependencyA.setType("jar");
//
//
//        dependencyA2 = new Dependency();
//        dependencyA2.setArtifactId("artifactId_A");
//        dependencyA2.setGroupId("groupId_A");
//        dependencyA2.setVersion("2.0");
//        dependencyA2.setType("jar");
//
//
//        dependencyB = new Dependency();
//        dependencyB.setArtifactId("artifactId_B");
//        dependencyB.setGroupId("groupId_B");
//        dependencyB.setVersion("1.0");
//        dependencyB.setType("jar");
    }

    @AfterClass
    public static void afterClass() {
        try {
            database.shutdownDatabase();
            searcher.shutdownSearcher();
            afterBase();
        } finally {
            System.out.println("Done.");
        }
    }

    private Dependency getDependency() {
        return getDependency(null);
    }

    private Dependency getDependency(String version) {
        Dependency dependency = new Dependency();
        dependency.setArtifactId("artifactId_" + dependencyCnt);
        dependency.setGroupId("groupId_" + dependencyCnt);
        if (version == null) {
            dependency.setVersion("1.0");
        } else {
            dependency.setVersion(version);
        }
        dependency.setType("jar");
        dependencyCnt++;
        return dependency;
    }

    @Test
    public void findGroupNodeEmptyDBTest() {
        Dependency dependency = getDependency();
        GroupNodeDecorator node = (GroupNodeDecorator) searcher.findGroupNode(dependency);
        assertNull(node);
    }

    @Test
    public void findGroupNodeTest() {
        Dependency dependency = getDependency();

        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);
        processor.addArtifact(dependency);
        GroupNodeDecorator node = (GroupNodeDecorator) searcher.findGroupNode(dependency);
        assertNotNull(node);
        assertTrue(node.getGroupId().equals(dependency.getGroupId()));
    }

    @Test
    public void findArtifactNodeTest() {
        Dependency dependency = getDependency();

        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);
        processor.addArtifact(dependency);
        ArtifactNodeDecorator node = (ArtifactNodeDecorator) searcher.findArtifactNode(dependency);
        assertNotNull(node);
        assertTrue(node.getArtifactId().equals(dependency.getArtifactId()));
    }

    @Test
    public void findVersionNodeTest() {
        Dependency dependency = getDependency();
        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);
        processor.addArtifact(dependency);
        VersionNodeDecorator node = (VersionNodeDecorator) searcher.findVersionNode(dependency);
        assertNotNull(node);
        assertTrue(node.getVersion().equals(dependency.getVersion()));

    }

    @Test
    public void getVersionNodesSingleResultTest() {
        Dependency dependency = getDependency();
        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);
        processor.addArtifact(dependency);
        List<VersionNode> versionNodes = searcher.getVersionNodes(dependency);
        assertNotNull(versionNodes);
        assertEquals(1, versionNodes.size());
        assertTrue(versionNodes.get(0).getVersion().equals(dependency.getVersion()));
    }

    @Test
    public void getVersionNodesMultiResultTest() {
        Dependency dependencyA = getDependency();
        Dependency dependencyA2 = dependencyA.clone();
        dependencyA2.setVersion("2.0");
        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);
        processor.addArtifact(dependencyA);
        processor.addArtifact(dependencyA2);
        List<VersionNode> versionNodes = searcher.getVersionNodes(dependencyA);
        assertNotNull(versionNodes);
        assertEquals(2, versionNodes.size());
        assertTrue(versionNodes.get(0).getVersion().equals(dependencyA.getVersion()));
        assertTrue(versionNodes.get(1).getVersion().equals(dependencyA2.getVersion()));
    }

    @Test
    public void getVersionNodesNonProcessedDependency() {
        Dependency dependency = getDependency();
        List<VersionNode> versionNodes = searcher.getVersionNodes(dependency);
        assertNotNull(versionNodes);
        assertEquals(0, versionNodes.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDependingArtifactsBadScope() {
        Dependency dependencyA = getDependency();
        Dependency dependencyB = getDependency();
        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);
        processor.addArtifact(dependencyA);
        processor.addArtifact(dependencyB);
        processor.addRelation(dependencyB, dependencyA);
    }

    @Test
    public void getDependingArtifacts() {
        Dependency dependencyA = getDependency();
        dependencyA.setScope("compile");
        Dependency dependencyB = getDependency();

        Map<DependencyScopeRelations, List<ArtifactNode>> result;
        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);
        processor.addArtifact(dependencyA);
        processor.addArtifact(dependencyB);
        processor.addRelation(dependencyB, dependencyA);
        result = searcher.getDependingArtifacts(dependencyA);
        assertTrue(result.containsKey(DependencyScopeRelations.compile));
        assertTrue(result.get(DependencyScopeRelations.compile).size() == 1);
        assertTrue(result.get(DependencyScopeRelations.compile).get(0).getArtifactId().equals(dependencyB.getArtifactId()));
    }

    @Test
    public void getDependingArtifactsNotFoundDependency() {
        Dependency dependency = getDependency();
        Map<DependencyScopeRelations, List<ArtifactNode>> result;
        result = searcher.getDependingArtifacts(dependency);
        for (DependencyScopeRelations dependencyScopeRelations : result.keySet()) {
            assertTrue(result.get(dependencyScopeRelations).size() == 0);
        }
    }

    @Test
    public void getVersionDependenciesTest() {
        Dependency dependencyA = getDependency();
        dependencyA.setScope("compile");
        Dependency dependencyB = getDependency();

        Map<VersionNode, List<VersionNode>> result;
        DependencyNodeProcessor processor = new DependencyNodeProcessorImpl(database, searcher, log);

        processor.addArtifact(dependencyA);
        processor.addArtifact(dependencyB);
        processor.addRelation(dependencyB, dependencyA);
        result = searcher.getVersionDependencies(dependencyA);
        assertEquals(1, result.keySet().size());
        for (VersionNode versionNode : result.keySet()) {
            assertTrue(versionNode.getVersion().equals("1.0"));
            VersionNode otherVersionNode = result.get(versionNode).get(0);
            assertTrue(otherVersionNode.getVersion().equals("1.0"));
        }
    }


    @Test
    public void getVersionDependenciesEmptyTest() {
        Dependency dependency = getDependency();
        Map<VersionNode, List<VersionNode>> result;
        result = searcher.getVersionDependencies(dependency);
        assertEquals(0, result.keySet().size());
    }
}