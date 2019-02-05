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

import grails.plugin.springsecurity.SpringSecurityService
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.context.i18n.LocaleContextHolder

import de.dkfz.tbi.otp.ProjectSelectionCommand
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.*

class ProjectSelectionFilters {
    PluginAwareResourceBundleMessageSource messageSource
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    SpringSecurityService springSecurityService

    def filters = {
        projectSelectionFilter(controller: '*', controllerExclude: 'errors', action: '*') {
            after = { Map model ->
                if (model != null && springSecurityService.isLoggedIn()) {
                    model.projectSelection = projectSelectionService.getSelectedProject()

                    List<Project> allProjects = projectService.getAllProjects()

                    // projects in groups
                    Map<String, List<ProjectSelectionCommand>> availableProjectsInGroups = [:]
                    Map<ProjectGroup, List<Project>> projectsMap = allProjects.groupBy { it.projectGroup }
                    List<Project> projectsWithoutGroup = projectsMap.remove(null)
                    projectsMap.each { ProjectGroup group, List<Project> projects ->
                        String displayName = messageSource.getMessage("header.projectSelection.allGroupOrCategory", [group.name].toArray(), LocaleContextHolder.getLocale())
                        availableProjectsInGroups.put(
                                group.name,
                                [new ProjectSelectionCommand(displayName: displayName, type: ProjectSelectionCommand.Type.GROUP, id: group.id)] +
                                projects.collect { new ProjectSelectionCommand(displayName: it.name, type: ProjectSelectionCommand.Type.PROJECT, id: it.id) }
                        )
                    }
                    model.availableProjectsInGroups = availableProjectsInGroups

                    // projects not in groups
                    List<ProjectSelectionCommand> availableProjectsWithoutGroup = []
                    projectsWithoutGroup.each { Project project ->
                        availableProjectsWithoutGroup.add(new ProjectSelectionCommand(displayName: project.name, type: ProjectSelectionCommand.Type.PROJECT, id: project.id))
                    }
                    model.availableProjectsWithoutGroup = availableProjectsWithoutGroup

                    // projects in categories
                    Map<String, List<ProjectSelectionCommand>> availableProjectsInCategories = [:]
                    allProjects.each { Project project ->
                        project.projectCategories.each { ProjectCategory category ->
                            if (!availableProjectsInCategories.get(category.name)) {
                                String displayName = messageSource.getMessage("header.projectSelection.allGroupOrCategory", [category.name].toArray(), LocaleContextHolder.getLocale())
                                availableProjectsInCategories.put(category.name, [new ProjectSelectionCommand(displayName: displayName, type: ProjectSelectionCommand.Type.CATEGORY, id: category.id)])
                            }
                            availableProjectsInCategories.get(category.name).add(new ProjectSelectionCommand(displayName: project.name, type: ProjectSelectionCommand.Type.PROJECT, id: project.id))
                        }
                    }
                    model.availableProjectsInCategories = availableProjectsInCategories
                }
            }
        }
    }
}
