package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import grails.converters.*
import org.springframework.validation.*

import static de.dkfz.tbi.otp.administration.Document.FormatType.*
import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

class EgaSubmissionController implements CheckAndCall, SubmitCommands {

    EgaSubmissionService egaSubmissionService
    EgaSubmissionValidationService egaSubmissionValidationService
    EgaSubmissionFileService egaSubmissionFileService
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

    def editSubmission() {
        Submission submission = Submission.get(params.id)
        if (submission.state != Submission.State.SELECTION) {
            redirect(action: "overview")
            return
        }
        switch (submission.selectionState) {
            case Submission.SelectionState.SELECT_SAMPLES:
                render(view: "selectSamples", model: selectSamples(submission))
                break
            case Submission.SelectionState.SAMPLE_INFORMATION:
                render(view: "sampleInformation", model: sampleInformation(submission))
                break
            case Submission.SelectionState.SELECT_FASTQ_FILES:
                render(view: "selectFastqFiles", model: selectFastqFiles(submission))
                break
            case Submission.SelectionState.SELECT_BAM_FILES:
                render(view: "selectBamFiles", model: selectBamFiles(submission))
                break
            default:
                redirect(action: "overview")
        }
    }

    def newSubmission() {
        Project project = Project.get(params.id)

        return [
                selectedProject : project,
                studyTypes      : Submission.StudyType,
                defaultStudyType: Submission.StudyType.CANCER_GENOMICS,
                cmd             : flash.cmd,
        ]
    }

    Map selectSamples(Submission submission) {
        return [
                submissionId: submission.id,
                project: submission.project,
                seqTypes : egaSubmissionService.seqTypeByProject(submission.project),
        ]
    }

    Map sampleInformation(Submission submission) {
        Map<String, String> egaSampleAliases = flash.egaSampleAliases ?: [:]
        Map<String, Boolean> selectedFastqs = [:]
        Map<String, Boolean> selectedBams = [:]

        Map<SampleSubmissionObject, Boolean> existingFastqs = egaSubmissionService.checkFastqFiles(submission)
        Map<SampleSubmissionObject, Boolean> existingBams = egaSubmissionService.checkBamFiles(submission)

        if (flash.fastqs && flash.bams) {
            existingFastqs.each { key, value ->
                String newKey = egaSubmissionValidationService.getIdentifierKeyFromSampleSubmissionObject(key)
                selectedFastqs.put(newKey, flash.fastqs.get(newKey) && value)
            }
            existingBams.each { key, value ->
                String newKey = egaSubmissionValidationService.getIdentifierKeyFromSampleSubmissionObject(key)
                selectedBams.put(newKey, flash.bams.get(newKey) && value)
            }
        } else {
            existingFastqs.each { key, value ->
                String newKey = egaSubmissionValidationService.getIdentifierKeyFromSampleSubmissionObject(key)
                selectedFastqs.put(newKey, value)
                selectedBams.put(newKey, !value)
            }
        }

        return [
                submissionId           : submission.id,
                sampleSubmissionObjects: submission.samplesToSubmit.sort { egaSubmissionValidationService.getIdentifierKeyFromSampleSubmissionObject(it) },
                egaSampleAliases       : egaSampleAliases,
                existingFastqs         : existingFastqs,
                existingBams           : existingBams,
                selectedFastqs         : selectedFastqs,
                selectedBams           : selectedBams,
        ]
    }

    Map selectFastqFiles(Submission submission) {
        return [
                submission: submission,
                dataFileList: egaSubmissionService.getDataFilesAndAlias(submission),
                dataFileSubmissionObject: submission.dataFilesToSubmit,
                egaFileAliases: flash.egaFileAliases,
                hasDataFiles: !submission.dataFilesToSubmit.empty,
                dataFilesHasFileAliases: !submission.dataFilesToSubmit*.egaAliasName.findAll().isEmpty(),
        ]
    }

    Map selectBamFiles(Submission submission) {
        return [
                submission: submission,
                bamFileList: egaSubmissionService.getBamFilesAndAlias(submission),
                bamFileSubmissionObject: submission.bamFilesToSubmit,
                egaFileAliases: flash.egaFileAliases,
                bamFilesHasFileAliases: !submission.bamFilesToSubmit*.egaAliasName.findAll().isEmpty(),
        ]
    }

