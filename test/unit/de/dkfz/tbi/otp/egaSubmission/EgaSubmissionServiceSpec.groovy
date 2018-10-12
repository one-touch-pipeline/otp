package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.domainfactory.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import grails.test.mixin.*
import grails.validation.*
import spock.lang.*

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
        Submission,
])
class EgaSubmissionServiceSpec extends Specification {

    EgaSubmissionService egaSubmissionService = new EgaSubmissionService()

    @Unroll
    void "test create submission all valid"() {
        given:
        Map params = [
                project       : DomainFactory.createProject(),
                egaBox        : egaBox,
                submissionName: submissionName,
                studyName     : studyName,
                studyType     : Submission.StudyType.CANCER_GENOMICS,
                studyAbstract : studyAbstract,
                pubMedId      : pubMedId,
        ]

        when:
        egaSubmissionService.createSubmission(params)
        Submission submission = CollectionUtils.exactlyOneElement(Submission.list())

        then:
        submission.egaBox == egaBox
        submission.submissionName == submissionName
        submission.studyType == studyType
        submission.studyAbstract == studyAbstract
        submission.pubMedId == pubMedId
        submission.state == Submission.State.SELECTION

        where:
        egaBox   | submissionName   | studyName   | studyType                            | studyAbstract   | pubMedId
        "egaBox" | "submissionName" | "studyName" | Submission.StudyType.CANCER_GENOMICS | "studyAbstract" | "pubMedId"
        "egaBox" | "submissionName" | "studyName" | Submission.StudyType.CANCER_GENOMICS | "studyAbstract" | null
    }

    @Unroll
    void "test create submission with exception #exceptionMessage"() {
        given:
        Map params = [
                project       : DomainFactory.createProject(),
                egaBox        : egaBox,
                submissionName: submissionName,
                studyName     : studyName,
                studyType     : studyType,
                studyAbstract : studyAbstract,
        ]

        when:
        egaSubmissionService.createSubmission(params)

        then:
        ValidationException exception = thrown()
        exception.message.contains(exceptionMessage)

        where:
        egaBox   | submissionName   | studyName   | studyType                            | studyAbstract   || exceptionMessage
        null     | "submissionName" | "studyName" | Submission.StudyType.CANCER_GENOMICS | "studyAbstract" || "'egaBox': rejected value [null]"
        "egaBox" | null             | "studyName" | Submission.StudyType.CANCER_GENOMICS | "studyAbstract" || "'submissionName': rejected value [null]"
        "egaBox" | "submissionName" | null        | Submission.StudyType.CANCER_GENOMICS | "studyAbstract" || "'studyName': rejected value [null]"
        "egaBox" | "submissionName" | "studyName" | null                                 | "studyAbstract" || "'studyType': rejected value [null]"
        "egaBox" | "submissionName" | "studyName" | Submission.StudyType.CANCER_GENOMICS | null            || "'studyAbstract': rejected value [null]"
    }

    void "test update state all fine"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        BamFileSubmissionObject bamFileSubmissionObject = SubmissionDomainFactory.createBamFileSubmissionObject()
        DataFileSubmissionObject dataFileSubmissionObject = SubmissionDomainFactory.createDataFileSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject()
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.save(flush: true)

        when:
        egaSubmissionService.updateSubmissionState(submission, Submission.State.FILE_UPLOAD_STARTED)

