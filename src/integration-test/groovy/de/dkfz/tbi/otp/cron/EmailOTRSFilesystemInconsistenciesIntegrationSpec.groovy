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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.LdapUserDetails
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService

@Rollback
@Integration
class EmailOTRSFilesystemInconsistenciesIntegrationSpec extends Specification implements UserDomainFactory {
    void setupData() {
        User testUser = createUser([
                username: 'jdoe',
                realName: 'John Doe',
                email   : 'jdoe@test.de',
                enabled : true,
        ]
        )
        User testPIUser = createUser([
                username: 'testPIusername',
                realName: 'testPIname',
                email   : "pi@test.de",
                enabled : true,
        ]
        )
        Project testProject = createProject(
                [
                        name     : 'TestProject',
                        unixGroup: 'TestUnixGroup',
                ]
        )
        ProjectRole projectRole1 = createProjectRole([
                name: 'BIOINFORMATICIAN',
        ])
        ProjectRole projectRole2 = createProjectRole([
                name: 'PI',
        ])
        createUserProjectRole([
                project      : testProject,
                user         : testUser,
                enabled      : true,
                accessToFiles: true,
                projectRoles : [projectRole1],

        ])
        createUserProjectRole([
                project     : testProject,
                user        : testPIUser,
                enabled     : true,
                projectRoles: [projectRole2],

        ])
    }

    void "wrappedExecute, send email with list of all inconsistencies when job runs"() {
        given:
        setupData()
        EmailOTRSFilesystemInconsistencies job = new EmailOTRSFilesystemInconsistencies([
                processingOptionService: new ProcessingOptionService(),
                ldapService            : Mock(LdapService) {
                    1 * getLdapUserDetailsByUserList(_) >> { List<User> otpUsers ->
                        return [
                                new LdapUserDetails([
                                        username         : 'jdoe',
                                        realName         : 'John Doe',
                                        mail             : 'jdoe@test.de',
                                        memberOfGroupList: ['test1', 'test2'],
                                ]),
                        ]
                    }
                    0 * _
                },
                mailHelperService      : Mock(MailHelperService) {
                    1 * sendEmail(_, _, _) >> { String subject, String body, String recipient ->
                        assert subject.startsWith("User Management Inconsistencies status")
                        assert body.contains("Name | userName | emailId | projectName | PI Name | PI Email | PI Name | PI Email")
                        assert body.contains("John Doe | jdoe | jdoe@test.de | TestProject | testPIname | pi@test.de")
                    }
                },
        ])
        when:
        job.wrappedExecute()

        then:
        noExceptionThrown()
    }

    void "wrappedExecute, don't send email when no inconsistencies are found"() {
        given:
        setupData()
        EmailOTRSFilesystemInconsistencies job = new EmailOTRSFilesystemInconsistencies([
                processingOptionService: new ProcessingOptionService(),
                ldapService            : Mock(LdapService) {
                    1 * getLdapUserDetailsByUserList(_) >> { List<User> otpUsers ->
                        return [
                                new LdapUserDetails([
                                        username         : 'jdoe',
                                        realName         : 'John Doe',
                                        mail             : 'jdoe@test.de',
                                        memberOfGroupList: ['test1', 'test2', 'TestUnixGroup'],
                                ]),
                        ]
                    }
                    0 * _
                },
                mailHelperService: Mock(MailHelperService) {
                    0 * sendEmail(_, _, _) >> { }
                }
        ])

        when:
        job.wrappedExecute()

        then:
        noExceptionThrown()
    }
}
