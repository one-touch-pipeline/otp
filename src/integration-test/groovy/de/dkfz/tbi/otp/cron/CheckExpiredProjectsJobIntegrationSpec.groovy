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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.time.LocalDate

@Rollback
@Integration
class CheckExpiredProjectsJobIntegrationSpec extends Specification implements UserDomainFactory {

    CheckExpiredProjectsJob job

    void setupData() {
        job = new CheckExpiredProjectsJob()
        job.projectService = new ProjectService()
    }

    @Unroll
    void "wrappedExecute, should inform about all expired projects that are not deleted nor archived"() {
        given:
        setupData()
        Project expiredProject1 = createProject([storageUntil: LocalDate.of(1970, 1, 1)])
        Project expiredProject2 = createProject([storageUntil: LocalDate.of(1970, 1, 1)])
        createProject()
        createProject([storageUntil: LocalDate.of(1970, 1, 1), state: Project.State.DELETED])
        createProject([state: Project.State.DELETED])

        job.mailHelperService = Mock(MailHelperService) {
            1 * saveMail(_, _)
        }
        job.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage(_) { properties -> [expiredProject1.name, expiredProject2.name].every { name -> properties.expiredProjects.contains(name) } }
            1 * createMessage(_, _)
        }

        expect:
        job.wrappedExecute()
    }
}
