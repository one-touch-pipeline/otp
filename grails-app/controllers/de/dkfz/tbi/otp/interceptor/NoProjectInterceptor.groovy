/*
 * Copyright 2011-2020 The OTP authors
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

import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.SecurityService

@CompileStatic
class NoProjectInterceptor {
    ProjectService projectService
    SecurityService securityService

    int order = 1

    NoProjectInterceptor() {
        matchAll()
                .except(controller: 'error')
                .except(controller: 'projectRequest')
                .except(controller: 'departmentConfiguration')
                .except(controller: 'ldap')
                .except(controller: 'projectCreation')
                .except(controller: 'privacyPolicy')
                .except(controller: 'logout', action: 'index')
                .except(controller: 'auth', action: 'logout')
                .except(controller: 'info')
                .except(controller: 'document')
                .except(controller: 'metaDataFields')
                .except(controller: 'softwareTool')
                .except(controller: 'speciesWithStrain')
                // admin pages
                .except(controller: 'userAdministration')
                .except(controller: 'roles')
                .except(controller: 'crashRecovery')
                .except(controller: 'processingOption')
                .except(controller: 'jobErrorDefinition')
                .except(controller: 'shutdown')
                .except(controller: 'console')
                .except(uri: '/static/**')
                .except(uri: '/webjars/**')
                .except(uri: '/statistic/projectCountPerDate**')
                .except(uri: '/statistic/laneCountPerDate**')
    }

    @Override
    boolean before() {
        if (securityService.loggedIn) {
            if (!projectService.allProjects) {
                forward(controller: "error", action: "noProject")
                return false
            }
        }
        return true
    }

    @Override
    boolean after() { return true }

    @Override
    void afterView() {
        // no-op
    }
}
