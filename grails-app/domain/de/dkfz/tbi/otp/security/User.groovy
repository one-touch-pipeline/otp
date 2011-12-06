package de.dkfz.tbi.otp.security

/**
 * Auto generated class by spring security plugin.
 *
 */
class User {

    transient springSecurityService

    String username
    String password
    boolean enabled
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired
    String jabberId
    String email

    static constraints = {
        username blank: false, unique: true
        password blank: false
        jabberId nullable: true
        email    nullable: true, email: true
    }

    static mapping = {
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
        password = springSecurityService.encodePassword(password)
    }
}
