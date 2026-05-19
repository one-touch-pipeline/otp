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
package de.dkfz.tbi.otp

import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import spock.lang.Specification

import de.dkfz.tbi.otp.cron.AbstractScheduledJob

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Integration
class SchedulingConfigurationIntegrationSpec extends Specification {

    @Autowired
    SchedulingConfiguration schedulingConfiguration

    @Autowired
    List<AbstractScheduledJob> scheduledJobs

    void "scheduledJobs, Spring injects all AbstractScheduledJob beans automatically"() {
        expect:
        // all concrete subclasses are present — at minimum the ones in ALL_JOB_CLASSES plus DeleteOnCheckJob and UpdateDepartmentHeadsJob
        scheduledJobs.size() >= AbstractScheduledJob.ALL_JOB_CLASSES.size()
        scheduledJobs.every { it instanceof AbstractScheduledJob }
    }

    void "scheduledJobs, every injected job has a non-null cron expression"() {
        expect:
        scheduledJobs.every { it.cronExpression != null && !it.cronExpression.empty }
    }

    void "configureTasks, was called by Spring at startup and grouped all jobs sharing the same cron into one single wrapper task"() {
        // no when block — Spring already called configureTasks automatically at startup
        // we just inspect what was recorded in registeredCronTasks
        expect:
        int expectedGroups = scheduledJobs.groupBy { it.cronExpression }.size()
        schedulingConfiguration.registeredCronTasks.size() == expectedGroups
    }

    void "configureTasks, was called by Spring at startup and the default 5 AM cron group is registered"() {
        expect:
        schedulingConfiguration.registeredCronTasks*.expression.contains("0 0 5 * * *")
    }

    void "real scheduler, job running every 1 second fires more often than job running every 2 seconds"() {
        given:
        AtomicInteger oneSecondCount = new AtomicInteger(0)
        AtomicInteger twoSecondCount = new AtomicInteger(0)

        // latch releases once the 1-second job has fired at least 5 times — that's our signal to stop waiting
        CountDownLatch latch = new CountDownLatch(5)

        AbstractScheduledJob oneSecondJob = new AbstractScheduledJob() {
            @Override
            void wrappedExecute() { }
            final String cronExpression = "*/1 * * * * *"

            @Override
            void execute() {
                oneSecondCount.incrementAndGet()
                latch.countDown()
            }
        }

        AbstractScheduledJob twoSecondJob = new AbstractScheduledJob() {
            @Override
            void wrappedExecute() { }
            final String cronExpression = "*/2 * * * * *"

            @Override
            void execute() { twoSecondCount.incrementAndGet() }
        }

        // fresh configuration with only our two test jobs — does not touch the Spring-managed instance
        SchedulingConfiguration configuration = new SchedulingConfiguration()
        configuration.scheduledJobs = [oneSecondJob, twoSecondJob]

        // real scheduler — not the no-op one from the test environment
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler()
        scheduler.poolSize = 2
        scheduler.initialize()

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar()

        when:
        configuration.configureTasks(registrar)
        // configureTasks internally calls threadPoolTaskScheduler() which returns the no-op scheduler
        // in the test environment — so we override it here with the real one before starting
        registrar.taskScheduler = scheduler
        registrar.afterPropertiesSet()              // actually starts scheduling

        latch.await(10, TimeUnit.SECONDS)           // wait until 5 executions of the 1s job (or 10s timeout)
        scheduler.shutdown()                        // stop the scheduler cleanly

        then:
        oneSecondCount.get() >= 5                   // fired at least 5 times in ~5 seconds
        twoSecondCount.get() > 0                    // fired at least once in that same window
        oneSecondCount.get() > twoSecondCount.get() // 1s job fired more than 2s job
    }
}
