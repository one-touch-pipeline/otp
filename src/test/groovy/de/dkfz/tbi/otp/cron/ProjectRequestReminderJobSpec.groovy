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

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.project.projectRequest.ProjectRequestService

import java.time.DayOfWeek
import java.time.LocalDate

class ProjectRequestReminderJobSpec extends Specification {

    @Unroll
    void "isAdditionalRunConditionMet returns true only on Monday (#dayOfWeek)"() {
        given:
        LocalDate fixedDate = LocalDate.of(2026, 4, 13).plusDays(dayOffset)
        ProjectRequestReminderJob job = createJob(fixedDate)

        expect:
        job.isAdditionalRunConditionMet() == isMonday

        where:
        dayOfWeek         | dayOffset | isMonday
        DayOfWeek.MONDAY  | 0         | true
        DayOfWeek.TUESDAY | 1         | false
    }

    void "wrappedExecute delegates to service"() {
        given:
        ProjectRequestService projectRequestService = Mock(ProjectRequestService)
        LocalDate fixedDate = LocalDate.of(2026, 4, 14)
        ProjectRequestReminderJob job = createJob(fixedDate)
        job.projectRequestService = projectRequestService

        when:
        job.wrappedExecute()

        then:
        1 * projectRequestService.sendReminderEmailsForPendingApprovals(fixedDate)
        0 * _
    }

    private ProjectRequestReminderJob createJob(LocalDate fixedDate) {
        return new ProjectRequestReminderJob() {
            @Override
            protected LocalDate today() {
                return fixedDate
            }
        }
    }
}
