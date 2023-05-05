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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Delimiter
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

class EgaSubmissionValidationServiceSpec extends Specification implements EgaSubmissionFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
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
                FastqImportInstance,
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
        ]
    }

    private final EgaSubmissionValidationService egaSubmissionValidationService = new EgaSubmissionValidationService()

    void "test validate rows"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject2)
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String, sampleSubmissionObject2.id as String]
        List<String> egaSampleAlias = ["abc", "dfg"]
        egaSubmissionValidationService.egaSubmissionFileService = new EgaSubmissionFileService()
        String content = egaSubmissionValidationService.egaSubmissionFileService.generateSampleInformationCsvFile(sampleObjectId, egaSampleAlias)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)

        expect:
        egaSubmissionValidationService.validateRows(spreadsheet, submission).valid
    }

    void "test validate rows with less rows"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        submission.addToSamplesToSubmit(createSampleSubmissionObject())
        List<String> sampleObjectId = [sampleSubmissionObject1.id as String]
        List<String> egaSampleAlias = ["abc"]
        egaSubmissionValidationService.egaSubmissionFileService = new EgaSubmissionFileService()
        String content = egaSubmissionValidationService.egaSubmissionFileService.generateSampleInformationCsvFile(sampleObjectId, egaSampleAlias)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)

        expect:
        !egaSubmissionValidationService.validateRows(spreadsheet, submission).valid
        egaSubmissionValidationService.validateRows(spreadsheet, submission).error == "There are less rows in the file as samples where selected"
    }

    void "test validate rows with wrong samples"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject1)
        List<String> sampleObjectId = [createSampleSubmissionObject().id as String]
        List<String> egaSampleAlias = ["abc"]
        egaSubmissionValidationService.egaSubmissionFileService = new EgaSubmissionFileService()
        String content = egaSubmissionValidationService.egaSubmissionFileService.generateSampleInformationCsvFile(sampleObjectId, egaSampleAlias)
        Spreadsheet spreadsheet = new Spreadsheet(content, Delimiter.COMMA)

        expect:
        !egaSubmissionValidationService.validateRows(spreadsheet, submission).valid
        egaSubmissionValidationService.validateRows(spreadsheet, submission).error == "Found and expected samples are different"
    }

    void "test getting identifier key from sample submission object"() {
        given:
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()
        EgaMapKey identifier = new EgaMapKey(sampleSubmissionObject)

        expect:
        identifier == egaSubmissionValidationService.getIdentifierKeyFromSampleSubmissionObject(sampleSubmissionObject)
    }

    @Unroll
    void "validateAliases, when '#name', then hasError: #hasError and errorMessage contains '#message'"() {
        given:
        createDataFileSubmissionObject([
                egaAliasName: 'existingDataFileAlias',
        ])
        createBamFileSubmissionObject([
                egaAliasName: 'existingBamFileAlias',
        ])

        when:
        Map map = egaSubmissionValidationService.validateAliases(aliases)

        then:
        map.hasErrors == hasError
        if (hasError) {
            assert map.errors.size() == 1
            assert map.errors.first().contains(message)
        } else {
            assert map.errors.empty
        }

        where:
        name                               | aliases                        || hasError | message
        'all fine'                         | ['a', 'b']                     || false    | ''
        'alias missing'                    | ['a', '', '']                  || true     | 'For some samples no alias is configured'
        'alias not unique'                 | ['a', 'a', 'b', 'b']           || true     | 'The following aliases are not unique'
        'alias already exist for datafile' | ['a', 'existingDataFileAlias'] || true     | 'The following aliases already exist'
        'alias already exist for bam file' | ['a', 'existingBamFileAlias']  || true     | 'The following aliases already exist'
    }

    @Unroll
    void "validateSampleInformationFormInput, when aliases are: #aliases, then hasErrors is #hasErrors and message contains #errorMessages"() {
        given:
        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject(
                egaAliasName: "testAlias",
        )
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject()
        List<String> sampleObjectIds = ["${sampleSubmissionObject1.id}", "${sampleSubmissionObject2.id}"]

        EgaSubmission egaSubmission = createEgaSubmission([
                samplesToSubmit: [
                        sampleSubmissionObject1,
                        sampleSubmissionObject2,
                ].toSet()
        ])

        when:
        Map map = egaSubmissionValidationService.validateSampleInformationFormInput(egaSubmission, sampleObjectIds, aliases, fileType)

        then:
        map.hasErrors == hasErrors
        if (hasErrors) {
            assert map.errors.size() == 1
            assert map.errors.first().contains(errorMessages)
        } else {
            assert map.errors.empty
        }

        where:
        aliases            | fileType                                                                 || hasErrors | errorMessages
        ["a", "b"]         | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || false     | ""
        ["a", "a"]         | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "Not all aliases are unique:"
        ["a", ""]          | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "no alias is set."
        ["a", "testAlias"] | [EgaSubmissionService.FileType.BAM, EgaSubmissionService.FileType.FASTQ] || true      | "The following aliases are already registered in the database: testAlias"
        ["a", "b"]         | [EgaSubmissionService.FileType.BAM, null]                                || true      | "no file type is selected."
    }
}
