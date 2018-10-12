package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import grails.converters.JSON
import org.springframework.validation.FieldError

class EgaSubmissionController implements CheckAndCall, SubmitCommands {

    EgaSubmissionService egaSubmissionService
    ProjectService projectService
    ProjectSelectionService projectSelectionService

    final private static String ERROR_TITLE = "Some errors occurred"

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

    def editSubmission(EditSubmissionControllerSubmitCommand cmd) {
        Submission submission = cmd.submission
        if (submission.samplesToSubmit.isEmpty()) {
            redirect(action: "selectSamples", params: ['submission.id': cmd.submission.id])
            return
        } else if (!submission.samplesToSubmit.isEmpty() && submission.bamFilesToSubmit.isEmpty() && submission.dataFilesToSubmit.isEmpty()) {
            flash.submission = cmd.submission
            redirect(action: "sampleInformation")
            return
        }
        redirect(action: "overview")

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
            flash.submission = cmd.submission
            redirect(action: "sampleInformation")
            return [:]
        }

        return [
                submissionId: cmd.submission.id,
                project: cmd.submission.project,
                seqTypes : egaSubmissionService.seqTypeByProject(cmd.submission.project),
        ]
    }

    Map sampleInformation() {
        Submission submission = flash.submission
        submission.refresh()

        Map<String, String> egaSampleAliases = flash.egaSampleAliases ?: [:]
        Map<String, Boolean> selectedFastqs = [:]
        Map<String, Boolean> selectedBams = [:]

        Map<SampleSubmissionObject, Boolean> existingFastqs = egaSubmissionService.checkFastqFiles(submission)
        Map<SampleSubmissionObject, Boolean> existingBams = egaSubmissionService.checkBamFiles(submission)

        if (flash.fastqs && flash.bams) {
            existingFastqs.each { key, value ->
                String newKey = egaSubmissionService.getIdentifierKeyFromSampleSubmissionObject(key)
                selectedFastqs.put(newKey, flash.fastqs.get(newKey) && value)
            }
            existingBams.each { key, value ->
                String newKey = egaSubmissionService.getIdentifierKeyFromSampleSubmissionObject(key)
                selectedBams.put(newKey, flash.bams.get(newKey) && value)
            }
        } else {
            existingFastqs.each { key, value ->
                String newKey = egaSubmissionService.getIdentifierKeyFromSampleSubmissionObject(key)
                selectedFastqs.put(newKey, value)
                selectedBams.put(newKey, !value)
            }
        }

        return [
                submissionId           : submission.id,
                sampleSubmissionObjects: submission.samplesToSubmit.sort { egaSubmissionService.getIdentifierKeyFromSampleSubmissionObject(it) },
                egaSampleAliases       : egaSampleAliases,
                existingFastqs         : existingFastqs,
                existingBams           : existingBams,
                selectedFastqs         : selectedFastqs,
                selectedBams           : selectedBams,
        ]
    }

    def sampleInformationUploadForm(SampleInformationUploadFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            FieldError errors = cmd.errors.fieldError
            pushError("'${errors.rejectedValue}' is not a valid value for '${errors.field}'. Error code: '${errors.code}'")
            flash.submission = cmd.submission
            redirect(action: "sampleInformation")
        } else if (cmd.upload == "Upload") {
            final String DELIMITER = ","
            try {
                String content = ""
                if (cmd.file.empty) {
                    pushError("No file selected")
                    flash.submission = cmd.submission
                    redirect(action: "sampleInformation")
                    return
                }
                content = new String(cmd.file.bytes)
                content = content.replace("\"", "")
                Spreadsheet spreadsheet = new Spreadsheet(content, DELIMITER)

                Map validateRows = egaSubmissionService.validateRows(spreadsheet, cmd.submission)
                if (!validateRows.valid) {
                    pushError(validateRows.error)
                    flash.submission = cmd.submission
                    redirect(action: "sampleInformation")
                } else if (!spreadsheet.getColumn(egaSubmissionService.INDIVIDUAL)) {
                    pushError("The column ${egaSubmissionService.INDIVIDUAL} does not exist")
                    flash.submission = cmd.submission
                    redirect(action: "sampleInformation")
                } else if (!spreadsheet.getColumn(egaSubmissionService.SAMPLE_TYPE)) {
                    pushError("The column ${egaSubmissionService.SAMPLE_TYPE} does not exist")
                    flash.submission = cmd.submission
                    redirect(action: "sampleInformation")
                } else if (!spreadsheet.getColumn(egaSubmissionService.SEQ_TYPE)) {
                    pushError("The column ${egaSubmissionService.SEQ_TYPE} does not exist")
                    flash.submission = cmd.submission
                    redirect(action: "sampleInformation")
                } else if (!egaSubmissionService.validateFileTypeFromInput(spreadsheet)) {
                    pushError("Wrong file type detected. Please use only ${EgaSubmissionService.FileType.collect().join(", ")}")
                    flash.submission = cmd.submission
                    redirect(action: "sampleInformation")
                } else {
                    flash.message = "File was uploaded"
                    flash.egaSampleAliases = egaSubmissionService.readEgaSampleAliasesFromFile(spreadsheet)
                    flash.fastqs = egaSubmissionService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.FASTQ)
                    flash.bams = egaSubmissionService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.BAM)
                    flash.submission = cmd.submission
                    redirect(action: "sampleInformation")
                }
            } catch (Throwable t) {
                log.error(t.message, t)
                pushError("Error: The creation failed for ${t.message.replaceAll('\n', ' ')}")
            }
        }
    }

    def sampleInformationForms(SampleInformationFormsSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            FieldError errors = cmd.errors.fieldError
            pushError("'${errors.rejectedValue}' is not a valid value for '${errors.field}'. Error code: '${errors.code}'")
            flash.submission = cmd.submission
            redirect(action: "sampleInformation")
        } else if (cmd.csv == "Download CSV") {
            String content = egaSubmissionService.generateCsvFile(cmd.sampleObjectId, cmd.egaSampleAlias, cmd.fileType)
            response.contentType = "application/octet-stream"
            response.setHeader("Content-disposition", "filename=sample_information.csv")
            response.outputStream << content.bytes
        } else if (cmd.next == "Next") {
            Map validateMap = egaSubmissionService.validateSampleInformationFormInput(cmd.sampleObjectId, cmd.egaSampleAlias, cmd.fileType)

            if (validateMap.hasErrors) {
                flash.message = ERROR_TITLE
                flash.errors = validateMap.errors
                flash.egaSampleAliases = validateMap.sampleAliases
                flash.fastqs = validateMap.fastqs
                flash.bams = validateMap.bams
                flash.submission = cmd.submission
                redirect(action: "sampleInformation")
            } else {
                egaSubmissionService.updateSampleSubmissionObjects(cmd.sampleObjectId, cmd.egaSampleAlias, cmd.fileType)
                redirect(action: "overview")
            }
        }
    }

    private void pushError(String message) {
        flash.message = ERROR_TITLE
        flash.errors = message
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
