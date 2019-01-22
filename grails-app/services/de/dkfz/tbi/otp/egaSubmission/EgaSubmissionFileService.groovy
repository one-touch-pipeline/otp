package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import grails.plugin.springsecurity.*
import groovy.transform.*

import java.nio.file.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

@CompileStatic
class EgaSubmissionFileService {

    CreateNotificationTextService createNotificationTextService
    EgaSubmissionService egaSubmissionService
    FileService fileService
    FileSystemService fileSystemService
    LsdfFilesService lsdfFilesService
    MailHelperService mailHelperService
    ProcessingOptionService processingOptionService
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

    Map<List<String>, String> readEgaSampleAliasesFromFile(Spreadsheet spreadsheet) {
        Map<List<String>, String> egaSampleAliases = [:]

        spreadsheet.dataRows.each {
            egaSampleAliases.put(getIdentifierKey(it), it.getCellByColumnTitle(EGA_SAMPLE_ALIAS.value).text)
        }

        return egaSampleAliases
    }

    Map<List<String>, Boolean> readBoxesFromFile(Spreadsheet spreadsheet, EgaSubmissionService.FileType fileType) {
        Map<List<String>, Boolean> map = [:]

        spreadsheet.dataRows.each {
            map.put(getIdentifierKey(it), it.getCellByColumnTitle(FILE_TYPE.value).text.toUpperCase() as EgaSubmissionService.FileType == fileType)
        }

        return map
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

    String generateCsvFile(List<String> sampleObjectId, List<String> alias, List<EgaSubmissionService.FileType> fileType) {
        StringBuilder contentBody = new StringBuilder()

        sampleObjectId.eachWithIndex { it, i ->
            SampleSubmissionObject sampleSubmissionObject = SampleSubmissionObject.get(it as Long)

            contentBody.append([
                    sampleSubmissionObject.sample.individual.displayName,
                    sampleSubmissionObject.sample.sampleType.displayName,
                    sampleSubmissionObject.seqType.toString(),
                    alias?.getAt(i) ?: "",
                    fileType?.getAt(i) ?: EgaSubmissionService.FileType.FASTQ,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                SAMPLE_TYPE,
                SEQ_TYPE,
                EGA_SAMPLE_ALIAS,
                FILE_TYPE,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    String generateDataFilesCsvFile(EgaSubmission submission) {
        StringBuilder contentBody = new StringBuilder()

        List<List> dataFilesAndAlias = egaSubmissionService.getDataFilesAndAlias(submission)
        Map dataFileAliases = egaSubmissionService.generateDefaultEgaAliasesForDataFiles(dataFilesAndAlias)

        submission.dataFilesToSubmit.sort { it.sampleSubmissionObject.egaAliasName }.each {
            contentBody.append([
                    it.dataFile.individual.displayName,
                    it.dataFile.sampleType.displayName,
                    it.dataFile.seqType.toString(),
                    it.sampleSubmissionObject.egaAliasName,
                    it.dataFile.run.seqCenter,
                    it.dataFile.run,
                    it.dataFile.seqTrack.laneId,
                    it.dataFile.seqTrack.normalizedLibraryName ?: "N/A",
                    it.dataFile.seqTrack.ilseId ?: "N/A",
                    dataFileAliases.get(it.dataFile.fileName + it.dataFile.run),
                    it.dataFile.fileName,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                SAMPLE_TYPE,
                SEQ_TYPE,
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

        List<List> bamFilesAndAlias = egaSubmissionService.getBamFilesAndAlias(submission)
        bamFilesAndAlias.sort { it[1] as String }
        Map bamFileAliases = egaSubmissionService.generateDefaultEgaAliasesForBamFiles(bamFilesAndAlias)

        bamFilesAndAlias.each {
            AbstractMergedBamFile bamFile = it[0] as AbstractMergedBamFile
            contentBody.append([
                    bamFile.individual.displayName,
                    bamFile.sampleType.displayName,
                    bamFile.seqType.toString(),
                    it[1],
                    bamFileAliases.get(bamFile.bamFileName + it[1]),
                    bamFile.bamFileName,
            ].join(",") + "\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                SAMPLE_TYPE,
                SEQ_TYPE,
                EGA_SAMPLE_ALIAS,
                EGA_FILE_ALIAS,
                FILENAME,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    void generateFilesToUploadFile(EgaSubmission submission) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(submission.project.realm)
        Path path = fileSystem.getPath("${submission.project.projectDirectory}/submission/${submission.id}/filesToUpload.tsv")
        StringBuilder out = new StringBuilder()
        User user = springSecurityService.getCurrentUser() as User

        submission.dataFilesToSubmit.each {
            out.append("${lsdfFilesService.getFileFinalPath(it.dataFile)}\t")
            out.append("${it.egaAliasName}\n")
        }

        submission.bamFilesToSubmit.each {
            out.append("${it.bamFile.getPathForFurtherProcessing()}\t")
            out.append("${it.egaAliasName}\n")
        }

        fileService.createFileWithContent(path, out.toString())

        String subject = "New EGA submission ${submission.id}"
        String content = createNotificationTextService.createMessage('egaSubmission.template.base', [
                user: user.realName,
                project: submission.project.name,
                submission: submission.id,
                numberOfFiles: submission.dataFilesToSubmit.size() + submission.bamFilesToSubmit.size(),
                path: path,
        ])
        mailHelperService.sendEmail(subject, content, [processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION)], user.email)
        submission.state = EgaSubmission.State.FILE_UPLOAD_STARTED
        submission.save(flush: true)
    }

    String generateSampleMetadataCsvFile(EgaSubmission submission) {
        StringBuilder contentBody = new StringBuilder()
        String unknown = "unknown"

        submission.samplesToSubmit.each {
            contentBody.append([
                    unknown,
                    it.egaAliasName,
                    unknown,
                    unknown,
                    unknown,
                    it.sample.sampleTypeCategory ?: unknown,
                    unknown,
                    unknown,
                    unknown,
                    unknown,
                    unknown,
                    unknown,
            ].join(",") + "\n")
        }

        String contentHeader = [
                TITLE,
                ALIAS,
                DESCRIPTION,
                SUBJECT_ID,
                BIO_SAMPLE_ID,
                CASE_OR_CONTROL,
                GENDER,
                ORGANISM_PART,
                ORGANISM,
                CELL_LINE,
                REGION,
                PHENOTYPE,
        ]*.value.join(",")

        return "${contentHeader}\n${contentBody}"
    }

    static List<String> getIdentifierKey(Row row) {
        return  [row.getCellByColumnTitle(INDIVIDUAL.value).text,
                 row.getCellByColumnTitle(SAMPLE_TYPE.value).text,
                 row.getCellByColumnTitle(SEQ_TYPE.value).text]
    }
}
