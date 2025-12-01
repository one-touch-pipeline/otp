/*
 * Copyright 2011-2025 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.InsufficientRightsException
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.utils.exceptions.SecurityContextAlreadyExistsException

@Rollback
@Integration
class SystemUserServiceIntegrationSpec extends Specification implements UserDomainFactory {

    protected String userName

    protected SystemUserService systemUserService

    void "useSystemUserAsOperator, if called, it use the system user"() {
        given:
        setupData()

        expect:
        systemUserService.useSystemUserAsOperator {
            Authentication authentication = SecurityContextHolder.context.authentication
            assert authentication
            assert authentication.name == userName
            assert authentication.authorities.size() == 1
            assert authentication.authorities.first().role == Role.ROLE_OPERATOR
            true
        }
        assert !SecurityContextHolder.context.authentication
    }

    void "useSystemUserAsAdmin, if called, it use the system user"() {
        given:
        setupData()

        expect:
        systemUserService.useSystemUserAsAdmin {
            Authentication authentication = SecurityContextHolder.context.authentication
            assert authentication
            assert authentication.name == userName
            assert authentication.authorities.size() == 1
            assert authentication.authorities.first().role == Role.ROLE_ADMIN
            true
        }
        assert !SecurityContextHolder.context.authentication
    }

    void "useUser, if called, it use the given system user with given role"() {
        given:
        setupData()
        String role = "role_${nextId}"

        expect:
        systemUserService.useUser(userName, role) {
            Authentication authentication = SecurityContextHolder.context.authentication
            assert authentication
            assert authentication.name == userName
            assert authentication.authorities.size() == 1
            assert authentication.authorities.first().role == role
            true
        }
        assert !SecurityContextHolder.context.authentication
    }

    void "useUser, if called and a security context exist, then fail"() {
        given:
        setupData()
        String role = "role_${nextId}"

        and:
        UserDetails userDetails = new User(userName, "", [])
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(userDetails, null, [])

        when:
        systemUserService.useUser(userName, role) {
            throw new InsufficientRightsException("should not be reached")
        }

        then:
        thrown(SecurityContextAlreadyExistsException)

        cleanup:
        SecurityContextHolder.clearContext()
    }

    void setupData() {
        userName = "user ${nextId}"
        createUser([
                username: userName,
        ])
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, userName)
        systemUserService = new SystemUserService()
        systemUserService.processingOptionService = new ProcessingOptionService()
    }
}
