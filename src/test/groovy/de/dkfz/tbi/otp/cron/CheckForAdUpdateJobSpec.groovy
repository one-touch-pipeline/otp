/*
 * Copyright 2011-2024 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.UserProjectRole

class CheckForAdUpdateJobSpec extends Specification implements DataTest, DomainFactoryCore {

    private final static String UNIX_GROUP = 'group'

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                UserProjectRole,
        ]
    }

    @Unroll
    void "fileAccessChangeRequested, when enabled '#enabled', accessToFiles: #accessToFiles, fileAccessChangeRequested: #fileAccessChangeRequested, groups: #groups, then callCount: #callCount and expectedFileAccessChangeRequested: #expectedFileAccessChangeRequested"() {
        given:
        UserProjectRole role = DomainFactory.createUserProjectRole([
                user                     : DomainFactory.createUser([enabled: enabled,]),
                project                  : createProject([unixGroup: UNIX_GROUP, state: projectState]),
                accessToFiles            : accessToFiles,
                fileAccessChangeRequested: fileAccessChangeRequested,
        ])
        CheckForAdUpdateJob job = new CheckForAdUpdateJob([
                identityProvider: Mock(IdentityProvider) {
                    callCount * getIdpUserDetailsByUsername(_) >> {
                        new IdpUserDetails([memberOfGroupList: groups])
                    }
                }
        ])

        when:
        job.wrappedExecute()

        then:
        role.fileAccessChangeRequested == expectedFileAccessChangeRequested

        where:
        enabled | accessToFiles | fileAccessChangeRequested | groups       || callCount | expectedFileAccessChangeRequested | projectState
        true    | true          | false                     | [UNIX_GROUP] || 0         | false                             | Project.State.OPEN
        true    | true          | false                     | ['abc']      || 0         | false                             | Project.State.OPEN
        true    | true          | false                     | []           || 0         | false                             | Project.State.OPEN
        true    | true          | true                      | [UNIX_GROUP] || 1         | false                             | Project.State.OPEN
        true    | true          | true                      | ['abc']      || 1         | true                              | Project.State.OPEN
        true    | true          | true                      | []           || 1         | true                              | Project.State.OPEN

        true    | false         | false                     | [UNIX_GROUP] || 0         | false                             | Project.State.OPEN
        true    | false         | false                     | ['abc']      || 0         | false                             | Project.State.OPEN
        true    | false         | false                     | []           || 0         | false                             | Project.State.OPEN
        true    | false         | true                      | [UNIX_GROUP] || 1         | true                              | Project.State.OPEN
        true    | false         | true                      | ['abc']      || 1         | false                             | Project.State.OPEN
        true    | false         | true                      | []           || 1         | false                             | Project.State.OPEN

        false   | true          | false                     | [UNIX_GROUP] || 0         | false                             | Project.State.OPEN
        false   | true          | false                     | ['abc']      || 0         | false                             | Project.State.OPEN
        false   | true          | false                     | []           || 0         | false                             | Project.State.OPEN
        false   | true          | true                      | [UNIX_GROUP] || 1         | true                              | Project.State.OPEN
        false   | true          | true                      | ['abc']      || 1         | false                             | Project.State.OPEN
        false   | true          | true                      | []           || 1         | false                             | Project.State.OPEN

        false   | false         | false                     | [UNIX_GROUP] || 0         | false                             | Project.State.OPEN
        false   | false         | false                     | ['abc']      || 0         | false                             | Project.State.OPEN
        false   | false         | false                     | []           || 0         | false                             | Project.State.OPEN
        false   | false         | true                      | [UNIX_GROUP] || 1         | true                              | Project.State.OPEN
        false   | false         | true                      | ['abc']      || 1         | false                             | Project.State.OPEN
        false   | false         | true                      | []           || 1         | false                             | Project.State.OPEN

        true    | true          | false                     | [UNIX_GROUP] || 0         | false                             | Project.State.DELETED
    }

    @Unroll
    void "scheduledJobRunPreconditionsMet, when ldapEnabled is #ldapEnabled and jobSystem is #jobSystem, then expected is #expected"() {
        given:
        CheckForAdUpdateJob job = new CheckForAdUpdateJob([
                configService          : Mock(ConfigService) {
                    _ * getLdapEnabled() >> ldapEnabled
                    0 * _
                },
                schedulerService       : Mock(SchedulerService) {
                    _ * isActive() >> jobSystem
                    0 * _
                },
                processingOptionService: new ProcessingOptionService(),
        ])

        when:
        boolean met = job.scheduledJobRunPreconditionsMet

        then:
        expected == met

        where:
        ldapEnabled | jobSystem || expected
        false       | false     || false
        false       | true      || false
        true        | false     || false
        true        | true      || true
    }
}
