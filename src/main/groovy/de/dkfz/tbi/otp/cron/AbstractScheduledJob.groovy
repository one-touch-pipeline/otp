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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.SessionUtils

import java.time.LocalDateTime

@Component
@Slf4j
abstract class AbstractScheduledJob {

    static final List<Class> ALL_JOB_CLASSES = [
            CellRangerDataCleanupJob,
            CheckForAdUpdateJob,
            DeactivateUsersJob,
            DeleteConcreteArtefactsOfOmittedWorkflowArtefactsJob,
            ScheduleUsersForDeactivationJob,
            UnknownLdapUsersJob,
            GenerateAndSendKPIsForNBI,
            FetchUserDataFromLdapJob,
            CheckFileAccessInconsistenciesJob,
            CheckExpiredProjects,
    ]

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    MailHelperService mailHelperService

    abstract void wrappedExecute()

    @SuppressWarnings(["CatchThrowable"])
    @Scheduled(cron="0 0 5 * * *")
    void execute() {
        SessionUtils.withNewSession {
            try {
                User.withTransaction {
                    if (!scheduledJobRunPreconditionsMet) {
                        log.info("Scheduled job ${this.class} did not meet the preconditions to run")
                        return
                    }
                    wrappedExecute()
                }
            } catch (Throwable t) {
                sendStacktraceToMaintainer(t)
            }
        }
    }

    boolean isScheduledJobRunPreconditionsMet() {
        return schedulerService.active &&
                processingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.CRONJOB_ACTIVE, this.class.canonicalName) &&
                additionalRunConditionMet
    }

    boolean isAdditionalRunConditionMet() {
        return true
    }

    void sendStacktraceToMaintainer(Throwable t) {
        String subject = "Exception occured in scheduled job ${this.class}"

        StringWriter sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        String body = "${LocalDateTime.now()}\n\n${sw}"

        mailHelperService.sendEmailToTicketSystem(subject, body)
    }
}
