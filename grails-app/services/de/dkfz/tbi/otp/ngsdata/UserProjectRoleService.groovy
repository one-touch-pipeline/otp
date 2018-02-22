package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize
import grails.validation.ValidationException
import org.springframework.validation.Errors

class UserProjectRoleService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'DELEGATE_USER_MANAGEMENT')")
    UserProjectRole updateManageUsers(UserProjectRole userProjectRole, boolean manageUsers) {
        assert userProjectRole: "the input userProjectRole must not be null"
        userProjectRole.manageUsers = manageUsers
        assert userProjectRole.save(flush: true, failOnError: true)
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateProjectRole(UserProjectRole userProjectRole, ProjectRole newProjectRole) {
        assert userProjectRole: "the input userProjectRole must not be null"
        assert newProjectRole: "the input projectRole must not be null"
        updateManageUsers(userProjectRole, false)
        userProjectRole.projectRole = newProjectRole
        assert userProjectRole.save(flush: true, failOnError: true)
        return userProjectRole
    }
}
