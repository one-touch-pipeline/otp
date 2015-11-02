package de.dkfz.tbi.otp.administration

import grails.plugin.springsecurity.annotation.Secured
import de.dkfz.tbi.otp.security.User

/**
 * Allows Switching to another User.
 *
 * This controller is meant for non-admin Users who have the privilege to switch
 * to another User, but it filters out admin and operator users. An admin user should
 * use the user administration controller where he can switch to any other user.
 *
 * Note: if a user with the role to switch user knows the internal URL
 * /j_spring_security_switch_user he can switch to any user without further protection.
 * So to say this is just a security by obscurity feature. But it is very unlikely that
 * a user with the role to switch user knows about this internal functionality.
 */
@Secured('ROLE_SWITCH_USER')
class SwitchUserController {
    /**
     * Dependency injection of userService
     */
    def userService

    def index() {
        [users: userService.switchableUsers()]
    }

    def switchUser(SwitchUserCommand cmd) {
        if (cmd.hasErrors()) {
            response.setStatus(404)
            return
        }
        String username = userService.switchableUser(cmd.id)
        if (!username) {
            response.setStatus(403)
            return
        }
        redirect(uri: "/j_spring_security_switch_user?j_username=${username}")
    }
}

class SwitchUserCommand {
    Long id
}
