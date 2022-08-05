/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Specification

import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.utils.exceptions.SecurityContextAlreadyExistsException

@Rollback
@Integration
class SystemUserUtilsSpec extends Specification implements UserDomainFactory {

    void "useSystemUser, if called, it use the system user"() {
        given:
        String userName = "systemuser ${nextId}"
        createUser([
                username: userName,
        ])
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, userName)

        expect:
        SystemUserUtils.useSystemUser {
            Authentication authentication = SecurityContextHolder.context.authentication
            assert authentication
            assert authentication.name == userName
            assert authentication.authorities.size() == 1
            assert authentication.authorities.first().role == Role.ROLE_OPERATOR
            true
        }
        assert !SecurityContextHolder.context.authentication
    }

    void "useUser, if called, it use the given system user"() {
        given:
        String userName = "systemUser ${nextId}"

        expect:
        SystemUserUtils.useUser(userName) {
            Authentication authentication = SecurityContextHolder.context.authentication
            assert authentication
            assert authentication.name == userName
            assert authentication.authorities.size() == 1
            assert authentication.authorities.first().role == Role.ROLE_OPERATOR
            true
        }
        assert !SecurityContextHolder.context.authentication
    }

    void "useUser, if called and a security context exist, then fail"() {
        given:
        String userName = "systemUser ${nextId}"
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(userName, "", [])
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(userDetails, null, [])

        when:
        SystemUserUtils.useUser(userName) {
            throw new OtpRuntimeException("should not be reached")
        }

        then:
        thrown(SecurityContextAlreadyExistsException)

        cleanup:
        SecurityContextHolder.clearContext()
    }
}
