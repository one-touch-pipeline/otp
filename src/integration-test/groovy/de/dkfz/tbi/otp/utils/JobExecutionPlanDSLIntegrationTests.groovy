/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Test

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.PlanValidatorService

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.plan
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse

@Rollback
@Integration
class JobExecutionPlanDSLIntegrationTests {

    PlanValidatorService planValidatorService

    @Test
    void testEmptyPlan() {
        assertEquals(0, JobExecutionPlan.count())
        plan("test") {
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        assertFalse(planValidatorService.validate(jep).isEmpty())

        assertEquals(1, JobExecutionPlan.count())
        plan("test2") {
            start("startJob", "testStartJob")
        }
        assertEquals(2, JobExecutionPlan.count())
        jep = JobExecutionPlan.list().last()
        assertFalse(planValidatorService.validate(jep).isEmpty())
    }
}
