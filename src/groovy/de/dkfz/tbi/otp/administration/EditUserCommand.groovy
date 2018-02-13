package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.security.User
import grails.validation.Validateable

@Validateable
class EditUserCommand implements Serializable {
    private static final long serialVersionUID = 1L

    User user
    String email
    String realName
    String asperaAccount

    static constraints = {
        email(nullable: false, blank: false, email: true, validator: { val, obj ->
            User userByMail = User.findByEmail(val)
            if (userByMail != null && userByMail != obj.user) {
                return 'Duplicate'
            }
        })
        realName(blank: false)
        asperaAccount(blank: true, nullable: true)
    }

    void setEmail(String email) {
        this.email = email?.trim()?.replaceAll(" +", " ")
    }

    void setRealName(String realName) {
        this.realName = realName?.trim()?.replaceAll(" +", " ")
    }

    void setAsperaAccount(String asperaAccount) {
        this.asperaAccount = asperaAccount?.trim()?.replaceAll(" +", " ") ?: null
    }
}