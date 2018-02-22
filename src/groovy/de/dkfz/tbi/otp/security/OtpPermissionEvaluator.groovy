package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import grails.compiler.*
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.*
import org.springframework.security.access.*
import org.springframework.security.acls.*
import org.springframework.security.core.*
import org.springframework.stereotype.*

@Component
@GrailsCompileStatic
class OtpPermissionEvaluator implements PermissionEvaluator {

    @Autowired AclPermissionEvaluator aclPermissionEvaluator

    private static final List permissions = ["OTP_READ_ACCESS", "MANAGE_USERS", "DELEGATE_USER_MANAGEMENT"]

    @Override
    boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) throws IllegalArgumentException {
        if (!auth || !targetDomainObject) {
            return false
        }
        if (permission instanceof String && permission in permissions) {
            switch (targetDomainObject.class) {
                case Project:
                    return checkProjectRolePermission(auth, (Project) targetDomainObject, permission)
                default:
                    return false
            }
        }
        return aclPermissionEvaluator.hasPermission(auth, targetDomainObject, permission)
    }

    @Override
    boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) throws IllegalArgumentException {
        if (!auth || !targetType) {
            return false
        }
        if (permission instanceof String && permission in permissions) {
            switch (targetType) {
                case Project.name:
                    return checkProjectRolePermission(auth, Project.get(targetId), (String) permission)
                default:
                    return false
            }
        }
        return aclPermissionEvaluator.hasPermission(auth, targetId, targetType, permission)
    }

    @CompileDynamic
    private boolean checkProjectRolePermission(Authentication auth, Project project, String permission) {
        User user = User.findByUsername(auth.principal.username)
        if (!user) {
            return false
        }

        UserProjectRole userProjectRole = UserProjectRole.findByProjectAndUser(project, user)
        if (!userProjectRole) {
            return false
        }
        if (!(user.enabled && userProjectRole.enabled)) {
            return false
        }

        switch (permission) {
            case "OTP_READ_ACCESS":
                return userProjectRole.projectRole?.accessToOtp
            case "MANAGE_USERS":
                return userProjectRole.getManageUsers()
            case "DELEGATE_USER_MANAGEMENT":
                return userProjectRole.projectRole?.manageUsersAndDelegate
            default:
                return false
        }
    }
}
