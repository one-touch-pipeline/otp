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
    EgaSubmission.StudyType studyType
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
    EgaSubmission submission
    EgaSubmission.State state

    static constraints = {
        state(validator: { val, obj ->
            if (val != EgaSubmission.State.SELECTION) {
                if (!obj.submission.samplesToSubmit) {
                    return 'No samples to submit are selected yet'
                }
                if (!obj.submission.bamFilesToSubmit && !obj.submission.dataFilesToSubmit) {
                    return 'No files to submit are selected yet'
                }
            }
        })
    }

    void setValue(String value) {
        this.state = value as EgaSubmission.State
    }
}

@Validateable
class SelectSamplesControllerSubmitCommand implements Serializable {
    EgaSubmission submission
    String next
    List<String> sampleAndSeqType
}

@Validateable
class UploadFormSubmitCommand implements Serializable {
    EgaSubmission submission
    String upload
    MultipartFile file

    static constraints = {
        upload matches: "Upload.*"
    }
}

@Validateable
class SampleInformationFormsSubmitCommand implements Serializable {
    EgaSubmission submission
    String csv
    String next
    String back
    List<String> sampleObjectId
    List<EgaSubmissionService.FileType> fileType
    List<String> egaSampleAlias

    static constraints = {
        next nullable: true
        csv nullable: true
        back nullable: true
    }
}

@Validateable
class SelectFilesDataFilesFormSubmitCommand implements Serializable {
    EgaSubmission submission
    String saveSelection
    String download
    String saveAliases
    List<Boolean> selectBox
    List<String> filename
    List<String> runName
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
    EgaSubmission submission
    String saveSelection
    String saveAliases
    String download
    List<String> fileId
    List<String> egaFileAlias
    List<String> egaSampleAlias

    static constraints = {
        download nullable: true
        saveAliases nullable: true
        saveSelection nullable: true
        egaFileAlias nullable: true
    }
}

@Validateable
class GenerateFilesToUploadFileSubmitCommand implements Serializable {
    EgaSubmission submission
    String save
}

@Validateable
class SampleMetadataFormSubmitCommand implements Serializable {
    EgaSubmission submission
    String download
}
