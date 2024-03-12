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
package de.dkfz.tbi.otp.workflow.analysis.sophia

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.AbstractBamFileServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
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
                        workflowVersion: null,
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
        job.abstractBamFileServiceFactoryService = Mock(AbstractBamFileServiceFactoryService) {
            getService(_) >> Mock(AbstractAbstractBamFileService) {
                getFinalInsertSizeFile(instance.sampleType1BamFile) >> tempDir.resolve('insert-size-1')
                getFinalInsertSizeFile(instance.sampleType2BamFile) >> tempDir.resolve('insert-size-2')
            }
        }
        job.roddyConfigValueService = new RoddyConfigValueService()
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
                bamfile_list                           : "${tempDir.resolve("${instance.sampleType2BamFile.sampleType.dirName}_${instance.sampleType2BamFile.individual.pid}_merged.mdup.bam")};" +
                        "${tempDir.resolve("${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType1BamFile.individual.pid}_merged.mdup.bam")}",
                possibleControlSampleNamePrefixes      : instance.sampleType2BamFile.sampleType.dirName,
                possibleTumorSampleNamePrefixes        : instance.sampleType1BamFile.sampleType.dirName,
                sample_list                            : "${instance.sampleType2BamFile.sampleType.dirName};${instance.sampleType1BamFile.sampleType.dirName}",
                selectSampleExtractionMethod           : 'version_2',
                matchExactSampleName                   : 'true',
                allowSampleTerminationWithIndex        : 'false',
                useLowerCaseFilenameForSampleExtraction: 'false',
                controlDefaultReadLength               : '100',
                controlMedianIsize                     : '3991.0',
                controlProperPairPercentage            : '91.32632075',
                controlStdIsizePercentage              : '231.0',
                tumorDefaultReadLength                 : '100',
                tumorMedianIsize                       : '3991.0',
                tumorProperPairPercentage              : '91.32632075',
                tumorStdIsizePercentage                : '231.0',
                insertsizesfile_list                   : "${tempDir.resolve("insert-size-2")};${tempDir.resolve("insert-size-1")}",
        ])
    }

    void "test getAdditionalParameters"() {
        expect:
        new SophiaExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }
}
