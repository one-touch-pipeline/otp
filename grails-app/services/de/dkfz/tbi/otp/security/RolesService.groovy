package de.dkfz.tbi.otp.security

import org.springframework.security.access.prepost.*
import org.springframework.security.acls.domain.*

class RolesService {

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<RolesWithUsers> getRolesAndUsers() {
        List<RolesWithUsers> roles = Role.list().collect { Role role ->
            new RolesWithUsers(role: role)
        }
        roles.each {
            it.users = UserRole.findAllByRole(it.role)*.user.flatten()
            it.users.sort { User user -> user.username }
        }
        roles.sort { it.role.authority }
        return roles
    }
}

class RolesWithUsers {
    Role role
    List<User> users = []
}
