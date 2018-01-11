package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class IlseNumberValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The ILSe number is valid.",
                "All rows have the same value in the column '${ILSE_NO.name()}'.",
                "The ILSe number appears in the path of the metadata file.",
                "The ILSe is not registered yet in OTP.",
        ]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        return false
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [ILSE_NO.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        if (allValueTuples) {
            Map<String, List<ValueTuple>> allIlseNumbers = allValueTuples.groupBy { it.getValue(ILSE_NO.name()) }
            if (allIlseNumbers.size() != 1) {
                context.addProblem((Set) allValueTuples.cells.sum(), Level.WARNING, "There are multiple ILSe numbers in the metadata file.")
            }
            allIlseNumbers.each { String ilseNo, List<ValueTuple> valueTuplesOfIlseNo ->
                ValueTuple tuple = CollectionUtils.exactlyOneElement(valueTuplesOfIlseNo)
                if (ilseNo != "") {
                    if (!ilseNo.isInteger()) {
                        context.addProblem(tuple.cells, Level.ERROR, "The ILSe number '${ilseNo}' is not an integer.", "At least one ILSe number is not an integer.")
                    } else if ((ilseNo as int) < 1000 || (ilseNo as int) > 999999) {
                        context.addProblem(tuple.cells, Level.WARNING, "The ILSe number '${ilseNo}' is out of range [1000..999999].", "At least one ILSe number is out of range [1000..999999].")
                    } else if (IlseSubmission.findByIlseNumber(ilseNo as int)) {
                        context.addProblem(tuple.cells, Level.WARNING, "The ILSe number '${ilseNo}' already exists.", "At least one ILSe number already exists.")
                    }
                    if (!context.metadataFile.toString().contains(ilseNo)) {
                        context.addProblem(tuple.cells, Level.WARNING, "The metadata file path '${context.metadataFile}' does not contain the ILSe number '${ilseNo}'.","At least one metadata file path does not contain the ILSe number.")
                    }
                }
            }
        }

    }

}
