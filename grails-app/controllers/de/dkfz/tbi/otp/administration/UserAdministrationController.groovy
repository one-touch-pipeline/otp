package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.user.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*
import grails.plugin.springsecurity.*
import grails.plugin.springsecurity.annotation.*
import org.springframework.validation.*

/**
 * @short Controller for user management.
 *
 * This controller is only useful to administrators and allows to manage all users.
 * There is a list of users which is rendered in a dataTable and allows to modify
 * the users' properties.
 */
@Secured('ROLE_ADMIN')
class UserAdministrationController {
    /**
     * Dependency Injection of RemoteUserService
     */
    UserService userService
    /**
     * Dependency Injection of SpringSecurityService
     */
    SpringSecurityService springSecurityService
    /**
     * Dependency Injection of GroupService
     */
    GroupService groupService

    /**
     * Default action showing the DataTable markup
     */
    def index = {
    }

    /**
     * Action to show the create User view
     */
    def create() {
        [roles: userService.getAllRoles(), groups: groupService.availableGroups()]
    }

    def createUser(CreateUserCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd as JSON
            return
        }
        Map data = [success: true, user: userService.createUser(cmd).sanitizedUserMap()]
        render data as JSON
    }

    /**
     * Action returning the DataTable content as JSON
     */
    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        dataToRender.iTotalRecords = userService.getUserCount()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List users = userService.getAllUsers()

        users.each { User user ->
            dataToRender.aaData << [user.id, user.username, user.email, user.enabled, user.accountExpired, user.accountLocked, user.passwordExpired]
        }
        render dataToRender as JSON
    }

    /**
     * Action to enable a given user
     */
    def enable = {
        try {
            Map data = [success: userService.enableUser(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            Map data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)lock a given user
     */
    def lockAccount = {
        try {
            Map data = [success: userService.lockAccount(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            Map data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)expire a given user
     */
    def expireAccount = {
        try {
            Map data = [success: userService.expireAccount(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            Map data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)expire the password of a given user
     */
    def expirePassword = {
        try {
            Map data = [success: userService.expirePassword(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            Map data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to edit an user
     */
    def show = {
        /*if (!springSecurityService.isAjax(request)) {
            render(template: "/templates/page", model: [link: g.createLink(action: "show", id: params.id), callback: "loadAdminUserCallback"])
            return
        }*/
        User user = userService.getUser(params.id as Long)
        List<Group> userGroups = groupService.groupsForUser(user)
        List<Group> availableGroups = []
        groupService.availableGroups().each { Group group ->
            if (!userGroups.contains(group)) {
                availableGroups << group
            }
        }
        [user: user, roles: userService.getAllRoles(), userRoles: userService.getRolesForUser(params.id as Long), groups: availableGroups, userGroups: userGroups]
    }

    /**
     * Action to add a role to a user
     */
    def addRole = { AddRemoveRoleCommand cmd ->
        Map data = [:]
        if (cmd.hasErrors()) {
            data.put("error", g.message(code: "user.administration.userRole.error.general"))
        } else {
            try {
                userService.addRoleToUser(cmd.userId, cmd.id)
                data.put("success", "true")
            } catch (UserNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.userNotFound"))
            } catch (RoleNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.roleNotFound"))
            }
        }
        render data as JSON
    }

    /**
     * Action to remove a role from a user
     */
    def removeRole = { AddRemoveRoleCommand cmd ->
        Map data = [:]
        if (cmd.hasErrors()) {
            data.put("error", g.message(code: "user.administration.userRole.error.general"))
        } else {
            try {
                userService.removeRoleFromUser(cmd.userId, cmd.id)
                data.put("success", "true")
            } catch (UserNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.userNotFound"))
            } catch (RoleNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.roleNotFound"))
            }
        }
        render data as JSON
    }


    /**
     * Action for editing non-security relevant user information.
     */
    def editUser = { EditUserCommand cmd ->
        checkErrorAndCallMethod(cmd, { userService.editUser(cmd) })
    }

    /**
     * Creates a given Group from command object
     * @param command
     * @return
     */
    def createGroup(GroupCommand command) {
        Map result = [:]
        if (command.hasErrors()) {
            List errors = []
            command.errors.allErrors.each {
                errors.add([field: it.getArguments()[0], message: "Error code: ${it.code}"])
            }
            result.put("errors", errors)
            render result as JSON
            return
        }
        try {
            Group group = groupService.createGroup(command)
            result.put("success", true)
            result.put("group", group)
        } catch (GroupCreationException e) {
            result.put("errors", [[field: 'name', message: e.message]])
        }
        render result as JSON
    }

    /**
     * Adds the user to the group
     * @param command
     * @return
     */
    def addGroup(AddRemoveGroupCommand command) {
        groupService.addUserToGroup(userService.getUser(command.userId), groupService.getGroup(command.id))
        def result = [success: true]
        render result as JSON
    }

    /**
     * Removes the user from the group
     * @param command
     * @return
     */
    def removeGroup(AddRemoveGroupCommand command) {
        groupService.removeUserFromGroup(userService.getUser(command.userId), groupService.getGroup(command.id))
        def result = [success: true]
        render result as JSON
    }

    private void checkErrorAndCallMethod(EditUserCommand cmd, Closure method) {
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


    /**
     * Resolves the error message for a field error
     * @param cmd The RegistrationCommand for resolving the errors
     * @param field The field to be tested
     * @param description A descriptive name of the field to be passed to unknown errors
     * @return The resolved error message or @c null if there is no error
     */
    private String resolveErrorMessage(RegistrationCommand cmd, String field, String description) {
        if (cmd.errors.getFieldError(field)) {
            switch (cmd.errors.getFieldError(field).code) {
            case "blank":
                return g.message(code: "user.administration.register.error.${field}.blank")
            case "validator.invalid":
                return g.message(code: "user.administration.register.error.${field}.invalid")
            case "email.invalid":
                return g.message(code: "user.administration.register.error.${field}.invalid")
            default:
                return g.message(code: "error.unknown", args: [description])
            }
        }
        return null
    }
}

/**
 * @short Command Object for add/remove role actions
 */
class AddRemoveRoleCommand {
    Long id
    Long userId

    static constraints = {
        id(nullable: false)
        userId(nullable: false)
    }
}

/**
 * @short Command object for User registration
 */
class RegistrationCommand {
    String username
    String email

    static constraints = {
        username(nullable: false, blank: false)
        email(nullable: false, email: true, blank: false)
    }

    User toUser() {
        return new User(username: this.username, email: this.email)
    }
}

class AddRemoveGroupCommand implements Serializable {
    private static final long serialVersionUID = 1L
    long id
    long userId
}
