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
