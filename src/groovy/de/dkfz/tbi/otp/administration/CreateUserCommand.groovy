package de.dkfz.tbi.otp.administration

import grails.validation.Validateable

import java.io.Serializable

import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User

@Validateable
class CreateUserCommand implements Serializable {

    String username
    String email
    String realName
    String asperaAccount
    List<Long> role
    List<Long> group

    static constraints = {
        username(validator: { value ->
            return User.findByUsername(value) == null
        })
        email(email: true)
        realName(blank: false, nullable: true)
        asperaAccount(nullable: true)
        role(nullable: true, validator: { value ->
            boolean valid = true
            value.each { id ->
                if (!Role.get(id)) {
                    valid = false
                }
            }
            return valid
        })
        group(nullable: true, validator: { value ->
            boolean valid = true
            value.each { id ->
                if (!Group.get(id)) {
                    valid = false
                }
            }
            return valid
        })
    }
}
