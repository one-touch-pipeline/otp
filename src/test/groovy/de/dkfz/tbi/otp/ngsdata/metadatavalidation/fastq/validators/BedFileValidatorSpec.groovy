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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.util.regex.Matcher

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName.NO_ALIGNMENT
import static de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName.OTP_ALIGNMENT

class BedFileValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                BedFile,
                Individual,
                LibraryPreparationKit,
                Project,
                ProcessingOption,
                Sample,
                SampleIdentifier,
                SampleType,
                SeqType,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
        ]
    }

    static final List<String> HEADER = [
            MetaDataColumn.SEQUENCING_TYPE,
            MetaDataColumn.SEQUENCING_READ_TYPE,
            MetaDataColumn.LIB_PREP_KIT,
            MetaDataColumn.SAMPLE_NAME,
            MetaDataColumn.TAGMENTATION,
            MetaDataColumn.PROJECT,
    ]*.name().asImmutable()

    static final String PARSE_PREFIX = 'PARSE'
    static final String PARSE_PROJECT = 'PROJECT'
    static final String PARSE_INDIVIDUAL = 'INDIVIDUAL'
    static final String PARSE_SAMPLE_TYPE = 'sampletype'

    static final String SAMPLE_NAME = 'sampleName'
    static final String PARSE_SAMPLE_NAME = "${PARSE_PREFIX}_${PARSE_PROJECT}_${PARSE_INDIVIDUAL}_${PARSE_SAMPLE_TYPE}"
    static final String PARSE_SAMPLE_NAME_NEW_PROJECT = "${PARSE_PREFIX}_new_${PARSE_INDIVIDUAL}_${PARSE_SAMPLE_TYPE}"
    static final String PARSE_SAMPLE_NAME_NEW_SAMPLE_TYPE = "${PARSE_PREFIX}_${PARSE_PROJECT}_${PARSE_INDIVIDUAL}_new"
    static final String LIB_PREP_KIT_NAME = 'libPrepKitName'


    @Unroll
    void 'validate with seqType = #seqTypeName, libraryLayout = #libraryLayout, liPrepKit = #libPrepKitName, sampleName = #sampleName, createSample = #createSample, decider = #alignmentDeciderBeanName, connectProjectReferenceGenome = #connectProjectToReferenceGenome, createBedFile = #createBedFile, tagmentationBasedLibrary = #tagmentationBasedLibrary expect error: #expectError'() {
        SeqType seqType = DomainFactory.createExomeSeqType()
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(name: LIB_PREP_KIT_NAME)
        Project project
        if (createSample) {
            project = DomainFactory.createSampleIdentifier(name: sampleName).project
        } else {
            project = DomainFactory.createProject(name: PARSE_PROJECT)
            DomainFactory.createSampleType(name: PARSE_SAMPLE_TYPE)
        }
        project.alignmentDeciderBeanName = alignmentDeciderBeanName
        project.sampleIdentifierParserBeanName = SampleIdentifierParserBeanName.DEEP
        project.save(flush: true)

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
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
                [MetaDataColumn.SEQUENCING_TYPE, MetaDataColumn.SEQUENCING_READ_TYPE, MetaDataColumn.LIB_PREP_KIT, MetaDataColumn.SAMPLE_NAME, MetaDataColumn.TAGMENTATION, MetaDataColumn.PROJECT]*.name(),
                [seqTypeName, libraryLayout, libPrepKitName, sampleName, tagmentationBasedLibrary, sampleName == PARSE_SAMPLE_NAME_NEW_PROJECT ? 'noProject' : project.name],
        ].collect { row ->
            row.join('\t')
        }.join('\n'))

        Collection<Problem> expectedProblems = expectError ? [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "No BED file is configured for sample '${sampleName}' (reference genome '${referenceGenome.name}') with library preparation kit '${libraryPreparationKit.name}'.", "No BED file is configured for at least on sample."),
        ] : []


        when:
        new BedFileValidator(
                libraryPreparationKitService: new LibraryPreparationKitService(),
                sampleIdentifierService: [
                        getSampleIdentifierParser: { SampleIdentifierParserBeanName sampleIdentifierParserBeanName ->
                            new SampleIdentifierParser() {
                                @Override
                                ParsedSampleIdentifier tryParse(String sampleIdentifier) {
                                    Matcher match = sampleIdentifier =~ /${PARSE_PREFIX}_(.*)_(.*)_(.*)/
                                    if (match.matches()) {
                                        return new DefaultParsedSampleIdentifier(match.group(1), match.group(2), match.group(3), sampleIdentifier, null)
                                    } else {
                                        return null
                                    }
                                }

                                @SuppressWarnings("UnusedMethodParameter")
                                @Override
                                boolean tryParsePid(String pid) {
                                    return true
                                }

                                @Override
                                String tryParseSingleCellWellLabel(String sampleIdentifier) {
                                    return null
                                }
                            }
                        }
                ] as SampleIdentifierService,
        ).validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)

        where:
        seqTypeName                    | libraryLayout        | libPrepKitName    | sampleName                        | createSample | alignmentDeciderBeanName | connectProjectToReferenceGenome | createBedFile | tagmentationBasedLibrary || expectError
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | true          | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | ''                       || true
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | 'true'                   || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | false                           | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | NO_ALIGNMENT             | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | ''                | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | 'unknown'         | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | ''                   | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | 'nonPaired'          | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        ''                             | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        'nonExome'                     | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_NAME                       | true         | OTP_ALIGNMENT            | true                            | false         | ''                       || false

        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | ''                                | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | 'unknown'                         | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false

        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | true          | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || true
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | false                           | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | NO_ALIGNMENT             | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | ''                | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | 'unknown'         | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | ''                   | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | 'nonPaired'          | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        ''                             | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        'nonExome'                     | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME                 | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME_NEW_PROJECT     | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_NAME_NEW_SAMPLE_TYPE | false        | OTP_ALIGNMENT            | true                            | false         | ''                       || false
    }

    @Unroll
    void 'validate, when column #missingHeader missing, then add no problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext([
                HEADER - missingHeader,
                ['?', '?', '?'],
        ].collect { row ->
            row.join('\t')
        }.join('\n'))

        when:
        new BedFileValidator().validate(context)

        then:
        context.problems.empty

        where:
        missingHeader << HEADER
    }
}
