package de.dkfz.tbi.otp.security

class RolesController {

    RolesService rolesService

    def index() {
        List<RolesWithUsersAndSeqCenters> roles = rolesService.getRolesAndUsers()

        List<RolesWithUsersAndSeqCenters> groupsAndUsers = roles.findAll { it.role.authority.startsWith("GROUP_") }
        rolesService.findSeqCenters(groupsAndUsers)

        roles = roles.findAll { !it.role.authority.startsWith("GROUP_") }

        return [
                rolesAndUsers: roles,
                groupsAndUsers: groupsAndUsers,
        ]
    }
}
