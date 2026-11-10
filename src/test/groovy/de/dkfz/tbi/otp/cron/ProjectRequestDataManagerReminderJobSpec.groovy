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

import de.dkfz.tbi.otp.project.projectRequest.ProjectRequestService

import java.time.LocalDate

class ProjectRequestDataManagerReminderJobSpec extends Specification {

    void "wrappedExecute passes today's date to service"() {
        given:
        ProjectRequestService projectRequestService = Mock(ProjectRequestService)
        LocalDate fixedDate = LocalDate.of(2026, 11, 8)
        ProjectRequestDataManagerReminderJob job = createJob(projectRequestService, fixedDate)

        when:
        job.wrappedExecute()

        then:
        1 * projectRequestService.sendDataManagerReminders(fixedDate)
        0 * _
    }

    private ProjectRequestDataManagerReminderJob createJob(ProjectRequestService projectRequestService, LocalDate fixedDate) {
        return new ProjectRequestDataManagerReminderJob(projectRequestService) {
            @Override
            protected LocalDate today() {
                return fixedDate
            }
        }
    }
}
