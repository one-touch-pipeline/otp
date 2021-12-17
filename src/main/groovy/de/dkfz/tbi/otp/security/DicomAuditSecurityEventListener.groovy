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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.security.access.event.AuthorizationFailureEvent
import org.springframework.security.authentication.event.*
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent
import org.springframework.stereotype.Component

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.xml.layer.EventIdentification
import de.dkfz.tbi.otp.ProjectSelectionService

/**
 * Spring Security Authentification event listeners for logging login processes to the Dicom audit log
 */
@Component
class DicomAuditSecurityEventListener implements ApplicationListener<AbstractAuthenticationEvent> {

    @Autowired
    ProjectSelectionService projectSelectionService

    @Override
    void onApplicationEvent(AbstractAuthenticationEvent event) {
        //if login success (AuthenticationSuccessEvent) then nothing should be done

        switch (event) {
            case { it instanceof AbstractAuthenticationFailureEvent } :
                // Login failure
                DicomAuditLogger.logUserLogin(
                        EventIdentification.EventOutcomeIndicator.MINOR_FAILURE,
                        (event.authentication.principal.hasProperty("username") ?
                                event.authentication.principal.username : event.authentication.principal) as String
                )
                break
            case { it instanceof InteractiveAuthenticationSuccessEvent } :
                // Login success, this event fires only on interactive (Non-automated) login
                DicomAuditLogger.logUserLogin(
                        EventIdentification.EventOutcomeIndicator.SUCCESS,
                        event.authentication.principal.username as String
                )
                break
            case { it instanceof AuthenticationSwitchUserEvent } :
                // User switch
                DicomAuditLogger.logUserSwitched(
                        EventIdentification.EventOutcomeIndicator.SUCCESS,
                        DicomAuditUtils.getRealUserName(event.authentication.principal.username as String),
                        event.targetUser?.username
                )
                break
            case { it instanceof AuthorizationFailureEvent } :
                DicomAuditLogger.logRestrictedFunctionUsed(
                        EventIdentification.EventOutcomeIndicator.MINOR_FAILURE,
                        DicomAuditUtils.getRealUserName(event.authentication.principal.username as String),
                        event.source.hasProperty("request") ? event.source.request.requestURI : "null"
                )
                break
        }
    }
}
