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
 *    Option 1: Decide if the user should have file access or not. Adapt the roles for all mentioned projects
 *    accordingly.
 *
 *    Option 2: Split the projects into separate unixGroups and add the respective users to the new groups.
 *
 *
 *    3. Note: Always resolve these only after all of check 2 have been resolved!
 *    Decide if the user should have file access or not.
 *    If so, give file-access role and add to unixGroup.
 *    If not, give a non-file-access role and remove from unixGroup.
 *
 */

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*

List output = []

LdapService ldapService = ctx.getBean('ldapService')


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
                output << sprintf("%-15s | %-20s | %-30s ", [details.cn, details.realName, details.mail])
            }
            output << "\n"
        }
    }
}


// Check 2
static boolean checkProjectRolesForFileAccessConflict(List<ProjectRole> projectRoles) {
    return projectRoles*.accessToFiles.unique().size() > 1
}

User.findAll().each { User user ->
    UserProjectRole[] allUserProjectRolesOfUser = UserProjectRole.findAllByUser(user)
    allUserProjectRolesOfUser*.project.unixGroup.unique().each { String unixGroup ->
        List<UserProjectRole> allUserProjectRolesForUserAndUnixGroup = UserProjectRole.findAllByUserAndProjectInList(user, Project.findAllByUnixGroup(unixGroup))
        if (checkProjectRolesForFileAccessConflict(allUserProjectRolesForUserAndUnixGroup*.projectRole.unique())) {
            output << "Conflict detected for User ${user.username} with the unix Group ${unixGroup}. Affected projects:"
            allUserProjectRolesForUserAndUnixGroup.each { UserProjectRole userProjectRole ->
                output << "${userProjectRole.project.name} (${userProjectRole.projectRole.name})"
            }
            output << ""
        }
    }
}


// Check 3
String format = "%-10s | %-6s | %-16s | %-10s | %s (%s)"
output << sprintf(format, ["in Unx-Grp", "Access", "Role", "User", "Project", "Unix Group"])
UserProjectRole.findAll().each { UserProjectRole userProjectRole ->
    if (!userProjectRole.user.username) {
        return
    }
    boolean fileAccessInOtp = userProjectRole.projectRole.accessToFiles
    boolean fileAccessInLdap = userProjectRole.project.unixGroup in ldapService.getGroupsOfUserByUsername(userProjectRole.user.username)
    if (fileAccessInOtp != fileAccessInLdap) {
        output << sprintf(
                format,
                [fileAccessInLdap, fileAccessInOtp, userProjectRole.projectRole.name,
                 userProjectRole.user.username, userProjectRole.project.name, userProjectRole.project.unixGroup]
        )
    }
}

println(output.join('\n'))

[]
