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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.PanCancerWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class PanCancerParseJobSpec extends Specification implements DataTest, PanCancerWorkflowDomainFactory, RoddyPanCancerFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                FileType,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyMergedBamQa,
                RoddySingleLaneQa,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    @TempDir
    Path tempDir

    void "test execution"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        RoddyBamFile roddyBamFile = createRoddyBamFile(RoddyBamFile)

        Path mergedQAJsonFile = tempDir.resolve("qa.json")
        mergedQAJsonFile.text = qaFileContent

        PanCancerParseJob job = new PanCancerParseJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> roddyBamFile
            0 * _
        }
        job.roddyQualityAssessmentService = new RoddyQualityAssessmentService()
        job.roddyQualityAssessmentService.roddyBamFileService = Mock(RoddyBamFileService) {
            getWorkSingleLaneQAJsonFiles(_) >> [(roddyBamFile.seqTracks.first()): mergedQAJsonFile]
            getWorkMergedQAJsonFile(_) >> mergedQAJsonFile
            getWorkMergedQATargetExtractJsonFile(_) >> mergedQAJsonFile
        }
        job.roddyQualityAssessmentService.referenceGenomeService = new ReferenceGenomeService()
        job.roddyQualityAssessmentService.abstractBamFileService = new AbstractBamFileService()
        job.qcTrafficLightService = Mock(QcTrafficLightService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.getRoddyBamFile(workflowStep) >> roddyBamFile

        when:
        job.execute(workflowStep)

        then:
        List<RoddySingleLaneQa> singleLaneQa = RoddySingleLaneQa.findAllByAbstractBamFile(roddyBamFile)
        singleLaneQa.each { qa ->
            qaValuesPropertiesMultipleChromosomes[qa.chromosome].each { k, v ->
                assert qa."${k}" == v
            }
        }
        List<RoddyMergedBamQa> mergedQa = RoddyMergedBamQa.findAllByAbstractBamFile(roddyBamFile)
        mergedQa.each { qa ->
            qaValuesPropertiesMultipleChromosomes[qa.chromosome].each { k, v ->
                assert qa."${k}" == v
            }
        }

        roddyBamFile.coverage == 0.011
        roddyBamFile.coverageWithN == 1866013.0

        1 * job.qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(roddyBamFile, _)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }

    void "test execute, when QA exists, objects should be reused"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        RoddyBamFile roddyBamFile = createRoddyBamFile(RoddyBamFile)
        List<RoddyMergedBamQa> existingMergedQa = qaValuesPropertiesMultipleChromosomes.collect { k, v ->
            new RoddyMergedBamQa(v + [
                    abstractBamFile              : roddyBamFile,
                    chromosome8QcBasesMapped     : 999,
                    percentageMatesOnDifferentChr: 777.123,
            ]).save(flush: true)
        }
        List<RoddySingleLaneQa> existingSingleLaneQa = qaValuesPropertiesMultipleChromosomes.collect { k, v ->
            new RoddySingleLaneQa(v + [
                    abstractBamFile              : roddyBamFile,
                    seqTrack                     : roddyBamFile.seqTracks.first(),
                    chromosome8QcBasesMapped     : 999,
                    percentageMatesOnDifferentChr: 777.123,
            ]).save(flush: true)
        }

        Path mergedQAJsonFile = tempDir.resolve("qa.json")
        mergedQAJsonFile.text = qaFileContent

        PanCancerParseJob job = new PanCancerParseJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> roddyBamFile
            0 * _
        }
        job.roddyQualityAssessmentService = new RoddyQualityAssessmentService()
        job.roddyQualityAssessmentService.roddyBamFileService = Mock(RoddyBamFileService) {
            getWorkSingleLaneQAJsonFiles(_) >> [(roddyBamFile.seqTracks.first()): mergedQAJsonFile]
            getWorkMergedQAJsonFile(_) >> mergedQAJsonFile
            getWorkMergedQATargetExtractJsonFile(_) >> mergedQAJsonFile
        }
        job.roddyQualityAssessmentService.referenceGenomeService = new ReferenceGenomeService()
        job.roddyQualityAssessmentService.abstractBamFileService = new AbstractBamFileService()
        job.qcTrafficLightService = Mock(QcTrafficLightService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.getRoddyBamFile(workflowStep) >> roddyBamFile

        when:
        job.execute(workflowStep)

        then:
        List<RoddySingleLaneQa> singleLaneQa = RoddySingleLaneQa.findAllByAbstractBamFile(roddyBamFile)
        singleLaneQa.each { qa ->
            qaValuesPropertiesMultipleChromosomes[qa.chromosome].each { k, v ->
                assert qa."${k}" == v
            }
        }
        CollectionUtils.containSame(singleLaneQa, existingSingleLaneQa)

        List<RoddyMergedBamQa> mergedQa = RoddyMergedBamQa.findAllByAbstractBamFile(roddyBamFile)
        mergedQa.each { qa ->
            qaValuesPropertiesMultipleChromosomes[qa.chromosome].each { k, v ->
                assert qa."${k}" == v
            }
        }
        CollectionUtils.containSame(mergedQa, existingMergedQa)

        roddyBamFile.coverage == 0.011
        roddyBamFile.coverageWithN == 1866013.0

        1 * job.qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(roddyBamFile, _)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }
}
