/*
 * Copyright 2011-2026 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService.OperatorAction
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.SystemUserService

@Rollback
@Integration
class CheckFileAccessInconsistenciesForUsersWithProjectRoleJobIntegrationSpec extends Specification implements UserDomainFactory {

    private static final String USER_ACCOUNT = 'jdoe'
    private static final String USER_REAL_NAME = 'John Doe'
    private static final String USER_EMAIL = 'jdoe@test.de'
    private static final String UNIX_GROUP_SECOND = 'OtherUnixGroup'
    private static final String UNIX_GROUP_PROJECT = 'UnixGroupProject'
    private static final String PROJECT_NAME_TEST = 'TestProject'

    IdentityProvider identityProvider
    CheckFileAccessInconsistenciesForUsersWithProjectRoleJob job

    void setupData() {
        identityProvider = Mock(IdentityProvider)
        job = new CheckFileAccessInconsistenciesForUsersWithProjectRoleJob(identityProvider: identityProvider, processingOptionService: new ProcessingOptionService())
    }

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
                state    : Project.State.OPEN,
        ])

        createUserProjectRole([
                project                  : testProject,
                user                     : testUser,
                enabled                  : projectEnabled,
                accessToFiles            : fileAccessOtp,
                fileAccessChangeRequested: fileAccessChangeRequested,
        ])

        IdpUserDetails idpUserDetails = new IdpUserDetails([
                username         : USER_ACCOUNT,
                realName         : USER_REAL_NAME,
                mail             : USER_EMAIL,
                memberOfGroupList: [fileAccessLdap ? UNIX_GROUP_PROJECT : UNIX_GROUP_SECOND],
        ])

        CheckFileAccessInconsistenciesForUsersWithProjectRoleJob job = new CheckFileAccessInconsistenciesForUsersWithProjectRoleJob([
                processingOptionService: new ProcessingOptionService(),
                identityProvider       : Mock(IdentityProvider) {
                    1 * getIdpUserDetailsByUserList(_) >> [idpUserDetails,]
                    1 * isUserDeactivated(_) >> ldapDisabled
                    0 * _
                },
                mailHelperService      : Mock(MailHelperService) {
                    mailCount * saveMail(_, _) >> { String subject, String body ->
                        assert subject.startsWith(CheckFileAccessInconsistenciesForUsersWithProjectRoleJob.SUBJECT)
                        assert body.contains(CheckFileAccessInconsistenciesForUsersWithProjectRoleJob.HEADER)
                        assert body.contains(USER_ACCOUNT)
                        assert body.contains(PROJECT_NAME_TEST)
                    }
                },
                userProjectRoleService : Mock(UserProjectRoleService) {
                    accessCount * setAccessToFiles(_, false, true)
                    mailCount * getCommand(UNIX_GROUP_PROJECT, testUser.username, commandOption)
                    0 * _
                },
                messageSourceService   : new MessageSourceService(
                        messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                            _ * getMessageInternal("projectUser.notification.fileAccessChange.subject.removed", [], _) >> "File access for project removed"
                            _ * getMessageInternal("projectUser.notification.fileAccessChange.body.removed.cron", [], _) >> "File access for project removed body"
                        }
                ),
                systemUserService      : Mock(SystemUserService) {
                    accessCount * useSystemUserAsOperator(_ as Closure) >> { Closure c ->
                        c.call()
                    }
                    0 * _
                },
        ])

        when:
        job.wrappedExecute()

        then:
        noExceptionThrown()

        where:
        name                                                     | fileAccessOtp | fileAccessLdap | fileAccessChangeRequested | otpEnabled | projectEnabled | ldapDisabled || mailCount | accessCount | commandOption
        'access in otp and ldap, with change request'            | true          | true           | true                      | true       | true           | false        || 0         | 0           | { !(it in [OperatorAction.ADD, OperatorAction.REMOVE]) }
        'access in otp and ldap, no change request'              | true          | true           | false                     | true       | true           | false        || 0         | 0           | { !(it in [OperatorAction.ADD, OperatorAction.REMOVE]) }
        'access in otp but not in ldap, with change request'     | true          | false          | true                      | true       | true           | false        || 1         | 0           | OperatorAction.ADD
        'access in otp but not in ldap, no change request'       | true          | false          | false                     | true       | true           | false        || 0         | 1           | OperatorAction.ADD
        'access in ldap and not in otp, with change request'     | false         | true           | true                      | true       | true           | false        || 1         | 0           | OperatorAction.REMOVE
        'access in ldap and not in otp, no change request'       | false         | true           | false                     | true       | true           | false        || 1         | 0           | OperatorAction.REMOVE
        'no file access in otp nor in ldap, with change request' | false         | false          | true                      | true       | true           | false        || 0         | 0           | { !(it in [OperatorAction.ADD, OperatorAction.REMOVE]) }
        'no file access in otp nor in ldap, no change request'   | false         | false          | false                     | true       | true           | false        || 0         | 0           | { !(it in [OperatorAction.ADD, OperatorAction.REMOVE]) }
        // some special cases
        'send mail also if disabled in otp'                      | false         | true           | true                      | false      | true           | false        || 1         | 0           | OperatorAction.REMOVE
        'send mail also if disabled in project'                  | false         | true           | true                      | true       | false          | false        || 1         | 0           | OperatorAction.REMOVE
        'send mail also if disabled in ldap'                     | false         | true           | true                      | true       | true           | true         || 1         | 0           | OperatorAction.REMOVE

        mailSending = mailCount ? 'send mail' : 'do not send mail'
    }

    void "wrappedExecute, should not be executed when project is deleted"() {
        given:
        User systemUser = createUser()
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, systemUser.username)

        IdpUserDetails idpUserDetails = new IdpUserDetails([
                username         : USER_ACCOUNT,
                realName         : USER_REAL_NAME,
                mail             : USER_EMAIL,
                memberOfGroupList: [UNIX_GROUP_PROJECT],
        ])

        User testUser = createUser([
                username: USER_ACCOUNT,
                realName: USER_REAL_NAME,
                email   : USER_EMAIL,
                enabled : true,
        ])
        Project testProject = createProject([
                name     : PROJECT_NAME_TEST,
                unixGroup: UNIX_GROUP_PROJECT,
                state    : Project.State.DELETED,
        ])

        createUserProjectRole([
                project                  : testProject,
                user                     : testUser,
                enabled                  : true,
                accessToFiles            : true,
                fileAccessChangeRequested: true,
        ])

        CheckFileAccessInconsistenciesForUsersWithProjectRoleJob job = new CheckFileAccessInconsistenciesForUsersWithProjectRoleJob([
                identityProvider       : Mock(IdentityProvider) {
                    1 * getIdpUserDetailsByUserList(_) >> [idpUserDetails]
                    0 * _
                },
                processingOptionService: new ProcessingOptionService(),
                mailHelperService      : Mock(MailHelperService) {
                    0 * saveMail(_, _) >> { String subject, String body ->
                        assert subject.startsWith(CheckFileAccessInconsistenciesForUsersWithProjectRoleJob.SUBJECT)
                        assert body.contains(CheckFileAccessInconsistenciesForUsersWithProjectRoleJob.HEADER)
                        assert body.contains(USER_ACCOUNT)
                        assert body.contains(PROJECT_NAME_TEST)
                    }
                },
                userProjectRoleService : Mock(UserProjectRoleService) {
                    0 * setAccessToFiles(_, false, true)
                    0 * _
                },
        ])

        when:
        job.wrappedExecute()

        then:
        noExceptionThrown()
    }

    void "test generateReportForUsersInOtpWithProjectRole, should send a mail if the fileAccess of a user was removed in OTP to match LDAP"() {
        given:
        setupData()
        User systemUser = createUser()
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, systemUser.username)

        User user = createUser([username: USER_ACCOUNT, realName: USER_REAL_NAME, email: USER_EMAIL, enabled: true])
        Project project = createProject([name: PROJECT_NAME_TEST, unixGroup: UNIX_GROUP_PROJECT])
        UserProjectRole userProjectRole = createUserProjectRole([
                project                  : project,
                user                     : user,
                accessToFiles            : true,
                fileAccessChangeRequested: false,
        ])
        IdpUserDetails idpUserDetails = new IdpUserDetails([
                username         : USER_ACCOUNT,
                realName         : USER_REAL_NAME,
                mail             : USER_EMAIL,
                memberOfGroupList: [UNIX_GROUP_SECOND],
        ])

        CheckFileAccessInconsistenciesForUsersWithProjectRoleJob job = new CheckFileAccessInconsistenciesForUsersWithProjectRoleJob([
                processingOptionService: new ProcessingOptionService(),
                identityProvider       : Mock(IdentityProvider) {
                    1 * getIdpUserDetailsByUserList(_) >> [idpUserDetails,]
                    1 * isUserDeactivated(_) >> false
                    0 * _
                },
                mailHelperService      : Mock(MailHelperService) {
                    1 * saveMail(
                            "File access for project ${project.name} removed",
                            "File access for project ${project.name} removed",
                            [user.email],
                    )
                },
                userProjectRoleService : Mock(UserProjectRoleService),
                messageSourceService   : new MessageSourceService(
                        messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                            _ * getMessageInternal("projectUser.notification.fileAccessChange.subject.removed", [], _) >> "File access for project ${project.name} removed"
                            _ * getMessageInternal("projectUser.notification.fileAccessChange.body.removed.cron", [], _) >> "File access for project ${project.name} removed"
                        }
                ),
                systemUserService      : Mock(SystemUserService) {
                    1 * useSystemUserAsOperator(_ as Closure) >> { Closure c ->
                        c.call()
                    }
                    0 * _
                },
        ])

        when:
        job.generateReportForUsersInOtpWithProjectRole()

        then:
        1 * job.userProjectRoleService.setAccessToFiles(userProjectRole, false, true)
    }
}
