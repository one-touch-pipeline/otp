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
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.alignment.AbstractAlignmentLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentLinkFileServiceFactoryService
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class SophiaExecuteJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @TempDir
    Path tempDir

    SophiaExecuteJob job
    SophiaInstance instance
    WorkflowStep workflowStep

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                FileType,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyMergedBamQa,
                RoddyWorkflowConfig,
                SampleTypePerProject,
                SophiaInstance,
        ]
    }

    void setupDataForGetConfigurationValues() {
        instance = SophiaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: createWorkflowVersion([
                                workflowVersion: "2.2.3",
                        ]),
                        workflow       : SophiaDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])

        job = new SophiaExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }
        job.sophiaWorkFileService = Mock(SophiaWorkFileService) {
            getDirectoryPath(instance) >> tempDir
        }
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.roddyConfigValueService.alignmentLinkFileServiceFactoryService = Mock(AlignmentLinkFileServiceFactoryService) {
            getService(_) >> Mock(AbstractAlignmentLinkFileService) {
                getPathForFurtherProcessing(instance.sampleType1BamFile) >> tempDir.resolve('bam1')
                getPathForFurtherProcessing(instance.sampleType2BamFile) >> tempDir.resolve('bam2')
            }
        }
    }

    void "test getRoddyResult"() {
        given:
        SophiaInstance instance = SophiaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : SophiaDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])

        SophiaExecuteJob job = new SophiaExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }

        expect:
        job.getRoddyResult(workflowStep) == instance
    }

    void "test getRoddyWorkflowName"() {
        expect:
        new SophiaExecuteJob().roddyWorkflowName == "SophiaWorkflow"
    }

    void "test getAnalysisConfiguration"() {
        expect:
        new SophiaExecuteJob().getAnalysisConfiguration(createSeqType()) == 'sophiaAnalysis'
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        !(new SophiaExecuteJob().filenameSectionKillSwitch)
    }

    void "test getConfigurationValues"() {
        given:
        setupDataForGetConfigurationValues()

        AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createQa(instance.sampleType1BamFile)
        AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createQa(instance.sampleType2BamFile)

        expect:
        TestCase.assertContainSame(job.getConfigurationValues(workflowStep, "{}"), [
                bamfile_list                           : [value: "${tempDir.resolve("${instance.sampleType2BamFile.sampleType.dirName}_${instance.sampleType2BamFile.individual.pid}_merged.mdup.bam")};" +
                        "${tempDir.resolve("${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType1BamFile.individual.pid}_merged.mdup.bam")}" as String],
                possibleControlSampleNamePrefixes      : [value: instance.sampleType2BamFile.sampleType.dirName],
                possibleTumorSampleNamePrefixes        : [value: instance.sampleType1BamFile.sampleType.dirName],
                sample_list                            : [value: "${instance.sampleType2BamFile.sampleType.dirName};${instance.sampleType1BamFile.sampleType.dirName}" as String],
                selectSampleExtractionMethod           : [value: 'version_2'],
                matchExactSampleName                   : [value: 'true', type: "boolean"],
                allowSampleTerminationWithIndex        : [value: 'false', type: "boolean"],
                useLowerCaseFilenameForSampleExtraction: [value: 'false', type: "boolean"],
                controlDefaultReadLength               : [value: '100'],
                controlMedianIsize                     : [value: '3991.0'],
                controlProperPairPercentage            : [value: '91.32632075'],
                controlStdIsizePercentage              : [value: '231.0'],
                tumorDefaultReadLength                 : [value: '100'],
                tumorMedianIsize                       : [value: '3991.0'],
                tumorProperPairPercentage              : [value: '91.32632075'],
                tumorStdIsizePercentage                : [value: '231.0'],
        ])
    }

    void "test getAdditionalParameters"() {
        expect:
        new SophiaExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }
}
