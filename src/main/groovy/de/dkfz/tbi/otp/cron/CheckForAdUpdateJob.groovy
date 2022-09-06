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
package de.dkfz.tbi.otp.cron

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.security.user.identityProvider.LdapService
import de.dkfz.tbi.otp.security.user.identityProvider.LdapUserDetails
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.security.User

/**
 * Scheduled job that checks if requested role changes are done.
 */
@Component
@Slf4j
class CheckForAdUpdateJob extends AbstractScheduledJob {

    @Autowired
    LdapService ldapService

    @Autowired
    ConfigService configService

    @Override
    boolean isAdditionalRunConditionMet() {
        return configService.ldapEnabled
    }

    @Override
    void wrappedExecute() {
        List<UserProjectRole> checkingUserProjectRoles = userProjectRolesToCheck()
        log.debug("UserProjectRoles to check: ${checkingUserProjectRoles.size()} (for ${checkingUserProjectRoles*.user.unique().size()} users)")

        checkingUserProjectRoles.groupBy {
            it.user
        }.each { User user, List<UserProjectRole> userProjectRoleService ->
            LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsername(user.username)
            userProjectRoleService.each { UserProjectRole userProjectRole ->
                if ((userProjectRole.accessToFiles && userProjectRole.user.enabled) ==
                        (ldapUserDetails && ldapUserDetails.memberOfGroupList?.contains(userProjectRole.project.unixGroup))) {
                    log.debug("File access of user ${user} in project ${userProjectRole.project.name} now matches the target")
                    UserProjectRole.withTransaction {
                        userProjectRole.fileAccessChangeRequested = false
                        userProjectRole.save(flush: true)
                    }
                }
            }
        }
        List<UserProjectRole> remainingUserProjectRoles = userProjectRolesToCheck()
        log.debug("UserProject Roles still to check: ${remainingUserProjectRoles.size()} (for ${remainingUserProjectRoles*.user.unique().size()} users)")
    }

    private List<UserProjectRole> userProjectRolesToCheck() {
        return UserProjectRole.findAllByFileAccessChangeRequested(true)
    }
}
