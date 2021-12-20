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

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

@CompileStatic
@Transactional
class EgaSubmissionFileService {

    CreateNotificationTextService createNotificationTextService
    EgaSubmissionService egaSubmissionService
    EgaFileContentService egaFileContentService
    FileService fileService
    FileSystemService fileSystemService
    LsdfFilesService lsdfFilesService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    ProjectService projectService
    SpringSecurityService springSecurityService

    enum EgaColumnName {
        INDIVIDUAL("Individual"),
        SAMPLE_TYPE("Sample Type"),
        SEQ_TYPE("Sequence Type"),
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

    String generateCsvFile(List<String> sampleObjectId, List<String> alias) {
        StringBuilder contentBody = new StringBuilder()

        sampleObjectId.eachWithIndex { it, i ->
            SampleSubmissionObject sampleSubmissionObject = SampleSubmissionObject.get(it as Long)

            contentBody.append([
                    sampleSubmissionObject.sample.individual.displayName,
                    sampleSubmissionObject.seqType.toString(),
                    sampleSubmissionObject.sample.sampleType.displayName,
                    alias?.getAt(i) ?: "",
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                SEQ_TYPE,
                SAMPLE_TYPE,
                EGA_SAMPLE_ALIAS,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    String generateDataFilesCsvFile(EgaSubmission submission) {
        StringBuilder contentBody = new StringBuilder()

        List<DataFileAndSampleAlias> dataFilesAndSampleAliases = egaSubmissionService.getDataFilesAndAlias(submission)
        Map dataFileFileAliases = egaSubmissionService.generateDefaultEgaAliasesForDataFiles(dataFilesAndSampleAliases)

        dataFilesAndSampleAliases.each {
            contentBody.append([
                    it.dataFile.individual.displayName,
                    it.dataFile.seqType.toString(),
                    it.dataFile.sampleType.displayName,
                    it.sampleSubmissionObject.egaAliasName,
                    it.dataFile.run.seqCenter,
                    it.dataFile.run,
                    it.dataFile.seqTrack.laneId,
                    it.dataFile.seqTrack.normalizedLibraryName ?: "N/A",
                    it.dataFile.seqTrack.ilseId ?: "N/A",
                    dataFileFileAliases.get(it.dataFile.fileName + it.dataFile.run),
                    it.dataFile.fileName,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                SEQ_TYPE,
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
                    it.bamFile.seqType.toString(),
                    it.bamFile.sampleType.displayName,
                    it.sampleSubmissionObject.egaAliasName,
                    bamFileFileAliases.get(it.bamFile.bamFileName + it.sampleSubmissionObject.egaAliasName),
                    it.bamFile.bamFileName,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                SEQ_TYPE,
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
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent, submission.project.realm,
                    "", FileService.OWNER_DIRECTORY_PERMISSION_STRING)
            fileService.createFileWithContent(path, it.value, submission.project.realm,
                    [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE] as Set<PosixFilePermission>)
        }
    }

    void sendEmail(EgaSubmission submission) {
        Path basePath = createPathForSubmission(submission)
        User user = springSecurityService.currentUser as User

        String subject = "New ${submission}"
        String content = messageSourceService.createMessage('egaSubmission.template.base', [
                user         : mailHelperService.senderName,
                project      : submission.project.name,
                submission   : submission.id,
                numberOfFiles: submission.dataFilesToSubmit.size() + submission.bamFilesToSubmit.size(),
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
