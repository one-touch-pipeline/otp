package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DataFilesWithAbsolutePath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

class RunNameInMetadataPathValidator implements MetadataValidator{

    @Override
    Collection<String> getDescriptions() {
        return ["If the metadata file contains exactly one run and it is not imported from midterm or use absolute paths, "+
                "the path of the metadata file should contain the run name."]
    }

    @Override
    void validate(MetadataValidationContext context) {
        List<Cell> runCells = context.spreadsheet.dataRows.collect {it.getCell(context.spreadsheet.getColumn(MetaDataColumn.RUN_ID.name()))}
        List<String> runNames = runCells.text.unique()

        if (runNames.size() == 1 &&
                !(context.directoryStructure instanceof DataFilesOnGpcfMidTerm) &&
                !(context.directoryStructure instanceof DataFilesWithAbsolutePath) &&
                !context.metadataFile.absolutePath.contains(runNames.first()) ) {
            context.addProblem(runCells as Set, Level.WARNING,
                    "The path of the metadata file should contain the run name.")
        }
    }
}
