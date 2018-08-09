package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.security.*
import grails.converters.*
import groovy.json.JsonBuilder
import groovy.transform.*
import org.apache.commons.lang.WordUtils
import org.springframework.validation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectUserController implements CheckAndCall {

    ProjectService projectService
    ProjectSelectionService projectSelectionService
    UserService userService
    UserProjectRoleService userProjectRoleService

    LdapService ldapService

    def index() {
        List<Project> projects = projectService.getAllProjects()
        if (!projects) {
            return [
                    projects: projects
            ]
        }

        ProjectSelection selection = projectSelectionService.getSelectedProject()
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)
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
            LdapUserDetails userMap = ldapService.getLdapUserDetailsByUsername(user.username)
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
                availableRoles: ProjectRole.findAll(),
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

    def updateProjectRole(UpdateProjectRoleCommand cmd) {
        if (cmd.hasErrors()) {
            flash.errors = cmd.errors.getFieldError().code
            flash.message = "An error occurred"
        } else {
            flash.errors = false
            flash.message = "Data stored successfully"
            userProjectRoleService.updateProjectRole(cmd.userProjectRole, ProjectRole.findByName(cmd.newRole))
        }
        redirect(action: "index")
    }

    def switchEnabledStatus(SwitchEnabledStatusCommand cmd) {
        if (cmd.hasErrors()) {
            flash.errors = cmd.errors.getFieldError().code
            flash.message = "An error occurred"
        } else {
            flash.errors = false
            flash.message = "User successfully ${cmd.enabled ? 'activated' : 'deactivated'}"
            userProjectRoleService.updateEnabledStatus(cmd.userProjectRole, cmd.enabled)
        }
        redirect(action: "index")
    }

    def addUserToProject(AddUserToProjectCommand cmd) {
        String message
        String errorMessage = ""
        if (cmd.hasErrors()) {
            FieldError cmdErrors = cmd.errors.getFieldError()
            errorMessage = "'" + cmdErrors.getRejectedValue() + "' is not a valid value for '" + cmdErrors.getField() + "'. Error code: '" + cmdErrors.code + "'"
            message = "An error occurred"
        } else {
            try {
                if (cmd.addViaLdap) {
                    userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(
                            cmd.project,
                            ProjectRole.findByName(cmd.projectRoleName),
                            cmd.searchString,
                            [
                                accessToOtp           : true, //TODO: OTP-2943: replace with input from GUI
                                accessToFiles         : false,
                                manageUsers           : false,
                                manageUsersAndDelegate: false,
                            ]
                    )
                } else {
                    userProjectRoleService.addExternalUserToProject(
                            cmd.project,
                            cmd.realName,
                            cmd.email,
                            ProjectRole.findByName(cmd.projectRoleName)
                    )
                }
                message = "Data stored successfully"
            } catch (AssertionError e) {
                message = "An error occurred"
                errorMessage = e.message
            }
        }
        flash.message = message
        flash.errors = errorMessage
        redirect(controller: "projectUser")
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

    JSON getUserSearchSuggestions(UserSearchSuggestionsCommand cmd) {
        render new JsonBuilder(ldapService.getListOfLdapUserDetailsByUsernameOrMailOrRealName(cmd.searchString, 20)) as JSON
    }
}

@TupleConstructor
enum PermissionStatus {
    APPROVED, PENDING_APPROVAL, PENDING_DENIAL, DENIED

    @Override
    String toString() {
        return WordUtils.uncapitalize(WordUtils.capitalizeFully(this.name(), ['_'] as char[]).replaceAll("_", ""))
    }
}

class UserEntry {
    boolean inLdap
    User user
    String realName
    String thumbnailPhoto
    String department
    String projectRoleName
    PermissionStatus otpAccess
    PermissionStatus fileAccess
    PermissionStatus manageUsers
    boolean deactivated
    UserProjectRole userProjectRole

    UserEntry(User user, Project project, LdapUserDetails ldapUserDetails) {
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        boolean fileAccess = userProjectRole.accessToFiles
        this.inLdap = ldapUserDetails.cn
        this.user = user
        this.realName = ldapUserDetails.realName
        this.thumbnailPhoto = ldapUserDetails.thumbnailPhoto.encodeAsBase64()
        this.department = ldapUserDetails.department
        this.projectRoleName = userProjectRole.projectRole.name
        this.otpAccess = inLdap && userProjectRole.accessToOtp ? PermissionStatus.APPROVED : PermissionStatus.DENIED
        this.fileAccess = getPermissionStatus(inLdap && fileAccess, project.unixGroup in ldapUserDetails.memberOfGroupList)
        this.manageUsers = inLdap && userProjectRole.manageUsers ? PermissionStatus.APPROVED : PermissionStatus.DENIED
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
            if (obj.userProjectRole?.manageUsersAndDelegate) {
                return 'This right can not be changed for users of this role'
            }
            if (!obj.userProjectRole?.accessToOtp) {
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

class SwitchEnabledStatusCommand implements Serializable {
    UserProjectRole userProjectRole
    boolean enabled

    static constraints = {
        enabled(validator: {val, obj ->
            if (val == obj.userProjectRole?.enabled) {
                return 'No Change'
            }
        })
    }

    void setEnabled(boolean value) {
        enabled = !value
    }
}

class UpdateProjectRoleCommand implements Serializable {
    UserProjectRole userProjectRole
    String newRole
    static constraints = {
        newRole(validator: { val, obj ->
            if (val == obj.userProjectRole.projectRole.name) {
                return 'No Change'
            }
        })
    }

    void setValue(String value) {
        this.newRole = value
    }
}

class AddUserToProjectCommand implements Serializable {
    Project project
    boolean addViaLdap = true

    String searchString
    String projectRoleName

    String realName
    String email

    static constraints = {
        addViaLdap(blank: false)
        searchString(nullable: true, validator: { val, obj ->
            if (obj.addViaLdap && !obj.searchString?.trim()) {
                return "searchString can not be empty"
            }
        })
        projectRoleName(nullable: true, validator: { val, obj ->
            if (!obj.projectRoleName) {
                return "No project role selected"
            }
        })
        realName(nullable: true, validator: { val, obj ->
            if (!obj.addViaLdap && !obj.realName?.trim()) {
                return "Real name can not be empty"
            }
        })
        email(nullable: true, email: true, validator: { val, obj ->
            if (!obj.addViaLdap && !obj.email?.trim()) {
                return "Email can not be empty"
            }
        })
    }

    void setAddViaLdap(String selectedMethod) {
        this.addViaLdap = (selectedMethod == 'true')
    }

    void setSearchString(String searchString) {
        this.searchString = searchString?.trim()?.replaceAll(" +", " ")
    }

    void setRealName(String realName) {
        this.realName = realName?.trim()?.replaceAll(" +", " ")
    }

    void setEmail(String email) {
        this.email = email?.trim()?.replaceAll(" ", "")
    }
}

class UserSearchSuggestionsCommand implements Serializable {
    String searchString

    void setValue(String value) {
        this.searchString = value
    }
}
