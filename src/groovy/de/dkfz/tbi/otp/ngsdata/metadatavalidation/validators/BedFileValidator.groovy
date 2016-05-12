package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Component
class BedFileValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Autowired
    AlignmentPassService alignmentPassService


    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' and the library layout is '${SeqType.LIBRARYLAYOUT_PAIRED}', a BED file should be configured in OTP to be used for the sample and library preparation kit."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE.name(), LIBRARY_LAYOUT.name(), LIB_PREP_KIT.name(), SAMPLE_ID.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        return false
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            validateValueTuple(context, valueTuple)
        }
    }

    void validateValueTuple(MetadataValidationContext context, ValueTuple valueTuple) {
        if (valueTuple.getValue(SEQUENCING_TYPE.name()) != SeqTypeNames.EXOME.seqTypeName || valueTuple.getValue(LIBRARY_LAYOUT.name()) != SeqType.LIBRARYLAYOUT_PAIRED) {
            return
        }

        String libraryPreparationKitName = valueTuple.getValue(LIB_PREP_KIT.name())
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(libraryPreparationKitName)
        if (!libraryPreparationKit) {
            return
        }

        String sampleId = valueTuple.getValue(SAMPLE_ID.name())

        Project project
        SampleType sampleType

        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
        if (sampleIdentifier) {
            project = sampleIdentifier.project
            sampleType = sampleIdentifier.sampleType

        } else {
            ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleId)
            if (!parsedSampleIdentifier) {
                return
            }

            project = CollectionUtils.atMostOneElement(Project.findAllByName(parsedSampleIdentifier.projectName))
            if (!project) {
                return
            }

            sampleType = CollectionUtils.atMostOneElement(SampleType.findAllByName(parsedSampleIdentifier.sampleTypeDbName))
            if (!sampleType) {
                return
            }
        }
        if (project.alignmentDeciderBeanName == 'noAlignmentDecider') {
            return
        }

        ReferenceGenome referenceGenome = ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
                project,
                SeqType.exomePairedSeqType,
                sampleType,
        )?.referenceGenome
        if (!referenceGenome) {
            return
        }

        BedFile bedFile = CollectionUtils.atMostOneElement(
                BedFile.findAllWhere(
                        libraryPreparationKit: libraryPreparationKit,
                        referenceGenome: referenceGenome,
                )
        )
        if (!bedFile) {
            context.addProblem(valueTuple.cells, Level.WARNING, "No BED file is configured for sample '${sampleId}' (reference genome '${referenceGenome.name}') with library preparation kit '${libraryPreparationKitName}'.")
        }
    }
}
