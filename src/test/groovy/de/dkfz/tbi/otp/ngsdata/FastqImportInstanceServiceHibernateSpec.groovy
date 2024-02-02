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
package de.dkfz.tbi.otp.ngsdata

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState

class FastqImportInstanceServiceHibernateSpec extends HibernateSpec implements ServiceUnitTest<FastqImportInstanceService>, DomainFactoryCore {

    @Override
    List<Class> getDomainClasses() {
        return [
                RawSequenceFile,
                FastqFile,
                FastqImportInstance,
        ]
    }

    @Unroll
    void "waiting, should return a valid FastqImportInstance or null object depending on the available FastqImportInstances"() {
        given:
        states.each {
            createFastqImportInstanceHelper(it)
        }

        when:
        FastqImportInstance fastqImportInstance = service.waiting()

        then:
        (fastqImportInstance != null) == returnOne

        where:
        states                                                                                                || returnOne
        [WorkflowCreateState.WAITING]                                                     || true
        [WorkflowCreateState.PROCESSING]                                                  || false
        [WorkflowCreateState.SUCCESS]                                                     || false
        [WorkflowCreateState.FAILED]                                                      || false
        [WorkflowCreateState.WAITING, WorkflowCreateState.WAITING]    || true
        [WorkflowCreateState.WAITING, WorkflowCreateState.PROCESSING] || true
        [WorkflowCreateState.WAITING, WorkflowCreateState.SUCCESS]    || true
        [WorkflowCreateState.WAITING, WorkflowCreateState.FAILED]     || true
        [WorkflowCreateState.PROCESSING, WorkflowCreateState.SUCCESS] || false
        [WorkflowCreateState.PROCESSING, WorkflowCreateState.FAILED]  || false

        name = "instances with states ${states.join(' and ')}"
        result = returnOne ? "return a waiting" : "do not return any"
    }

    void "waiting, when multiple waiting and no one is in process, then return the oldest"() {
        given:
        FastqImportInstance fastqImportInstanceExpected = (1..3).collect {
            createFastqImportInstanceHelper(WorkflowCreateState.WAITING)
        }.first()

        when:
        FastqImportInstance fastqImportInstance = service.waiting()

        then:
        fastqImportInstance == fastqImportInstanceExpected
    }

    @Unroll
    void "waiting, when #name, then  #result"() {
        given:
        createFastqImportInstanceHelper(WorkflowCreateState.PROCESSING, import1Projects)
        createFastqImportInstanceHelper(WorkflowCreateState.WAITING, import2Projects)

        when:
        FastqImportInstance fastqImportInstance = service.waiting()

        then:
        (fastqImportInstance != null) == returnAny

        where:
        import1Projects | import2Projects || returnAny
        ["p1"]          | ["p2"]          || true
        ["p1"]          | ["p2", "p3"]    || true
        ["p1", "p2"]    | ["p3"]          || true
        ["p1", "p2"]    | ["p3", "p4"]    || true
        ["p1"]          | ["p1"]          || false
        ["p1"]          | ["p1", "p2"]    || false
        ["p1", "p2"]    | ["p2"]          || false
        ["p1", "p2"]    | ["p1", "p2"]    || false
        ["p1", "p2"]    | ["p2", "p3"]    || false

        name = "one waiting with project ${import1Projects.join(' and ')} and one waiting with project ${import2Projects.join(' and ')}"
        result = returnAny ? "return the waiting" : "do not return any"
    }

    private FastqImportInstance createFastqImportInstanceHelper(WorkflowCreateState state, List<String> projectNames = ["name_${nextId}"]) {
        FastqImportInstance fastqImportInstance = createFastqImportInstance([
                state: state,
        ])
        fastqImportInstance.sequenceFiles = projectNames.collect { String projectName ->
            createFastqFile([
                    fastqImportInstance: fastqImportInstance,
                    seqTrack           : createSeqTrack([
                            sample: createSample([
                                    individual: createIndividual([
                                            project: CollectionUtils.atMostOneElement(Project.findAllByName(projectName)) ?: createProject([
                                                    name: projectName,
                                            ]),
                                    ]),
                            ]),
                    ]),
            ])
        }
        return fastqImportInstance
    }
}
