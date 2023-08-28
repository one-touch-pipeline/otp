/*
 * Copyright 2011-2023 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider

/**
 *  Correct the fileAccess flag of all UserProjectRoles to match the status in the LDAP.
 */

// input area
// ----------------------

/**
 * flag to allow a try and rollback the changes at the end (true) or do the changes(false)
 */
boolean tryRun = true

// ------------------------------------
// work

IdentityProvider identityProvider = ctx.getBean('identityProvider')
UserProjectRoleService uprs = ctx.getBean('userProjectRoleService')

Map<String, List<String>> cache = [:]

UserProjectRole.withTransaction {
    List<UserProjectRole> allUPRs = UserProjectRole.findAll()
    allUPRs.each { UserProjectRole userProjectRole ->
        if (!userProjectRole.user.username) {
            return
        }
        User user = userProjectRole.user
        if (!cache[user.username]) {
            cache[user.username] = identityProvider.getGroupsOfUser(user)
        }
        boolean fileAccessInOtp = userProjectRole.accessToFiles
        boolean fileAccessInLdap = userProjectRole.project.unixGroup in cache[user.username]
        if (fileAccessInOtp != fileAccessInLdap) {
            println "${userProjectRole.user.username} ${userProjectRole.project.name} ${userProjectRole.projectRoles.name.join(", ")}: ${fileAccessInOtp} -> ${fileAccessInLdap}"
            uprs.setAccessToFiles(userProjectRole, fileAccessInLdap, true)
        }
    }
    assert !tryRun: "Rollback, since only tryRun."
}
''
