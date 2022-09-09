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

import grails.validation.*

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.security.user.identityProvider.LdapService
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails

/**
 *  Looks up all users that are unknown to OTP and creates them.
 *  An unknown user is a user, whose username is associated with a group in LDAP, but not with a User object in OTP.
 *  LDAP groups are resolved via the unix group field of Project.
 *
 *  Failed User creations are listed. Resolve those manually.
 */

List<String> blacklistedUsernames = [

]

String getListInfo(String text, List list) {
    return "${text}\n${list}\n${list.size()} entries\n"
}

LdapService ldapService = ctx.ldapService
UserService userService = ctx.userService

List<String> allUsernames = User.list()*.username.sort()

println(getListInfo("Unique usernames of OTP users:", allUsernames))

List<String> usernamesInUnixGroups = []

Project.list().each { Project project ->
    String groupDistinguishedName = ldapService.getDistinguishedNameOfGroupByGroupName(project.unixGroup)
    usernamesInUnixGroups << ldapService.getGroupMembersByDistinguishedName(groupDistinguishedName)
}

usernamesInUnixGroups = usernamesInUnixGroups.flatten().unique().sort()

println(getListInfo("Unique users over all unix groups used in OTP:", usernamesInUnixGroups))

List<String> ldapOnlyUsers = usernamesInUnixGroups - allUsernames - blacklistedUsernames

println(getListInfo("Users to be created:", ldapOnlyUsers))

assert false: "Assert for debug, remove to continue"

String separator = ","
List<String> userPropertiesToPrint = ["username", "email", "realName"]

List<String> fails = []

println("Created users:")
println(userPropertiesToPrint.join(separator))
ldapOnlyUsers.each { String username ->
    IdpUserDetails idpUserDetails = ldapService.getIdpUserDetailsByUsername(username)
    try {
        User user = userService.createUser(idpUserDetails.username, idpUserDetails.mail, idpUserDetails.realName)
        println(userPropertiesToPrint.collect { user[it] }.join(separator))
    } catch (ValidationException e) {
        fails << "Failed when creating user '${username}' (LDAP username: ${idpUserDetails.username}, mail: ${idpUserDetails.mail})\n${e}"
    }
}
println("\n\nFailed to create:")
println(fails.join("\n"))

''
