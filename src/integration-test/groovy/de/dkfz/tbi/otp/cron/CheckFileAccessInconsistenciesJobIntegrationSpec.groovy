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
package de.dkfz.tbi.otp.cron

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.LdapUserDetails
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService

@Rollback
@Integration
class CheckFileAccessInconsistenciesJobIntegrationSpec extends Specification implements UserDomainFactory {

    private static final String USER_ACCOUNT = 'jdoe'
    private static final String USER_REAL_NAME = 'John Doe'
    private static final String USER_EMAIL = 'jdoe@test.de'
    private static final String UNIX_GROUP_SECOND = 'OtherUnixGroup'
    private static final String UNIX_GROUP_PROJECT = 'UnixGroupProject'
    private static final String PROJECT_NAME_TEST = 'TestProject'

    @Unroll
    void "wrappedExecute, when #name, then #mailSending"() {
        given:
        User systemUser = createUser()
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, systemUser.username)

        User testUser = createUser([
                username: USER_ACCOUNT,
                realName: USER_REAL_NAME,
                email   : USER_EMAIL,
                enabled : true,
        ])
        Project testProject = createProject([
                name     : PROJECT_NAME_TEST,
                unixGroup: UNIX_GROUP_PROJECT,
        ])

        createUserProjectRole([
                project                  : testProject,
                user                     : testUser,
                enabled                  : projectEnabled,
                accessToFiles            : fileAcessOtp,
                fileAccessChangeRequested: fileAccessChangeRequested,
        ])

        LdapUserDetails ldapUserDetails = new LdapUserDetails([
                username         : USER_ACCOUNT,
                realName         : USER_REAL_NAME,
                mail             : USER_EMAIL,
                memberOfGroupList: [fileAccessLdap ? UNIX_GROUP_PROJECT : UNIX_GROUP_SECOND],
        ])

        CheckFileAccessInconsistenciesJob job = new CheckFileAccessInconsistenciesJob([
                processingOptionService: new ProcessingOptionService(),
                ldapService            : Mock(LdapService) {
                    1 * getLdapUserDetailsByUserList(_) >> [ldapUserDetails,]
                    1 * isUserDeactivated(_) >> ldapDisabled
                    0 * _
                },
                mailHelperService      : Mock(MailHelperService) {
                    mailCount * sendEmailToTicketSystem(_, _) >> { String subject, String body ->
                        assert subject.startsWith(CheckFileAccessInconsistenciesJob.SUBJECT)
                        assert body.contains(CheckFileAccessInconsistenciesJob.HEADER)
                        assert body.contains(USER_ACCOUNT)
                        assert body.contains(PROJECT_NAME_TEST)
                    }
                },
                userProjectRoleService : Mock(UserProjectRoleService) {
                    accessCount * setAccessToFiles(_, false, true)
                    0 * _
                },
        ])

        when:
        job.wrappedExecute()

        then:
        noExceptionThrown()

        where:
        name                                                    | fileAcessOtp | fileAccessLdap | fileAccessChangeRequested | otpEnabled | projectEnabled | ldapDisabled || mailCount | accessCount
        'access in otp and ldap, no change request'             | true         | true           | true                      | true       | true           | false        || 0         | 0
        'access in otp and ldap, but change request'            | true         | true           | false                     | true       | true           | false        || 0         | 0
        'access in otp but not in ldap, no change request'      | true         | false          | true                      | true       | true           | false        || 1         | 0
        'access in otp but not in ldap, but change request'     | true         | false          | false                     | true       | true           | false        || 0         | 1
        'access in ldap and not in otp, no change request'      | false        | true           | true                      | true       | true           | false        || 1         | 0
        'access in ldap and not in otp, but change request'     | false        | true           | false                     | true       | true           | false        || 1         | 0
        'no file access in otp nor in ldap, no change request'  | false        | false          | true                      | true       | true           | false        || 0         | 0
        'no file access in otp nor in ldap, but change request' | false        | false          | false                     | true       | true           | false        || 0         | 0
        //some special cases
        'send mail also if disabled in otp'                     | false        | true           | true                      | false      | true           | false        || 1         | 0
        'send mail also if disabled in project'                 | false        | true           | true                      | true       | false          | false        || 1         | 0
        'send mail also if disabled in ldap'                    | false        | true           | true                      | true       | true           | true         || 1         | 0

        mailSending = mailCount ? 'send mail' : 'do not send mail'
    }
}
