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

package nl.pieni.maven.dependency_analyzer.filter;

import org.apache.maven.model.Dependency;

import java.util.ArrayList;
import java.util.List;

/**
 * Include filter for the list of dependencies supplied.
 * (See http://maven.apache.org/shared/maven-common-artifact-filters/source-repository.html)
 */
public class DependencyIncludeFilter extends AbstractIncludeFilter<List<Dependency>, List<Dependency>> {

    /**
     * Default constructor
     * @param includePatterns patters to include
     */
    public DependencyIncludeFilter(final List<String> includePatterns) {
        super(includePatterns);
    }

    /**
     * Perform a filtering
     * @param toFilter list of dependencies to filter
     * @return filtered list
     */
    public List<Dependency> filter(final List<Dependency> toFilter) {
        List<Dependency> result = new ArrayList<Dependency>();
        for (Dependency gav : toFilter) {
            boolean add = include(gav);
            if (add) {
                result.add(gav);
            }
        }
        return result;
    }


    /**
     * include this dependency?
     * @param dependency the dependency
     * @return true when inclusion required
     */
    private boolean include(final Dependency dependency) {
        for (String pattern : patterns) {
            if (include(dependency, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check dependency against the inclusion pattern
     * @param dependency a {@link org.apache.maven.model.Dependency}
     * @param pattern the pattern
     * @return true when inclusion required
     */
    private boolean include(  final Dependency dependency,  final String pattern )
    {
        String[] tokens = new String[] {
            dependency.getGroupId(),
            dependency.getArtifactId(),
        };

        String[] patternTokens = pattern.split( ":" );

        // fail immediately if pattern tokens outnumber tokens to match
        boolean matched = ( patternTokens.length <= tokens.length );

        for ( int i = 0; matched && i < patternTokens.length; i++ )
        {
            matched = matches( tokens[i], patternTokens[i] );
        }

        return matched;
    }


}
