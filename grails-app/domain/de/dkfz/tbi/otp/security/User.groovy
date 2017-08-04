package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.TimeStamped
import de.dkfz.tbi.otp.utils.Entity

/**
 * Auto generated class by spring security plugin.
 *
 */
class User implements TimeStamped, Entity {

    transient springSecurityService

    String username
    String password
    boolean enabled
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired
    String email

    static constraints = {
        username blank: false, unique: true
        password blank: false
        email    nullable: true, email: true
    }

    static mapping = {
        table 'users'
        password column: '`password`'
    }

    Set<Role> getAuthorities() {
        UserRole.findAllByUser(this).collect { it.role } as Set
    }

    def beforeInsert() {
        encodePassword()
    }

    def beforeUpdate() {
        if (isDirty('password')) {
            encodePassword()
        }
    }

    protected void encodePassword() {
        // Password is set to a character which does not map to any character in an SHA sum
        // Set to invalid as password is in ldap
        password = "*"
    }

    /**
     *
     * @return User without any security relevant information.
     */
    User sanitizedUser() {
        return new User(id: this.id, username: this.username, email: this.email)
    }

    /**
     *
     * @return Map with information about User without any security relevant information.
     */
    Map sanitizedUserMap() {
        return [id: this.id, username: this.username, email: this.email]
    }
}
