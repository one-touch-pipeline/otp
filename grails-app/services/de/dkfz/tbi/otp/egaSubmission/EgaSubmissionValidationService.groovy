package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

class EgaSubmissionValidationService {

    EgaSubmissionFileService egaSubmissionFileService

    Map validateColumns(Spreadsheet spreadsheet, List<EgaSubmissionFileService.EgaColumnName> requiredColumnNames) {
        boolean hasError = false
        String error = ""
        requiredColumnNames.each {
            if (!spreadsheet.getColumn(it.value)) {
                hasError = true
                error = "The column ${it.value} does not exist"
                return
            }
        }
        return [
                "hasError": hasError,
                "error": error,
        ]
    }

    Map validateRows(Spreadsheet spreadsheet, Submission submission) {
        boolean valid = false
        String error = ""

        if (spreadsheet.dataRows.size() == submission.samplesToSubmit.size()) {
            List samplesFromDB = submission.samplesToSubmit.collect {
                getIdentifierKeyFromSampleSubmissionObject(it)
            }.sort()

            List samplesFromFile = spreadsheet.dataRows.collect {
                egaSubmissionFileService.getIdentifierKey(it)
            }.sort()

            valid = samplesFromDB == samplesFromFile
            if (!valid) {
                error = "Found and expected samples are different"
            }
        } else {
            error = "There are ${spreadsheet.dataRows.size() > submission.samplesToSubmit.size() ? "more" : "less"} " +
                    "rows in the file as samples where selected"
        }

        return [
                "valid": valid,
                "error": error,
        ]
    }

    Map validateSampleInformationFormInput(List<String> sampleObjectId, List<String> alias, List<EgaSubmissionService.FileType> fileType) {
        List<String> errors = []
        boolean hasErrors = false
        Map<String, String> sampleAliases =  [:]
        Map<String, Boolean> fastqs = [:]
        Map<String, Boolean> bams = [:]

        if (fileType.findAll().size() != sampleObjectId.size()) {
            hasErrors = true
            errors += "For some samples files types are not selected."
        }
        if (alias.unique(false).size() != sampleObjectId.size()) {
            hasErrors = true
            errors += "Not all aliases are unique."
        }
        alias.sort(false).each {
            if (it == "" ) {
                hasErrors = true
                errors += "For some samples no alias is configured."
            }
            if (SampleSubmissionObject.findByEgaAliasName(it)) {
                hasErrors = true
                errors += "Alias ${it} already exist.".toString()
            }
        }

        alias.eachWithIndex { it, i ->
            sampleAliases.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.findById(sampleObjectId[i] as Long)), it)
        }
        sampleObjectId.eachWithIndex { it, i ->
            fastqs.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.findById(it as Long)), fileType[i] == EgaSubmissionService.FileType.FASTQ)
            bams.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.findById(it as Long)), fileType[i] == EgaSubmissionService.FileType.BAM)
        }

        return [
                hasErrors: hasErrors,
                errors: errors.unique(),
                fastqs: fastqs,
                bams: bams,
                sampleAliases: sampleAliases,
        ]
    }

    Map validateAliases (List<String> alias) {
        List<String> errors = []
        boolean hasErrors = false

        if (alias.size() != alias.unique(false).size()) {
            hasErrors = true
            errors += "Not all aliases are unique."
        }

        alias.sort(false).each {
            if (it == "" ) {
                hasErrors = true
                errors += "For some samples no alias is configured."
            }
            if (DataFileSubmissionObject.findByEgaAliasName(it) || BamFileSubmissionObject.findByEgaAliasName(it)) {
                hasErrors = true
                errors += "Alias ${it} already exist.".toString()
            }
        }

        return [
                hasErrors: hasErrors,
                errors: errors.unique(),
        ]
    }

    boolean validateFileTypeFromInput(Spreadsheet spreadsheet) {
        List<String> fileTypes = spreadsheet.dataRows.collect {
            it.getCellByColumnTitle(FILE_TYPE.value).text.toUpperCase()
        }

        if (!(fileTypes.unique().size() > 2)) {
            return EgaSubmissionService.FileType.collect { it.toString() }.containsAll(fileTypes)
        }
        return false
    }

    String getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject sampleSubmissionObject) {
        return sampleSubmissionObject.sample.individual.displayName +
                sampleSubmissionObject.sample.sampleType.displayName +
                sampleSubmissionObject.seqType.toString()
    }
}
