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
package de.dkfz.tbi.otp.egaSubmission

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.util.spreadsheet.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

@Transactional
class EgaSubmissionFileService {

    CreateNotificationTextService createNotificationTextService
    EgaSubmissionService egaSubmissionService
    EgaFileContentService egaFileContentService
    FileService fileService
    FileSystemService fileSystemService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    ProjectService projectService
    SecurityService securityService

    enum EgaColumnName {
        INDIVIDUAL("Individual"),
        INDIVIDUAL_UUID("Patient UUID"),
        SAMPLE_TYPE("Sample Type"),
        SEQ_TYPE_NAME("Sequence Type Name"),
        SEQUENCING_READ_TYPE("Sequencing Read Type"),
        SINGLE_CELL("Single Cell"),
        EGA_SAMPLE_ALIAS("Sample Name at EGA"),
        FILE_TYPE("File Type"),
        SEQ_CENTER("Seq Center"),
        RUN("Run"),
        LANE("Lane"),
        LIBRARY("Library"),
        ILSE("Ilse"),
        EGA_FILE_ALIAS("Filename at EGA"),
        FILENAME("Filename in OTP"),

        TITLE("title"),
        ALIAS("alias"),
        DESCRIPTION("description"),
        SUBJECT_ID("subjectId"),
        BIO_SAMPLE_ID("bioSampleId"),
        CASE_OR_CONTROL("caseOrControl"),
        GENDER("gender"),
        ORGANISM_PART("organismPart"),
        ORGANISM("organism"),
        CELL_LINE("cellLine"),
        REGION("region"),
        PHENOTYPE("phenotype"),

        final String value

        EgaColumnName(String value) {
            this.value = value
        }
    }

    Spreadsheet createSpreadsheetFromFileByteArray(byte[] fileByteArray) {
        String content = new String(fileByteArray)
        content = content.replace("\"", "")
        return new Spreadsheet(content, Delimiter.COMMA)
    }

    Map<EgaMapKey, String> readEgaSampleAliasesFromFile(Spreadsheet spreadsheet) {
        Map<EgaMapKey, String> egaSampleAliases = [:]

        spreadsheet.dataRows.each {
            egaSampleAliases.put(getIdentifierKey(it), it.getCellByColumnTitle(EGA_SAMPLE_ALIAS.value).text)
        }

        return egaSampleAliases
    }

    Map<String, String> readEgaFileAliasesFromFile(Spreadsheet spreadsheet, boolean isBam) {
        Map<String, String> egaAliases = [:]

        spreadsheet.dataRows.each {
            if (isBam) {
                egaAliases.put(
                        it.getCellByColumnTitle(FILENAME.value).text + it.getCellByColumnTitle(EGA_SAMPLE_ALIAS.value).text,
                        it.getCellByColumnTitle(EGA_FILE_ALIAS.value).text
                )
            } else {
                egaAliases.put(
                        it.getCellByColumnTitle(FILENAME.value).text + it.getCellByColumnTitle(RUN.value).text,
                        it.getCellByColumnTitle(EGA_FILE_ALIAS.value).text)
            }
        }

        return egaAliases
    }

