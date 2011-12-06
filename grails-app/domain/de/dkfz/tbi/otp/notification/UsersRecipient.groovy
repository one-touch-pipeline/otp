package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.security.User

/**
 * Concrete Recipient representing a group of Users.
 *
 */
class UsersRecipient extends Recipient {
    static hasMany = [users: User]

    static mapping = {
        users(lazy: false)
    }

    static constraints = {
    }
}
