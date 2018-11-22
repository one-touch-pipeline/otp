package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
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

    LdapService ldapService

    /**
     * Default action showing the DataTable markup
     */
    def index() {

    }

    /**
     * Action to show the create User view
     */
    def create() {
        return [ roles: Role.findAll().sort { it.authority } ]
    }

    def createUser(CreateUserCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd as JSON
            return
        }
        List<Role> roles = Role.findAllByIdInList(cmd.role)
        Map data = [
                success: true,
                user: userService.createUser(
                        cmd.username,
                        cmd.email,
                        cmd.realName,
                        cmd.asperaAccount,
                        roles,
                        cmd.group,
                ).sanitizedUserMap()
        ]
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
            dataToRender.aaData << [user.id, user.username, user.email, user.enabled, user.accountExpired, user.accountLocked]
        }
        render dataToRender as JSON
    }

    JSON enable(UpdateUserFlagCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.enableUser(cmd.user, cmd.flag) })
    }

    JSON lockAccount(UpdateUserFlagCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.lockAccount(cmd.user, cmd.flag) })
    }

    JSON expireAccount(UpdateUserFlagCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.expireAccount(cmd.user, cmd.flag) })
    }

    def show(SelectUserCommmand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("An error occurred", [cmd.errors.getFieldError().code])
            redirect(action: "index")
            return
        }
        User user = cmd.user
        Map<String, List<Role>> roleLists = [:]
        roleLists['userRole'] = userService.getRolesOfUser(user)
        roleLists['availableRole'] = userService.getAllRoles() - roleLists['userRole']
        roleLists['userGroup'] = userService.getGroupsOfUser(user)
        roleLists['availableGroup'] = userService.getAllGroups() - roleLists['userGroup']

        return [
                user: user,
                userProjectRolesSortedByProjectName: UserProjectRole.findAllByUser(user).sort { it.project.name },
                ldapGroups: ldapService.getGroupsOfUserByUsername(user.username) ?: [],
                roleLists: roleLists,
        ]
    }

    def editUser(EditUserCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("An error occurred", [cmd.errors.getFieldError().code])
        } else {
            flash.message = new FlashMessage("User ${cmd.user.username} successfully edited")
            userService.editUser(cmd.user, cmd.email, cmd.realName, cmd.asperaAccount)
        }
        redirect(action: "show", params: ['user.id': cmd.user.id] )
    }

    JSON addRole(AddOrRemoveRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.addRoleToUser(cmd.user, cmd.role) })
    }

    JSON removeRole(AddOrRemoveRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.removeRoleFromUser(cmd.user, cmd.role) })
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

class AddOrRemoveRoleCommand implements Serializable {
    User user
    Role role
}

class UpdateUserFlagCommand implements Serializable {
    User user
    boolean flag

    static constraints = {
        user(nullable: false)
        flag(nullable: false)
    }
}

class SelectUserCommmand implements Serializable {
    User user
}
