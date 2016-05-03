package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.utils.Entity

/**
 * Auto generated class by spring security plugin.
 *
 */
class Role implements Entity {

    String authority

    static mapping = {
        cache true
    }

    static constraints = {
        authority blank: false, unique: true
    }
}
