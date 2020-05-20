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
package de.dkfz.tbi.otp.interceptor

import grails.plugin.springsecurity.SpringSecurityService
import groovy.transform.CompileStatic
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.context.i18n.LocaleContextHolder

import de.dkfz.tbi.otp.ProjectSelectionCommand
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@CompileStatic
class ProjectSelectionInterceptor {
    PluginAwareResourceBundleMessageSource messageSource
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    SpringSecurityService springSecurityService

    int order = 2

    ProjectSelectionInterceptor() {
        matchAll().except(controller: 'errors')
    }

    @Override
    boolean before() {
        if (springSecurityService.loggedIn) {
            projectSelectionService.setSelectedProject(params.remove(ProjectSelectionService.PROJECT_SELECTION_PARAMETER) as String)
        }
        true
    }

    @Override
    boolean after() {
        if (model != null && springSecurityService.loggedIn) {
            model.selectedProject = projectSelectionService.selectedProject

            List<Project> allProjects = projectService.allProjects
            model.availableProjects = allProjects

            model.projectParameter = ProjectSelectionService.PROJECT_SELECTION_PARAMETER
            // projects in groups
            Map<String, List<ProjectSelectionCommand>> availableProjectsInGroups = [:]
            Map<ProjectGroup, List<Project>> projectsMap = allProjects.groupBy { it.projectGroup }
            List<Project> projectsWithoutGroup = projectsMap.remove(null)
            projectsMap.each { ProjectGroup group, List<Project> projects ->
                String displayName = messageSource.getMessage("header.projectSelection.allGroupOrCategory", [group.name].toArray(), LocaleContextHolder.locale)
                availableProjectsInGroups.put(
                        group.name,
                        [
                            new ProjectSelectionCommand(displayName: displayName, type: ProjectSelectionCommand.Type.GROUP, id: group.id)
                        ] + projects.collect {
                            new ProjectSelectionCommand(displayName: it.name, type: ProjectSelectionCommand.Type.PROJECT, id: it.id)
                        }
                )
            }
            model.availableProjectsInGroups = availableProjectsInGroups

            // projects not in groups
            List<ProjectSelectionCommand> availableProjectsWithoutGroup = []
            projectsWithoutGroup?.each { Project project ->
                availableProjectsWithoutGroup.add(
                        new ProjectSelectionCommand(displayName: project.name, type: ProjectSelectionCommand.Type.PROJECT, id: project.id)
                )
            }
            model.availableProjectsWithoutGroup = availableProjectsWithoutGroup
        }
        true
    }

    @Override
    void afterView() {
        // no-op
    }
}
