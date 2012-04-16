package de.dkfz.tbi.otp.user

/**
 * @short Exception thrown during registration of new user.
 * 
 */
class RegistrationException extends UserManagementException implements Serializable {
    private static final long serialVersionUID = 1L
    RegistrationException(userName) {
        this("Error occurred during registration of new user ${userName}".toString(), userName)
    }

    RegistrationException(String message, String userName) {
        super(message, userName)
    }
}
