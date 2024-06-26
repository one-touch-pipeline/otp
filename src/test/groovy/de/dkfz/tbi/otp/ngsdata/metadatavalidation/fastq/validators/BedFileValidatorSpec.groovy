/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.parser.TestSampleIdentifierParser
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersionSelectorService
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame

class BedFileValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BedFile,
                Individual,
                LibraryPreparationKit,
                Project,
                ProcessingOption,
                Sample,
                SampleIdentifier,
                SampleType,
                SeqType,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
        ]
    }

    static final List<String> HEADER = [
            MetaDataColumn.SEQUENCING_TYPE,
            MetaDataColumn.SEQUENCING_READ_TYPE,
            MetaDataColumn.LIB_PREP_KIT,
            MetaDataColumn.SAMPLE_NAME,
            MetaDataColumn.PROJECT,
    ]*.name().asImmutable()

    static final String PARSE_PROJECT = 'PROJECT'
    static final String PARSE_INDIVIDUAL = 'INDIVIDUAL'
    static final String PARSE_SAMPLE_TYPE = 'sampletype'

    static final String SAMPLE_NAME = 'sampleName'
    static final String PARSE_SAMPLE_NAME = "${PARSE_PROJECT}#${PARSE_INDIVIDUAL}#${PARSE_SAMPLE_TYPE}"
    static final String PARSE_SAMPLE_NAME_NEW_PROJECT = "new#${PARSE_INDIVIDUAL}#${PARSE_SAMPLE_TYPE}"
    static final String PARSE_SAMPLE_NAME_NEW_SAMPLE_TYPE = "${PARSE_PROJECT}#${PARSE_INDIVIDUAL}#new"
    static final String LIB_PREP_KIT_NAME = 'libPrepKitName'

    @Unroll
    void 'validate with BedFileValidator and different settings, expecting error: #expectError'() {
        SeqType seqType = DomainFactory.createExomeSeqType()
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit(name: LIB_PREP_KIT_NAME)
        Project project
        if (createSample) {
            project = DomainFactory.createSampleIdentifier(name: sampleName).project
        } else {
            project = createProject(name: PARSE_PROJECT)
            createSampleType(name: PARSE_SAMPLE_TYPE)
        }
        project.sampleIdentifierParserBeanName = SampleIdentifierParserBeanName.DEEP
        project.save(flush: true)

        ReferenceGenome referenceGenome = createReferenceGenome()
        if (connectProjectToReferenceGenome) {
            DomainFactory.createReferenceGenomeProjectSeqType([
                    referenceGenome: referenceGenome,
                    project        : project,
                    seqType        : seqType,
            ])
        }
        if (createBedFile) {
            DomainFactory.createBedFile(
                    libraryPreparationKit: libraryPreparationKit,
                    referenceGenome: referenceGenome,
            )
        }

        MetadataValidationContext context = MetadataValidationContextFactory.createContext([
                [MetaDataColumn.SEQUENCING_TYPE, MetaDataColumn.SEQUENCING_READ_TYPE, MetaDataColumn.LIB_PREP_KIT, MetaDataColumn.SAMPLE_NAME, MetaDataColumn.PROJECT]*.name(),
                [seqTypeName, libraryLayout, libPrepKitName, sampleName, sampleName == PARSE_SAMPLE_NAME_NEW_PROJECT ? 'noProject' : project.name],
        ]*.join('\t').join('\n'))

        Collection<Problem> expectedProblems = expectError ? [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.WARNING,
                        "No BED file is configured for sample '${sampleName}' (reference genome '${referenceGenome.name}') with library preparation kit '${libraryPreparationKit.name}'.", "No BED file is configured for at least on sample."),
        ] : []

        when:
        new BedFileValidator(
                libraryPreparationKitService: new LibraryPreparationKitService(),
                validatorHelperService: new ValidatorHelperService(seqTypeService: new SeqTypeService()),
                sampleIdentifierService: [
                        getSampleIdentifierParser: { SampleIdentifierParserBeanName sampleIdentifierParserBeanName ->
                            new TestSampleIdentifierParser()
                        }
                ] as SampleIdentifierService,
                workflowVersionSelectorService: Mock(WorkflowVersionSelectorService) {
                    hasAlignmentConfigForProjectAndSeqType(_, _) >> (doesAlign ? [new WorkflowVersion()] : [])
                },
        ).validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)

        where:
        seqTypeName                    | libraryLayout             | libPrepKitName    | sampleName                        | createSample | doesAlign | connectProjectToReferenceGenome | createBedFile || expectError
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | true      | true                            | true          || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | true      | true                            | false         || true
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | true      | false                           | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | false     | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | ''                | SAMPLE_NAME                       | true         | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | 'unknown'         | SAMPLE_NAME                       | true         | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | ''                        | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | 'nonPaired'               | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | true      | true                            | false         || false
        ''                             | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | true      | true                            | false         || false
        'nonExome'                     | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | ''                                | false        | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | 'unknown'                         | false        | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | true      | true                            | true          || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | true      | true                            | false         || true
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | true      | false                           | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | false     | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | ''                | PARSE_SAMPLE_NAME                 | false        | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | 'unknown'         | PARSE_SAMPLE_NAME                 | false        | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | ''                        | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | 'nonPaired'               | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | true      | true                            | false         || false
        ''                             | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | true      | true                            | false         || false
        'nonExome'                     | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME_NEW_PROJECT     | false        | true      | true                            | false         || false
        SeqTypeNames.EXOME.seqTypeName | SequencingReadType.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME_NEW_SAMPLE_TYPE | false        | true      | true                            | false         || false
    }

    @Unroll
    void 'validate, when column #missingHeader missing, then add no problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext([
                HEADER - missingHeader,
                ['?', '?', '?'],
        ]*.join('\t').join('\n'))

        when:
        new BedFileValidator().validate(context)

        then:
        context.problems.empty

        where:
        missingHeader << HEADER
    }
}
