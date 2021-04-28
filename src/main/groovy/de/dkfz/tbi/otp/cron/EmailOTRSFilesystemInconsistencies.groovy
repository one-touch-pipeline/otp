/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.cron

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.LdapUserDetails
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.security.User

@SuppressWarnings("AnnotationsForJobs")
@Scope("singleton")
@Component
@Slf4j
class EmailOTRSFilesystemInconsistencies extends ScheduledJob {
    @Autowired
    LdapService ldapService

    @Override
    void wrappedExecute() {
        String mailReceiver = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_OTP_MAINTENANCE)
        String mailContent = getMailContent()
        if (mailContent) {
            mailHelperService.sendEmail("User Management Inconsistencies status", mailContent, mailReceiver)
        }
    }

    String getMailContent() {
        List<String> content = []
        List<User> userList = User.findAllByEnabled(true)
        List<LdapUserDetails> ldapUserDetailsList = ldapService.getLdapUserDetailsByUserList(userList)

        UserProjectRole.findAll().sort {
            [it.project.name, it.user.username,].join(',')
        }.each { UserProjectRole userProjectRole ->
            if (!userProjectRole.user.username) {
                return
            }
            boolean fileAccessInOtp = userProjectRole.accessToFiles
            List<String> groupsOfUser = ldapUserDetailsList.find { it.username == userProjectRole.user.username }?.memberOfGroupList ?: []
            boolean fileAccessInLdap = userProjectRole.project.unixGroup in groupsOfUser

            if (fileAccessInOtp != fileAccessInLdap) {
                content << userProjectRole.user.realName + " | "
                content << userProjectRole.user.username + " | "
                content << userProjectRole.user.email + " | "
                content << userProjectRole.project.name + " | "
                List<User> list = UserProjectRoleService.getProjectAuthorities(userProjectRole.project)
                list.each {
                    content << it.realName + " | "
                    content << it.email + " | "
                }
                content << "\n"
            }
        }
        List<String> finalContent = []
        if (content) {
            String contentHeader = "Name | userName | emailId | projectName | PI Name | PI Email | PI Name | PI Email\n"
            finalContent << contentHeader
            finalContent << content.join('')
        }
        return finalContent.join('')
    }
}
