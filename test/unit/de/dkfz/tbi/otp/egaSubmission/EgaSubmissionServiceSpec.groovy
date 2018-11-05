package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.domainfactory.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import grails.validation.*
import org.junit.*
import org.junit.rules.*
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

    private EgaSubmissionService egaSubmissionService = new EgaSubmissionService()

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
        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject()
        BamFileSubmissionObject bamFileSubmissionObject = SubmissionDomainFactory.createBamFileSubmissionObject(
                sampleSubmissionObject: sampleSubmissionObject,
        )
        DataFileSubmissionObject dataFileSubmissionObject = SubmissionDomainFactory.createDataFileSubmissionObject(
                sampleSubmissionObject: sampleSubmissionObject,
        )
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
        Submission submission = SubmissionDomainFactory.createSubmission()
        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject()

        when:
        egaSubmissionService.updateSampleSubmissionObjects(submission, ["${sampleSubmissionObject.id}"], ["newAlias"], [EgaSubmissionService.FileType.BAM])

        then:
        sampleSubmissionObject.egaAliasName == "newAlias"
        sampleSubmissionObject.useBamFile
        !sampleSubmissionObject.useFastqFile
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

    void "test get bam files and alias"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject(
                sample: bamFile.sample,
                seqType: bamFile.seqType,
        )
        BamFileSubmissionObject bamFileSubmissionObject = SubmissionDomainFactory.createBamFileSubmissionObject(
                bamFile: bamFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)

        when:
        List bamFilesAndAlias = egaSubmissionService.getBamFilesAndAlias(submission)

        then:
        bamFilesAndAlias*.get(0) == [bamFile]
        bamFilesAndAlias*.get(1) == [sampleSubmissionObject.egaAliasName]
    }

    void "test create data file submission objects"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        List<Boolean> selectBox = [true, null]
        List<String> filenames = [DomainFactory.createDataFile().fileName, DomainFactory.createDataFile().fileName]
        List<String> egaSampleAliases = [
                SubmissionDomainFactory.createSampleSubmissionObject().egaAliasName,
                SubmissionDomainFactory.createSampleSubmissionObject().egaAliasName,
        ]

        when:
        egaSubmissionService.createDataFileSubmissionObjects(submission, selectBox, filenames, egaSampleAliases)

        then:
        submission.dataFilesToSubmit.size() == DataFileSubmissionObject.findAll().size()
    }

    void "test update data file submission objects"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        List<String> egaFileAliases = ["someMagicAlias"]
        DataFile dataFile = DomainFactory.createDataFile()
        List<String> fileNames = [dataFile.fileName]
        DataFileSubmissionObject dataFileSubmissionObject = SubmissionDomainFactory.createDataFileSubmissionObject(
                dataFile: dataFile,
        )
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)

        when:
        egaSubmissionService.updateDataFileSubmissionObjects(fileNames, egaFileAliases, submission)

        then:
        dataFileSubmissionObject.egaAliasName == egaFileAliases.first()
    }

    void "test create bam file submission objects"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        List<String> filenames = [DomainFactory.createRoddyBamFile().id, DomainFactory.createRoddyBamFile().id]
        List<String> egaSampleAliases = [
                SubmissionDomainFactory.createSampleSubmissionObject().egaAliasName,
                SubmissionDomainFactory.createSampleSubmissionObject().egaAliasName,
        ]
        List<String> egaFileAliases = [
                "alias1",
                "alias2",
        ]

        when:
        egaSubmissionService.createBamFileSubmissionObjects(submission, filenames, egaFileAliases, egaSampleAliases)

        then:
        submission.bamFilesToSubmit.size() == BamFileSubmissionObject.findAll().size()
    }
}
