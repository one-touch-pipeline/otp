/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflow.analysis.sophia

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.*

class SophiaConditionalSkipJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                ProcessingPriority,
                Project,
                WorkflowRun,
                Pipeline,
                LibraryPreparationKit,
                SampleType,
                Sample,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                RoddyBamFile,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                RoddyMergedBamQa,
                ExternallyProcessedBamFileQualityAssessment,
        ]
    }

    AbstractBamFile tumorBamFile
    AbstractBamFile controlBamFile
    SophiaConditionalSkipJob job

    void setup() {
        job = new SophiaConditionalSkipJob()
    }

    void "checkBamFiles should run through successfully with RoddyBamFile"() {
        given:
        tumorBamFile = createBamFile()
        controlBamFile = createBamFile()
        DomainFactory.createRoddyMergedBamQa([
                abstractBamFile: tumorBamFile,
        ])
        DomainFactory.createRoddyMergedBamQa([
                abstractBamFile: controlBamFile,
        ])

        when:
        job.checkBamFiles(tumorBamFile, controlBamFile)

        then:
        0 * _
    }

    void "checkBamFiles should run through successfully with ExternallyProcessedBamFile"() {
        given:
        tumorBamFile = DomainFactory.createExternallyProcessedBamFile([maximumReadLength: 150])
        controlBamFile = DomainFactory.createExternallyProcessedBamFile([maximumReadLength: 150])
        DomainFactory.createExternallyProcessedBamFileQualityAssessment(tumorBamFile)
        DomainFactory.createExternallyProcessedBamFileQualityAssessment(controlBamFile)

        when:
        job.checkBamFiles(tumorBamFile, controlBamFile)

        then:
        0 * _
    }

    void "checkBamFiles should throw skip exception when maximalReadLength is not set by #bamFileType bam file"() {
        given:
        tumorBamFile = createCustomBamFile([maximalReadLength: tumorMaximalReadLength])
        controlBamFile = createCustomBamFile([maximalReadLength: controlMaximalReadLength])

        when:
        job.checkBamFiles(tumorBamFile, controlBamFile)

        then:
        SkipWorkflowStepException skipException = thrown(SkipWorkflowStepException)
        skipException.message.contains("MaximalReadLength")
        skipException.skipMessage.category == WorkflowStepSkipMessage.Category.MAXIMAL_READ_LENGTH_MISSING

        and:
        0 * _

        where:
        bamFileType         | tumorMaximalReadLength | controlMaximalReadLength
        'tumor'             | false                  | true
        'control'           | true                   | false
        'control and tumor' | false                  | false
    }

    void "checkBamFiles should throw skip exception when SophiaWorkflowQualityAssessment does not exist for #bamFileType bam file"() {
        given:
        tumorBamFile = createBamFile()
        controlBamFile = createBamFile()
        if (tumorQA) {
            DomainFactory.createRoddyMergedBamQa([
                    abstractBamFile: tumorBamFile,
            ])
        }
        if (controlQA) {
            DomainFactory.createRoddyMergedBamQa([
                    abstractBamFile: controlBamFile,
            ])
        }

        when:
        job.checkBamFiles(tumorBamFile, controlBamFile)

        then:
        SkipWorkflowStepException skipException = thrown(SkipWorkflowStepException)
        skipException.message.contains("No SophiaWorkflowQualityAssessment")
        skipException.skipMessage.category == WorkflowStepSkipMessage.Category.SOPHIA_WORKFLOW_COMPATIBLE_QUALITY_ASSESSMENT_MISSING

        and:
        0 * _

        where:
        bamFileType         | tumorQA | controlQA
        'tumor'             | false   | true
        'control'           | true    | false
        'control and tumor' | false   | false
    }

    AbstractBamFile createCustomBamFile(Map properties) {
        AbstractBamFile bamFile
        if (properties.maximalReadLength) {
            bamFile = DomainFactory.createExternallyProcessedBamFile([maximumReadLength: null])
        } else {
            bamFile = DomainFactory.createExternallyProcessedBamFile()
        }
        DomainFactory.createExternallyProcessedBamFileQualityAssessment(bamFile)
        return bamFile
    }
}
