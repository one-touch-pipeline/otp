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

    private static final List permissions = ["OTP_READ_ACCESS", "MANAGE_USERS", "DELEGATE_USER_MANAGEMENT", "ADD_USER"]

    @Override
    boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) throws IllegalArgumentException {
        if (!auth) {
            return false
        }
        if (permission instanceof String && permission in permissions) {
            switch (targetDomainObject?.class) {
                case Project:
                    return checkProjectRolePermission(auth, (Project) targetDomainObject, permission)
                case null:
                    return checkObjectIndependentPermission(auth, permission)
                default:
                    return false
            }
        }
        return aclPermissionEvaluator.hasPermission(auth, targetDomainObject, permission)
    }

    @Override
    boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) throws IllegalArgumentException {
        if (!auth) {
            return false
        }
        if (permission instanceof String && permission in permissions) {
            switch (targetType) {
                case Project.name:
                    return checkProjectRolePermission(auth, Project.get(targetId), (String) permission)
                case null:
                    return checkObjectIndependentPermission(auth, permission)
                default:
                    return false
            }
        }
        return aclPermissionEvaluator.hasPermission(auth, targetId, targetType, permission)
    }

    @CompileDynamic
    private boolean checkObjectIndependentPermission(Authentication auth, String permission) {
        User activeUser = User.findByUsername(auth.principal.username)
        if (!activeUser) {
            return false
        }

        if (!activeUser.enabled) {
            return false
        }

        switch (permission) {
            case "ADD_USER":
                return UserProjectRole.createCriteria().list {
                    eq("user", activeUser)
                    or {
                        eq("manageUsers", true)
                        eq("manageUsersAndDelegate", true)
                    }
                    eq("enabled", true)
                }
            default:
                return false
        }
    }

    @CompileDynamic
    private boolean checkProjectRolePermission(Authentication auth, Project project, String permission) {
        User activeUser = User.findByUsername(auth.principal.username)
        if (!activeUser) {
            return false
        }

        UserProjectRole userProjectRole = UserProjectRole.findByProjectAndUser(project, activeUser)
        if (!userProjectRole) {
            return false
        }
        if (!(activeUser.enabled && userProjectRole.enabled)) {
            return false
        }

        switch (permission) {
            case "OTP_READ_ACCESS":
                return userProjectRole.accessToOtp
            case "MANAGE_USERS":
                return userProjectRole.getManageUsers()
            case "DELEGATE_USER_MANAGEMENT":
                return userProjectRole.manageUsersAndDelegate
            default:
                return false
        }
    }
}
