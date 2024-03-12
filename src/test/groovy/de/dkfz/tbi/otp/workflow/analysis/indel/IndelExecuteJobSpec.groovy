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
package de.dkfz.tbi.otp.workflow.analysis.indel

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.IndelDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class IndelExecuteJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @TempDir
    Path tempDir

    Path workDir
    IndelExecuteJob job
    IndelCallingInstance instance
    WorkflowStep workflowStep

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BedFile,
                FastqFile,
                FastqImportInstance,
                FileType,
                IndelCallingInstance,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyMergedBamQa,
                RoddyWorkflowConfig,
                SampleTypePerProject,
        ]
    }

    void setupDataForGetConfigurationValues() {
        instance = IndelDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : IndelDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])
        workDir = tempDir.resolve('work-dir')

        job = new IndelExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }
        job.indelWorkFileService = Mock(IndelWorkFileService) {
            getDirectoryPath(instance) >> workDir
        }
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(instance.referenceGenome) >> { new File("/fasta-path") }
        }
        job.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> tempDir
        }
    }

    void "test getRoddyResult"() {
        given:
        IndelCallingInstance instance = IndelDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : IndelDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])

        IndelExecuteJob job = new IndelExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }

        expect:
        job.getRoddyResult(workflowStep) == instance
    }

    void "test getRoddyWorkflowName"() {
        expect:
        new IndelExecuteJob().roddyWorkflowName == "IndelCallingWorkflow"
    }

    void "test getAnalysisConfiguration"() {
        expect:
        new IndelExecuteJob().getAnalysisConfiguration(createSeqType()) == 'indelCallingAnalysis'
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        !(new IndelExecuteJob().filenameSectionKillSwitch)
    }

    void "test getConfigurationValues"() {
        given:
        setupDataForGetConfigurationValues()

        expect:
        TestCase.assertContainSame(job.getConfigurationValues(workflowStep, "{}"), [
                bamfile_list                           : "${workDir.resolve("${instance.sampleType2BamFile.sampleType.dirName}_${instance.sampleType2BamFile.individual.pid}_merged.mdup.bam")};" +
                        "${workDir.resolve("${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType1BamFile.individual.pid}_merged.mdup.bam")}",
                possibleControlSampleNamePrefixes      : instance.sampleType2BamFile.sampleType.dirName,
                possibleTumorSampleNamePrefixes        : instance.sampleType1BamFile.sampleType.dirName,
                sample_list                            : "${instance.sampleType2BamFile.sampleType.dirName};${instance.sampleType1BamFile.sampleType.dirName}",
                selectSampleExtractionMethod           : 'version_2',
                matchExactSampleName                   : 'true',
                allowSampleTerminationWithIndex        : 'false',
                useLowerCaseFilenameForSampleExtraction: 'false',
                REFERENCE_GENOME                       : '/fasta-path',
                CHR_SUFFIX                             : instance.referenceGenome.chromosomeSuffix,
                CHR_PREFIX                             : instance.referenceGenome.chromosomePrefix,
                VCF_NORMAL_HEADER_COL                  : instance.sampleType2BamFile.sampleType.dirName,
                VCF_TUMOR_HEADER_COL                   : instance.sampleType1BamFile.sampleType.dirName,
                SEQUENCE_TYPE                          : instance.sampleType1BamFile.seqType.roddyName,
                analysisMethodNameOnOutput             : 'work-dir',
        ])
    }

    void "test getConfigurationValues, when BED file is needed"() {
        given:
        setupDataForGetConfigurationValues()

        instance.sampleType1BamFile.seqType.needsBedFile = true
        instance.sampleType1BamFile.seqType.save(flush: true)
        BedFile bedFile = DomainFactory.createBedFile(referenceGenome: instance.sampleType1BamFile.referenceGenome, libraryPreparationKit: instance.sampleType1BamFile.mergingWorkPackage.libraryPreparationKit)

        job.bedFileService = Mock(BedFileService) {
            filePath(bedFile) >> Paths.get('/bed-path')
        }

        expect:
        TestCase.assertContainSame(job.getConfigurationValues(workflowStep, "{}"), [
                bamfile_list                           : "${workDir.resolve("${instance.sampleType2BamFile.sampleType.dirName}_${instance.sampleType2BamFile.individual.pid}_merged.mdup.bam")};" +
                        "${workDir.resolve("${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType1BamFile.individual.pid}_merged.mdup.bam")}",
                possibleControlSampleNamePrefixes      : instance.sampleType2BamFile.sampleType.dirName,
                possibleTumorSampleNamePrefixes        : instance.sampleType1BamFile.sampleType.dirName,
                sample_list                            : "${instance.sampleType2BamFile.sampleType.dirName};${instance.sampleType1BamFile.sampleType.dirName}",
                selectSampleExtractionMethod           : 'version_2',
                matchExactSampleName                   : 'true',
                allowSampleTerminationWithIndex        : 'false',
                useLowerCaseFilenameForSampleExtraction: 'false',
                REFERENCE_GENOME                       : '/fasta-path',
                CHR_SUFFIX                             : instance.referenceGenome.chromosomeSuffix,
                CHR_PREFIX                             : instance.referenceGenome.chromosomePrefix,
                VCF_NORMAL_HEADER_COL                  : instance.sampleType2BamFile.sampleType.dirName,
                VCF_TUMOR_HEADER_COL                   : instance.sampleType1BamFile.sampleType.dirName,
                SEQUENCE_TYPE                          : instance.sampleType1BamFile.seqType.roddyName,
                analysisMethodNameOnOutput             : 'work-dir',
                EXOME_CAPTURE_KIT_BEDFILE              : '/bed-path',
        ])
    }

    void "test getAdditionalParameters"() {
        expect:
        new IndelExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }
}
