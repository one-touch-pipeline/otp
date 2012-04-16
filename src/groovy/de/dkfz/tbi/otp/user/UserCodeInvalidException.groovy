package de.dkfz.tbi.otp.user

/**
 * @short Exception indicating that a user change code is not valid.
 *
 * This exception should be thrown in case a user tries to e.g. validate the registration or
 * reset the password with a code which is not valid.
 *
 */
class UserCodeInvalidException extends UserManagementException implements Serializable {
    private static final long serialVersionUID = 1L
    private String code

    UserCodeInvalidException(String userName, Long id, String code) {
        super("Code ${code} for changing user identified by ${userName} is invalid".toString(), userName)
        this.code = code
        setId(id)
    }

    public String getCode() {
        return code
    }
}
