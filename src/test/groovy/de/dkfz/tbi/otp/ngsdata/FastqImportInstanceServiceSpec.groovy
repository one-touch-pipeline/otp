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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class FastqImportInstanceServiceSpec extends Specification implements ServiceUnitTest<FastqImportInstanceService>, DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        [
                FastqImportInstance,
        ]
    }

    void "findCountWithWaitingState, should return the correct count of FastqImportInstance in WAITING state"() {
        given:
        [
            FastqImportInstance.WorkFlowTriggerState.PROCESSING,
            FastqImportInstance.WorkFlowTriggerState.SUCCESS,
            FastqImportInstance.WorkFlowTriggerState.FAILED,
            FastqImportInstance.WorkFlowTriggerState.WAITING,
            FastqImportInstance.WorkFlowTriggerState.WAITING,
        ].each {
            createFastqImportInstance([
                    state : it
            ])
        }

        when:
        int count = service.findCountWithWaitingState()

        then:
        count == 2
    }

    void "changeProcessToWait, should change the state from PROCESSING to WAITING"() {
        given:
        FastqImportInstance fastqImportInstance = createFastqImportInstance([
                state : FastqImportInstance.WorkFlowTriggerState.PROCESSING,
                otrsTicket: createOtrsTicket(),
        ])

        when:
        service.changeProcessToWait()

        then:
        fastqImportInstance.state == FastqImportInstance.WorkFlowTriggerState.WAITING
    }

    @Unroll
    void "waiting, should return a valid FastqImportInstance or null object depending on the available FastqImportInstancea"() {
        given:
        states.each {
            createFastqImportInstance([
                    state : it,
            ])
        }

        when:
        FastqImportInstance fastqImportInstance = service.waiting()

        then:
        (fastqImportInstance == null) == checkCondition

        where:
        states                                                                                                    || checkCondition
        [ FastqImportInstance.WorkFlowTriggerState.WAITING,    FastqImportInstance.WorkFlowTriggerState.WAITING ] || false
        [ FastqImportInstance.WorkFlowTriggerState.PROCESSING, FastqImportInstance.WorkFlowTriggerState.SUCCESS ] || true
        [ FastqImportInstance.WorkFlowTriggerState.WAITING,    FastqImportInstance.WorkFlowTriggerState.FAILED  ] || false
        [ FastqImportInstance.WorkFlowTriggerState.PROCESSING, FastqImportInstance.WorkFlowTriggerState.SUCCESS ] || true
    }
}
