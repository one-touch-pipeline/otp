package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ProjectService
import grails.converters.JSON
import org.springframework.validation.FieldError

class EgaSubmissionController implements CheckAndCall {

    EgaSubmissionService egaSubmissionService
    ProjectService projectService
    ProjectSelectionService projectSelectionService

    def overview() {

        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        List<Submission> submissions = Submission.findAllByProject(project).sort { it.submissionName.toLowerCase() }

        return [
                projects         : projects,
                project          : project,
                submissions      : submissions,
                submissionStates : Submission.State
        ]
    }

    def newSubmission(NewSubmissionControllerSubmitCommand cmd) {
        String message
        boolean hasErrors
        if (cmd.submit == "Submit") {
            hasErrors = cmd.hasErrors()
            if (hasErrors) {
                FieldError errors = cmd.errors.getFieldError()
                message = "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"
            }
            else {
                egaSubmissionService.createSubmission([
                        project       : cmd.project,
                        egaBox        : cmd.egaBox,
                        submissionName: cmd.submissionName,
                        studyName     : cmd.studyName,
                        studyType     : cmd.studyType,
                        studyAbstract : cmd.studyAbstract,
                        pubMedId      : cmd.pubMedId
                ])
                redirect(action: "overview")
            }
        }

        return [
                selectedProject : projectService.getProject(params.project as long),
                studyTypes      : Submission.StudyType,
                message         : message,
                cmd             : cmd,
                hasErrors       : hasErrors,
        ]

    }

    JSON updateSubmissionState(UpdateSubmissionStateCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            egaSubmissionService.updateSubmissionState(cmd.submission, cmd.state)
        })
    }
}

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
