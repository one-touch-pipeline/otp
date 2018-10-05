package de.dkfz.tbi.otp.user

/**
 * @short Exception indicating that a User could not be found.
 *
 * This exception should be thrown whenever it is tried to access a User by either
 * Id or login identifier, but there is no such User present in the database.
 */
class UserNotFoundException extends UserManagementException implements Serializable{
    private static final long serialVersionUID = 1L
    UserNotFoundException(Long id) {
        super("No user for given id ${id}".toString(), id)
    }

    UserNotFoundException(String userName) {
        super("User with identifier ${userName} not found".toString(), userName)
    }
}
