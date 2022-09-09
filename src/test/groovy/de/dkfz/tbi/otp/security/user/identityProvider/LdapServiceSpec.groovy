/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.security.user.identityProvider

import grails.testing.gorm.DataTest
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.query.LdapQuery
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.administration.LdapKey
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.User

import java.time.LocalDateTime
import java.time.ZoneId

class LdapServiceSpec extends Specification implements DataTest {

    static final LocalDateTime LDAP_START_DATETIME = LocalDateTime.of(1601, 1, 1, 0, 0)
    static final LocalDateTime UNIX_START_DATETIME = LocalDateTime.of(1970, 1, 1, 0, 0)

    static String accountDisableUserName = "accountDisableUser"
    static String deletedOuUserName      = "deletedOuUser"
    static String accountExpiresUserName = "accountExpiresUser"
    static String normalUserName         = "normalUser"

    static String companyName            = "@example.com"

    static long gapMilliSec = UNIX_START_DATETIME.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli() -
                              LDAP_START_DATETIME.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()

    LdapService service
    User user

    Map testUsers = [
        (accountDisableUserName) : [ id: 1, username: accountDisableUserName, email: accountDisableUserName + companyName],
        (deletedOuUserName)      : [ id: 2, username: deletedOuUserName,      email: deletedOuUserName      + companyName],
        (accountExpiresUserName) : [ id: 3, username: accountExpiresUserName, email: accountExpiresUserName + companyName],
        (normalUserName)         : [ id: 4, username: normalUserName,         email: normalUserName         + companyName],
    ]

    // reusable codes
    Closure findAccountDisableUsers = { LdapQuery query, IsUserDeactivatedMapper mapper ->
        return query.base().empty && query.attributes()[0] == "cn" && query.attributes()[1] == LdapKey.USER_ACCOUNT_CONTROL &&
                user.username == accountDisableUserName ? [ user.username ] : []
    }
    Closure findDeletedOuUsers = { LdapQuery query, IsUserDeactivatedMapper mapper ->
        return query.base().toString().endsWith(LdapKey.DELETED_USERS) &&
                user.username == deletedOuUserName ? [ user.username ] : []
    }

    Closure findAccountExpiresUsers = { LdapQuery query, UsernameAttributesMapper mapper ->
        String queryString = query.filter()
        return  queryString.contains("cn=${user.username}") &&
                queryString.contains(LdapKey.ACCOUNT_EXPIRES + ">=1") &&
                queryString.contains(LdapKey.ACCOUNT_EXPIRES + "<=") &&
                user.username == accountExpiresUserName ? [ user.username ] : []
    }

    void setup() {
        service = new LdapService()

        service.configService = Mock(ConfigService) {
            getLdapServer()                   >> 'server'
            getLdapSearchBase()               >> 'base'
            getLdapManagerDistinguishedName() >> 'cn=admin'
            getLdapManagerPassword()          >> 'secret'
            getLdapSearchAttribute()          >> "cn"
        }

        service.processingOptionService = Mock(ProcessingOptionService)
    }

    @Unroll
    void "test user is disabled, when user in ldap has ACCOUNTDISABLE bit set, is in DeletedUsers list ou, or has accountExpires attribute less than now"() {
        given:
        service.ldapTemplate = Mock(LdapTemplate) {
            nCallsDisable * search(_, _) >> { LdapQuery query, IsUserDeactivatedMapper mapper ->
                return findAccountDisableUsers(query, mapper) + findDeletedOuUsers(query, mapper)
            }
            nCallsExpires * search(_, _) >> { LdapQuery query, UsernameAttributesMapper mapper ->
                return findAccountExpiresUsers(query, mapper)
            }
        }

        when:
        user = new User(testUsers[userName])

        then:
        service.isUserDeactivated(user)

        where:
        userName               | nCallsDisable | nCallsExpires
        accountDisableUserName | 1             | 0
        deletedOuUserName      | 2             | 0
        accountExpiresUserName | 2             | 1
    }

    void "test a regular otp user, who should not be disabled"() {
        given:
        service.ldapTemplate = Mock(LdapTemplate) {
            2 * search(_, _) >> { LdapQuery query, IsUserDeactivatedMapper mapper ->
                return findAccountDisableUsers(query, mapper) + findDeletedOuUsers(query, mapper)
            }
            1 * search(_, _) >> { LdapQuery query, UsernameAttributesMapper mapper ->
                return findAccountExpiresUsers(query, mapper)
            }
        }

        when:
        user = new User(testUsers[normalUserName])

        then:
        !service.isUserDeactivated(user)
    }
}
