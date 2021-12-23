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
package de.dkfz.tbi.otp.config

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam.ReplaceSourceWithLinkJob
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.ExecutePanCanJob

@Rollback
@Integration
class TypeValidatorsIntegrationSpec extends Specification {

    @Unroll
    void "check JOB_NAME for value '#name' should return '#ret'"() {
        expect:
        returnValue == TypeValidators.JOB_NAME_SEQ_TYPE.validate(valueClosure())

        where:
        name                       | valueClosure                                       || returnValue
        'empty'                    | { '' }                                             || false
        'null'                     | { null }                                           || false
        'OtherName'                | { 'OtherJobName' }                                 || false
        //example for roddy job
        'ExecutePanCanJob'         | { ExecutePanCanJob.simpleName }                    || false
        //example for no cluster job
        'ReplaceSourceWithLinkJob' | { ReplaceSourceWithLinkJob.simpleName }            || false
    }
}
