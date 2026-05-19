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

import org.springframework.scheduling.config.CronTask
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import spock.lang.Specification

import de.dkfz.tbi.otp.cron.AbstractScheduledJob

class SchedulingConfigurationSpec extends Specification {

    // helper: create a job that reports its own cron and records its execution
    private AbstractScheduledJob jobWithCron(String cron, List<String> executionLog = [], String name = cron) {
        return new AbstractScheduledJob() {
            @Override
            void wrappedExecute() { }

            @Override
            String getCronExpression() { return cron }

            @Override
            void execute() { executionLog << name }
        }
    }

    void "configureTasks, two jobs sharing the same cron are grouped into one wrapper task that runs both sequentially"() {
        given:
        SchedulingConfiguration configuration = new SchedulingConfiguration()
        configuration.scheduledJobs = [
                jobWithCron("0 0 5 * * *"),
                jobWithCron("0 0 5 * * *"),
        ]

        ScheduledTaskRegistrar registrar = Mock()

        when:
        configuration.configureTasks(registrar)

        then:
        1 * registrar.setTaskScheduler(_)
        1 * registrar.addCronTask { CronTask t -> t.expression == "0 0 5 * * *" }
        0 * registrar._
    }

    void "configureTasks, jobs with different crons are registered as separate tasks"() {
        given:
        SchedulingConfiguration configuration = new SchedulingConfiguration()
        configuration.scheduledJobs = [
                jobWithCron("0 0 5 * * *"),
                jobWithCron("0 */5 * * * *"),
        ]

        ScheduledTaskRegistrar registrar = Mock()

        when:
        configuration.configureTasks(registrar)

        then:
        1 * registrar.setTaskScheduler(_)
        1 * registrar.addCronTask { CronTask t -> t.expression == "0 0 5 * * *" }
        1 * registrar.addCronTask { CronTask t -> t.expression == "0 */5 * * * *" }
        0 * registrar._
    }

    void "configureTasks, jobs in the same cron group are executed sequentially in list order"() {
        given:
        List<String> executionOrder = []
        SchedulingConfiguration configuration = new SchedulingConfiguration()
        configuration.scheduledJobs = [
                jobWithCron("0 0 5 * * *", executionOrder, "first"),
                jobWithCron("0 0 5 * * *", executionOrder, "second"),
                jobWithCron("0 0 5 * * *", executionOrder, "third"),
        ]

        // capture the CronTask that gets registered instead of scheduling it for real
        CronTask capturedTask
        ScheduledTaskRegistrar registrar = Mock {
            addCronTask(_ as CronTask) >> { CronTask t -> capturedTask = t }
        }
        configuration.configureTasks(registrar)

        when:
        capturedTask.runnable.run()

        then:
        executionOrder == ["first", "second", "third"]
    }

    void "configureTasks, a job in its own cron group runs independently"() {
        given:
        List<String> executionOrder = []
        SchedulingConfiguration configuration = new SchedulingConfiguration()
        configuration.scheduledJobs = [
                jobWithCron("0 0 5 * * *", executionOrder, "fiveAM"),
                jobWithCron("0 */5 * * * *", executionOrder, "everyFive"),
        ]

        Map<String, CronTask> capturedTasks = [:]
        ScheduledTaskRegistrar registrar = Mock {
            addCronTask(_ as CronTask) >> { CronTask t -> capturedTasks[t.expression] = t }
        }
        configuration.configureTasks(registrar)

        when:
        capturedTasks["0 */5 * * * *"].runnable.run()

        then:
        executionOrder == ["everyFive"]   // the 5 AM job was not triggered
    }
}
