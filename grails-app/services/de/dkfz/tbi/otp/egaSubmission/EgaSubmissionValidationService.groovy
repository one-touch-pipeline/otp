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
                "error"   : error,
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

    Map validateSampleInformationFormInput(EgaSubmission egaSubmission, List<String> sampleObjectId, List<String> alias,
                                           List<EgaSubmissionService.FileType> fileType) {
        List<CharSequence> errors = []
        Map<EgaMapKey, String> sampleAliases = [:]
        Map<EgaMapKey, Boolean> fastqs = [:]
        Map<EgaMapKey, Boolean> bams = [:]

        Map<Long, SampleSubmissionObject> sampleSubmissionObjectMap = egaSubmission.samplesToSubmit.collectEntries {
            [(it.id): it]
        }

        int size = sampleSubmissionObjectMap.size()
        Map<SampleSubmissionObject, String> sampleSubmissionObjectAliasMap = new HashMap<SampleSubmissionObject, String>(size)

        for (int i = 0; i < size; i++) {
            SampleSubmissionObject sampleSubmissionObject = sampleSubmissionObjectMap[(sampleObjectId[i] as Long)]
            EgaMapKey key = getIdentifierKeyFromSampleSubmissionObject(sampleSubmissionObject)
            String aliasValue = alias[i]
            EgaSubmissionService.FileType fileTypeValue = fileType[i]

            if (aliasValue) {
                sampleSubmissionObjectAliasMap[sampleSubmissionObject] = aliasValue
                sampleAliases.put(key, aliasValue)
            } else {
                errors << "For Sample ${sampleSubmissionObject} no alias is set."
            }

            if (fileType[i]) {
                fastqs.put(key, fileTypeValue == EgaSubmissionService.FileType.FASTQ)
                bams.put(key, fileTypeValue == EgaSubmissionService.FileType.BAM)
            } else {
                errors << "For Sample ${sampleSubmissionObject} no file type is selected."
            }
        }

        Map<String, Map<SampleSubmissionObject, String>> aliasWithDuplicates = sampleSubmissionObjectAliasMap.groupBy {
            it.value
        }.findAll {
            it.value.size() > 1
        }
        if (aliasWithDuplicates) {
            List<CharSequence> message = []
            message << "Not all aliases are unique: "

            aliasWithDuplicates.each {
                message << "${it.key}: ${it.value.keySet()*.toString().join(', ')}"
            }
            errors << message.join('\n    ')
        }

        List<SampleSubmissionObject> aliasesInDatabase = findAllAliasesInDatabase(sampleAliases.values().toList())
        if (aliasesInDatabase) {
            errors << "The following aliases are already registered in the database: ${aliasesInDatabase*.egaAliasName.sort().join(', ')}"
        }

        return [
                hasErrors    : !errors.empty,
                errors       : errors.unique().sort(),
                fastqs       : fastqs,
                bams         : bams,
                sampleAliases: sampleAliases,
        ]
    }

    @CompileDynamic
    private List<SampleSubmissionObject> findAllAliasesInDatabase(List<String> aliases) {
        return aliases ? SampleSubmissionObject.findAllByEgaAliasNameInList(aliases) : []
    }

    Map validateAliases(List<String> alias) {
        List<CharSequence> errors = []

        List<String> duplicateList = alias.groupBy {
            it
        }.findAll {
            it.value.size() > 1
        }*.key.sort()

        if (duplicateList.contains(null) || duplicateList.contains('')) {
            errors += "For some samples no alias is configured."
            duplicateList.removeAll([null, ''])
        }
        if (duplicateList) {
            errors += "The following aliases are not unique: ${duplicateList.join(', ')}"
        }

        List<String> existingAliases = findAlreadyUsedAliases(alias)
        if (existingAliases) {
            errors += "The following aliases already exist: ${existingAliases.join(', ')}"
        }

        return [
                hasErrors: !errors.empty,
                errors   : errors.unique(),
        ]
    }

    @CompileDynamic
    private List<String> findAlreadyUsedAliases(List<String> alias) {
        List<String> existingAliases = []
        existingAliases.addAll(DataFileSubmissionObject.findAllByEgaAliasNameInList(alias)*.egaAliasName)
        existingAliases.addAll(BamFileSubmissionObject.findAllByEgaAliasNameInList(alias)*.egaAliasName)
        return existingAliases.unique().sort()
    }

    EgaMapKey getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject sampleSubmissionObject) {
        return new EgaMapKey(sampleSubmissionObject)
    }
}
