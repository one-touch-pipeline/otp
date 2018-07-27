package de.dkfz.tbi.otp.user

import de.dkfz.tbi.otp.OtpException

/**
 * @short Base class for all User management related exceptions.
 *
 */
abstract class UserManagementException extends OtpException implements Serializable {
    private static final long serialVersionUID = 1L
    private String userName = null
    private Long id = null

    protected UserManagementException(String userName) {
        this("Unknown error while managing user with username ${userName}".toString(), userName)
    }

    protected UserManagementException(String message, String userName) {
        super(message)
        this.userName = userName
    }

    protected UserManagementException(long id) {
        this("Unknown eror while managing user with id ${id}".toString(), id)
    }

    protected UserManagementException(String message, Long id) {
        super(message)
        this.id = id
    }

    protected setUserName(String userName) {
        this.userName = userName
    }

    protected setId(Long id) {
        this.id = id
    }

    public String getUserName() {
        return this.userName
    }

    public Long getId() {
        return this.id
    }
}