    def newSubmissionForm(NewSubmissionControllerSubmitCommand cmd) {
        if (cmd.submit == "Submit") {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(ERROR_TITLE, cmd.errors)
                flash.cmd = cmd
                redirect(action: "newSubmission", params: ['id': cmd.project.id])
                return
            }
            Submission submission = egaSubmissionService.createSubmission([
                    project       : cmd.project,
                    egaBox        : cmd.egaBox,
                    submissionName: cmd.submissionName,
                    studyName     : cmd.studyName,
                    studyType     : cmd.studyType,
                    studyAbstract : cmd.studyAbstract,
                    pubMedId      : cmd.pubMedId,
            ])
            redirect(action: "editSubmission", params: ['id': submission.id])
        }
    }

    def selectSamplesForm(SelectSamplesControllerSubmitCommand cmd) {
        if (cmd.next == "Next") {
            cmd.sampleAndSeqType.findAll().each {
                String[] sampleAndSeqType = it.split("-")

                egaSubmissionService.saveSampleSubmissionObject(
                        cmd.submission,
                        Sample.findById(sampleAndSeqType[0] as long),
                        SeqType.findById(sampleAndSeqType[1] as long)
                )
            }
            redirect(action: "editSubmission", params: ['id': cmd.submission.id])
        }
    }

    def sampleInformationUploadForm(UploadFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }
        Spreadsheet spreadsheet = readFile(cmd)
        Map validateRows = egaSubmissionValidationService.validateRows(spreadsheet, cmd.submission)
        Map validateColumns = egaSubmissionValidationService.validateColumns(spreadsheet, [
                INDIVIDUAL,
                SAMPLE_TYPE,
                SEQ_TYPE,
        ])
        if (!validateRows.valid) {
            pushError(validateRows.error, cmd.submission, true)
        } else if (validateColumns.hasError) {
            pushError(validateColumns.error, cmd.submission, true)
        } else if (!egaSubmissionValidationService.validateFileTypeFromInput(spreadsheet)) {
            pushError("Wrong file type detected. Please use only ${EgaSubmissionService.FileType.collect().join(", ")}",
                    cmd.submission, true)
        } else {
            flash.message = new FlashMessage("File was uploaded")
            flash.egaSampleAliases = egaSubmissionFileService.readEgaSampleAliasesFromFile(spreadsheet)
            flash.fastqs = egaSubmissionFileService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.FASTQ)
            flash.bams = egaSubmissionFileService.readBoxesFromFile(spreadsheet, EgaSubmissionService.FileType.BAM)
            redirect(action: "editSubmission", params: ['id': cmd.submission.id])
        }
    }

    def sampleInformationForms(SampleInformationFormsSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.csv == "Download CSV") {
            String content = egaSubmissionFileService.generateCsvFile(cmd.sampleObjectId, cmd.egaSampleAlias, cmd.fileType)
            response.contentType = CSV.mimeType
            response.setHeader("Content-disposition", "filename=sample_information.csv")
            response.outputStream << content.bytes
            return
        }

        if (cmd.next == "Next") {
            Map validateMap = egaSubmissionValidationService.validateSampleInformationFormInput(cmd.sampleObjectId, cmd.egaSampleAlias, cmd.fileType)

            if (validateMap.hasErrors) {
                flash.egaSampleAliases = validateMap.sampleAliases
                flash.fastqs = validateMap.fastqs
                flash.bams = validateMap.bams
                pushErrors(validateMap.errors, cmd.submission)
            } else {
                egaSubmissionService.updateSampleSubmissionObjects(cmd.submission, cmd.sampleObjectId, cmd.egaSampleAlias, cmd.fileType)
                redirect(action: "editSubmission", params: ['id': cmd.submission.id])
            }
        }
    }

    def selectFilesDataFilesForm(SelectFilesDataFilesFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.saveSelection == "Confirm with file selection") {
            saveSelection(cmd)
            return
        }

        if (cmd.download == "Download") {
            String content = egaSubmissionFileService.generateDataFilesCsvFile(cmd.submission)
            response.contentType = CSV.mimeType
            response.setHeader("Content-disposition", "filename=fastq_files_information.csv")
            response.outputStream << content.bytes
            return
        }

        if (cmd.saveAliases == "Save aliases") {
            if (cmd.egaFileAlias.isEmpty()) {
                pushError("No file aliases are configured.", cmd.submission, true)
            } else {
                Map errors = egaSubmissionValidationService.validateAliases(cmd.egaFileAlias)
                if (errors.hasErrors) {
                    pushErrors(errors.errors, cmd.submission)
                } else {
                    egaSubmissionService.updateDataFileSubmissionObjects(cmd.filename, cmd.egaFileAlias, cmd.submission)
                    if (cmd.submission.selectionState != Submission.SelectionState.SELECT_BAM_FILES) {
                        egaSubmissionFileService.generateFilesToUploadFile(cmd.submission)
                    }
                    flash.message = new FlashMessage("SAVED")
                    redirect(action: "editSubmission", params: ['id': cmd.submission.id])
                }
            }
        }
    }

    def saveSelection(SelectFilesDataFilesFormSubmitCommand cmd) {
        if (cmd.selectBox) {
            Submission submission = cmd.submission
            Set<SampleSubmissionObject> fastqSamples = submission.samplesToSubmit.findAll { it.useFastqFile }
            List selection = cmd.selectBox.withIndex().collect { it, i ->
                it ? cmd.egaSampleAlias[i] : null
            }.findAll().unique()

            if (selection.size() != fastqSamples.size()) {
                fastqSamples.each {
                    if (!selection.contains(it.egaAliasName)) {
                        pushError("For ${it.sample.displayName} no file is selected", cmd.submission)
                    }
                }
                redirect(action: "editSubmission", params: ['id': cmd.submission.id])
            } else {
                egaSubmissionService.createDataFileSubmissionObjects(submission, cmd.selectBox, cmd.filename, cmd.egaSampleAlias)
                redirect(action: "editSubmission", params: ['id': cmd.submission.id])
            }
        } else {
            pushError("No files to select are detected.", cmd.submission, true)
        }
    }

    def dataFilesListFileUploadForm(UploadFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.upload == "Upload FASTQ meta file") {
            Spreadsheet spreadsheet = readFile(cmd)
            Map validateColumns = egaSubmissionValidationService.validateColumns(spreadsheet, [
                    RUN,
                    FILENAME,
                    EGA_FILE_ALIAS,
            ])
            if (spreadsheet.dataRows.size() != cmd.submission.dataFilesToSubmit.size()) {
                pushError("Found and expected number of files are different", cmd.submission, true)
            } else if (validateColumns.hasError) {
                pushError(validateColumns.error, cmd.submission, true)
            } else {
                flash.message = new FlashMessage("File was uploaded")
                flash.egaFileAliases = egaSubmissionFileService.readEgaFileAliasesFromFile(spreadsheet, false)
                redirect(action: "editSubmission", params: ['id': cmd.submission.id])
            }
            return
        }

        if (cmd.upload == "Upload BAM meta file") {
            Spreadsheet spreadsheet = readFile(cmd)
            Map validateColumns = egaSubmissionValidationService.validateColumns(spreadsheet, [
                    EGA_SAMPLE_ALIAS,
                    FILENAME,
                    EGA_FILE_ALIAS,
            ])
            if (spreadsheet.dataRows.size() != egaSubmissionService.getBamFilesAndAlias(cmd.submission).size()) {
                pushError("Found and expected number of files are different", cmd.submission, true)
            } else if (validateColumns.hasError) {
                pushError(validateColumns.error, cmd.submission, true)
            } else {
                flash.message = new FlashMessage("File was uploaded")
                flash.egaFileAliases = egaSubmissionFileService.readEgaFileAliasesFromFile(spreadsheet, true)
                redirect(action: "editSubmission", params: ['id': cmd.submission.id])
            }
        }
    }

    Spreadsheet readFile(UploadFormSubmitCommand cmd) {
        final String DELIMITER = ","
        if (cmd.file.empty) {
            pushError("No file selected", cmd.submission, true)
            return
        }
        String content = new String(cmd.file.bytes)
        content = content.replace("\"", "")
        return new Spreadsheet(content, DELIMITER)
    }

    def selectFilesBamFilesForm(SelectFilesBamFilesFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.save == "Save aliases") {
            if (cmd.egaFileAlias) {
                Map errors = egaSubmissionValidationService.validateAliases(cmd.egaFileAlias)
                if (errors.hasErrors) {
                    pushErrors(errors.errors, cmd.submission)
                } else {
                    egaSubmissionService.createBamFileSubmissionObjects(cmd.submission, cmd.fileId, cmd.egaFileAlias, cmd.egaSampleAlias)
                    egaSubmissionFileService.generateFilesToUploadFile(cmd.submission)
                    redirect(action: "editSubmission", params: ['id': cmd.submission.id])
                }
            } else {
                pushError("No files to select are detected.", cmd.submission, true)
            }
            return
        }

        if (cmd.download == "Download") {
            String content = egaSubmissionFileService.generateBamFilesCsvFile(cmd.submission)
            response.contentType = CSV.mimeType
            response.setHeader("Content-disposition", "filename=bam_files_information.csv")
            response.outputStream << content.bytes
        }
    }

    def generateFilesToUploadFile(GenerateFilesToUploadFileSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
        }

        if (cmd.save == "Save FASTQ and BAM") {
            egaSubmissionFileService.generateFilesToUploadFile(cmd.submission)
            redirect(action: "overview")
        }
    }

    private void pushError(String message, Submission submission, boolean redirectFlag = false) {
        flash.message = new FlashMessage(ERROR_TITLE, [message])
        if (redirectFlag) {
            redirect(action: "editSubmission", params: ['id': submission.id])
        }
    }

    private void pushError(FieldError errors, Submission submission) {
        pushError("'${errors.rejectedValue}' is not a valid value for '${errors.field}'. Error code: '${errors.code}'",
                    submission, true)
    }

    private void pushErrors(List errors, Submission submission) {
        flash.message = new FlashMessage(ERROR_TITLE, errors)
        redirect(action: "editSubmission", params: ['id': submission.id])
    }

    JSON updateSubmissionState(UpdateSubmissionStateSubmitCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            egaSubmissionService.updateSubmissionState(cmd.submission, cmd.state)
        }
    }

    JSON dataTableSelectSamples(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()

        List data = []
        List<List> sampleSeqType = egaSubmissionService.getSampleAndSeqType(project)

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
