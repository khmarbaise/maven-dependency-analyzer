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
package nl.pieni.maven.dependency_analyzer.export;

import nl.pieni.maven.dependency_analyzer.database.DependencyDatabaseSearcher;
import nl.pieni.maven.dependency_analyzer.export.report.DependencyReport;
import nl.pieni.maven.dependency_analyzer.neo4j.enums.ScopedRelation;
import nl.pieni.maven.dependency_analyzer.neo4j.export.report.DependencyReportImpl;
import nl.pieni.maven.dependency_analyzer.node.ArtifactNode;
import nl.pieni.maven.dependency_analyzer.node.GroupNode;
import nl.pieni.maven.dependency_analyzer.node.VersionNode;
import nl.pieni.maven.dependency_analyzer.export.log.LogWriter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link DependencyReport}
 */
public class DependencyReportTest {

    private static final String LINESEPERATOR = System.getProperty("line.separator");

    private DependencyDatabaseSearcher searcher;
    private Dependency reportDependency1;


    @Before
    public void setup() {
        //Setup the dependency objects
        reportDependency1 = new Dependency();
        reportDependency1.setGroupId("reportGroupNode");
        reportDependency1.setArtifactId("reportArtifactNode");
        reportDependency1.setVersion("1.0");


        /* Setup the node interactions */
        ArtifactNode reportArtifactNode=mock(ArtifactNode.class);
        when(reportArtifactNode.getArtifactId()).thenReturn("reportAID");
        when(reportArtifactNode.getType()).thenReturn("jar");
        GroupNode reportGroupNode=mock(GroupNode.class);
        when(reportGroupNode.getGroupId()).thenReturn("reportGID");
        VersionNode reportVersion1=mock(VersionNode.class);
        when(reportVersion1.getVersion()).thenReturn("1.0");
        VersionNode reportVersion2=mock(VersionNode.class);
        when(reportVersion2.getVersion()).thenReturn("2.0");

        //The parent child relations
        when(reportArtifactNode.getParent()).thenReturn(reportGroupNode);
        when(reportVersion1.getParent()).thenReturn(reportArtifactNode);
        when(reportVersion2.getParent()).thenReturn(reportArtifactNode);

        GroupNode otherGroupNode=mock(GroupNode.class);
        when(otherGroupNode.getGroupId()).thenReturn("otherGID");
        ArtifactNode otherArtifactNode=mock(ArtifactNode.class);
        when(otherArtifactNode.getArtifactId()).thenReturn("otherAID");
        when(otherArtifactNode.getType()).thenReturn("jar");
        VersionNode otherVersionNode=mock(VersionNode.class);
        when(otherVersionNode.getVersion()).thenReturn("1.0");
        when(otherVersionNode.getParent()).thenReturn(otherArtifactNode);
        when(otherArtifactNode.getParent()).thenReturn(otherGroupNode);


        /* Searcher mocking*/
        searcher = mock(DependencyDatabaseSearcher.class);
        //The version nodes of the reporting dependency
        List<VersionNode> versionNodes = new ArrayList<VersionNode>();
        versionNodes.add(reportVersion1);
        versionNodes.add(reportVersion2);
        when(searcher.getVersionNodes(reportDependency1)).thenReturn(versionNodes);
        //The artifact node of the report
        when(searcher.findArtifactNode(reportDependency1)).thenReturn(reportArtifactNode);
        //The group of the report
        when(searcher.findGroupNode(reportDependency1)).thenReturn(reportGroupNode);

        Map<ScopedRelation, List<ArtifactNode>> dependingArtifacts = new HashMap<ScopedRelation, List<ArtifactNode>>();
        List<ArtifactNode> artifacts = new ArrayList<ArtifactNode>();
        artifacts.add(otherArtifactNode);
        dependingArtifacts.put(ScopedRelation.compile, artifacts);
        when(searcher.getDependingArtifacts(reportDependency1)).thenReturn(dependingArtifacts);

        Map<VersionNode, List<VersionNode>> versionDependencyMap = new HashMap<VersionNode, List<VersionNode>>();
        List<VersionNode> version1DependencyList = new ArrayList<VersionNode>();
        version1DependencyList.add(otherVersionNode);
        versionDependencyMap.put(reportVersion1, version1DependencyList);
        versionDependencyMap.put(reportVersion2, version1DependencyList);

        when(searcher.getVersionDependencies(reportDependency1)).thenReturn(versionDependencyMap);
    }

    /**
     * Test the output of the writer
     *
     * @throws IOException When IO Error
     */
    @Test
    public void stringWriterTest() throws IOException {

        DependencyReport report = new DependencyReportImpl(searcher);
        StringWriter writer = mock(StringWriter.class);
        report.createReport(reportDependency1, writer);

        verify(writer).write("Report for Artifact: \"reportGID:reportAID\"" + LINESEPERATOR);
        verify(writer).write("Available versions" + LINESEPERATOR);
        verify(writer, times(2)).write("\t1.0" + LINESEPERATOR);
        verify(writer, times(2)).write("\t2.0" + LINESEPERATOR);
        verify(writer).write("Incoming relations" + LINESEPERATOR);
        verify(writer).write("\tScope: compile" + LINESEPERATOR);
        verify(writer, times(2)).write("\t\totherGID:otherAID:jar:1.0" + LINESEPERATOR);
        verify(writer).write("\t\totherGID:otherAID:jar" + LINESEPERATOR);
        verify(writer).write("Version specific relations" + LINESEPERATOR);

    }


    @Test
    public void stringWriterEmptyTest() throws IOException {
        Dependency dependency = new Dependency();
        dependency.setGroupId("a");
        dependency.setArtifactId("b");
        dependency.setVersion("1.0");

        DependencyReport report = new DependencyReportImpl(searcher);
        StringWriter writer = mock(StringWriter.class);
        report.createReport(dependency, writer);
    }


    /**
     * The the writer with a {@link org.apache.maven.plugin.logging.Log} as writer
     *
     * @throws IOException When IO Error
     */
    @Test
    public void logWriterTest() throws IOException {

        DependencyReport report = new DependencyReportImpl(searcher);

        SystemStreamLog systemStreamLog = mock(SystemStreamLog.class);
        LogWriter writer = new LogWriter(systemStreamLog);
        report.createReport(reportDependency1, writer);

        verify(systemStreamLog).info("Report for Artifact: \"reportGID:reportAID\"");
        verify(systemStreamLog).info("Available versions");
        verify(systemStreamLog, times(2)).info("\t1.0");
        verify(systemStreamLog, times(2)).info("\t2.0");
        verify(systemStreamLog).info("Incoming relations");
        verify(systemStreamLog).info("\tScope: compile");
        verify(systemStreamLog, times(2)).info("\t\totherGID:otherAID:jar:1.0");
        verify(systemStreamLog).info("\t\totherGID:otherAID:jar");
        verify(systemStreamLog).info("Version specific relations");
    }

}
