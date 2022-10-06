/* Copyright 2006-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dkfz.tbi.otp.utils

import org.springframework.security.web.savedrequest.SavedRequest
import org.springframework.web.multipart.MultipartHttpServletRequest

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * This code was copied from the Grails Spring Security Core plugin
 * (grails.plugin.springsecurity.SpringSecurityService and grails.plugin.springsecurity.SpringSecurityUtils)
 */
class RequestUtilService {

    public static final String XML_HTTP_REQUEST = 'XMLHttpRequest'
    public static final String X_REQUESTED_WITH = 'X-Requested-With'
    private static final String MULTIPART_HTTP_SERVLET_REQUEST_KEY = MultipartHttpServletRequest.name
    public static final String SAVED_REQUEST = 'SPRING_SECURITY_SAVED_REQUEST'

    /**
     * Check if the request was triggered by an Ajax call.
     * @param request the request
     * @return <code>true</code> if Ajax
     */
    boolean isAjax(HttpServletRequest request) {
        // check the current request's headers
        if (XML_HTTP_REQUEST == request.getHeader(X_REQUESTED_WITH)) {
            return true
        }

        // look for an ajax=true parameter
        if ('true' == request.getParameter('ajax')) {
            return true
        }

        // process multipart requests
        MultipartHttpServletRequest multipart = (MultipartHttpServletRequest)request.getAttribute(MULTIPART_HTTP_SERVLET_REQUEST_KEY)
        if ('true' == multipart?.getParameter('ajax')) {
            return true
        }

        // check the SavedRequest's headers
        HttpSession httpSession = request.getSession(false)
        if (httpSession) {
            SavedRequest savedRequest = (SavedRequest)httpSession.getAttribute(SAVED_REQUEST)
            if (savedRequest) {
                return savedRequest.getHeaderValues(X_REQUESTED_WITH).contains(MULTIPART_HTTP_SERVLET_REQUEST_KEY)
            }
        }

        return false
    }
}
