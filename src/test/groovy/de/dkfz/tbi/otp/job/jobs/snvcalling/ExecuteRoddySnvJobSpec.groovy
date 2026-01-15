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
package de.dkfz.tbi.otp.job.jobs.snvcalling

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

class ExecuteRoddySnvJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                Project,
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
                RoddyBamFile,
                SnvCallingInstance,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
        ]
    }

    @TempDir
    Path tempDir

    void "prepareAndReturnWorkflowSpecificCValues, when snvCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddySnvJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert snvCallingInstance')
    }

    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(tempDir.resolve("fasta.fa").toFile())
        File chromosomeLength = tempDir.toFile()

        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> tempDir
        }

        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService         : configService,
                individualService     : individualService,
                snvCallingService     : Spy(SnvCallingService) {
                    1 * validateInputBamFiles(_) >> { }
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * fastaFilePath(_) >> fasta
                    1 * chromosomeLengthFile(_) >> chromosomeLength
                    0 * _
                },
        ])
        job.fileSystemService = new TestFileSystemService()
        job.snvCallingService.individualService = individualService
        job.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        AbstractBamFile bamFileDisease = snvCallingInstance.sampleType1BamFile
        AbstractBamFile bamFileControl = snvCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing.path
        String bamFileControlPath = bamFileControl.pathForFurtherProcessing.path

        String analysisMethodNameOnOutput = "snv_results${File.separator}${snvCallingInstance.seqType.libraryLayoutDirName}${File.separator}" +
                "${snvCallingInstance.sampleType1BamFile.sampleType.dirName}_${snvCallingInstance.sampleType2BamFile.sampleType.dirName}" +
                "${File.separator}${snvCallingInstance.instanceName}"

        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(snvCallingInstance.referenceGenome, chromosomeNames)

        List<String> expectedList = [
                "bamfile_list:${bamFileControlPath};${bamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHROMOSOME_LENGTH_FILE:${chromosomeLength.path}",
                "CHR_SUFFIX:${snvCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${snvCallingInstance.referenceGenome.chromosomePrefix}",
                "${job.getChromosomeIndexParameterWithoutMitochondrium(snvCallingInstance.referenceGenome)}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(snvCallingInstance)

        then:
        expectedList == returnedList

        cleanup:
        configService.clean()
    }

    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddySnvJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new SnvCallingInstance(),
        ]
    }

    void "validate, when all fine, set processing state to finished"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> tempDir
        }

        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_) >> { }
                },
                snvCallingService         : Spy(SnvCallingService) {
                    1 * validateInputBamFiles(_) >> { }
                    1 * getResultRequiredForRunYapsaAndEnsureIsReadableAndNotEmpty(_) >> { }
                },
        ])
        job.snvCallingService.individualService = individualService

        job.fileSystemService = new TestFileSystemService()
        job.fileService = new FileService()

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createRoddySnvResultFiles(snvCallingInstance, individualService)

        when:
        job.validate(snvCallingInstance)

        then:
        snvCallingInstance.processingState == AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }

    void "validate, when snvCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddySnvJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input roddyResult must not be null. Expression')
    }

    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString

        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> tempDir
        }

        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createRoddySnvResultFiles(snvCallingInstance, individualService)

        when:
        job.validate(snvCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        snvCallingInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }

    @Unroll
    void "validate, when file not exists, throw assert"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> tempDir
        }

        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_) >> { }
                },
        ])

        job.fileSystemService = new TestFileSystemService()
        job.snvCallingService = new SnvCallingService()
        job.snvCallingService.individualService = individualService

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createRoddySnvResultFiles(snvCallingInstance, individualService)

        Path fileToDelete = fileClousure(snvCallingInstance, job.snvCallingService)
        new FileService().deleteDirectoryRecursively(fileToDelete)

        when:
        job.validate(snvCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.toString())
        snvCallingInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()

        where:
        fileClousure << [
                { SnvCallingInstance it, SnvCallingService service ->
                    it.workExecutionStoreDirectory.toPath()
                },
                { SnvCallingInstance it, SnvCallingService service ->
                    it.workExecutionDirectories.first().toPath()
                },
                { SnvCallingInstance it, SnvCallingService service ->
                    service.getCombinedPlotPath(it)
                },
                { SnvCallingInstance it, SnvCallingService service ->
                    service.getSnvCallingResult(it)
                },
                { SnvCallingInstance it, SnvCallingService service ->
                    service.getSnvDeepAnnotationResult(it)
                },
        ]
    }
}
