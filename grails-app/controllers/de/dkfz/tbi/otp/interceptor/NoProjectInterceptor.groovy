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

import grails.plugin.springsecurity.SpringSecurityService
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.project.ProjectService

@CompileStatic
class NoProjectInterceptor {
    ProjectService projectService
    SpringSecurityService springSecurityService

    int order = 1

    NoProjectInterceptor() {
        matchAll()
                .except(controller: 'errors')
                .except(controller: 'projectRequest')
                .except(controller: 'projectCreation')
                .except(controller: 'privacyPolicy')
                .except(controller: 'logout', action: 'index')
                .except(controller: 'info')
                .except(controller: 'document')
                // admin pages
                .except(controller: 'userAdministration')
                .except(controller: 'roles')
                .except(controller: 'crashRecovery')
                .except(controller: 'processingOption')
                .except(controller: 'jobErrorDefinition')
                .except(controller: 'dicom')
                .except(controller: 'shutdown')
    }

    @Override
    boolean before() {
        if (springSecurityService.loggedIn) {
            if (!projectService.allProjects) {
                forward(controller: "errors", action: "noProject")
                return false
            }
        }
        true
    }

    @Override
    boolean after() { true }

    @Override
    void afterView() {
        // no-op
    }
}
