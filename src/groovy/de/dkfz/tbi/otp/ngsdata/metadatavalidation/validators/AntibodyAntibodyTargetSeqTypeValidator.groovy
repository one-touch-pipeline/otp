package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetadataImportService
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY_TARGET
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_BASED_LIBRARY

@Component
class AntibodyAntibodyTargetSeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "Antibody target must be given if the sequencing type is '${SeqTypeNames.CHIP_SEQ.seqTypeName}'.",
                "Antibody target and antibody should not be given if the sequencing type is not '${SeqTypeNames.CHIP_SEQ.seqTypeName}'.",

        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [ANTIBODY_TARGET.name(), ANTIBODY.name(), SEQUENCING_TYPE.name(), TAGMENTATION_BASED_LIBRARY.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == ANTIBODY_TARGET.name() || columnTitle == ANTIBODY.name() || columnTitle == TAGMENTATION_BASED_LIBRARY.name()) {
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String antibodyTarget = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ""
            String antibody = valueTuple.getValue(ANTIBODY.name()) ?: ""
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)

            if ((antibodyTarget || antibody) && seqType != SeqTypeNames.CHIP_SEQ.seqTypeName) {
                context.addProblem(valueTuple.cells, Level.WARNING, "Antibody target ('${antibodyTarget}') and/or antibody ('${antibody}') are/is provided although data is no ChIP seq data. OTP will ignore the values.")
            }
            if (seqType == SeqTypeNames.CHIP_SEQ.seqTypeName && !antibodyTarget) {
                context.addProblem(valueTuple.cells, Level.ERROR, "Antibody target is not provided although data is ChIP seq data.")
            }
        }
    }
}
