package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.security.*
import grails.converters.*
import groovy.transform.*
import org.springframework.validation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectUserController {

    ProjectService projectService
    ProjectSelectionService projectSelectionService
    ProjectOverviewService projectOverviewService
    UserService userService
    UserProjectRoleService userProjectRoleService

    LdapService ldapService

    def index() {
        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects.first()
        }

        project = exactlyOneElement(Project.findAllByName(project.name, [fetch: [projectCategories: 'join', projectGroup: 'join']]))

        List<User> projectUsers = UserProjectRole.findAllByProject(project)*.user
        List<String> nonDatabaseUsers = []

        String groupDistinguishedName = ldapService.getDistinguishedNameOfGroupByGroupName(project.unixGroup)
        List<String> groupMemberUsername = ldapService.getGroupMembersByDistinguishedName(groupDistinguishedName)

        groupMemberUsername.each { String username ->
            User user = User.findByUsername(username)
            if (user) {
                projectUsers.add(user)
            } else {
                nonDatabaseUsers.add(username)
            }
        }

        projectUsers.unique()
        projectUsers.sort { it.username }

        List<UserEntry> enabledProjectUsers = []
        List<UserEntry> disabledProjectUsers = []
        List<String> usersWithoutUserProjectRole = []
        projectUsers.each { User user ->
            LdapUserDetails userMap = ldapService.getUserMapByUsername(user.username)
            UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
            if (userProjectRole) {
                UserEntry userEntry = new UserEntry(user, project, userMap)
                if (userProjectRole.enabled) {
                    enabledProjectUsers.add(userEntry)
                } else {
                    disabledProjectUsers.add(userEntry)
                }
            } else {
                usersWithoutUserProjectRole.add(user.username)
            }
        }

        return [
                projects: projects,
                project: project,
                enabledProjectUsers: enabledProjectUsers,
                disabledProjectUsers: disabledProjectUsers,
                usersWithoutUserProjectRole: usersWithoutUserProjectRole,
                unknownUsersWithFileAccess: nonDatabaseUsers,
                availableRoles: ProjectRole.findAll()*.name,
                hasErrors: params.hasErrors,
                message: params.message,
        ]
    }

    def updateManageUsers(UpdateManageUsersCommand cmd) {
        if (cmd.hasErrors()) {
            flash.errors = cmd.errors.getFieldError().code
            flash.message = "An error occurred"
        } else {
            flash.errors = false
            flash.message = "Data stored successfully"
            userProjectRoleService.updateManageUsers(cmd.userProjectRole, cmd.manageUsers)
        }
        redirect(action: "index")
    }

    JSON updateName(UpdateUserRealNameCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.updateRealName(cmd.user, cmd.newName) })
    }

    JSON updateEmail(UpdateUserEmailCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.updateEmail(cmd.user, cmd.newEmail) })
    }

    JSON updateAspera(UpdateUserAsperaCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.updateAsperaAccount(cmd.user, cmd.newAspera) })
    }

    JSON updateProjectRole(UpdateProjectRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, { userProjectRoleService.updateProjectRole(cmd.userProjectRole, ProjectRole.findByName(cmd.newRole)) })
    }

    private void checkErrorAndCallMethod(Serializable cmd, Closure method) {
        Map data
        if (cmd.hasErrors()) {
            data = getErrorData(cmd.errors.getFieldError())
        } else {
            method()
            data = [success: true]
        }
        render data as JSON
    }

    private Map getErrorData(FieldError errors) {
        return [success: false, error: "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"]
    }
}

@TupleConstructor
enum PermissionStatus {
    APPROVED("ok.png", "access approved"),
    PENDING_APPROVAL("ok_grey.png", "access will be granted"),
    PENDING_DENIAL("error_grey.png", "access will be removed"),
    DENIED("error.png", "access denied")

    final String iconName
    final String description
}

class UserEntry {
    User user
    String thumbnailPhoto
    String department
    ProjectRole projectRole
    PermissionStatus otpAccess
    PermissionStatus fileAccess
    boolean manageUsers = false
    boolean deactivated
    UserProjectRole userProjectRole

    UserEntry(User user, Project project, LdapUserDetails ldapUserDetails) {
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        ProjectRole projectRole = userProjectRole?.projectRole
        boolean fileAccess = projectRole?.accessToFiles ?: false
        this.user = user
        this.thumbnailPhoto = ldapUserDetails.thumbnailPhoto.encodeAsBase64()
        this.department = ldapUserDetails.department
        this.projectRole = projectRole
        this.otpAccess = projectRole?.accessToOtp ? PermissionStatus.APPROVED : PermissionStatus.DENIED
        this.fileAccess = getPermissionStatus(fileAccess, project.unixGroup in ldapUserDetails.memberOfGroupList)
        this.manageUsers = projectRole?.manageUsersAndDelegate || userProjectRole?.manageUsers
        this.deactivated = ldapUserDetails.deactivated
        this.userProjectRole = userProjectRole
    }

    private static PermissionStatus getPermissionStatus(boolean hasProjectRolePermission, boolean isMemberOfGroup) {
        if (hasProjectRolePermission && isMemberOfGroup) {
            return PermissionStatus.APPROVED
        } else if (hasProjectRolePermission && !isMemberOfGroup) {
            return PermissionStatus.PENDING_APPROVAL
        } else if (!hasProjectRolePermission && isMemberOfGroup) {
            return PermissionStatus.PENDING_DENIAL
        } else if (!hasProjectRolePermission && !isMemberOfGroup) {
            return PermissionStatus.DENIED
        }
    }
}

class UpdateUserRealNameCommand implements Serializable {
    User user
    String newName
    static constraints = {
        newName(blank: false, validator: {val, obj ->
            if (val == obj.user?.realName) {
                return 'No Change'
            }
        })
    }
    void setValue(String value) {
        this.newName = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateUserEmailCommand implements Serializable {
    User user
    String newEmail
    static constraints = {
        newEmail(nullable: false, email:true, blank: false, validator: {val, obj ->
            if (val == obj.user?.email) {
                return 'No Change'
            } else if (User.findByEmail(val)) {
                return 'Duplicate'
            }
        })
    }
    void setValue(String value) {
        this.newEmail = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateUserAsperaCommand implements Serializable {
    User user
    String newAspera
    static constraints = {
        newAspera(blank: true, nullable: true, validator: {val, obj ->
            if (val == obj.user?.asperaAccount) {
                return 'No Change'
            }
        })
    }
    void setValue(String value) {
        this.newAspera = value?.trim()?.replaceAll(" +", " ") ?: null
    }
}

class UpdateManageUsersCommand implements Serializable {
    UserProjectRole userProjectRole
    boolean manageUsers

    static constraints = {
        manageUsers(validator: {val, obj ->
            if (obj.userProjectRole?.projectRole?.manageUsersAndDelegate) {
                return 'This right can not be changed for users of this role'
            }
            if (!obj.userProjectRole?.projectRole?.accessToOtp) {
                return 'User needs access to OTP to gain this right'
            }
            if (val == obj.userProjectRole?.manageUsers) {
                return 'No Change'
            }
        })
    }

    void setManageUsers(boolean value) {
        manageUsers = !value
    }
}

class UpdateProjectRoleCommand implements Serializable {
    UserProjectRole userProjectRole
    String newRole
    static constraints = {
        newRole(validator: {val, obj ->
            if (val == obj.userProjectRole.projectRole.name) {
                return 'No Change'
            }
        })
    }
    void setValue(String value) {
        this.newRole = value
    }
}
