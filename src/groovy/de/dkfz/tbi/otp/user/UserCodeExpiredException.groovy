package de.dkfz.tbi.otp.user

/**
 * @short Exception indicating that a user change code has already expired.
 *
 * This exception should be thrown in case a user tries to e.g. validate the registration or
 * reset the password with a code which has expired.
 */
class UserCodeExpiredException extends UserManagementException implements Serializable {
    private static final long serialVersionUID = 1L

    UserCodeExpiredException(String userName, Long id) {
        super("Code for changing user identified by ${userName} has expired".toString(), userName)
        setId(id)
    }
}
