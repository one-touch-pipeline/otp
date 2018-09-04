package de.dkfz.tbi.otp.security

class RolesController {

    RolesService rolesService

    def index() {
        List<RolesWithUsers> roles = rolesService.getRolesAndUsers()

        List<RolesWithUsers> groupsAndUsers = roles.findAll { it.role.authority.startsWith("GROUP_") }

        roles = roles.findAll { !it.role.authority.startsWith("GROUP_") }

        return [
                rolesAndUsers: roles,
                groupsAndUsers: groupsAndUsers,
        ]
    }
}
