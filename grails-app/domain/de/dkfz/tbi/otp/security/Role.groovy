package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.utils.Entity

/**
 * Auto generated class by spring security plugin.
 *
 */
class Role implements Entity {

    static final String ROLE_ADMIN = 'ROLE_ADMIN'
    static final String ROLE_OPERATOR = 'ROLE_OPERATOR'
    static final String ROLE_SWITCH_USER = 'ROLE_SWITCH_USER'

    static final List<String> REQUIRED_ROLES = [
            ROLE_ADMIN,
    ].asImmutable()

    static final List<String> IMPORTANT_ROLES = REQUIRED_ROLES + [
            ROLE_OPERATOR,
            ROLE_SWITCH_USER,
    ].asImmutable()


    String authority

    static mapping = {
        cache true
    }

    static constraints = {
        authority blank: false, unique: true
    }
}
