/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService

@Transactional
class HomeService {
    ProjectService projectService

    List<ProjectData> getAllProjects() {
        getProjectData(projectService.allProjects)
    }

    List<ProjectData> getAllPublicProjects() {
        getProjectData(projectService.allPublicProjects)
    }

    private List<ProjectData> getProjectData(List<Project> projects) {
        if (!projects) {
            return []
        }

        Map<Long, SeqType> seqTypeById = SeqType.all.collectEntries { [it.id, it] }

        List<List> list = SeqTrack.createCriteria().list {
            createAlias("sample.individual.project", "project")
            sample {
                individual {
                    "in"("project", projects)
                }
            }
            projections {
                groupProperty("project.id")
                groupProperty("seqType.id")
                countDistinct("sample.id", 'samples')
            }
        } as List<List>

        Map<Object, List<List>> seqTypeNumberOfSamplesByProject = list.groupBy {
            it[0]
        }

        Map<Object, List<List>> pis = UserProjectRole.createCriteria().list {
            projections {
                "in"("project", projects)
                eq("enabled", true)
                projectRoles {
                    eq("name", ProjectRole.Basic.PI.name())
                }
                user {
                    eq("enabled", true)
                    eq("accountExpired", false)
                    eq("accountLocked", false)
                    eq("passwordExpired", false)
                    property("realName")
                }
                property("project.id")
            }
        }.groupBy { it[1] }

        return projects.collect { Project project ->
            String description = project.description ?: ""
            String shortDescription = description.length() > 200 ? "${description[0..200]}..." : description
            new ProjectData(
                    project.name,
                    project.projectType,
                    pis[project.id]*.first() as List<String>,
                    project.displayName,
                    description,
                    shortDescription,
                    seqTypeNumberOfSamplesByProject[project.id].collect { seqTypeNumberOfSamples ->
                        new ProjectData.SeqTypeNumberOfSamples(
                                seqTypeById[seqTypeNumberOfSamples[1]].displayName,
                                seqTypeNumberOfSamples[2] as int
                        )
                    }.sort { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.seqType, b.seqType) }
            )
        }.sort { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.displayName, b.displayName) }
    }

    @TupleConstructor
    static class ProjectData {
        String name
        Project.ProjectType projectType
        List<String> pis
        String displayName
        String description
        String shortDescription
        List<SeqTypeNumberOfSamples> st

        @TupleConstructor
        static class SeqTypeNumberOfSamples {
            String seqType
            int numberOfSamples
        }
    }
}
