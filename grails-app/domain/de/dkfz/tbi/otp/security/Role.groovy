package de.dkfz.tbi.otp.security

/**
 * Auto generated class by spring security plugin.
 *
 */
class Role {

    String authority

    static mapping = {
        cache true
    }

    static constraints = {
        authority blank: false, unique: true
    }
}
