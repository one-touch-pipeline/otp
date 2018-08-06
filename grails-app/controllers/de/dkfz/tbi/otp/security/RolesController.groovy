package de.dkfz.tbi.otp.security

class RolesController {

    RolesService rolesService

    def index() {
        List<RolesWithUsers> allRoles = rolesService.getRolesAndUsers()
        List<RolesWithUsers> rolesAndUsers = allRoles.findAll { it.role.authority.startsWith("ROLE_") }
        List<RolesWithUsers> groupsAndUsers = allRoles.findAll { it.role.authority.startsWith("GROUP_") }
        List<RolesWithUsers> others = allRoles - rolesAndUsers - groupsAndUsers

        return [
                roleLists: [ rolesAndUsers, groupsAndUsers, others ]
        ]
    }
}
