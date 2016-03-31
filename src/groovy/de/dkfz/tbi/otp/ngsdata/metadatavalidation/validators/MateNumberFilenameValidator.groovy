package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.LibraryLayout
import de.dkfz.tbi.otp.ngsdata.MetaDataService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class MateNumberFilenameValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["If a mate number can be parsed from the filename, it is consistent with the entry in the '${MATE}' column."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [FASTQ_FILE.name(), LIBRARY_LAYOUT.name(), MATE.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == FASTQ_FILE.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        } else {
            return true
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        Integer fastqFileColumnIndex = context.spreadsheet.header.getCellByColumnTitle(FASTQ_FILE.name()).columnIndex
        Integer libraryLayoutColumnIndex = context.spreadsheet.header.getCellByColumnTitle(LIBRARY_LAYOUT.name())?.columnIndex
        Integer mateColumnIndex = context.spreadsheet.header.getCellByColumnTitle(MATE.name())?.columnIndex
        valueTuples.each { ValueTuple valueTuple ->
            String libraryLayout = valueTuple.getValue(LIBRARY_LAYOUT.name())
            String mateNumber = valueTuple.getValue(MATE.name())
            String fileName = valueTuple.getValue(FASTQ_FILE.name())
            Integer mateCount = LibraryLayout.values().find { it.name() == libraryLayout }?.mateCount
            try {
                int fileNameMateNumber = MetaDataService.findOutMateNumber(fileName)
                if (mateCount == 1) {
                    if (!(fileNameMateNumber == 1)) {
                        context.addProblem(valueTuple.cells.findAll({
                            it.columnIndex == fastqFileColumnIndex ||
                                    it.columnIndex == libraryLayoutColumnIndex
                        }), Level.WARNING, "The mate number '${fileNameMateNumber}' parsed from filename '${fileName}' is not viable with library layout '${libraryLayout}'." +
                                " If you ignore this warning, OTP will ignore the mate number parsed from the filename.")
                    }
                } else {
                    if (mateNumber != null && fileNameMateNumber.toString() != mateNumber) {
                        context.addProblem(valueTuple.cells.findAll({
                            it.columnIndex == fastqFileColumnIndex ||
                            it.columnIndex == mateColumnIndex
                        }), Level.WARNING, "The value '${mateNumber}' in the ${MATE} column is different from the mate number '${fileNameMateNumber}' parsed from the filename '${fileName}'. " +
                                "If you ignore this warning, OTP will use the mate number parsed from the filename and ignore the value in the ${MATE} column.")

                    }
                }
            } catch (RuntimeException e) {
                if (libraryLayout != null && mateCount != 1) {
                    if (e.message == "cannot find mateNumber for ${fileName}".toString()) {
                        context.addProblem(valueTuple.cells.findAll({
                            it.columnIndex == fastqFileColumnIndex ||
                            it.columnIndex == libraryLayoutColumnIndex
                        }), Level.ERROR, "Cannot parse mate number from filename '${fileName}' or imply from the library layout '${libraryLayout}'.")
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