    String generateSampleSelectionCsvFile(List<EgaMapKey> egaMapKeys) {
        StringBuilder contentBody = new StringBuilder()

        egaMapKeys.each { it ->
            contentBody.append([
                    it.individualName,
                    it.individualUuid,
                    it.seqTypeName,
                    it.sequencingReadType,
                    it.singleCell,
                    it.sampleTypeName,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                EgaColumnName.INDIVIDUAL_UUID,
                SEQ_TYPE_NAME,
                SEQUENCING_READ_TYPE,
                SINGLE_CELL,
                SAMPLE_TYPE,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    String generateSampleInformationCsvFile(List<String> sampleObjectId, List<String> alias) {
        StringBuilder contentBody = new StringBuilder()

        sampleObjectId.eachWithIndex { it, i ->
            SampleSubmissionObject sampleSubmissionObject = SampleSubmissionObject.get(it as Long)

            contentBody.append([
                    sampleSubmissionObject.sample.individual.displayName,
                    sampleSubmissionObject.sample.individual.uuid,
                    sampleSubmissionObject.seqType.displayName,
                    sampleSubmissionObject.seqType.libraryLayout,
                    sampleSubmissionObject.seqType.singleCellDisplayName,
                    sampleSubmissionObject.sample.sampleType.displayName,
                    alias?.getAt(i) ?: "",
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                EgaColumnName.INDIVIDUAL_UUID,
                SEQ_TYPE_NAME,
                SEQUENCING_READ_TYPE,
                SINGLE_CELL,
                SAMPLE_TYPE,
                EGA_SAMPLE_ALIAS,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    String generateRawSequenceFilesCsvFile(EgaSubmission submission) {
        StringBuilder contentBody = new StringBuilder()

        List<RawSequenceFileAndSampleAlias> dataFilesAndSampleAliases = egaSubmissionService.getRawSequenceFilesAndAlias(submission)
        Map dataFileFileAliases = egaSubmissionService.generateDefaultEgaAliasesForRawSequenceFiles(dataFilesAndSampleAliases)

        dataFilesAndSampleAliases.each {
            contentBody.append([
                    it.rawSequenceFile.individual.displayName,
                    it.rawSequenceFile.individual.uuid,
                    it.rawSequenceFile.seqType.displayName,
                    it.rawSequenceFile.seqType.libraryLayout,
                    it.rawSequenceFile.seqType.singleCellDisplayName,
                    it.rawSequenceFile.sampleType.displayName,
                    it.sampleSubmissionObject.egaAliasName,
                    it.rawSequenceFile.run.seqCenter,
                    it.rawSequenceFile.run,
                    it.rawSequenceFile.seqTrack.laneId,
                    it.rawSequenceFile.seqTrack.normalizedLibraryName ?: "N/A",
                    it.rawSequenceFile.seqTrack.ilseId ?: "N/A",
                    dataFileFileAliases.get(it.rawSequenceFile.fileName + it.rawSequenceFile.run),
                    it.rawSequenceFile.fileName,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                INDIVIDUAL_UUID,
                SEQ_TYPE_NAME,
                SEQUENCING_READ_TYPE,
                SINGLE_CELL,
                SAMPLE_TYPE,
                EGA_SAMPLE_ALIAS,
                SEQ_CENTER,
                RUN,
                LANE,
                LIBRARY,
                ILSE,
                EGA_FILE_ALIAS,
                FILENAME,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    String generateBamFilesCsvFile(EgaSubmission submission) {
        StringBuilder contentBody = new StringBuilder()

        List<BamFileAndSampleAlias> bamFilesAndSampleAliases = egaSubmissionService.getBamFilesAndAlias(submission)
        Map bamFileFileAliases = egaSubmissionService.generateDefaultEgaAliasesForBamFiles(bamFilesAndSampleAliases)

        bamFilesAndSampleAliases.each {
            contentBody.append([
                    it.bamFile.individual.displayName,
                    it.bamFile.individual.uuid,
                    it.bamFile.seqType.displayName,
                    it.bamFile.seqType.libraryLayout,
                    it.bamFile.seqType.singleCellDisplayName,
                    it.bamFile.sampleType.displayName,
                    it.sampleSubmissionObject.egaAliasName,
                    bamFileFileAliases.get(it.bamFile.bamFileName + it.sampleSubmissionObject.egaAliasName),
                    it.bamFile.bamFileName,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                INDIVIDUAL_UUID,
                SEQ_TYPE_NAME,
                SEQUENCING_READ_TYPE,
                SINGLE_CELL,
                SAMPLE_TYPE,
                EGA_SAMPLE_ALIAS,
                EGA_FILE_ALIAS,
                FILENAME,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    private Path createPathForSubmission(EgaSubmission submission) {
        return projectService.getProjectDirectory(submission.project).resolve('submission').resolve(submission.id.toString())
    }

    void createFilesForUpload(EgaSubmission submission) {
        Path basePath = createPathForSubmission(submission)

        Map<String, String> filesToCreate = [:]
        filesToCreate << egaFileContentService.createFilesToUploadFileContent(submission)
        filesToCreate << egaFileContentService.createSingleFastqFileMapping(submission)
        filesToCreate << egaFileContentService.createPairedFastqFileMapping(submission)
        filesToCreate << egaFileContentService.createBamFileMapping(submission)

        filesToCreate.each {
            Path path = basePath.resolve(it.key)
            Files.deleteIfExists(path)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent,
                    "", FileService.OWNER_DIRECTORY_PERMISSION_STRING)
            fileService.createFileWithContent(path, it.value, [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE] as Set<PosixFilePermission>)
        }
    }

    void sendEmail(EgaSubmission submission) {
        Path basePath = createPathForSubmission(submission)
        User user = securityService.currentUser

        String subject = "New ${submission}"
        String content = messageSourceService.createMessage('egaSubmission.template.base', [
                user         : mailHelperService.senderName,
                project      : submission.project.name,
                submission   : submission.id,
                numberOfFiles: submission.rawSequenceFilesToSubmit.size() + submission.bamFilesToSubmit.size(),
                path         : basePath,
        ])
        mailHelperService.sendEmailToTicketSystem(subject, content, user.email)
    }

    void prepareSubmissionForUpload(EgaSubmission submission) {
        createFilesForUpload(submission)
        sendEmail(submission)
        submission.state = EgaSubmission.State.FILE_UPLOAD_STARTED
        submission.save(flush: true)
    }

    static EgaMapKey getIdentifierKey(Row row) {
        return new EgaMapKey(row)
    }
}
