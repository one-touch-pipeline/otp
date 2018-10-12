package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.ngsdata.Project
import grails.validation.Validateable
import org.springframework.web.multipart.MultipartFile

trait SubmitCommands { }

@Validateable
class NewSubmissionControllerSubmitCommand implements Serializable {
    Project project
    String egaBox
    String submissionName
    String studyName
    Submission.StudyType studyType
    String studyAbstract
    String pubMedId
    String submit

    static constraints = {
        egaBox blank: false
        submissionName blank: false
        studyName blank: false
        studyAbstract blank: false
    }
}

@Validateable
class UpdateSubmissionStateCommand implements Serializable {
    Submission submission
    Submission.State state

    static constraints = {
        state(validator: { val, obj ->
            if (val != Submission.State.SELECTION && !obj.submission.bamFilesToSubmit) {
                return 'No bam files to submit are selected yet'
            }
            if (val != Submission.State.SELECTION && !obj.submission.dataFilesToSubmit) {
                return 'No data files to submit are selected yet'
            }
            if (val != Submission.State.SELECTION && !obj.submission.samplesToSubmit) {
                return 'No samples to submit are selected yet'
            }
        })
    }

    void setValue(String value) {
        this.state = value as Submission.State
    }
}

@Validateable
class EditSubmissionControllerSubmitCommand implements Serializable {
    Submission submission
}

@Validateable
class SelectSamplesControllerSubmitCommand implements Serializable {
    Submission submission
    String next
    List<String> sampleAndSeqType
}

@Validateable
class SampleInformationUploadFormSubmitCommand implements Serializable {
    Submission submission
    String upload
    MultipartFile file
}

@Validateable
class SampleInformationFormsSubmitCommand implements Serializable {
    Submission submission
    String csv
    String next
    List<String> sampleObjectId
    List<EgaSubmissionService.FileType> fileType
    List<String> egaSampleAlias

    static constraints = {
        next nullable: true
        csv nullable: true
    }
}
