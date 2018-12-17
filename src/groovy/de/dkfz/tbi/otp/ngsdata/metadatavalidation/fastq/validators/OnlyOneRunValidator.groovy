package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesOnGpcfMidTerm
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level

@Component
class OnlyOneRunValidator implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return Collections.emptyList()
    }

    @Override
    void validate(MetadataValidationContext context) {
        List<Cell> runCells = context.spreadsheet.dataRows.collect { it.getCell(context.spreadsheet.getColumn(MetaDataColumn.RUN_ID.name())) }
        List<String> runNames = runCells*.text.unique()
        List<Cell> ilseCells = context.spreadsheet.dataRows.collect { it.getCell(context.spreadsheet.getColumn(MetaDataColumn.ILSE_NO.name())) }
        List<String> ilsen = ilseCells*.text.unique()

        if (runNames.size() > 1 &&
                !(ilsen.size() == 1 && ilsen != [""] && ilsen != [null] &&
                        context.directoryStructure instanceof DataFilesOnGpcfMidTerm)
        ) {
            context.addProblem(runCells as Set, Level.WARNING,
                    "Metadata file contains data from more than one run.")
        }
    }
}
