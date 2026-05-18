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
package de.dkfz.tbi.otp.workflowExecution

import spock.lang.Specification
import spock.lang.Unroll

class MultiApiVersionWorkflowSpec extends Specification {

    private static final List<String> JOB_LIST_V1 = ["firstJob", "middleJob", "lastJob"].asImmutable()
    private static final List<String> JOB_LIST_V2 = ["firstJobV2", "lastJobV2"].asImmutable()

    private final MultiApiVersionWorkflow workflow = new MultiApiVersionWorkflow() {

        @Override
        List<String> getJobList(Integer identifier) {
            switch (identifier) {
                case 1: return JOB_LIST_V1
                case 2: return JOB_LIST_V2
                default: return []
            }
        }

        @Override
        Artefact createCopyOfArtefact(Artefact artefact) { return null }

        @Override
        void reconnectDependencies(Artefact artefact, Artefact newArtefact, String role) {
        }

        final String userDocumentation = null

        @Override
        boolean isAlignment() { return false }

        @Override
        boolean isAnalysis() { return false }
    }

    private WorkflowRun createStubRun(Integer apiVersionIdentifier) {
        return Stub(WorkflowRun) {
            getWorkflowVersion() >> Stub(WorkflowVersion) {
                getApiVersion() >> Stub(WorkflowApiVersion) {
                    getIdentifier() >> apiVersionIdentifier
                }
            }
        }
    }

    private WorkflowStep createStubStep(String beanName, Integer apiVersionIdentifier) {
        return Stub(WorkflowStep) {
            getBeanName() >> beanName
            getWorkflowRun() >> createStubRun(apiVersionIdentifier)
        }
    }

    @Unroll
    void "getFirstJobBeanName for api version #identifier should return first job of that version's list"() {
        expect:
        workflow.getFirstJobBeanName(createStubRun(identifier)) == expectedFirst

        where:
        identifier | expectedFirst
        1          | "firstJob"
        2          | "firstJobV2"
    }

    @Unroll
    void "getNextJobBeanName for '#beanName' (api version #identifier) should return '#expectedNext'"() {
        expect:
        workflow.getNextJobBeanName(createStubStep(beanName, identifier)) == expectedNext

        where:
        identifier | beanName     | expectedNext
        1          | "firstJob"   | "middleJob"
        1          | "middleJob"  | "lastJob"
        1          | "lastJob"    | null
        2          | "firstJobV2" | "lastJobV2"
        2          | "lastJobV2"  | null
    }

    void "getFirstJobBeanName with unknown api version should throw IllegalArgumentException"() {
        when:
        workflow.getFirstJobBeanName(createStubRun(99))

        then:
        IllegalArgumentException e = thrown()
        e.message.contains("99")
    }

    void "getNextJobBeanName with unknown api version should throw IllegalArgumentException"() {
        when:
        workflow.getNextJobBeanName(createStubStep("firstJob", 99))

        then:
        IllegalArgumentException e = thrown()
        e.message.contains("99")
    }

    void "getNextJobBeanName with unknown beanName should throw IllegalStateException"() {
        when:
        workflow.getNextJobBeanName(createStubStep("unknownJob", 1))

        then:
        IllegalStateException e = thrown()
        e.message.contains("unknownJob")
    }
}
