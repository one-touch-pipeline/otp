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
package de.dkfz.tbi.otp.egaSubmission

import grails.plugin.springsecurity.SpringSecurityService
import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.Delimiter
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import java.nio.file.FileSystem
import java.nio.file.Path

import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

class EgaSubmissionFileServiceSpec extends Specification implements EgaSubmissionFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractMergedBamFile,
                BamFileSubmissionObject,
                DataFile,
                DataFileSubmissionObject,
                EgaSubmission,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                Sample,
                SampleSubmissionObject,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    private final EgaSubmissionFileService egaSubmissionFileService = new EgaSubmissionFileService()

    @Rule
    TemporaryFolder temporaryFolder

    void "test generate csv content file"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = [HelperUtils.uniqueString, HelperUtils.uniqueString]
        List<EgaSubmissionService.FileType> fileTypes = [EgaSubmissionService.FileType.FASTQ, EgaSubmissionService.FileType.BAM]

        when:
        String content = egaSubmissionFileService.generateCsvFile(sampleObjectId, egaSampleAlias, fileTypes)

        then:
        content == "${INDIVIDUAL.value}," +
                "${SEQ_TYPE.value}," +
                "${SAMPLE_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${FILE_TYPE.value}\n" +
                "${sampleSubmissionObject1.sample.individual.displayName}," +
                "${sampleSubmissionObject1.seqType}," +
                "${sampleSubmissionObject1.sample.sampleType.displayName}," +
                "${egaSampleAlias[0]}," +
                "${fileTypes[0]}\n" +
                "${sampleSubmissionObject2.sample.individual.displayName}," +
                "${sampleSubmissionObject2.seqType}," +
                "${sampleSubmissionObject2.sample.sampleType.displayName}," +
                "${egaSampleAlias[1]}," +
                "${fileTypes[1]}\n"
    }

    void "test read from file"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = [HelperUtils.uniqueString, HelperUtils.uniqueString]
        List<EgaSubmissionService.FileType> fileTypes = [EgaSubmissionService.FileType.FASTQ, EgaSubmissionService.FileType.BAM]
        String content = egaSubmissionFileService.generateCsvFile(sampleObjectId, egaSampleAlias, fileTypes)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)

        when:
        Map egaSampleAliases = egaSubmissionFileService.readEgaSampleAliasesFromFile(spreadsheet)
        Map fastqs = egaSubmissionFileService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.FASTQ)
        Map bams = egaSubmissionFileService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.BAM)

        then:
        egaSampleAliases.size() == 2
        fastqs*.value == [true, false]
        bams*.value == [false, true]
    }

    void "test generate data files csv file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createEgaSubmission()
        DataFile dataFile = DomainFactory.createDataFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: dataFile.seqTrack.sample,
                seqType: dataFile.seqType,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject(
                dataFile: dataFile,
                sampleSubmissionObject: sampleSubmissionObject
        )
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)
        String dataFileAlias = egaSubmissionFileService.egaSubmissionService.generateDefaultEgaAliasesForDataFiles([
                new DataFileAndSampleAlias(
                        dataFile,
                        sampleSubmissionObject,
                ),
        ]).get(dataFile.fileName + dataFile.run)

        when:
        String content = egaSubmissionFileService.generateDataFilesCsvFile(submission)

        then:
        content == "${INDIVIDUAL.value}," +
                "${SEQ_TYPE.value}," +
                "${SAMPLE_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${SEQ_CENTER.value}," +
                "${RUN.value}," +
                "${LANE.value}," +
                "${LIBRARY.value}," +
                "${ILSE.value}," +
                "${EGA_FILE_ALIAS.value}," +
                "${FILENAME.value}\n" +
                "${dataFileSubmissionObject.dataFile.individual.displayName}," +
                "${dataFileSubmissionObject.dataFile.seqType}," +
                "${dataFileSubmissionObject.dataFile.sampleType}," +
                "${dataFileSubmissionObject.sampleSubmissionObject.egaAliasName}," +
                "${dataFileSubmissionObject.dataFile.run.seqCenter}," +
                "${dataFileSubmissionObject.dataFile.run}," +
                "${dataFileSubmissionObject.dataFile.seqTrack.laneId}," +
                "${dataFileSubmissionObject.dataFile.seqTrack.normalizedLibraryName ?: "N/A"}," +
                "${dataFileSubmissionObject.dataFile.seqTrack.ilseId ?: "N/A"}," +
                "${dataFileAlias}," +
                "${dataFileSubmissionObject.dataFile.fileName}\n"
    }

    void "test generate bam files csv file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createEgaSubmission()
        RoddyBamFile roddyBamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: roddyBamFile.sample,
                seqType: roddyBamFile.seqType,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject(
                bamFile: roddyBamFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)
        String bamFileAlias = egaSubmissionFileService.egaSubmissionService.generateDefaultEgaAliasesForBamFiles([
                new BamFileAndSampleAlias(
                        roddyBamFile,
                        sampleSubmissionObject,
                ),
        ]).get(roddyBamFile.bamFileName + sampleSubmissionObject.egaAliasName)

        when:
        String content = egaSubmissionFileService.generateBamFilesCsvFile(submission)

        then:
        content == "${INDIVIDUAL.value}," +
                "${SEQ_TYPE.value}," +
                "${SAMPLE_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${EGA_FILE_ALIAS.value}," +
                "${FILENAME.value}\n" +
                "${bamFileSubmissionObject.bamFile.individual.displayName}," +
                "${bamFileSubmissionObject.bamFile.seqType}," +
                "${bamFileSubmissionObject.bamFile.sampleType}," +
                "${bamFileSubmissionObject.sampleSubmissionObject.egaAliasName}," +
                "${bamFileAlias}," +
                "${bamFileSubmissionObject.bamFile.bamFileName}\n"
    }

    void "test read ega file aliases from file with data file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createEgaSubmission()
        DataFile dataFile = createDataFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: dataFile.seqTrack.sample,
                seqType: dataFile.seqType,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject(
                dataFile: dataFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)
        String content = egaSubmissionFileService.generateDataFilesCsvFile(submission)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)
        String dataFileAlias = egaSubmissionFileService.egaSubmissionService.generateDefaultEgaAliasesForDataFiles([
                new DataFileAndSampleAlias(
                        dataFile,
                        sampleSubmissionObject,
                ),
        ]).get(dataFile.fileName + dataFile.run)

        when:
        Map fileAliases = egaSubmissionFileService.readEgaFileAliasesFromFile(spreadsheet, false)

        then:
        fileAliases.get(dataFileSubmissionObject.dataFile.fileName + dataFileSubmissionObject.dataFile.run) == dataFileAlias
    }

    void "test read ega file aliases from file with bam file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createEgaSubmission()
        RoddyBamFile roddyBamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: roddyBamFile.sample,
                seqType: roddyBamFile.seqType,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject(
                bamFile: roddyBamFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)
        String content = egaSubmissionFileService.generateBamFilesCsvFile(submission)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)
        String bamFileAlias = egaSubmissionFileService.egaSubmissionService.generateDefaultEgaAliasesForBamFiles([
                new BamFileAndSampleAlias(
                        roddyBamFile,
                        sampleSubmissionObject
                ),
        ]).get(roddyBamFile.bamFileName + sampleSubmissionObject.egaAliasName)

        when:
        Map fileAliases = egaSubmissionFileService.readEgaFileAliasesFromFile(spreadsheet, true)

        then:
        fileAliases.get(bamFileSubmissionObject.bamFile.bamFileName + bamFileSubmissionObject.sampleSubmissionObject.egaAliasName) == bamFileAlias
    }

    void "createFilesForUpload, when submission is given, then create all expected files"() {
        given:
        new TestConfigService()

        Path basePath = temporaryFolder.newFolder().toPath()
        EgaSubmission egaSubmission = createEgaSubmission()

        egaSubmissionFileService.egaFileContentService = Mock(EgaFileContentService) {
            1 * createFilesToUploadFileContent(egaSubmission) >> [
                    mapping: 'mappingContent',
            ]
            1 * createSingleFastqFileMapping(egaSubmission) >> [
                    fastqSingle1: 'contentFastqSingle1',
                    fastqSingle2: 'contentFastqSingle2',
            ]
            1 * createPairedFastqFileMapping(egaSubmission) >> [
                    fastqPaired1: 'contentFastqPaired1',
                    fastqPaired2: 'contentFastqPaired2',
            ]
            1 * createBamFileMapping(egaSubmission) >> [
                    bam1: 'contentBam1',
                    bam2: 'contentBam2',
            ]
            0 * _
        }
        egaSubmissionFileService.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem(_) >> Mock(FileSystem) {
                1 * getPath(*_) >> basePath
            }
        }
        egaSubmissionFileService.fileService = Mock(FileService) {
            1 * createFileWithContent(basePath.resolve('mapping'), 'mappingContent', _, _)
            1 * createFileWithContent(basePath.resolve('fastqSingle1'), 'contentFastqSingle1', _, _)
            1 * createFileWithContent(basePath.resolve('fastqSingle2'), 'contentFastqSingle2', _, _)
            1 * createFileWithContent(basePath.resolve('fastqPaired1'), 'contentFastqPaired1', _, _)
            1 * createFileWithContent(basePath.resolve('fastqPaired2'), 'contentFastqPaired2', _, _)
            1 * createFileWithContent(basePath.resolve('bam1'), 'contentBam1', _, _)
            1 * createFileWithContent(basePath.resolve('bam2'), 'contentBam2', _, _)
        }

        when:
        egaSubmissionFileService.createFilesForUpload(egaSubmission)

        then:
        noExceptionThrown()
    }

    @SuppressWarnings('UnnecessaryObjectReferences')
    void "sendEmail, when submission is given, then send email"() {
        given:
        new TestConfigService()

        Path basePath = temporaryFolder.newFolder().toPath()
        EgaSubmission egaSubmission = createEgaSubmission()

        String emailSubject = "New ${egaSubmission}"
        String content = "some content"
        String recipient = "a.b@c.de"
        User user = new User(
                realName: "Real Name",
                email: "ua.ub@uc.ude",
        )

        egaSubmissionFileService.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem(_) >> Mock(FileSystem) {
                1 * getPath(*_) >> basePath
            }
        }
        egaSubmissionFileService.springSecurityService = Mock(SpringSecurityService) {
            1 * getCurrentUser() >> user
        }
        egaSubmissionFileService.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsString(_) >> recipient
        }
        egaSubmissionFileService.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage(_, _) >> content
        }
        egaSubmissionFileService.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(emailSubject, content, [recipient], user.email)
        }

        when:
        egaSubmissionFileService.sendEmail(egaSubmission)

        then:
        noExceptionThrown()
    }

    @SuppressWarnings('UnnecessaryObjectReferences')
    void "prepareSubmissionForUpload, when submission is given, then files are created, email is send state is changed to FILE_UPLOAD_STARTED"() {
        given:
        new TestConfigService()

        Path basePath = temporaryFolder.newFolder().toPath()
        EgaSubmission egaSubmission = createEgaSubmission([
                samplesToSubmit : [createSampleSubmissionObject()] as Set,
                bamFilesToSubmit: [createBamFileSubmissionObject()] as Set,
        ])

        String emailSubject = "New ${egaSubmission}"
        String content = "some content"
        String recipient = "a.b@c.de"
        User user = new User(
                realName: "Real Name",
                email: "ua.ub@uc.ude",
        )

        egaSubmissionFileService.fileSystemService = Mock(FileSystemService) {
            2 * getRemoteFileSystem(_) >> Mock(FileSystem) {
                2 * getPath(*_) >> basePath
            }
        }
        egaSubmissionFileService.egaFileContentService = Mock(EgaFileContentService) {
            1 * createFilesToUploadFileContent(egaSubmission) >> [:]
            1 * createSingleFastqFileMapping(egaSubmission) >> [:]
            1 * createPairedFastqFileMapping(egaSubmission) >> [:]
            1 * createBamFileMapping(egaSubmission) >> [:]
            0 * _
        }
        egaSubmissionFileService.fileService = Mock(FileService) {
            _ * createFileWithContent(_, _, _)
        }
        egaSubmissionFileService.springSecurityService = Mock(SpringSecurityService) {
            1 * getCurrentUser() >> user
        }
        egaSubmissionFileService.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsString(_) >> recipient
        }
        egaSubmissionFileService.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage(_, _) >> content
        }
        egaSubmissionFileService.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(emailSubject, content, [recipient], user.email)
        }

        when:
        egaSubmissionFileService.prepareSubmissionForUpload(egaSubmission)

        then:
        egaSubmission.state == EgaSubmission.State.FILE_UPLOAD_STARTED
    }
}
