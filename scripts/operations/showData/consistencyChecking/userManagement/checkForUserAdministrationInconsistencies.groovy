package operations.showData.consistencyChecking.userManagement
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

/**
 *  Check for inconsistencies in the user administration
 *
 *  Executes three checks, to find inconsistencies in:
 *    1. group memberships of users in OTP and users in the unix group
 *    2. file access rights of a specific user in projects with a common unixGroup
 *    3. in access to files given by the project role and the actual membership in the unix group
 *
 *
 *  Resolving errors:
 *    1.
 *    1.1: "Following Users have not been added to the project"
 *    Add the user to the project.
 *
 *    1.2: "Following Users could not be resolved to a user in the OTP database"
 *    Create the user and add them to the project. Adding an unknown user automatically creates them.
 *    If they are not added to the project they will appear as part of 1.1.
 *
 *
 *    2.
 *    Option 1: Decide if the user should have file access or not. Adapt the file access flag for all
 *    mentioned projects accordingly.
 *
 *    Option 2: Split the projects into separate unixGroups and add the respective users to the new groups.
 *
 *
 *    3. Note: Always resolve these only after all of check 2 have been resolved!
 *    Decide if the user should have file access or not.
 *    If so, set the file-access flag and add the user to unixGroup.
 *    If not, remove the file-access flag and remove the user from unixGroup.
 */

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.*

List output = []

LdapService ldapService = ctx.getBean('ldapService')
UserProjectRoleService uprs = ctx.getBean('userProjectRoleService')


// this cache maps username to LdapUserDetails in an effort to reduce the number of duplicate ldap requests
Map<String, LdapUserDetails> cache = [:]

// Check 1
Project.findAll().each { Project project ->
    List<User> projectUsers = UserProjectRole.findAllByProject(project)*.user
    List<String> nonDatabaseUsers = []

    String groupDistinguishedName = ldapService.getDistinguishedNameOfGroupByGroupName(project.unixGroup)
    List<String> ldapGroupMembers = ldapService.getGroupMembersByDistinguishedName(groupDistinguishedName)

    ldapGroupMembers.each { String username ->
        User user = User.findByUsername(username)
        if (user) {
            projectUsers.add(user)
        } else {
            nonDatabaseUsers.add(username)
        }
    }

    projectUsers.unique()
    projectUsers.sort { it.username }

    List<User> usersWithoutUserProjectRole = []
    projectUsers.each { User user ->
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        if (!userProjectRole) {
            usersWithoutUserProjectRole.add(user)
        }
    }
    if (usersWithoutUserProjectRole || nonDatabaseUsers) {
        output << "Project: ${project.name} (${project.unixGroup})"
        if (usersWithoutUserProjectRole) {
            output << "Following Users have not been added to the project:"
            usersWithoutUserProjectRole.each { User user ->
                output << sprintf("%-15s | %-20s | %-30s ", [user.username, user.realName, user.email])
            }
            output << "\n"
        }
        if (nonDatabaseUsers) {
            output << "Following Users could not be resolved to a user in the OTP database:"
            nonDatabaseUsers.each { String username ->
                LdapUserDetails details
                if (cache[username]) {
                    details = cache[username]
                } else {
                    details = ldapService.getLdapUserDetailsByUsername(username)
                    cache[username] = details
                }
                output << sprintf("%-15s | %-20s | %-30s ", [details.username, details.realName, details.mail])
            }
            output << "\n"
        }
    }
}


// Check 2
User.findAll().each { User user ->
    UserProjectRole[] allUserProjectRolesOfUser = UserProjectRole.findAllByUser(user)
    allUserProjectRolesOfUser*.project.unixGroup.unique().each { String unixGroup ->
        List<Project> projects = Project.findAllByUnixGroup(unixGroup)
        List<UserProjectRole> allUserProjectRolesForUserAndUnixGroup = projects ? UserProjectRole.findAllByUserAndProjectInList(user, projects) : []
        if (allUserProjectRolesForUserAndUnixGroup*.accessToFiles.unique().size() > 1) {
            output << "Conflict detected for User ${user.username} with the unix Group ${unixGroup}. Affected projects:\n"+
                    allUserProjectRolesForUserAndUnixGroup*.project.name.join(", ")+"\n"
        }
    }
}


// Check 3
String format = "%-10s | %-6s | %-6s | %-7s | %-7s | %-10s | %s (%s)"
output << sprintf(format, ["in Unx-Grp", "Access", "ldap", "planned", "enabled", "User", "Project", "Unix Group"])
output << sprintf(format, ["", "", "deact.", "deact.", "in OTP", "", "", "", ""])
UserProjectRole.findAll().sort {
    [it.project.name, it.user.username,].join(',')
}.each { UserProjectRole userProjectRole ->
    if (!userProjectRole.user.username) {
        return
    }
    boolean fileAccessInOtp = userProjectRole.accessToFiles
    boolean fileAccessInLdap = userProjectRole.project.unixGroup in ldapService.getGroupsOfUser(userProjectRole.user)
    boolean ldapDeactivated = ldapService.isUserDeactivated(userProjectRole.user)
    if (fileAccessInOtp && !fileAccessInLdap) {
        uprs.setAccessToFiles(userProjectRole, false, true)
        fileAccessInOtp = false
    }
    if (fileAccessInOtp != fileAccessInLdap) {
        output << sprintf(
                format,
                [fileAccessInLdap, fileAccessInOtp, ldapDeactivated, userProjectRole.user.plannedDeactivationDate as boolean, userProjectRole.enabled,
                 userProjectRole.user.username, userProjectRole.project.name, userProjectRole.project.unixGroup]
        )
    }
}

println(output.join('\n'))

[]
