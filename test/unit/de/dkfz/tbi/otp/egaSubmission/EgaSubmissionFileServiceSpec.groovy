package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.domainFactory.submissions.ega.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import grails.plugin.springsecurity.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

@Mock([
        AbstractMergedBamFile,
        BamFileSubmissionObject,
        DataFile,
        DataFileSubmissionObject,
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
        RunSegment,
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
        EgaSubmission,
])
class EgaSubmissionFileServiceSpec extends Specification implements EgaSubmissionFactory {

    private EgaSubmissionFileService egaSubmissionFileService = new EgaSubmissionFileService()

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
                "${SAMPLE_TYPE.value}," +
                "${SEQ_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${FILE_TYPE.value}\n" +
                "${sampleSubmissionObject1.sample.individual.displayName}," +
                "${sampleSubmissionObject1.sample.sampleType.displayName}," +
                "${sampleSubmissionObject1.seqType.toString()}," +
                "${egaSampleAlias[0]}," +
                "${fileTypes[0]}\n" +
                "${sampleSubmissionObject2.sample.individual.displayName}," +
                "${sampleSubmissionObject2.sample.sampleType.displayName}," +
                "${sampleSubmissionObject2.seqType.toString()}," +
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
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        when:
        Map egaSampleAliases = egaSubmissionFileService.readEgaSampleAliasesFromFile(spreadsheet)
        Map fastqs = egaSubmissionFileService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.FASTQ)
        Map bams = egaSubmissionFileService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.BAM)

        then:
        egaSampleAliases.size() == 2
        fastqs*.value == [true, false]
        bams*.value  == [false, true]
    }





    void "test generate data files csv file"() {
        given:
        EgaSubmission submission = createSubmission()
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject()
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)

        when:
        String content = egaSubmissionFileService.generateDataFilesCsvFile(submission)

        then:
        content == "${INDIVIDUAL.value}," +
                "${SAMPLE_TYPE.value}," +
                "${SEQ_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${SEQ_CENTER.value}," +
                "${RUN.value}," +
                "${LANE.value}," +
                "${LIBRARY.value}," +
                "${ILSE.value}," +
                "${EGA_FILE_ALIAS.value}," +
                "${FILENAME.value}\n" +
                "${dataFileSubmissionObject.dataFile.individual.displayName}," +
                "${dataFileSubmissionObject.dataFile.sampleType}," +
                "${dataFileSubmissionObject.dataFile.seqType}," +
                "${dataFileSubmissionObject.sampleSubmissionObject.egaAliasName}," +
                "${dataFileSubmissionObject.dataFile.run.seqCenter}," +
                "${dataFileSubmissionObject.dataFile.run}," +
                "${dataFileSubmissionObject.dataFile.seqTrack.laneId}," +
                "${dataFileSubmissionObject.dataFile.seqTrack.normalizedLibraryName ?: "N/A"}," +
                "${dataFileSubmissionObject.dataFile.seqTrack.ilseId ?: "N/A"}," +
                "," +
                "${dataFileSubmissionObject.dataFile.fileName}\n"
    }

    void "test generate bam files csv file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createSubmission()
        RoddyBamFile roddyBamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: roddyBamFile.sample,
                seqType: roddyBamFile.seqType,
        )
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject(
                bamFile: roddyBamFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)

        when:
        String content = egaSubmissionFileService.generateBamFilesCsvFile(submission)

        then:
        content == "${INDIVIDUAL.value}," +
                "${SAMPLE_TYPE.value}," +
                "${SEQ_TYPE.value}," +
                "${EGA_SAMPLE_ALIAS.value}," +
                "${EGA_FILE_ALIAS.value}," +
                "${FILENAME.value}\n" +
                "${bamFileSubmissionObject.bamFile.individual.displayName}," +
                "${bamFileSubmissionObject.bamFile.sampleType}," +
                "${bamFileSubmissionObject.bamFile.seqType}," +
                "${bamFileSubmissionObject.sampleSubmissionObject.egaAliasName}," +
                "," +
                "${bamFileSubmissionObject.bamFile.bamFileName}\n"
    }

    void "test read ega file aliases from file with data file"() {
        given:
        EgaSubmission submission = createSubmission()
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject()
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)
        String content = egaSubmissionFileService.generateDataFilesCsvFile(submission)
        content = content.replace(",,", ",${dataFileSubmissionObject.egaAliasName},")
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        when:
        Map fileAliases = egaSubmissionFileService.readEgaFileAliasesFromFile(spreadsheet, false)

        then:
        fileAliases.get(dataFileSubmissionObject.dataFile.fileName + dataFileSubmissionObject.dataFile.run) == dataFileSubmissionObject.egaAliasName
    }

    void "test read ega file aliases from file with bam file"() {
        given:
        egaSubmissionFileService.egaSubmissionService = new EgaSubmissionService()
        EgaSubmission submission = createSubmission()
        RoddyBamFile roddyBamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: roddyBamFile.sample,
                seqType: roddyBamFile.seqType,
        )
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject(
                bamFile: roddyBamFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)
        String content = egaSubmissionFileService.generateBamFilesCsvFile(submission)
        content = content.replace(",,", ",${bamFileSubmissionObject.egaAliasName},")
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        when:
        Map fileAliases = egaSubmissionFileService.readEgaFileAliasesFromFile(spreadsheet, true)

        then:
        fileAliases.get(bamFileSubmissionObject.bamFile.bamFileName + bamFileSubmissionObject.sampleSubmissionObject.egaAliasName) == bamFileSubmissionObject.egaAliasName
    }

    void "test generate filesToUpload file"() {
        given:
        new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().absolutePath,
        ])
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject()
        EgaSubmission submission = createSubmission()
        bamFileSubmissionObject.bamFile.workPackage.bamFileInProjectFolder = bamFileSubmissionObject.bamFile
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)

        String emailSubject = "New EGA submission ${submission.id}"
        String content = "some content"
        String recipient = "a.b@c.de"
        User user = new User(
                realName: "Real Name",
                email: "ua.ub@uc.ude",
        )

        egaSubmissionFileService.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(_) >> "someFastqPath"
        }

        egaSubmissionFileService.processingOptionService = Mock (ProcessingOptionService) {
            1 * findOptionAsString(_) >> recipient
        }

        egaSubmissionFileService.fileSystemService = Mock (FileSystemService) {
            1 * getFilesystemForProcessingForRealm(_) >> FileSystems.default
        }

        egaSubmissionFileService.springSecurityService = Mock (SpringSecurityService) {
            1 * getCurrentUser() >> user
        }

        egaSubmissionFileService.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(emailSubject, content, [recipient], user.email)
        }

        egaSubmissionFileService.fileService = Mock (FileService) {
            1 * createFileWithContent(_, _)
        }

        egaSubmissionFileService.createNotificationTextService = Mock (CreateNotificationTextService) {
            1 * createMessage(_, _) >> content
        }

        expect:
        egaSubmissionFileService.generateFilesToUploadFile(submission)
    }
}
