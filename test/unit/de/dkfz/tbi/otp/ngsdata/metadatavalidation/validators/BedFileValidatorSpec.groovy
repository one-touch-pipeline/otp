package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import java.util.regex.*

import static de.dkfz.tbi.TestCase.*

@Mock([
        BedFile,
        Individual,
        LibraryPreparationKit,
        Project,
        ProjectCategory,
        ProcessingOption,
        Sample,
        SampleIdentifier,
        SampleType,
        SeqType,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        ])
class BedFileValidatorSpec extends Specification {

    static
    final List<String> HEADER = [MetaDataColumn.SEQUENCING_TYPE, MetaDataColumn.LIBRARY_LAYOUT, MetaDataColumn.LIB_PREP_KIT, MetaDataColumn.SAMPLE_ID, MetaDataColumn.TAGMENTATION_BASED_LIBRARY]*.name().asImmutable()

    static final String PARSE_PREFIX = 'PARSE'
    static final String PARSE_PROJECT = 'PROJECT'
    static final String PARSE_INDIVIDUAL = 'INDIVIDUAL'
    static final String PARSE_SAMPLE_TYPE = 'SAMPLETYPE'

    static final String SAMPLE_ID = 'sampleId'
    static final String PARSE_SAMPLE_ID = "${PARSE_PREFIX}_${PARSE_PROJECT}_${PARSE_INDIVIDUAL}_${PARSE_SAMPLE_TYPE}"
    static final String PARSE_SAMPLE_ID_NEW_PROJECT = "${PARSE_PREFIX}_new_${PARSE_INDIVIDUAL}_${PARSE_SAMPLE_TYPE}"
    static final String PARSE_SAMPLE_ID_NEW_SAMPLE_TYPE = "${PARSE_PREFIX}_${PARSE_PROJECT}_${PARSE_INDIVIDUAL}_new"
    static final String LIB_PREP_KIT_NAME = 'libPrepKitName'


    @Unroll
    void 'validate with seqType = #seqTypeName, libraryLayout = #libraryLayout, liPrepKit = #libPrepKitName, sampleId = #sampleId, createSample = #createSample, decider = #alignmentDeciderBeanName, connectProjectReferenceGenome = #connectProjectToReferenceGenome, createBedFile = #createBedFile, tagmentationBasedLibrary = #tagmentationBasedLibrary expect error: #expectError'() {
        SeqType seqType = DomainFactory.createExomeSeqType()
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(name: LIB_PREP_KIT_NAME)

        Project project
        if (createSample) {
            project = DomainFactory.createSampleIdentifier(name: sampleId).project
        } else {
            project = DomainFactory.createProject(name: PARSE_PROJECT)
            DomainFactory.createSampleType(name: PARSE_SAMPLE_TYPE)
        }
        project.alignmentDeciderBeanName = alignmentDeciderBeanName

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
                [MetaDataColumn.SEQUENCING_TYPE.name(), MetaDataColumn.LIBRARY_LAYOUT.name(), MetaDataColumn.LIB_PREP_KIT.name(), MetaDataColumn.SAMPLE_ID.name(), MetaDataColumn.TAGMENTATION_BASED_LIBRARY.name()],
                [seqTypeName, libraryLayout, libPrepKitName, sampleId, tagmentationBasedLibrary],
        ].collect { row ->
            row.join('\t')
        }.join('\n'))

        Collection<Problem> expectedProblems = expectError ? [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "No BED file is configured for sample '${sampleId}' (reference genome '${referenceGenome.name}') with library preparation kit '${libraryPreparationKit.name}'.", "No BED file is configured for at least on sample."),
        ] : []


        when:
        new BedFileValidator(
                libraryPreparationKitService: new LibraryPreparationKitService(),
                sampleIdentifierService: [
                        getSampleIdentifierParsers: { ->
                            [new SampleIdentifierParser() {

                                @Override
                                ParsedSampleIdentifier tryParse(String sampleIdentifier) {
                                    Matcher match = sampleIdentifier =~ /${PARSE_PREFIX}_(.*)_(.*)_(.*)/
                                    if (match.matches()) {
                                        return new DefaultParsedSampleIdentifier(match.group(1), match.group(2), match.group(3), sampleIdentifier)
                                    } else {
                                        return null
                                    }
                                }

                                @SuppressWarnings("UnusedMethodParameter")
                                @Override
                                boolean tryParsePid(String pid) {
                                    return true
                                }

                                @SuppressWarnings("UnusedMethodParameter")
                                @Override
                                boolean isForProject(String projectName) {
                                    return true
                                }
                            }]
                        }
                ] as SampleIdentifierService,
        ).validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)

        where:
        seqTypeName                    | libraryLayout        | libPrepKitName    | sampleId                        | createSample | alignmentDeciderBeanName     | connectProjectToReferenceGenome | createBedFile | tagmentationBasedLibrary || expectError
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | true          | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || true
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | 'true'                   || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | false                           | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'noAlignmentDecider'         | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | ''                | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | 'unknown'         | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | ''                   | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | 'nonPaired'          | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        ''                             | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        'nonExome'                     | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | SAMPLE_ID                       | true         | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false

        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | ''                              | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | 'unknown'                       | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false

        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | true          | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || true
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | false                           | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'noAlignmentDecider'         | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | ''                | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | 'unknown'         | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | ''                   | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | 'nonPaired'          | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        ''                             | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        'nonExome'                     | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID                 | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID_NEW_PROJECT     | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
        SeqTypeNames.EXOME.seqTypeName | LibraryLayout.PAIRED | LIB_PREP_KIT_NAME | PARSE_SAMPLE_ID_NEW_SAMPLE_TYPE | false        | 'defaultOtpAlignmentDecider' | true                            | false         | ''                       || false
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
