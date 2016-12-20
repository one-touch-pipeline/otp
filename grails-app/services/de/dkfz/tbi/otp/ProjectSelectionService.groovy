package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.*
import org.codehaus.groovy.grails.context.support.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.util.*
import org.springframework.context.*
import org.springframework.context.i18n.*
import org.springframework.security.access.prepost.*


class ProjectSelectionService {
    ApplicationContext applicationContext
    PluginAwareResourceBundleMessageSource messageSource
    ProjectService projectService
    SpringSecurityService springSecurityService

    private static final String PROJECT_SELECTION_KEY = "PROJECT_SELECTION"


    public ProjectSelection getSelectedProject() {
        if (springSecurityService.isLoggedIn()) {
            GrailsHttpSession session = WebUtils.retrieveGrailsWebRequest().getSession()
            if (!(ProjectSelection)session.getAttribute(PROJECT_SELECTION_KEY)) {
                setSelectedProject(
                        // projectService.allProjects (instead of Project.all) is used here because spring security annotations
                        // don't work when calling a method in the same class
                        projectService.allProjects,
                        messageSource.getMessage("header.projectSelection.allProjects", [].toArray(), LocaleContextHolder.getLocale())
                )
            }
            return (ProjectSelection) session.getAttribute(PROJECT_SELECTION_KEY)
        } else {
            return null
        }
    }

    @PreFilter(value = "hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'read')", filterTarget = "projects")
    public setSelectedProject(List<Project> projects, String displayName) {
        GrailsHttpSession session = WebUtils.retrieveGrailsWebRequest().getSession()
        ProjectSelection projectSelection = new ProjectSelection(projects: projects, displayName: displayName)
        session.setAttribute(PROJECT_SELECTION_KEY, projectSelection)
    }
}

class ProjectSelection {
    List<Project> projects
    String displayName

    boolean asBoolean() {
        projects.asBoolean()
    }
}
