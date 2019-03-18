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

package de.dkfz.tbi.otp

import grails.plugin.springsecurity.SpringSecurityService
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsHttpSession
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreFilter

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ProjectService


class ProjectSelectionService {
    ApplicationContext applicationContext
    PluginAwareResourceBundleMessageSource messageSource
    ProjectService projectService
    SpringSecurityService springSecurityService

    private static final String PROJECT_SELECTION_KEY = "PROJECT_SELECTION"


    ProjectSelection getSelectedProject() {
        if (springSecurityService.isLoggedIn()) {
            GrailsHttpSession session = WebUtils.retrieveGrailsWebRequest().getSession()
            if (!(ProjectSelection) session.getAttribute(PROJECT_SELECTION_KEY)) {
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

    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject, 'OTP_READ_ACCESS')")
    Project getProjectFromProjectSelectionOrAllProjects(ProjectSelection projectSelection) {
        if (projectSelection.projects.size() == 1) {
            return projectSelection.projects.first()
        } else if (projectService.allProjects.size() > 0) {
            return projectService.allProjects.first()
        } else {
            return null
        }
    }

    @PreFilter(value = "hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'OTP_READ_ACCESS')", filterTarget = "projects")
    void setSelectedProject(List<Project> projects, String displayName) {
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
