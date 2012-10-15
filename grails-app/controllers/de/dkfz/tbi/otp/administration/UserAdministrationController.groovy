package de.dkfz.tbi.otp.administration

import grails.plugins.springsecurity.Secured
import grails.converters.JSON
import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.user.RoleNotFoundException
import de.dkfz.tbi.otp.user.UserNotFoundException
import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.otp.OtpException

/**
 * @short Controller for user management.
 *
 * This controller is only useful to administrators and allows to manage all users.
 * There is a list of users which is rendered in a dataTable and allows to modify
 * the users' properties.
 *
 */
@Secured('ROLE_ADMIN')
class UserAdministrationController {
    /**
     * Dependency Injection of RemoteUserService
     */
    def userService
    /**
     * Dependency Injection of SpringSecurityService
     */
    def springSecurityService
    /**
     * Dependency Injection of GroupService
     */
    def groupService

    /**
     * Default action showing the DataTable markup
     */
    def index = {
    }

    /**
     * Action returning the DataTable content as JSON
     */
    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        dataToRender.iTotalRecords = userService.getUserCount()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List users = userService.getAllUsers(cmd.iDisplayStart, cmd.iDisplayLength)
        users.each { user ->
            dataToRender.aaData << [user.id, user.username, user.email, user.jabberId, user.enabled, user.accountExpired, user.accountLocked, user.passwordExpired]
        }
        render dataToRender as JSON
    }

    /**
     * Action to enable a given user
     */
    def enable = {
        try {
            def data = [success: userService.enableUser(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)lock a given user
     */
    def lockAccount = {
        try {
            def data = [success: userService.lockAccount(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)expire a given user
     */
    def expireAccount = {
        try {
            def data = [success: userService.expireAccount(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)expire the password of a given user 
     */
    def expirePassword = {
        try {
            def data = [success: userService.expirePassword(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
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
        [user: user, roles: userService.getAllRoles(), userRoles: userService.getRolesForUser(params.id as Long), groups: availableGroups, userGroups: userGroups,
            next: userService.getNextUser(user),
            previous: userService.getPreviousUser(user)]
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
     * Action to render the view to register a new user as admin
     */
    def register = {
    }

    /**
     * Action to perform the registration of a new user as admin
     */
    def performRegistration = { RegistrationCommand cmd ->
        def data = [:]
        if (cmd.hasErrors()) {
            data.put("error", true)
            data.put("username", resolveErrorMessage(cmd, "username", "User Name"))
            data.put("email", resolveErrorMessage(cmd, "email", "Email"))
        } else {
            try {
                data.put("user", userService.register(cmd.toUser()))
                data.put("success", true)
            } catch (OtpException e) {
                data.clear()
                data.put("error", true)
                data.put("username", e.message)
            }
        }
        render data as JSON
    }

    /**
     * Action for editing non-security relevant user information.
     */
    def editUser = { EditUserCommand cmd ->
        Map data = [:]
        if (cmd.hasErrors()) {
            data.put("error", true)
            data.put("username", resolveErrorMessage(cmd, "username", "Username"))
            data.put("email", resolveErrorMessage(cmd, "email", "Email"))
            data.put("jabber", resolveErrorMessage(cmd, "jabber", "Jabber"))
        } else {
            userService.editUser(cmd.toUser())
            data.put("success", true)
        }
        render data as JSON
    }

    /**
     * Creates a given Group from command object
     * @param command
     * @return
     */
    def createGroup(GroupCommand command) {
        if (command.hasErrors()) {
            render command.errors as JSON
            return
        }
        Map result = [:]
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

    private String resolveErrorMessage(EditUserCommand cmd, String field, String description) {
        if (cmd.errors.getFieldError(field)) {
            switch (cmd.errors.getFieldError(field).code) {
            case "blank":
                return g.message(code: "user.administration.${field}.blank")
            case "email.invalid":
                return g.message(code: "user.administration.${field}.invalid")
            default:
                return g.message(code: "error.unknown", args: [description])
            }
        }
        return null
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
    String jabber

    static constraints = {
        username(nullable: false, blank: false)
        email(nullable: false, email: true, blank: false)
        jabber(nullable: true, blank: true, email: true)
    }

    User toUser() {
        return new User(username: this.username, email: this.email, jabberId: this.jabber)
    }
}

/**
 * @short Command Object to validate the user before editing.
 */
class EditUserCommand implements Serializable {
    private static final long serialVersionUID = 1L
    String username
    String email
    String jabber

    static constraints = {
        username(nullable: false, blank: false)
        email(nullable: false, blank: false, email: true)
        jabber(nullable: true, blank: true, email: true)
    }

    /**
     *
     * @return The command object as a User
     */
    User toUser() {
        return new User(username: this.username, email: this.email, jabberId: this.jabber)
    }
}

class AddRemoveGroupCommand implements Serializable {
    private static final long serialVersionUID = 1L
    long id
    long userId
}
