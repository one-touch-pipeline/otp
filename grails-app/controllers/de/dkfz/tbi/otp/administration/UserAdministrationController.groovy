/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.administration

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.util.TimeFormats

/**
 * @short Controller for user management.
 *
 * This controller is only useful to administrators and allows to manage all users.
 * There is a list of users which is rendered in a dataTable and allows to modify
 * the users' properties.
 */
@Secured("hasRole('ROLE_ADMIN')")
class UserAdministrationController implements CheckAndCall {

    static allowedMethods = [
            index          : "GET",
            dataTableSource: "POST",
            enable         : "POST",
            show           : "GET",
            ldapProperties : "GET",
            editUser       : "POST",
            addRole        : "POST",
            removeRole     : "POST",
    ]

    /**
     * Dependency Injection of RemoteUserService
     */
    UserService userService
    UserProjectRoleService userProjectRoleService
    LdapService ldapService

    /**
     * Default action showing the DataTable markup
     */
    def index() {
    }

    /**
     * Action returning the DataTable content as JSON
     */
    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        dataToRender.iTotalRecords = userService.userCount
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List users = userService.allUsers

        users.each { User user ->
            String dateString
            if (user.plannedDeactivationDate) {
                dateString = TimeFormats.DATE_TIME.getFormattedDate(user.plannedDeactivationDate)
            } else {
                dateString = ""
            }
            dataToRender.aaData << [
                    id                   : user.id,
                    username             : user.username,
                    realname             : user.realName,
                    email                : user.email,
                    deactivationDate     : dateString,
                    enabled              : user.enabled,
                    acceptedPrivacyPolicy: user.acceptedPrivacyPolicy,
            ]
        }
        render dataToRender as JSON
    }

    JSON enable(UpdateUserFlagCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.enableUser(cmd.user, cmd.flag) })
    }

    def show(SelectUserCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("An error occurred", cmd.errors)
            redirect(action: "index")
            return [:]
        }
        User user = cmd.user
        Map<String, List<Role>> roleLists = [:]
        roleLists['userRole'] = userService.getRolesOfUser(user)
        roleLists['availableRole'] = userService.allRoles - roleLists['userRole']
        roleLists['userGroup'] = userService.getGroupsOfUser(user)
        roleLists['availableGroup'] = userService.allGroups - roleLists['userGroup']

        return [
                userProjectRoles       : userProjectRoleService.findAllByUser(user).sort { it.project.name },
                user                   : user,
                ldapGroups             : ldapService.getGroupsOfUser(user) ?: [],
                roleLists              : roleLists,
                userExistsInLdap       : ldapService.existsInLdap(user),
                userAccountControlValue: ldapService.getUserAccountControlOfUser(user),
                userAccountControlMap  : ldapService.getAllUserAccountControlFlagsOfUser(user),
                cmd                    : flash.cmd as EditUserCommand,
        ]
    }

    def ldapProperties(SelectUserCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("An error occurred", cmd.errors)
            redirect(action: "index")
            return [:]
        }
        User user = cmd.user
        boolean userExists = ldapService.existsInLdap(user)
        Map ldapProperties = userExists ? ldapService.getAllLdapValuesForUser(user) : [:]

        return [
                user            : user,
                userExistsInLdap: userExists,
                ldapProperties  : ldapProperties,
        ]
    }

    def editUser(EditUserCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("An error occurred", cmd.errors)
            flash.cmd = cmd
        } else {
            flash.message = new FlashMessage("User ${cmd.user.username} successfully edited")
            userService.editUser(cmd.user, cmd.email, cmd.realName)
        }
        redirect(action: "show", params: ["user.id": cmd.user.id])
    }

    JSON addRole(AddOrRemoveRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.addRoleToUser(cmd.user, cmd.role) })
    }

    JSON removeRole(AddOrRemoveRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.removeRoleFromUser(cmd.user, cmd.role) })
    }
}

class SelectUserCommand implements Validateable {
    User user
}

class AddOrRemoveRoleCommand extends SelectUserCommand {
    Role role
}

class UpdateUserFlagCommand extends SelectUserCommand {
    boolean flag
}
