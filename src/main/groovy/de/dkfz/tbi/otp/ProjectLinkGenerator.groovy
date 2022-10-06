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
package de.dkfz.tbi.otp

import asset.pipeline.grails.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.security.SecurityService

class ProjectLinkGenerator extends LinkGenerator {

    @Autowired
    ProjectSelectionService projectSelectionService

    @Autowired
    SecurityService securityService

    ProjectLinkGenerator(final String serverUrl) {
        super(serverUrl)
    }

    @Override
    String link(Map attrs, String encoding = 'UTF-8') {
        Object paramsAttribute = attrs.get(ATTRIBUTE_PARAMS)
        if (paramsAttribute != [:] && securityService.loggedIn) {
            Map params = paramsAttribute instanceof Map ? paramsAttribute as Map : [:]
            if (!params.get(ProjectSelectionService.PROJECT_SELECTION_PARAMETER) && projectSelectionService.selectedProject) {
                attrs.put(ATTRIBUTE_PARAMS, [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): projectSelectionService.selectedProject] << params)
            }
        }
        return super.link(attrs, encoding)
    }
}
