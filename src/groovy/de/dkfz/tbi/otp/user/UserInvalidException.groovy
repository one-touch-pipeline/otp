package de.dkfz.tbi.otp.user

/**
 * @short Exception indicating that a User object does not validate.
 *
 * This exception should be thrown whenever a User object does not validate,
 * either when it is initially created or updated later one.
 *
 * For security reasons the Exception does not include the User data which was
 * tried to be modified.
 */
class UserInvalidException extends UserManagementException implements Serializable {
    private static final long serialVersionUID = 1L
    UserInvalidException(String userName) {
        super("The user ${userName} does not validate".toString(), userName)
    }
}
