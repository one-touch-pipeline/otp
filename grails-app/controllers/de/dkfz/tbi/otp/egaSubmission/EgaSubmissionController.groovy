package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import org.springframework.validation.FieldError

class EgaSubmissionController implements CheckAndCall {

    EgaSubmissionService egaSubmissionService
    ProjectService projectService
    ProjectSelectionService projectSelectionService

    Map overview() {
        List<Project> projects = projectService.allProjects
        ProjectSelection selection = projectSelectionService.selectedProject
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        List<Submission> submissions = Submission.findAllByProject(project).sort { it.submissionName.toLowerCase() }

        return [
                projects         : projects,
                project          : project,
                submissions      : submissions,
                submissionStates : Submission.State,
        ]
    }

    Map newSubmission(NewSubmissionControllerSubmitCommand cmd) {
        String message
        boolean hasErrors
        if (cmd.submit == "Submit") {
            hasErrors = cmd.hasErrors()
            if (hasErrors) {
                FieldError errors = cmd.errors.fieldError
                message = "'" + errors.rejectedValue + "' is not a valid value for '" + errors.field + "'. Error code: '" + errors.code + "'"
            } else {
                Submission submission = egaSubmissionService.createSubmission([
                        project       : cmd.project,
                        egaBox        : cmd.egaBox,
                        submissionName: cmd.submissionName,
                        studyName     : cmd.studyName,
                        studyType     : cmd.studyType,
                        studyAbstract : cmd.studyAbstract,
                        pubMedId      : cmd.pubMedId,
                ])
                redirect(action: "selectSamples", params: ['submission.id': submission.id])
                return [:]
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

    Map editSubmission(EditSubmissionControllerSubmitCommand cmd) {
        if (cmd.submission.samplesToSubmit.isEmpty()) {
            redirect(action: "selectSamples", params: ['submission.id': cmd.submission.id])
            return [:]
        }
        redirect(action: "overview")
        return [:]

        //TODO add new pages in the other issues
    }

    Map selectSamples(SelectSamplesControllerSubmitCommand cmd) {
        if (cmd.next == "Next") {
            cmd.sampleAndSeqType.findAll().each {
                String[] sampleAndSeqType = it.split("-")

                egaSubmissionService.saveSampleSubmissionObject(
                        cmd.submission,
                        Sample.findById(sampleAndSeqType[0] as long),
                        SeqType.findById(sampleAndSeqType[1] as long)
                )
            }
            redirect(action: "overview")
            return [:]
        }

        return [
                submissionId: cmd.submission.id,
                project: cmd.submission.project,
                seqTypes : egaSubmissionService.seqTypeByProject(cmd.submission.project),
        ]
    }

    JSON updateSubmissionState(UpdateSubmissionStateCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            egaSubmissionService.updateSubmissionState(cmd.submission, cmd.state)
        }
    }

    JSON dataTableSelectSamples(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()

        List data = []
        List<List> sampleSeqType = SeqTrack.createCriteria().list {
            projections {
                sample {
                    property('id')
                    individual {
                        eq('project', project)
                        property('pid')
                    }
                    sampleType {
                        property('name')
                    }
                }
                property('seqType')
            }
        }.unique().sort()

        sampleSeqType.each {
            long sampleId = it[0]
            String individualPid = it[1]
            String sampleTypeName = it[2]
            SeqType seqType = it[3]

            data.add([
                "${sampleId}-${seqType.id}",
                individualPid,
                sampleTypeName,
                seqType.toString(),
            ])
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
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

class EditSubmissionControllerSubmitCommand implements Serializable {
    Submission submission
}

class SelectSamplesControllerSubmitCommand implements Serializable {
    Submission submission
    String next
    List<String> sampleAndSeqType
}
