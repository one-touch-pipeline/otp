/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.user

import de.dkfz.tbi.otp.OtpException

/**
 * @short Base class for all User management related exceptions.
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

    String getUserName() {
        return this.userName
    }

    Long getId() {
        return this.id
    }
}
