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
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.SystemUserUtils

@SuppressWarnings("AnnotationsForJobs")
@Scope("singleton")
@Component
@Slf4j
class CheckFileAccessInconsistenciesJob extends AbstractScheduledJob {

    static final String SUBJECT = "User Management Inconsistencies status"

    /**
     * the header of the table of inconsistencies
     */
    static final String HEADER = [
            "in Unix-group",
            "File Access (OTP)",
            "ldap deact.",
            "planned deact.",
            "enabled in OTP",
            "user",
            "project",
            "unix group",
    ].join('\t')

    @Autowired
    LdapService ldapService

    @Autowired
    UserProjectRoleService userProjectRoleService

    @Override
    void wrappedExecute() {
        String mailContent = createMailContent()
        if (mailContent) {
            mailHelperService.sendEmailToTicketSystem(SUBJECT, mailContent)
        }
    }

    String createMailContent() {
        String body = createTableBody()
        return body ? HEADER + '\n' + body : ''
    }

    String createTableBody() {
        List<String> content = []

        List<User> userList = User.findAllByUsernameIsNotNull()
        Map<String, LdapUserDetails> ldapUserDetailsByUsername = ldapService.getLdapUserDetailsByUserList(userList).collectEntries {
            [(it.username): it]
        }

        UserProjectRole.findAllByUserInList(userList).each { UserProjectRole userProjectRole ->
            User user = userProjectRole.user
            Project project = userProjectRole.project

            boolean fileAccessInOtp = userProjectRole.accessToFiles
            List<String> groupsOfUser = ldapUserDetailsByUsername[user.username]?.memberOfGroupList ?: []
            boolean fileAccessInLdap = project.unixGroup in groupsOfUser
            boolean ldapDeactivated = ldapService.isUserDeactivated(user)

            if (fileAccessInOtp && !fileAccessInLdap && !userProjectRole.fileAccessChangeRequested) {
                SystemUserUtils.useSystemUser {
                    userProjectRoleService.setAccessToFiles(userProjectRole, false, true)
                }
            } else if (fileAccessInOtp != fileAccessInLdap) {
                content << [
                        fileAccessInLdap,
                        fileAccessInOtp,
                        ldapDeactivated,
                        user.plannedDeactivationDate as boolean,
                        user.enabled,
                        user.username,
                        project.name,
                        project.unixGroup,
                ].join('\t')
            }
        }
        return content.sort().join('\n')
    }
}
