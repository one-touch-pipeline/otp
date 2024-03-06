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
package de.dkfz.tbi.otp.job.jobs.aceseq

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

import java.nio.file.*

class ExecuteRoddyAceseqJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                AceseqInstance,
                AceseqQc,
                RawSequenceFile,
                FastqFile,
                FileType,
                IndelCallingInstance,
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
                SophiaInstance,
                ReferenceGenome,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
        ]
    }

    @TempDir
    Path tempDir

    AceseqInstance aceseqInstance

    TestConfigService configService
    IndividualService individualService
    FileService fileService

    void setup() {
        aceseqInstance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()
        aceseqInstance.samplePair.mergingWorkPackage1.bamFileInProjectFolder = aceseqInstance.sampleType1BamFile
        assert aceseqInstance.samplePair.mergingWorkPackage1.save(flush: true)
        aceseqInstance.samplePair.mergingWorkPackage2.bamFileInProjectFolder = aceseqInstance.sampleType2BamFile
        assert aceseqInstance.samplePair.mergingWorkPackage2.save(flush: true)

        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])
        individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> tempDir
        }
    }

    void cleanup() {
        configService?.clean()
    }

    void "prepareAndReturnWorkflowSpecificCValues, when aceseqInstance is null, throw assert"() {
        when:
        new ExecuteRoddyAceseqJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert aceseqInstance')
    }

    @SuppressWarnings('NoFilesReadableRule')
    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(tempDir.resolve("fasta.fa").toFile())
        File chromosomeLength = CreateFileHelper.createFile(tempDir.resolve("chrTotalLength.tsv").toFile())
        File gcContent = CreateFileHelper.createFile(tempDir.resolve("gcContentFile.tsv").toFile())

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstance(aceseqInstance.samplePair)
        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance, individualService)

        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService         : configService,
                fileService           : Mock(FileService) {
                    toFile(_) >> { new File() }
                    ensureFileIsReadableAndNotEmptyStatic(_) >> true
                },
                aceseqService         : Mock(AceseqService) {
                    1 * validateInputBamFiles(_) >> { }
                    getWorkDirectory(_) >> { Paths.get("/asdf") }
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * checkReferenceGenomeFilesAvailability(_) >> { }
                    1 * fastaFilePath(_) >> fasta
                    1 * chromosomeLengthFile(_) >> chromosomeLength
                    1 * gcContentFile(_) >> gcContent
                },
        ])
        job.sophiaService = new SophiaService()
        job.sophiaService.individualService = individualService

        ReferenceGenome referenceGenome = DomainFactory.createAceseqReferenceGenome()

        RoddyBamFile bamFileDisease = aceseqInstance.sampleType1BamFile as RoddyBamFile
        RoddyBamFile bamFileControl = aceseqInstance.sampleType2BamFile as RoddyBamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        bamFileDisease.mergingWorkPackage.referenceGenome = referenceGenome
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        bamFileControl.mergingWorkPackage.referenceGenome = referenceGenome
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing.path
        String bamFileControlPath = bamFileControl.pathForFurtherProcessing.path

        List<String> expectedList = [
                "bamfile_list:${bamFileControlPath};${bamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta}",
                "CHROMOSOME_LENGTH_FILE:${chromosomeLength}",
                "CHR_SUFFIX:${referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${referenceGenome.chromosomePrefix}",
                "aceseqOutputDirectory:${job.aceseqService.getWorkDirectory(aceseqInstance)}",
                "svOutputDirectory:${job.aceseqService.getWorkDirectory(aceseqInstance)}",
                "MAPPABILITY_FILE:${referenceGenome.mappabilityFile}",
                "REPLICATION_TIME_FILE:${referenceGenome.replicationTimeFile}",
                "GC_CONTENT_FILE:${gcContent}",
                "GENETIC_MAP_FILE:${referenceGenome.geneticMapFile}",
                "KNOWN_HAPLOTYPES_FILE:${referenceGenome.knownHaplotypesFile}",
                "KNOWN_HAPLOTYPES_LEGEND_FILE:${referenceGenome.knownHaplotypesLegendFile}",
                "GENETIC_MAP_FILE_X:${referenceGenome.geneticMapFileX}",
                "KNOWN_HAPLOTYPES_FILE_X:${referenceGenome.knownHaplotypesFileX}",
                "KNOWN_HAPLOTYPES_LEGEND_FILE_X:${referenceGenome.knownHaplotypesLegendFileX}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(aceseqInstance)

        then:
        expectedList == returnedList
        Path file = job.sophiaService.getFinalAceseqInputFile(sophiaInstance)
        Files.size(file) > 0L
        file.absolute
        Files.isRegularFile(file)
        Files.isReadable(file)
    }

    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddyAceseqJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new AceseqInstance(),
        ]
    }

    void "validate, when all fine, set processing state to finished"() {
        given:
        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_) >> { }
                },
                aceseqService             : Mock(AceseqService) {
                    1 * validateInputBamFiles(_) >> { }
                },
        ])

        DomainFactory.createAceseqQc([:], [:], [:], aceseqInstance)

        CreateRoddyFileHelper.createAceseqResultFiles(aceseqInstance, individualService)

        when:
        job.validate(aceseqInstance)

        then:
        aceseqInstance.processingState == AnalysisProcessingStates.FINISHED
    }

    void "validate, when aceseqInstance is null, throw assert"() {
        when:
        new ExecuteRoddyAceseqJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input aceseqInstance must not be null. Expression')
    }

    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString
        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])

        DomainFactory.createAceseqQc([:], [:], [:], aceseqInstance)

        CreateRoddyFileHelper.createAceseqResultFiles(aceseqInstance, individualService)

        when:
        job.validate(aceseqInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        aceseqInstance.processingState != AnalysisProcessingStates.FINISHED
    }

    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_) >> { }
                },
        ])

        DomainFactory.createAceseqQc([:], [:], [:], aceseqInstance)

        CreateRoddyFileHelper.createAceseqResultFiles(aceseqInstance, individualService)

        File fileToDelete = fileClousure(aceseqInstance)
        assert fileToDelete.delete() || fileToDelete.deleteDir()

        when:
        job.validate(aceseqInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.path)
        aceseqInstance.processingState != AnalysisProcessingStates.FINISHED

        where:
        fileClousure << [
                { AceseqInstance it ->
                    it.workExecutionStoreDirectory
                },
                { AceseqInstance it ->
                    it.workExecutionDirectories.first()
                },
        ]
    }
}
