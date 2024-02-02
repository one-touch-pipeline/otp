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
package de.dkfz.tbi.otp.job.processing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles

import static de.dkfz.tbi.otp.job.processing.ExecutionState.*

@Rollback
@Integration
class ProcessServiceIntegrationSpec extends Specification implements UserAndRoles {

    @Autowired
    ProcessService processService

    ProcessingStep step

    void setupData() {
        createUserAndRoles()
        step = DomainFactory.createProcessingStep()
    }

    private ProcessingStepUpdate addUpdate(ExecutionState state, Date date) {
        return DomainFactory.createProcessingStepUpdate(processingStep: step, state: state, date: date)
    }

    private List<ProcessingStepUpdate> createUpdates(List<List> updateProperties) {
        return updateProperties.collect {
            addUpdate(it[0] as ExecutionState, new Date(it[1] as Long))
        }
    }

    void "getProcessingStepDuration, typical setup"() {
        given:
        setupData()
        createUpdates([
            [CREATED,  10],
            [STARTED,  20],
            [FINISHED, 40],
            [SUCCESS,  50],
        ])

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step)
        }

        then:
        40 == result
    }

    void "getProcessingStepDuration, multiple STARTED and FINISHED entries"() {
        given:
        setupData()
        createUpdates([
            [CREATED,  10],
            [STARTED,  20],
            [FINISHED, 30],
            [STARTED,  40],
            [FINISHED, 50],
            [SUCCESS,  60],
        ])

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step)
        }

        then:
        50 == result
    }

    void "getProcessingStepDuration, multiple FAILURE states, should normally not happen"() {
        given:
        setupData()
        createUpdates([
                [CREATED,   10],
                [STARTED,   20],
                [FINISHED,  30],
                [FAILURE,   40],
                [RESUMED,   50],
                [FINISHED,  60],
                [FAILURE,   70],
                [RESTARTED, 80],
        ])

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step)
        }

        then:
        60 == result
    }

    void "getProcessingStepDuration, typical setup after shutdown"() {
        given:
        setupData()
        createUpdates([
            [CREATED,   10],
            [STARTED,   20],
            [SUSPENDED, 30],
            [RESUMED,   40],
            [STARTED,   50],
            [FINISHED,  60],
            [SUCCESS,   70],
        ])

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step)
        }

        then:
        60 == result
    }

    @Unroll
    void "getProcessingStepDuration for finished ProcessingStep (#lastState)"() {
        given:
        setupData()
        createUpdates([
                [CREATED,   10],
                [STARTED,   20],
                [lastState, 40],
        ])

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step)
        }

        then:
        30 == result

        where:
        lastState << [FAILURE, FINISHED, SUCCESS]
    }

    void "getProcessingStepDuration, for restarted ProcessingStep"() {
        given:
        setupData()
        createUpdates([
                [CREATED,   10],
                [STARTED,   20],
                [FAILURE,   40],
                [RESTARTED, 50],
        ])

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step)
        }

        then:
        30 == result
    }

    void "getProcessingStepDuration for unfinished ProcessingStep"() {
        given:
        setupData()
        createUpdates([
                [CREATED,   10],
                [STARTED,   20],
                [SUSPENDED, 30],
                [RESUMED,   40],
                [STARTED,   50],
        ])

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step, new Date(60))
        }

        then:
        50 == result
    }

    void "getProcessingStepDuration without any updates"() {
        given:
        setupData()

        Long result

        when:
        result = doWithAuth(OPERATOR) {
            processService.getProcessingStepDuration(step)
        }

        then:
        0 == result
    }
}
