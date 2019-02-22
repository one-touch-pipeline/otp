/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.egaSubmission

import grails.converters.JSON
import org.springframework.validation.FieldError

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.administration.Document.FormatType.CSV
import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

class EgaSubmissionController implements CheckAndCall, SubmitCommands {

    static allowedMethods = [
            overview          : "GET",
            newSubmission     : "GET",
            selectSamples     : "GET",
            sampleInformation : "GET",
            selectFastqFiles  : "GET",
            selectBamFiles    : "GET",
            sampleMetadata    : "GET",

            newSubmissionForm           : "POST",
            selectSamplesForm           : "POST",
            sampleInformationUploadForm : "POST",
            sampleInformationForms      : "POST",
            selectFilesDataFilesForm    : "POST",
            dataFilesListFileUploadForm : "POST",
            bamFilesListFileUploadForm  : "POST",
            selectFilesBamFilesForm     : "POST",
            sampleMetadataForm          : "POST",
    ]

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

        List<EgaSubmission> submissions = EgaSubmission.findAllByProject(project).sort { it.submissionName.toLowerCase() }

        return [
                projects         : projects,
                project          : project,
                submissions      : submissions,
                submissionStates : EgaSubmission.State,
        ]
    }

    def editSubmission() {
        EgaSubmission submission = EgaSubmission.get(params.id)
        if (submission.state != EgaSubmission.State.SELECTION) {
            redirect(action: "overview")
            return
        }
        switch (submission.selectionState) {
            case EgaSubmission.SelectionState.SELECT_SAMPLES:
                render(view: "selectSamples", model: selectSamples(submission))
                break
            case EgaSubmission.SelectionState.SAMPLE_INFORMATION:
                render(view: "sampleInformation", model: sampleInformation(submission))
                break
            case EgaSubmission.SelectionState.SELECT_FASTQ_FILES:
                render(view: "selectFastqFiles", model: selectFastqFiles(submission))
                break
            case EgaSubmission.SelectionState.SELECT_BAM_FILES:
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
                studyTypes      : EgaSubmission.StudyType,
                defaultStudyType: EgaSubmission.StudyType.CANCER_GENOMICS,
                cmd             : flash.cmd,
        ]
    }

    Map selectSamples(EgaSubmission submission) {
        return [
                submissionId      : submission.id,
                project           : submission.project,
                seqTypes          : egaSubmissionService.seqTypeByProject(submission.project),
                samplesWithSeqType: flash.samplesWithSeqType ?: [],
        ]
    }

    Map sampleInformation(EgaSubmission submission) {
        Map<String, String> egaSampleAliases = flash.egaSampleAliases ?: [:]
        Map<SampleSubmissionObject, Boolean> existingFastqs = egaSubmissionService.checkFastqFiles(submission)
        Map<SampleSubmissionObject, Boolean> existingBams = egaSubmissionService.checkBamFiles(submission)
        Map<List<String>, Boolean> selectedFastqs = new HashMap<>(existingBams.size())
        Map<List<String>, Boolean> selectedBams = new HashMap<>(existingBams.size())

        if (flash.fastqs && flash.bams) {
            existingFastqs.each { key, value ->
                List<String> newKey = getIdentifierKeyFromSampleSubmissionObject(key)
                selectedFastqs.put(newKey, flash.fastqs.get(newKey) && value)
            }
            existingBams.each { key, value ->
                List<String> newKey = getIdentifierKeyFromSampleSubmissionObject(key)
                selectedBams.put(newKey, flash.bams.get(newKey) && value)
            }
        } else {
            existingBams.each { key, value ->
                List<String> newKey = getIdentifierKeyFromSampleSubmissionObject(key)
                selectedFastqs.put(newKey, !value)
                selectedBams.put(newKey, value)
            }
        }

        return [
                submissionId           : submission.id,
                sampleSubmissionObjects: submission.samplesToSubmit.sort { getIdentifierKeyFromSampleSubmissionObject(it) },
                egaSampleAliases       : egaSampleAliases,
                existingFastqs         : existingFastqs,
                existingBams           : existingBams,
                selectedFastqs         : selectedFastqs,
                selectedBams           : selectedBams,
        ]
    }

    Map selectFastqFiles(EgaSubmission submission) {
        List dataFilesAndAliasList = egaSubmissionService.getDataFilesAndAlias(submission)
        Map egaFileAliases = flash.egaFileAliases ?: egaSubmissionService.generateDefaultEgaAliasesForDataFiles(dataFilesAndAliasList)

        return [
                submission: submission,
                dataFileList: dataFilesAndAliasList,
                dataFileSubmissionObject: submission.dataFilesToSubmit,
                egaFileAliases: egaFileAliases,
                hasDataFiles: !submission.dataFilesToSubmit.empty,
                dataFilesHasFileAliases: !submission.dataFilesToSubmit*.egaAliasName.findAll().isEmpty(),
        ]
    }

    Map selectBamFiles(EgaSubmission submission) {
        List bamFilesAndAliasList = egaSubmissionService.getBamFilesAndAlias(submission)
        Map egaFileAliases = flash.egaFileAliases ?: egaSubmissionService.generateDefaultEgaAliasesForBamFiles(bamFilesAndAliasList)

        return [
                submission: submission,
                bamFileList: bamFilesAndAliasList,
                bamFileSubmissionObject: submission.bamFilesToSubmit,
                egaFileAliases: egaFileAliases,
                bamFilesHasFileAliases: !submission.bamFilesToSubmit*.egaAliasName.findAll().isEmpty(),
                hasFiles: !submission.bamFilesToSubmit.empty,
        ]
    }

    Map sampleMetadata() {
        return [
                submission: EgaSubmission.get(params.id)
        ]
    }

    Map experimentalMetadata() {
        EgaSubmission submission = EgaSubmission.get(params.id)

        return [
                submission: submission,
                metadata  : egaSubmissionService.getExperimentalMetadata(submission),
        ]
    }

    def helpPage() { }

    def newSubmissionForm(NewSubmissionControllerSubmitCommand cmd) {
        if (cmd.submit == "Submit") {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(ERROR_TITLE, cmd.errors)
                flash.cmd = cmd
                redirect(action: "newSubmission", params: ['id': cmd.project.id])
                return
            }
            EgaSubmission submission = egaSubmissionService.createSubmission([
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
        if (cmd.next == "Confirm") {
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
        if (spreadsheet) {
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

        if (cmd.next == "Confirm") {
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

        if (cmd.back == "Back to selection") {
            flash.samplesWithSeqType = egaSubmissionService.deleteSampleSubmissionObjects(cmd.submission)
            redirect(action: "editSubmission", params: ['id': cmd.submission.id])
        }
    }

    def selectFilesDataFilesForm(SelectFilesDataFilesFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.saveSelection == "Confirm with file selection") {
            saveDataFileSelection(cmd)
            return
        }

        if (cmd.download == "Download") {
            String content = egaSubmissionFileService.generateDataFilesCsvFile(cmd.submission)
            response.contentType = CSV.mimeType
            response.setHeader("Content-disposition", "filename=fastq_files_information.csv")
            response.outputStream << content.bytes
            return
        }

        if (cmd.saveAliases == "Confirm with aliases") {
            if (cmd.egaFileAlias.isEmpty()) {
                pushError("No file aliases are configured.", cmd.submission, true)
            } else {
                Map errors = egaSubmissionValidationService.validateAliases(cmd.egaFileAlias)
                if (errors.hasErrors) {
                    pushErrors(errors.errors, cmd.submission)
                    Map egaFileAliases = [:]
                    cmd.egaFileAlias.eachWithIndex { it, i ->
                        DataFile dataFile = cmd.submission.dataFilesToSubmit.find {
                            it.dataFile.fileName == cmd.filename[i] &&
                            it.dataFile.run.name == cmd.runName[i]
                        }.dataFile
                        egaFileAliases.put(dataFile.fileName + dataFile.run, it)
                    }
                    flash.egaFileAliases = egaFileAliases
                } else {
                    egaSubmissionService.updateDataFileSubmissionObjects(cmd.filename, cmd.egaFileAlias, cmd.submission)
                    if (cmd.submission.selectionState != EgaSubmission.SelectionState.SELECT_BAM_FILES) {
                        egaSubmissionFileService.generateFilesToUploadFile(cmd.submission)
                    }
                    flash.message = new FlashMessage("SAVED")
                    redirect(action: "editSubmission", params: ['id': cmd.submission.id])
                }
            }
        }
    }

    def saveDataFileSelection(SelectFilesDataFilesFormSubmitCommand cmd) {
        if (cmd.selectBox) {
            EgaSubmission submission = cmd.submission
            Set<SampleSubmissionObject> fastqSamples = submission.samplesToSubmit.findAll { it.useFastqFile }
            List selection = cmd.selectBox.withIndex().collect { it, i ->
                it ? cmd.egaSampleAlias[i] : null
            }.findAll().unique()

            if (selection.size() != fastqSamples.size()) {
                fastqSamples.each {
                    if (!selection.contains(it.egaAliasName)) {
                        pushError("For previously selected sample ${it.sample.displayName} no file is selected", cmd.submission)
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
            if (spreadsheet) {
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
            }
        }
    }

    def bamFilesListFileUploadForm(UploadFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.upload == "Upload BAM meta file") {
            Spreadsheet spreadsheet = readFile(cmd)
            if (spreadsheet) {
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
    }

    Spreadsheet readFile(UploadFormSubmitCommand cmd) {
        if (cmd.file.empty) {
            pushError("No file selected", cmd.submission, true)
            return
        }
        String content = new String(cmd.file.bytes)
        content = content.replace("\"", "")
        return new Spreadsheet(content, Spreadsheet.Delimiter.COMMA)
    }

    def selectFilesBamFilesForm(SelectFilesBamFilesFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.saveSelection == "Confirm with file selection") {
            egaSubmissionService.createBamFileSubmissionObjects(cmd.submission)
            redirect(action: "editSubmission", params: ['id': cmd.submission.id])
            return
        }

        if (cmd.saveAliases == "Confirm with aliases") {
            if (cmd.egaFileAlias) {
                Map errors = egaSubmissionValidationService.validateAliases(cmd.egaFileAlias)
                if (errors.hasErrors) {
                    pushErrors(errors.errors, cmd.submission)
                    Map egaFileAliases = [:]
                    cmd.egaFileAlias.eachWithIndex { it, i ->
                        egaFileAliases.put(AbstractMergedBamFile.findById(cmd.fileId[i] as long).bamFileName + cmd.egaSampleAlias[i], it)
                    }
                    flash.egaFileAliases = egaFileAliases
                } else {
                    egaSubmissionService.updateBamFileSubmissionObjects(cmd.fileId, cmd.egaFileAlias, cmd.submission)
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

    def sampleMetadataForm(SampleMetadataFormSubmitCommand cmd) {
        if (cmd.hasErrors()) {
            pushError(cmd.errors.fieldError, cmd.submission)
            return
        }

        if (cmd.download == "Download") {
            String content = egaSubmissionFileService.generateSampleMetadataCsvFile(cmd.submission)
            response.contentType = CSV.mimeType
            response.setHeader("Content-disposition", "filename=sample_metadata.csv")
            response.outputStream << content.bytes
        }
    }

    private void pushError(String message, EgaSubmission submission, boolean redirectFlag = false) {
        flash.message = new FlashMessage(ERROR_TITLE, [message])
        if (redirectFlag) {
            redirect(action: "editSubmission", params: ['id': submission.id])
        }
    }

    private void pushError(FieldError errors, EgaSubmission submission) {
        pushError("'${errors.rejectedValue}' is not a valid value for '${errors.field}'. Error code: '${errors.code}'",
                    submission, true)
    }

    private void pushErrors(List errors, EgaSubmission submission) {
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

    List<String> getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject sampleSubmissionObject) {
        return [
                sampleSubmissionObject.sample.individual.displayName,
                sampleSubmissionObject.sample.sampleType.displayName,
                sampleSubmissionObject.seqType.toString(),
        ]
    }
}
