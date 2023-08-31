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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.Workflow

class MergingCriteriaSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MergingCriteria,
                Project,
                Realm,
                SeqType,
                Workflow,
        ]
    }

    void "test that for Exome data LibPrepKit must be true"() {
        expect:
        DomainFactory.createMergingCriteriaLazy([
                seqType: DomainFactory.createExomeSeqType(),
                useLibPrepKit: true,
        ])
    }

    void "test that for Exome data LibPrepKit must be true, should fail when it is false"() {
        given:
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteria()
        SeqType seqType = DomainFactory.createExomeSeqType()

        when:
        mergingCriteria.useLibPrepKit = false
        mergingCriteria.seqType = seqType

        then:
        TestCase.assertValidateError(mergingCriteria, "useLibPrepKit", "exome", false)
    }

    void "test that for WGBS data LibPrepKit must be false"() {
        expect:
        DomainFactory.createMergingCriteriaLazy([
                seqType   : DomainFactory.createWholeGenomeBisulfiteSeqType(),
                useLibPrepKit: false,
        ])
    }

    void "test that for WGBS data LibPrepKit must be false, should fail when it is true"() {
        given:
        SeqType seqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        createWorkflow(name: WgbsWorkflow.WGBS_WORKFLOW, supportedSeqTypes: [seqType] as Set)
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteria()

        when:
        mergingCriteria.useLibPrepKit = true
        mergingCriteria.seqType = seqType

        then:
        TestCase.assertValidateError(mergingCriteria, "useLibPrepKit", "wgbs", true)
    }
}
