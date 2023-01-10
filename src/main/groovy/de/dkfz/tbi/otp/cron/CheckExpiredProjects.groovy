/*
 * Copyright 2011-2023 The OTP authors
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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.time.LocalDate

@Component
@Slf4j
class CheckExpiredProjects extends AbstractScheduledJob {

    @Autowired
    ProjectService projectService

    @Autowired
    MessageSourceService messageSourceService

    /**
     * Run this job only once per month.
     * @return true if it is the first day of a month
     */
    @Override
    boolean isAdditionalRunConditionMet() {
        return LocalDate.now().dayOfMonth == 1
    }

    @Override
    void wrappedExecute() {
        sendMailContainingAllExpiredProjects()
    }

    private sendMailContainingAllExpiredProjects() {
        Map<Project, List<User>> expiredProjects = projectService.expiredProjectsWithPIs

        if (expiredProjects) {
            List<String> expProjectsTable = ["project\texpired on\tPIs"]
            expProjectsTable += expiredProjects.collect { Project project, List<User> users ->
                String userString = users.collect { "${it.realName} (${it.email})" }.join(", ")
                return "${project.name}\t${project.storageUntil}\t${userString}"
            }

            String subject = messageSourceService.createMessage("notification.cron.expiredProjects.subject", [:])
            String content = messageSourceService.createMessage("notification.cron.expiredProjects.body", [
                    expiredProjects: expProjectsTable.join('\n'),
            ])
            mailHelperService.sendEmailToTicketSystem(subject, content)
        }
    }
}
