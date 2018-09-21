package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainfactory.SubmissionDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.test.mixin.Mock
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

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
                pubMedId      : pubMedId
        ]

        when:
        new EgaSubmissionService().createSubmission(params)
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
                studyAbstract : studyAbstract
        ]

        when:
        new EgaSubmissionService().createSubmission(params)

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
        new EgaSubmissionService().updateSubmissionState(submission, Submission.State.FILE_UPLOAD_STARTED)

        then:
        Submission.State.FILE_UPLOAD_STARTED == submission.state
    }

    void "test update state without files"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()

        when:
        new EgaSubmissionService().updateSubmissionState(submission, Submission.State.FILE_UPLOAD_STARTED)

        then:
        ValidationException exception = thrown()
        exception.message.contains("rejected value [null]")
    }
}
