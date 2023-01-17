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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.grails.web.util.WebUtils
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService

import javax.servlet.http.HttpServletRequest

@CompileStatic
@Transactional
class ProjectSelectionService {
    ProjectService projectService

    static final String PROJECT_SELECTION_PARAMETER = "project"
    private static final String PROJECT_SELECTION_KEY = "de.dkfz.tbi.otp.projectselection"
    private static final String PROJECT_REQUEST_KEY = "de.dkfz.tbi.otp.projectrequest"

    /**
     * This method is used the set the selected project for the current request.
     * It must only be used in ProjectSelectionInterceptor.
     */
    void setSelectedProject(String projectName) {
        Project projectRequested = projectService.getProjectByNameAsList(projectName).find()
        currentRequest.setAttribute(PROJECT_REQUEST_KEY, projectRequested)
        Project projectSelected = projectRequested ?: projectService.allProjects.find()
        currentRequest.setAttribute(PROJECT_SELECTION_KEY, projectSelected)
    }

    /**
     * Returns either the project that the user selected if they have access to it; or the first available project that the user can access; or null.
     * It should be used in actions that render regular GSP pages.
     */
    Project getSelectedProject() {
        return currentRequest.getAttribute(PROJECT_SELECTION_KEY) as Project
    }

    /**
     * Returns the exact {@link Project} selected by the user, if they have access to it; and throws an exception otherwise.
     * It should be used in actions that alter data (after form submissions), and actions used by AJAX requests.
     */
    Project getRequestedProject() {
        Project project = currentRequest.getAttribute(PROJECT_REQUEST_KEY) as Project
        if (!project) {
            throw new AccessDeniedException("Access denied for the requested project.")
        }
        return project
    }

    private HttpServletRequest getCurrentRequest() {
        WebUtils.retrieveGrailsWebRequest().currentRequest
    }
}
