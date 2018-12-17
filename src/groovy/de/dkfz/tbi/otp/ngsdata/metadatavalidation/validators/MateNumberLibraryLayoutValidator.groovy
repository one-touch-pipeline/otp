package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component
import org.springsource.loaded.*

import de.dkfz.tbi.otp.ngsdata.LibraryLayout
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

@Component
class MateNumberLibraryLayoutValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The mate number is inside the valid range for the library layout.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [MetaDataColumn.MATE.name(),
                MetaDataColumn.LIBRARY_LAYOUT.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == MetaDataColumn.LIBRARY_LAYOUT.name()) {
            mandatoryColumnMissing(context, columnTitle)
        }
        return false
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String libraryLayoutName = it.getValue(MetaDataColumn.LIBRARY_LAYOUT.name())
            String mateNumber = it.getValue(MetaDataColumn.MATE.name())
            if (libraryLayoutName && mateNumber && mateNumber.isInteger()) {
                LibraryLayout libraryLayout = LibraryLayout.values().find {
                    it.toString() == libraryLayoutName
                }
                if (!libraryLayout) {
                    context.addProblem(it.cells, Level.WARNING, "OTP does not know the library layout '${libraryLayoutName}' and can therefore not validate the mate number.","OTP does not know at least one library layout and can therefore not validate the mate number.")
                } else {
                    int mate = mateNumber.toInteger()
                    int mateCount = libraryLayout.mateCount
                    if (mate > mateCount) {
                        context.addProblem(it.cells, Level.ERROR, "The mate number '${mateNumber}' is bigger then the allowed value for the library layout '${libraryLayoutName}' of '${mateCount}'.","At least one mate number is bigger then the allowed value for the library layout.")
                    }
                }
            }
        }
    }
}
