package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CENTER_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ILSE_NO

@Component
class IlseNumberSeqCenterValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["An ILSe number is given if and only if the sequencing center is 'DKFZ'."]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == ILSE_NO.name()) {
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [ILSE_NO.name(), CENTER_NAME.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String seqCenterName = valueTuple.getValue(CENTER_NAME.name())
            String ilseNumber = valueTuple.getValue(ILSE_NO.name())
            if (ilseNumber && seqCenterName != "DKFZ") {
                context.addProblem(valueTuple.cells, Level.WARNING, "ILSe number is available although data was provided by '${seqCenterName}'.")
            }
            if (seqCenterName == "DKFZ" && !ilseNumber) {
                context.addProblem(valueTuple.cells, Level.WARNING, "ILSe number is not available although data was provided by '${seqCenterName}'.")
            }
        }
    }

}
