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

import grails.gorm.transactions.Transactional
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator

import static de.dkfz.tbi.otp.security.DicomAuditUtils.getRealUserName

@Component("dicomAuditConsoleHandler")
@Transactional
class DicomAuditConsoleHandler {
    // Hack: The dicomAuditConsolseHandler#log method is a pseudo condition
    // that always returns true and logs the access to the console as side effect.
    // There is sadly no other way to intercept the access to these URLs.
    boolean log() {
        DicomAuditLogger.logEmergencyOverrideStart(EventOutcomeIndicator.SUCCESS,
            getRealUserName(SecurityContextHolder.getContext().authentication.principal.username))
        return true
    }
}
