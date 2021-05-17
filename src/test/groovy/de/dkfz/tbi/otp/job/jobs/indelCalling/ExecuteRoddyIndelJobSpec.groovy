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
package de.dkfz.tbi.otp.job.jobs.indelCalling

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

import java.nio.file.FileSystems

class ExecuteRoddyIndelJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                BedFile,
                DataFile,
                FileType,
                IndelCallingInstance,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                Project,
                ProcessingOption,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SequencingKitLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                ReferenceGenome,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                Realm,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
        ]
    }

    @Rule
    public TemporaryFolder temporaryFolder


    void "prepareAndReturnWorkflowSpecificCValues, when roddyIndelCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddyIndelJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert indelCallingInstance')
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine and WGS, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "fasta.fa"))

        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                indelCallingService   : Mock(IndelCallingService) {
                    1 * validateInputBamFiles(_)
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * fastaFilePath(_) >> fasta
                    0 * _
                },
                fileSystemService     : Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                },
                fileService           : new FileService([
                        remoteShellHelper: Mock(RemoteShellHelper) {
                            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                                return new ProcessOutput(command, '', 0)
                            }
                        }
                ]),
        ])

        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])

        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        AbstractMergedBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = indelCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String analysisMethodNameOnOutput = "indel_results/${indelCallingInstance.seqType.libraryLayoutDirName}/" +
                "${indelCallingInstance.sampleType1BamFile.sampleType.dirName}_${indelCallingInstance.sampleType2BamFile.sampleType.dirName}/" +
                "${indelCallingInstance.instanceName}"

        String finalBamFileControlPath = "${indelCallingInstance.workDirectory}/${bamFileControl.sampleType.dirName}_${bamFileControl.individual.pid}_merged.mdup.bam"
        String finalBamFileDiseasePath = "${indelCallingInstance.workDirectory}/${bamFileDisease.sampleType.dirName}_${bamFileDisease.individual.pid}_merged.mdup.bam"

        List<String> expectedList = [
                "bamfile_list:${finalBamFileControlPath};${finalBamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHR_SUFFIX:${indelCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${indelCallingInstance.referenceGenome.chromosomePrefix}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
                "VCF_NORMAL_HEADER_COL:${bamFileControl.sampleType.dirName}",
                "VCF_TUMOR_HEADER_COL:${bamFileDisease.sampleType.dirName}",
                "SEQUENCE_TYPE:${bamFileDisease.seqType.roddyName}",
                "selectSampleExtractionMethod:version_2",
                "matchExactSampleName:true",
                "allowSampleTerminationWithIndex:false",
                "useLowerCaseFilenameForSampleExtraction:false",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(indelCallingInstance)

        then:
        expectedList == returnedList

        cleanup:
        configService.clean()
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine and WES, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "fasta.fa"))
        File bedFile = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "bed.txt"))

        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                indelCallingService   : Mock(IndelCallingService) {
                    1 * validateInputBamFiles(_)
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * fastaFilePath(_) >> fasta
                    0 * _
                },
                bedFileService        : Mock(BedFileService) {
                    1 * filePath(_) >> bedFile
                },
                fileSystemService     : Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                },
                fileService           : new FileService([
                        remoteShellHelper: Mock(RemoteShellHelper) {
                            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                                return new ProcessOutput(command, '', 0)
                            }
                        }
                ]),
        ])
        new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        SeqType seqType = DomainFactory.createExomeSeqType()

        LibraryPreparationKit kit = DomainFactory.createLibraryPreparationKit()
        indelCallingInstance.containedSeqTracks*.libraryPreparationKit = kit
        assert indelCallingInstance.containedSeqTracks*.save(flush: true)
        indelCallingInstance.samplePair.mergingWorkPackage1.libraryPreparationKit = kit
        assert indelCallingInstance.samplePair.mergingWorkPackage1.save(flush: true)
        indelCallingInstance.samplePair.mergingWorkPackage2.libraryPreparationKit = kit
        assert indelCallingInstance.samplePair.mergingWorkPackage2.save(flush: true)

        DomainFactory.createBedFile(
                libraryPreparationKit: kit,
                referenceGenome: indelCallingInstance.sampleType1BamFile.referenceGenome
        )

        indelCallingInstance.samplePair.mergingWorkPackage1.seqType = seqType
        assert indelCallingInstance.samplePair.mergingWorkPackage1.save(flush: true)
        indelCallingInstance.samplePair.mergingWorkPackage2.seqType = seqType
        assert indelCallingInstance.samplePair.mergingWorkPackage2.save(flush: true)
        indelCallingInstance.containedSeqTracks*.seqType = seqType
        assert indelCallingInstance.containedSeqTracks*.save(flush: true)

        AbstractMergedBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = indelCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        DomainFactory.createMergingCriteriaLazy(project: bamFileDisease.mergingWorkPackage.project, seqType: bamFileDisease.mergingWorkPackage.seqType)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String analysisMethodNameOnOutput = "indel_results/${indelCallingInstance.seqType.libraryLayoutDirName}/" +
                "${indelCallingInstance.sampleType1BamFile.sampleType.dirName}_${indelCallingInstance.sampleType2BamFile.sampleType.dirName}/" +
                "${indelCallingInstance.instanceName}"

        String finalBamFileControlPath = "${indelCallingInstance.workDirectory}/${bamFileControl.sampleType.dirName}_${bamFileControl.individual.pid}_merged.mdup.bam"
        String finalBamFileDiseasePath = "${indelCallingInstance.workDirectory}/${bamFileDisease.sampleType.dirName}_${bamFileDisease.individual.pid}_merged.mdup.bam"

        List<String> expectedList = [
                "bamfile_list:${finalBamFileControlPath};${finalBamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHR_SUFFIX:${indelCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${indelCallingInstance.referenceGenome.chromosomePrefix}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
                "VCF_NORMAL_HEADER_COL:${bamFileControl.sampleType.dirName}",
                "VCF_TUMOR_HEADER_COL:${bamFileDisease.sampleType.dirName}",
                "SEQUENCE_TYPE:${bamFileDisease.seqType.roddyName}",
                "EXOME_CAPTURE_KIT_BEDFILE:${bedFile}",
                "selectSampleExtractionMethod:version_2",
                "matchExactSampleName:true",
                "allowSampleTerminationWithIndex:false",
                "useLowerCaseFilenameForSampleExtraction:false",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(indelCallingInstance)

        then:
        expectedList == returnedList
    }


    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddyIndelJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new IndelCallingInstance(),
        ]
    }


    void "validate, when all fine, set processing state to finished"() {
        given:
        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                configService             : new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path]),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _)
                },
                indelCallingService       : Mock(IndelCallingService) {
                    1 * validateInputBamFiles(_)
                },
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createIndelResultFiles(indelCallingInstance)

        expect:
        job.validate(indelCallingInstance)
    }


    void "validate, when indelCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddyIndelJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input indelCallingInstance must not be null. Expression')
    }


    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString
        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                configService             : new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path]),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createIndelResultFiles(indelCallingInstance)

        when:
        job.validate(indelCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        indelCallingInstance.processingState != AnalysisProcessingStates.FINISHED
    }


    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                configService             : new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path]),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _)
                },
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createIndelResultFiles(indelCallingInstance)

        File fileToDelete = fileClousure(indelCallingInstance)
        assert fileToDelete.delete() || fileToDelete.deleteDir()

        when:
        job.validate(indelCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.path)
        indelCallingInstance.processingState != AnalysisProcessingStates.FINISHED

        where:
        fileClousure << [
                { IndelCallingInstance it ->
                    it.workExecutionStoreDirectory
                },
                { IndelCallingInstance it ->
                    it.workExecutionDirectories.first()
                },
                { IndelCallingInstance it ->
                    it.getCombinedPlotPath()
                },
                { IndelCallingInstance it ->
                    it.resultFilePathsToValidate.first()
                },
                { IndelCallingInstance it ->
                    it.resultFilePathsToValidate.last()
                },
                { IndelCallingInstance it ->
                    it.getIndelQcJsonFile()
                },
                { IndelCallingInstance it ->
                    it.getSampleSwapJsonFile()
                },
        ]
    }
}
