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

import de.dkfz.tbi.otp.security.user.identityProvider.LdapService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 *  Creates a UserProjectRole entry for all users that can be found in the LDAP group
 *  but are not yet connected to the project.
 *
 *  Only uses usernames that already have a User entity, does not create new Users.
 *  If you need to create all missing users, use createUnknownUsers.groovy
 *
 *  The used ProjectRole and flags can be configured at the top of the script.
 *
 *  For documentational purposes this script generates very verbose output.
 */

// ProjectRole to use for the new UserProjectRoles
Set<ProjectRole> projectRoles = CollectionUtils.atMostOneElement(ProjectRole.findAllByName(ProjectRole.Basic.BIOINFORMATICIAN.name())) as Set<ProjectRole>
// UserProjectRole flags
boolean enabled = true
boolean accessToFiles = true
boolean manageUsers = false
boolean manageUsersAndDelegate = false
boolean receivesNotifications = true

List<String> blacklistedUsernames = [
]

assert projectRoles: "ProjectRole not found"

String getSortedListInfo(String text, List list) {
    return "${text}\n${list.sort()}\n${list.size()} entries\n"
}

LdapService ldapService = ctx.ldapService

List<String> allUsernames = User.list()*.username

List<UserProjectRole> newUPRs = []

UserProjectRole.withTransaction {
    Project.list().sort { it.name }.each { Project project ->
        println("Project: ${project.name}")

        List<String> usernamesInProject = UserProjectRole.createCriteria().list {
            eq("project", project)
            user {
                isNotNull("username")
            }
        }*.user.username
        println(getSortedListInfo("Usernames of project:", usernamesInProject ?: []))

        String groupDistinguishedName = ldapService.getDistinguishedNameOfGroupByGroupName(project.unixGroup)
        List<String> usernamesInLdapGroup = ldapService.getGroupMembersByDistinguishedName(groupDistinguishedName)
        println(getSortedListInfo("Username in LDAP group:", usernamesInLdapGroup ?: []))

        List<String> unconnectedUsernames = usernamesInLdapGroup - usernamesInProject
        println(getSortedListInfo("Unconnected usernames:", unconnectedUsernames ?: []))

        List<String> usersToConnect = unconnectedUsernames.intersect(allUsernames)
        println(getSortedListInfo("Unconnected usernames with an OTP User:", usersToConnect ?: []))
        println("\n")
        usersToConnect.each { String username ->
            if (!(username in blacklistedUsernames)) {
                User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(username))
                newUPRs << new UserProjectRole(
                        user: user,
                        project: project,
                        projectRoles: projectRoles,
                        enabled: enabled,
                        accessToOtp: ldapService.isUserInIdpAndActivated(user),
                        accessToFiles: accessToFiles,
                        manageUsers: manageUsers,
                        manageUsersAndDelegate: manageUsersAndDelegate,
                        receivesNotifications: receivesNotifications,
                )
            }
        }
    }

    println("UserProjectRoles to be created:")
    println("username,project_name,project_role_name")
    newUPRs.each { UserProjectRole userProjectRole ->
        println("${userProjectRole.user.username},${userProjectRole.project.name},${userProjectRole.projectRoles.name.join(", ")}")
    }

    assert false: "Assert for debug, remove to continue"

    newUPRs*.save(flush: true)
}
''
