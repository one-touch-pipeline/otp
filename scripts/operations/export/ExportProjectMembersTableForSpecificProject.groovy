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

import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.ngsdata.UserEntry
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

/*
This script export the project members table for a given project.
The column "Project Access" is not exported since there is no information provided in this column.
 */

//input
String projectName = '???'

//work
IdentityProvider identityProvider = ctx.getBean('identityProvider')

def project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName))

List<UserProjectRole> userProjectRoles = UserProjectRole.findAllByProject(project)
String table = userProjectRoles.collect { UserProjectRole upr ->
    IdpUserDetails idpUserDetails = identityProvider.getIdpUserDetailsByUsername(upr.user.username)
    new UserEntry(upr.user, upr.project, idpUserDetails)
}.sort { UserEntry userEntry ->
    userEntry.user.username
}.collect { UserEntry userEntry ->
    [
            userEntry.realName,
            userEntry.user.username,
            userEntry.department ?: '',
            userEntry.user.email,
            userEntry.projectRoleNames.join(", "),
            userEntry.otpAccess,
            userEntry.fileAccess,
            userEntry.manageUsers,
            userEntry.manageUsersAndDelegate,
            userEntry.receivesNotifications,
            userEntry.deactivated,
            !userEntry.userProjectRole.enabled,
    ].join('\t')
}.join('\n')

println "Name\tAccount\tDept.\tMail\tRole\tAccess to OTP\tAccess to Files\tManage users\tCan delegate management\tReceives notifications\tDeactivated in ldap\tIsFormerUser"
println table
