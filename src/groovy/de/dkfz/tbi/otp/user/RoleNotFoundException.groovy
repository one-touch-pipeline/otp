package de.dkfz.tbi.otp.user

import de.dkfz.tbi.otp.OtpException

/**
 * @short Exception indicating that a Role could not be found.
 *
 */
class RoleNotFoundException extends OtpException implements Serializable {
    private static final long serialVersionUID = 1L
    Long id = null
    String authority = null

    RoleNotFoundException(Long id) {
        super("Role with id ${id} not found".toString())
        this.id = id
    }

    RoleNotFoundException(String authority) {
        super("Role with authority ${authority} not found".toString())
        this.authority = authority
    }
}
