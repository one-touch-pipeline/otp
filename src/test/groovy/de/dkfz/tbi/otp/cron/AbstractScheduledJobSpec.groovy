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

class AbstractScheduledJobSpec extends Specification {

    // minimal concrete subclass just to instantiate the abstract class
    private final AbstractScheduledJob job = new AbstractScheduledJob() {
        @Override
        void wrappedExecute() { }
    }

    void "getCronExpression, returns the default 5 AM daily cron expression"() {
        expect:
        job.cronExpression == "0 0 5 * * *"
    }

    void "getCronExpression, child that overrides it returns its own cron expression"() {
        given:
        AbstractScheduledJob childJob = new AbstractScheduledJob() {
            @Override
            void wrappedExecute() { }
            final String cronExpression = "0 */5 * * * *"
        }

        expect:
        childJob.cronExpression == "0 */5 * * * *"
    }

    void "getCronExpression, two children with different overrides return independent values"() {
        given:
        AbstractScheduledJob jobA = new AbstractScheduledJob() {
            @Override void wrappedExecute() { }
            final String cronExpression = "0 0 6 * * *"
        }
        AbstractScheduledJob jobB = new AbstractScheduledJob() {
            @Override void wrappedExecute() { }
            final String cronExpression = "0 0 7 * * *"
        }

        expect:
        jobA.cronExpression == "0 0 6 * * *"
        jobB.cronExpression == "0 0 7 * * *"
        jobA.cronExpression != jobB.cronExpression
    }
}
