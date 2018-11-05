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
class UpdateSubmissionStateSubmitCommand implements Serializable {
    Submission submission
    Submission.State state

    static constraints = {
        state(validator: { val, obj ->
            if (val != Submission.State.SELECTION) {
                if (!obj.submission.samplesToSubmit) {
                    return 'No samples to submit are selected yet'
                }
                if (!obj.submission.bamFilesToSubmit || !obj.submission.dataFilesToSubmit) {
                    return 'No files to submit are selected yet'
                }
            }
        })
    }

    void setValue(String value) {
        this.state = value as Submission.State
    }
}

@Validateable
class SelectSamplesControllerSubmitCommand implements Serializable {
    Submission submission
    String next
    List<String> sampleAndSeqType
}

@Validateable
class UploadFormSubmitCommand implements Serializable {
    Submission submission
    String upload
    MultipartFile file

    static constraints = {
        upload matches: "Upload [A-Z]+ meta file"
    }
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

@Validateable
class SelectFilesDataFilesFormSubmitCommand implements Serializable {
    Submission submission
    String saveSelection
    String download
    String saveAliases
    List<Boolean> selectBox
    List<String> filename
    List<String> egaFileAlias
    List<String> egaSampleAlias

    static constraints = {
        download nullable: true
        selectBox nullable: true
        saveSelection nullable: true
        saveAliases nullable: true
        egaFileAlias nullable: true
    }
}

@Validateable
class SelectFilesBamFilesFormSubmitCommand implements Serializable {
    Submission submission
    String save
    String download
    List<String> fileId
    List<String> egaFileAlias
    List<String> egaSampleAlias

    static constraints = {
        download nullable: true
        save nullable: true
    }
}

@Validateable
class GenerateFilesToUploadFileSubmitCommand implements Serializable {
    Submission submission
    String save
}
