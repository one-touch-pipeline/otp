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

package de.dkfz.tbi.otp.security

import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static de.dkfz.tbi.otp.security.DicomAuditUtils.getRealUserName

/**
 * A handler class for Spring Security logout events,
 * forwarding the logout to the Dicom audit logging framework.
 */
class DicomAuditLogoutHandler implements LogoutHandler {
    @Override
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        DicomAuditLogger.logUserLogout(EventOutcomeIndicator.SUCCESS, getRealUserName(authentication.principal.username))
    }
}
