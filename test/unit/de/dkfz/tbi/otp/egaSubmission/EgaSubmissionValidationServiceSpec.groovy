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

import grails.test.mixin.Mock
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

@Mock([
        AbstractMergedBamFile,
        BamFileSubmissionObject,
        DataFile,
        DataFileSubmissionObject,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleSubmissionObject,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        EgaSubmission,
])
class EgaSubmissionValidationServiceSpec extends Specification implements EgaSubmissionFactory {

    private EgaSubmissionValidationService egaSubmissionValidationService = new EgaSubmissionValidationService()

    void "test validate rows"() {
        given:
        EgaSubmission submission = createSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject2)
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = ["abc", "dfg"]
        List<EgaSubmissionService.FileType> fileTypes = [EgaSubmissionService.FileType.FASTQ, EgaSubmissionService.FileType.BAM]
        egaSubmissionValidationService.egaSubmissionFileService = new EgaSubmissionFileService()
        String content = egaSubmissionValidationService.egaSubmissionFileService.generateCsvFile(sampleObjectId, egaSampleAlias, fileTypes)
        Spreadsheet spreadsheet = new Spreadsheet(content, Spreadsheet.Delimiter.COMMA)

        expect:
        egaSubmissionValidationService.validateRows(spreadsheet, submission).valid
    }

    void "test validate rows with less rows"() {
        given:
        EgaSubmission submission = createSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        submission.addToSamplesToSubmit(createSampleSubmissionObject())
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String]
        List<String> egaSampleAlias = ["abc"]
        List<EgaSubmissionService.FileType> fileTypes = [EgaSubmissionService.FileType.FASTQ]
        egaSubmissionValidationService.egaSubmissionFileService = new EgaSubmissionFileService()
        String content = egaSubmissionValidationService.egaSubmissionFileService.generateCsvFile(sampleObjectId, egaSampleAlias, fileTypes)
        Spreadsheet spreadsheet = new Spreadsheet(content, Spreadsheet.Delimiter.COMMA)

        expect:
        !egaSubmissionValidationService.validateRows(spreadsheet, submission).valid
        egaSubmissionValidationService.validateRows(spreadsheet, submission).error == "There are less rows in the file as samples where selected"
    }

    void "test validate rows with wrong samples"() {
        given:
        EgaSubmission submission = createSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        List<String> sampleObjectId = [createSampleSubmissionObject().id as String]
        List<String> egaSampleAlias = ["abc"]
        List<EgaSubmissionService.FileType> fileTypes = [EgaSubmissionService.FileType.FASTQ]
        egaSubmissionValidationService.egaSubmissionFileService = new EgaSubmissionFileService()
        String content = egaSubmissionValidationService.egaSubmissionFileService.generateCsvFile(sampleObjectId, egaSampleAlias, fileTypes)
        Spreadsheet spreadsheet = new Spreadsheet(content, Spreadsheet.Delimiter.COMMA)

        expect:
        !egaSubmissionValidationService.validateRows(spreadsheet, submission).valid
        egaSubmissionValidationService.validateRows(spreadsheet, submission).error == "Found and expected samples are different"
    }

    void "test getting identifier key from sample submission object"() {
        given:
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()
        List<String> identifier = [sampleSubmissionObject.sample.individual.displayName,
                sampleSubmissionObject.sample.sampleType.displayName,
                sampleSubmissionObject.seqType.toString()]

        expect:
        identifier == egaSubmissionValidationService.getIdentifierKeyFromSampleSubmissionObject(sampleSubmissionObject)
    }

    @Unroll
    void "test validate sample information form input"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject(
                egaAliasName: "testAlias",
        )
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        List<String> sampleObjectIds = ["${sampleSubmissionObject1.id}", "${sampleSubmissionObject2.id}"]

        when:
        Map map = egaSubmissionValidationService.validateSampleInformationFormInput(sampleObjectIds, aliases, fileType)

        then:
        map.hasErrors == hasErrors
        !map.errors ?: map.errors.contains(errorMessages)

        where:
        aliases            | fileType                                                                 || hasErrors | errorMessages
        ["a", "b"]         | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || false     | ""
        ["a", "a"]         | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "Not all aliases are unique."
        ["a", ""]          | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "For some samples no alias is configured."
        ["a", "testAlias"] | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "Alias testAlias already exist."
        ["a", "b"]         | [EgaSubmissionService.FileType.BAM, null]                                || true      | "For some samples files types are not selected."
    }

    @Unroll
    void "test validate file type from input"() {
        given:
        List<String> sampleObjectId = [createSampleSubmissionObject().id as String]
        List<String> egaSampleAlias = ["abc"]
        List<EgaSubmissionService.FileType> fileTypes = [EgaSubmissionService.FileType.FASTQ]
        egaSubmissionValidationService.egaSubmissionFileService = new EgaSubmissionFileService()
        String content = egaSubmissionValidationService.egaSubmissionFileService.generateCsvFile(sampleObjectId, egaSampleAlias, fileTypes).replace("FASTQ", fileType)
        Spreadsheet spreadsheet = new Spreadsheet(content, Spreadsheet.Delimiter.COMMA)

        expect:
        egaSubmissionValidationService.validateFileTypeFromInput(spreadsheet) == result

        where:
        fileType | result
        "FASTQ"  | true
        "WRONG"  | false
    }
}
