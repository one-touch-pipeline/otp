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
package de.dkfz.tbi.otp.workflow.analysis.snv

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.AbstractBamFileServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SnvDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class SnvExecuteJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @TempDir
    Path tempDir

    SnvExecuteJob job
    RoddySnvCallingInstance instance
    WorkflowStep workflowStep

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                FileType,
                MergingWorkPackage,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyMergedBamQa,
                RoddySnvCallingInstance,
                RoddyWorkflowConfig,
                SampleTypePerProject,
        ]
    }

    void setupDataForGetConfigurationValues() {
        instance = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : SnvDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])

        job = new SnvExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }
        job.snvWorkFileService = Mock(SnvWorkFileService) {
            getDirectoryPath(instance) >> tempDir.resolve('work-dir')
        }
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.roddyConfigValueService.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()
        job.roddyConfigValueService.abstractBamFileServiceFactoryService = Mock(AbstractBamFileServiceFactoryService) {
            getService(_) >> Mock(AbstractAbstractBamFileService) {
                getPathForFurtherProcessing(instance.sampleType1BamFile) >> tempDir.resolve('bam1')
                getPathForFurtherProcessing(instance.sampleType2BamFile) >> tempDir.resolve('bam2')
            }
        }
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(instance.referenceGenome) >> { new File("/fasta-path") }
            chromosomeLengthFile(instance.sampleType2BamFile.mergingWorkPackage) >> { new File("/chr-length-path") }
        }
        job.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> tempDir
        }
    }

    void "test getRoddyResult"() {
        given:
        RoddySnvCallingInstance instance = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : SnvDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])

        SnvExecuteJob job = new SnvExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }

        expect:
        job.getRoddyResult(workflowStep) == instance
    }

    void "test getRoddyWorkflowName"() {
        expect:
        new SnvExecuteJob().roddyWorkflowName == "SNVCallingWorkflow"
    }

    void "test getAnalysisConfiguration"() {
        expect:
        new SnvExecuteJob().getAnalysisConfiguration(createSeqType()) == 'snvCallingAnalysis'
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        !(new SnvExecuteJob().filenameSectionKillSwitch)
    }

    void "test getConfigurationValues"() {
        given:
        setupDataForGetConfigurationValues()

        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(instance.referenceGenome, chromosomeNames)

        expect:
        TestCase.assertContainSame(job.getConfigurationValues(workflowStep, "{}"), [
                bamfile_list                     : "${tempDir.resolve('bam2')};${tempDir.resolve('bam1')}",
                possibleControlSampleNamePrefixes: instance.sampleType2BamFile.sampleType.dirName,
                possibleTumorSampleNamePrefixes  : instance.sampleType1BamFile.sampleType.dirName,
                sample_list                      : "${instance.sampleType2BamFile.sampleType.dirName};${instance.sampleType1BamFile.sampleType.dirName}",
                CHROMOSOME_LENGTH_FILE           : '/chr-length-path',
                REFERENCE_GENOME                 : '/fasta-path',
                CHR_SUFFIX                       : instance.referenceGenome.chromosomeSuffix,
                CHR_PREFIX                       : instance.referenceGenome.chromosomePrefix,
                CHROMOSOME_INDICES               : '( 1 2 3 4 5 X Y M )',
                analysisMethodNameOnOutput       : 'work-dir',
        ])
    }

    void "test getAdditionalParameters"() {
        expect:
        new SnvExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }
}
