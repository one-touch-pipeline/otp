/*
 * Copyright 2011-2023 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.SecurityService
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
        return [
                AbstractBamFile,
                BamFileSubmissionObject,
                RawSequenceFile,
                RawSequenceFileSubmissionObject,
                EgaSubmission,
                FastqFile,
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

    @TempDir
    Path tempDir

    void "test generate csv content file"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = [HelperUtils.uniqueString, HelperUtils.uniqueString]

        when:
        String content = egaSubmissionFileService.generateSampleInformationCsvFile(sampleObjectId, egaSampleAlias)

        then:
        content == "${INDIVIDUAL.value}," +
                "${INDIVIDUAL_UUID.value}," +
                "${SEQ_TYPE_NAME.value}," +
                "${SEQUENCING_READ_TYPE.value}," +
                "${SINGLE_CELL.value}," +
                "${SAMPLE_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}\n" +
                "${sampleSubmissionObject1.sample.individual.displayName}," +
                "${sampleSubmissionObject1.sample.individual.uuid}," +
                "${sampleSubmissionObject1.seqType.displayName}," +
                "${sampleSubmissionObject1.seqType.libraryLayout}," +
                "${sampleSubmissionObject1.seqType.singleCellDisplayName}," +
                "${sampleSubmissionObject1.sample.sampleType.displayName}," +
                "${egaSampleAlias[0]}\n" +
                "${sampleSubmissionObject2.sample.individual.displayName}," +
                "${sampleSubmissionObject2.sample.individual.uuid}," +
                "${sampleSubmissionObject2.seqType.displayName}," +
                "${sampleSubmissionObject2.seqType.libraryLayout}," +
                "${sampleSubmissionObject2.seqType.singleCellDisplayName}," +
                "${sampleSubmissionObject2.sample.sampleType.displayName}," +
                "${egaSampleAlias[1]}\n"
    }

    void "test read from file"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = [HelperUtils.uniqueString, HelperUtils.uniqueString]
        String content = egaSubmissionFileService.generateSampleInformationCsvFile(sampleObjectId, egaSampleAlias)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)

        when:
        Map egaSampleAliases = egaSubmissionFileService.readEgaSampleAliasesFromFile(spreadsheet)

        then:
        egaSampleAliases.size() == 2
    }

    void "test generate data files csv file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createEgaSubmission()
        RawSequenceFile rawSequenceFile = DomainFactory.createFastqFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: rawSequenceFile.seqTrack.sample,
                seqType: rawSequenceFile.seqType,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        RawSequenceFileSubmissionObject submissionObject = createRawSequenceFileSubmissionObject(
                sequenceFile: rawSequenceFile,
                sampleSubmissionObject: sampleSubmissionObject
        )
        submission.addToRawSequenceFilesToSubmit(submissionObject)
        String rawSequenceFileAlias = egaSubmissionFileService.egaSubmissionService.generateDefaultEgaAliasesForRawSequenceFiles([
                new RawSequenceFileAndSampleAlias(
                        rawSequenceFile,
                        sampleSubmissionObject,
                ),
        ]).get(rawSequenceFile.fileName + rawSequenceFile.run)

        when:
        String content = egaSubmissionFileService.generateRawSequenceFilesCsvFile(submission)

        then:
        content == "${INDIVIDUAL.value}," +
                "${INDIVIDUAL_UUID.value}," +
                "${SEQ_TYPE_NAME.value}," +
                "${SEQUENCING_READ_TYPE.value}," +
                "${SINGLE_CELL.value}," +
                "${SAMPLE_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${SEQ_CENTER.value}," +
                "${RUN.value}," +
                "${LANE.value}," +
                "${LIBRARY.value}," +
                "${ILSE.value}," +
                "${EGA_FILE_ALIAS.value}," +
                "${FILENAME.value}\n" +
                "${submissionObject.sequenceFile.individual.displayName}," +
                "${submissionObject.sequenceFile.individual.uuid}," +
                "${submissionObject.sequenceFile.seqType.displayName}," +
                "${submissionObject.sequenceFile.seqType.libraryLayout}," +
                "${submissionObject.sequenceFile.seqType.singleCellDisplayName}," +
                "${submissionObject.sequenceFile.sampleType}," +
                "${submissionObject.sampleSubmissionObject.egaAliasName}," +
                "${submissionObject.sequenceFile.run.seqCenter}," +
                "${submissionObject.sequenceFile.run}," +
                "${submissionObject.sequenceFile.seqTrack.laneId}," +
                "${submissionObject.sequenceFile.seqTrack.normalizedLibraryName ?: "N/A"}," +
                "${submissionObject.sequenceFile.seqTrack.ilseId ?: "N/A"}," +
                "${rawSequenceFileAlias}," +
                "${submissionObject.sequenceFile.fileName}\n"
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
                "${INDIVIDUAL_UUID.value}," +
                "${SEQ_TYPE_NAME.value}," +
                "${SEQUENCING_READ_TYPE.value}," +
                "${SINGLE_CELL.value}," +
                "${SAMPLE_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${EGA_FILE_ALIAS.value}," +
                "${FILENAME.value}\n" +
                "${bamFileSubmissionObject.bamFile.individual.displayName}," +
                "${bamFileSubmissionObject.bamFile.individual.uuid}," +
                "${bamFileSubmissionObject.bamFile.seqType.displayName}," +
                "${bamFileSubmissionObject.bamFile.seqType.libraryLayout}," +
                "${bamFileSubmissionObject.bamFile.seqType.singleCellDisplayName}," +
                "${bamFileSubmissionObject.bamFile.sampleType}," +
                "${bamFileSubmissionObject.sampleSubmissionObject.egaAliasName}," +
                "${bamFileAlias}," +
                "${bamFileSubmissionObject.bamFile.bamFileName}\n"
    }

    void "test read ega file aliases from file with data file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createEgaSubmission()
        RawSequenceFile rawSequenceFile = createFastqFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: rawSequenceFile.seqTrack.sample,
                seqType: rawSequenceFile.seqType,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        RawSequenceFileSubmissionObject submissionObject = createRawSequenceFileSubmissionObject(
                sequenceFile: rawSequenceFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToRawSequenceFilesToSubmit(submissionObject)
        String content = egaSubmissionFileService.generateRawSequenceFilesCsvFile(submission)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)
        String rawSequenceFileAlias = egaSubmissionFileService.egaSubmissionService.generateDefaultEgaAliasesForRawSequenceFiles([
                new RawSequenceFileAndSampleAlias(
                        rawSequenceFile,
                        sampleSubmissionObject,
                ),
        ]).get(rawSequenceFile.fileName + rawSequenceFile.run)

        when:
        Map fileAliases = egaSubmissionFileService.readEgaFileAliasesFromFile(spreadsheet, false)

        then:
        fileAliases.get(submissionObject.sequenceFile.fileName + submissionObject.sequenceFile.run) == rawSequenceFileAlias
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
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])

        egaSubmissionFileService.projectService = new ProjectService()
        egaSubmissionFileService.projectService.configService = configService
        egaSubmissionFileService.projectService.fileSystemService = new TestFileSystemService()

        EgaSubmission egaSubmission = createEgaSubmission()

        Path basePath = egaSubmissionFileService.projectService.getProjectDirectory(egaSubmission.project).resolve('submission')
                .resolve(egaSubmission.id.toString())

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
        TestConfigService configService = new TestConfigService()

        EgaSubmission egaSubmission = createEgaSubmission()

        String emailSubject = "New ${egaSubmission}"
        String content = "some content"
        User user = new User(
                realName: "Real Name",
                email: "ua.ub@uc.ude",
        )

        egaSubmissionFileService.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem(_) >> Mock(FileSystem) {
                1 * getPath(*_) >> tempDir
            }
        }
        egaSubmissionFileService.securityService = Mock(SecurityService) {
            1 * getCurrentUser() >> user
        }
        egaSubmissionFileService.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage(_, _) >> content
        }
        egaSubmissionFileService.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(emailSubject, content, user.email)
        }
        egaSubmissionFileService.projectService = new ProjectService()
        egaSubmissionFileService.projectService.configService = configService
        egaSubmissionFileService.projectService.fileSystemService = egaSubmissionFileService.fileSystemService

        when:
        egaSubmissionFileService.sendEmail(egaSubmission)

        then:
        noExceptionThrown()
    }

    @SuppressWarnings('UnnecessaryObjectReferences')
    void "prepareSubmissionForUpload, when submission is given, then files are created, email is send state is changed to FILE_UPLOAD_STARTED"() {
        given:
        TestConfigService configService = new TestConfigService()

        EgaSubmission egaSubmission = createEgaSubmission([
                samplesToSubmit : [createSampleSubmissionObject()] as Set,
                bamFilesToSubmit: [createBamFileSubmissionObject()] as Set,
        ])

        String emailSubject = "New ${egaSubmission}"
        String content = "some content"
        User user = new User(
                realName: "Real Name",
                email: "ua.ub@uc.ude",
        )

        egaSubmissionFileService.fileSystemService = Mock(FileSystemService) {
            2 * getRemoteFileSystem(_) >> Mock(FileSystem) {
                2 * getPath(*_) >> tempDir
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
        egaSubmissionFileService.securityService = Mock(SecurityService) {
            1 * getCurrentUser() >> user
        }
        egaSubmissionFileService.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage(_, _) >> content
        }
        egaSubmissionFileService.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(emailSubject, content, user.email)
        }
        egaSubmissionFileService.projectService = new ProjectService()
        egaSubmissionFileService.projectService.configService = configService
        egaSubmissionFileService.projectService.fileSystemService = egaSubmissionFileService.fileSystemService

        when:
        egaSubmissionFileService.prepareSubmissionForUpload(egaSubmission)

        then:
        egaSubmission.state == EgaSubmission.State.FILE_UPLOAD_STARTED
    }
}
