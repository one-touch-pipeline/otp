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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.AuditLog.Action
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional
class AuditLogService {

    ProcessingOptionService processingOptionService
    SecurityService securityService

    private AuditLog createActionLog(User user, Action action, String description) {
        AuditLog actionLog = new AuditLog([
                user       : user,
                action     : action,
                description: description,
        ])
        actionLog.save(flush: true)
        return actionLog
    }

    AuditLog logAction(Action action, String description) {
        return createActionLog(securityService.userSwitchInitiator, action, description)
    }

    AuditLog logActionWithSystemUser(Action action, String description) {
        String userName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER)
        assert userName: "no system user is defined"
        User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(userName),
                "Could not find user '${userName}' in the database")
        return createActionLog(user, action, description)
    }
}