        then:
        Submission.State.FILE_UPLOAD_STARTED == submission.state
    }

    void "test update state without files"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()

        when:
        egaSubmissionService.updateSubmissionState(submission, Submission.State.FILE_UPLOAD_STARTED)

        then:
        ValidationException exception = thrown()
        exception.message.contains("rejected value [null]")
    }

    void "test save submission object all fine"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        Sample sample = DomainFactory.createSample()
        SeqType seqType = DomainFactory.createSeqType()

        when:
        egaSubmissionService.saveSampleSubmissionObject(submission, sample, seqType)

        then:
        !submission.samplesToSubmit.isEmpty()
        submission.samplesToSubmit.first().seqType == seqType
        submission.samplesToSubmit.first().sample == sample
    }

    @Unroll
    void "test save submission object"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()

        when:
        egaSubmissionService.saveSampleSubmissionObject(submission, sample(), seqType())

        then:
        ValidationException exception = thrown()
        exception.message.contains("rejected value [null]")

        where:
        sample                               | seqType
        ( { null } )                         | { DomainFactory.createSeqType() }
        ( { DomainFactory.createSample() } ) | { null }
    }

    void "test update submission object all fine"() {
        given:
        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject()

        when:
        egaSubmissionService.updateSampleSubmissionObjects(["${sampleSubmissionObject.id}"], ["newAlias"], [EgaSubmissionService.FileType.BAM])

        then:
        sampleSubmissionObject.egaAliasName == "newAlias"
        sampleSubmissionObject.useBamFile
        !sampleSubmissionObject.useFastqFile
    }

    void "test generate csv content file"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = SubmissionDomainFactory.createSampleSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject2 = SubmissionDomainFactory.createSampleSubmissionObject()
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = ["abc", "dfg"]
        List<FileType> fileType = [EgaSubmissionService.FileType.FASTQ, EgaSubmissionService.FileType.BAM]

        when:
        String content = egaSubmissionService.generateCsvFile(sampleObjectId, egaSampleAlias, fileType)

        then:
        content == "${egaSubmissionService.INDIVIDUAL}," +
                "${egaSubmissionService.SAMPLE_TYPE}," +
                "${egaSubmissionService.SEQ_TYPE}," +
                "${egaSubmissionService.EGA_SAMPLE_ALIAS}," +
                "${egaSubmissionService.FILE_TYPE}\n" +
                "${sampleSubmissionObject1.sample.individual.displayName}," +
                "${sampleSubmissionObject1.sample.sampleType.displayName}," +
                "${sampleSubmissionObject1.seqType.displayName}," +
                "${egaSampleAlias[0]}," +
                "${fileType[0]}\n" +
                "${sampleSubmissionObject2.sample.individual.displayName}," +
                "${sampleSubmissionObject2.sample.sampleType.displayName}," +
                "${sampleSubmissionObject2.seqType.displayName}," +
                "${egaSampleAlias[1]}," +
                "${fileType[1]}\n"
    }

    void "test read from file"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = SubmissionDomainFactory.createSampleSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject2 = SubmissionDomainFactory.createSampleSubmissionObject()
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = ["abc", "dfg"]
        List<FileType> fileType = [EgaSubmissionService.FileType.FASTQ, EgaSubmissionService.FileType.BAM]
        String content = egaSubmissionService.generateCsvFile(sampleObjectId, egaSampleAlias, fileType)
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        when:
        Map egaSampleAliases = egaSubmissionService.readEgaSampleAliasesFromFile(spreadsheet)
        Map fastqs = egaSubmissionService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.FASTQ)
        Map bams = egaSubmissionService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.BAM)

        then:
        egaSampleAliases.size() == 2
        fastqs*.value == [true, false]
        bams*.value  == [false, true]
    }

    void "test validate rows"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = SubmissionDomainFactory.createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        SampleSubmissionObject sampleSubmissionObject2 = SubmissionDomainFactory.createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject2)
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = ["abc", "dfg"]
        List<FileType> fileType = [EgaSubmissionService.FileType.FASTQ, EgaSubmissionService.FileType.BAM]
        String content = egaSubmissionService.generateCsvFile(sampleObjectId, egaSampleAlias, fileType)
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        expect:
        egaSubmissionService.validateRows(spreadsheet, submission).valid
    }

    void "test validate rows with less rows"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = SubmissionDomainFactory.createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        submission.addToSamplesToSubmit(SubmissionDomainFactory.createSampleSubmissionObject())
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String]
        List<String> egaSampleAlias = ["abc"]
        List<FileType> fileType = [EgaSubmissionService.FileType.FASTQ]
        String content = egaSubmissionService.generateCsvFile(sampleObjectId, egaSampleAlias, fileType)
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        expect:
        !egaSubmissionService.validateRows(spreadsheet, submission).valid
        egaSubmissionService.validateRows(spreadsheet, submission).error == "There are less rows in the file as samples where selected"
    }

    void "test validate rows with wrong samples"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = SubmissionDomainFactory.createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        List<String> sampleObjectId = [SubmissionDomainFactory.createSampleSubmissionObject().id as String]
        List<String> egaSampleAlias = ["abc"]
        List<FileType> fileType = [EgaSubmissionService.FileType.FASTQ]
        String content = egaSubmissionService.generateCsvFile(sampleObjectId, egaSampleAlias, fileType)
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        expect:
        !egaSubmissionService.validateRows(spreadsheet, submission).valid
        egaSubmissionService.validateRows(spreadsheet, submission).error == "Found and expected samples are different"
    }

    void "test getting identifier key from sample submission object"() {
        given:
        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject()
        String string = sampleSubmissionObject.sample.individual.displayName +
                sampleSubmissionObject.sample.sampleType.displayName +
                sampleSubmissionObject.seqType.displayName

        expect:
        string == egaSubmissionService.getIdentifierKeyFromSampleSubmissionObject(sampleSubmissionObject)
    }

    @Unroll
    void "test check file types with data file"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        if (withDataFile) {
            DomainFactory.createDataFile(
                    seqTrack: seqTrack,
                    fileWithdrawn: withdrawn,
            )
        } else {
            DomainFactory.createDataFile()
        }

        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
        )

        Submission submission = SubmissionDomainFactory.createSubmission(
                project: seqTrack.project
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.save(flush: true)

        when:
        Map map = egaSubmissionService.checkFastqFiles(submission)

        then:
        map.get(sampleSubmissionObject) == result

        where:
        withDataFile | withdrawn | result
        true         | false     | true
        false        | false     | false
        true         | true      | false
    }

    @Unroll
    void "test validate sample information form input"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = SubmissionDomainFactory.createSampleSubmissionObject(
                egaAliasName: "testAlias",
        )
        SampleSubmissionObject sampleSubmissionObject2 = SubmissionDomainFactory.createSampleSubmissionObject()
        List<String> sampleObjectIds = ["${sampleSubmissionObject1.id}", "${sampleSubmissionObject2.id}"]

        when:
        Map map = egaSubmissionService.validateSampleInformationFormInput(sampleObjectIds, aliases, fileType)

        then:
        map.hasErrors == hasErrors
        !map.errors ?: map.errors.contains(errorMessages)

        where:
        aliases            | fileType                                                                 || hasErrors | errorMessages
        ["a", "b"]         | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || false     | ""
        ["a", "a"]         | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "Not all aliases are unique."
        ["a", ""]          | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "For some samples no alias is configured."
        ["a", "testAlias"] | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "Alias testAlias already exist."
        ["a", "b"]         | [EgaSubmissionService.FileType.BAM, null]                                || true      | "For some samples files types are not selected."
    }

    @Unroll
    void "test validate file type from input"() {
        given:
        List<String> sampleObjectId = [SubmissionDomainFactory.createSampleSubmissionObject().id as String]
        List<String> egaSampleAlias = ["abc"]
        List<FileType> fileTypeList = [EgaSubmissionService.FileType.FASTQ]
        String content = egaSubmissionService.generateCsvFile(sampleObjectId, egaSampleAlias, fileTypeList).replace("FASTQ", fileType)
        Spreadsheet spreadsheet = new Spreadsheet(content, ",")

        expect:
        egaSubmissionService.validateFileTypeFromInput(spreadsheet) == result

        where:
        fileType | result
        "FASTQ"  | true
        "WRONG"  | false
    }
}
