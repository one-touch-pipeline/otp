package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE

@Component
class DataFilesInSameDirectory implements DirectoryStructure {

    @Override
    String getDescription() {
        return 'data files in same directory as metadata file'
    }

    @Override
    List<String> getColumnTitles() {
        return [FASTQ_FILE.name()]
    }

    @Override
    Path getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
        String fileName = valueTuple.getValue(FASTQ_FILE.name())
        if (OtpPath.isValidPathComponent(fileName)) {
            return context.metadataFile.resolveSibling(fileName)
        } else {
            context.addProblem(valueTuple.cells, Level.ERROR, "'${fileName}' is not a valid file name.", "At least one file name is not a valid file name.")
            return null
        }
    }
}
