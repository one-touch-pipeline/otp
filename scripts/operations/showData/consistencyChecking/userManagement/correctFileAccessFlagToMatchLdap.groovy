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

import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.security.User

/**
 *  Correct the fileAccess flag of all UserProjectRoles to match the status in the LDAP.
 */

LdapService ldapService = ctx.ldapService

Map<String, List<String>> cache = [:]

UserProjectRole.withTransaction {
    List<UserProjectRole> allUPRs = UserProjectRole.findAll()
    allUPRs.each { UserProjectRole userProjectRole ->
        if (!userProjectRole.user.username) {
            return
        }
        User user = userProjectRole.user
        if (!cache[user.username]) {
            cache[user.username] = ldapService.getGroupsOfUser(user)
        }
        boolean fileAccessInOtp = userProjectRole.accessToFiles
        boolean fileAccessInLdap = userProjectRole.project.unixGroup in cache[user.username]
        if (fileAccessInOtp != fileAccessInLdap) {
            println "${userProjectRole.user.username} ${userProjectRole.project.name} ${userProjectRole.projectRole.name}: ${fileAccessInOtp} -> ${fileAccessInLdap}"
            userProjectRole.accessToFiles = fileAccessInLdap
            userProjectRole.save()
        }
    }
    allUPRs.last().save(flush: true)
    assert false: "Assert for debug, remove to continue"
}
''
