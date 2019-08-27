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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.FILE_TYPE

@CompileStatic
@Transactional
class EgaSubmissionValidationService {

    EgaSubmissionFileService egaSubmissionFileService

    Map validateColumns(Spreadsheet spreadsheet, List<EgaSubmissionFileService.EgaColumnName> requiredColumnNames) {
        boolean hasError = false
        String error = ""
        requiredColumnNames.each {
            if (!spreadsheet.getColumn(it.value)) {
                hasError = true
                error = "The column ${it.value} does not exist"
            }
        }
        return [
                "hasError": hasError,
                "error": error,
        ]
    }

    Map validateRows(Spreadsheet spreadsheet, EgaSubmission submission) {
        boolean valid = false
        String error = ""

        if (spreadsheet.dataRows.size() == submission.samplesToSubmit.size()) {
            List<EgaMapKey> samplesFromDB = submission.samplesToSubmit.collect {
                getIdentifierKeyFromSampleSubmissionObject(it)
            }.sort()

            List<EgaMapKey> samplesFromFile = spreadsheet.dataRows.collect {
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

    @CompileDynamic
    Map validateSampleInformationFormInput(List<String> sampleObjectId, List<String> alias, List<EgaSubmissionService.FileType> fileType) {
        List<String> errors = []
        boolean hasErrors = false
        Map<List<String>, String> sampleAliases =  [:]
        Map<List<String>, Boolean> fastqs = [:]
        Map<List<String>, Boolean> bams = [:]

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
            sampleAliases.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.get(sampleObjectId[i] as Long)), it)
        }
        sampleObjectId.eachWithIndex { it, i ->
            fastqs.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.get(it as Long)), fileType[i] == EgaSubmissionService.FileType.FASTQ)
            bams.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.get(it as Long)), fileType[i] == EgaSubmissionService.FileType.BAM)
        }

        return [
                hasErrors: hasErrors,
                errors: errors.unique(),
                fastqs: fastqs,
                bams: bams,
                sampleAliases: sampleAliases,
        ]
    }

    @CompileDynamic
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
            return EgaSubmissionService.FileType.values()*.toString().containsAll(fileTypes)
        }
        return false
    }

    EgaMapKey getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject sampleSubmissionObject) {
        return new EgaMapKey(sampleSubmissionObject)
    }
}
