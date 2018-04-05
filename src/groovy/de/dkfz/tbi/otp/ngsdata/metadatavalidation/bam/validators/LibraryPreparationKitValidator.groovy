package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.LIBRARY_PREPARATION_KIT
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SEQUENCING_TYPE


@Component
class LibraryPreparationKitValidator extends ValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The library preparation kit is registered in OTP"]
    }

    @Override
    List<String> getColumnTitles(BamMetadataValidationContext context) {
        return [LIBRARY_PREPARATION_KIT.name(), SEQUENCING_TYPE.name()]
    }

    @Override
    boolean columnMissing(BamMetadataValidationContext context, String columnTitle) {
        return true
    }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {

        valueTuples.each {
            String libraryPreparationKit = it.getValue(LIBRARY_PREPARATION_KIT.name())
            String seqType = it.getValue(SEQUENCING_TYPE.name())

            if (libraryPreparationKit) {
                if (!LibraryPreparationKit.findByName(libraryPreparationKit)) {
                    context.addProblem(it.cells, Level.ERROR, "The ${LIBRARY_PREPARATION_KIT} '${libraryPreparationKit}' is not registered in OTP.", "At least one ${LIBRARY_PREPARATION_KIT} is not registered in OTP.")
                }
            } else {
                if (seqType.toUpperCase() == SeqTypeNames.EXOME.seqTypeName) {
                    context.addProblem(it.cells, Level.WARNING, "The ${SEQUENCING_TYPE} is '${seqType}' but no ${LIBRARY_PREPARATION_KIT} is given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.", "If the ${SEQUENCING_TYPE} is '${SeqTypeNames.EXOME.seqTypeName}' the ${LIBRARY_PREPARATION_KIT} should be given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.")
                }
            }
        }
    }
}
