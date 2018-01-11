package de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class DataFilesWithAbsolutePath implements DirectoryStructure {

    @Override
    String getDescription() {
        return 'data files given by absolute path'
    }

    @Override
    List<String> getColumnTitles() {
        return [FASTQ_FILE.name()]
    }

    @Override
    Path getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
        String fileName = valueTuple.getValue(FASTQ_FILE.name())
        if (OtpPath.isValidAbsolutePath(fileName)) {
            return fileSystem.getPath(fileName)
        } else {
            context.addProblem(valueTuple.cells, Level.ERROR, "'${fileName}' is not a valid absolute path.", "At least one file path is not a valid absolute path.")
            return null
        }
    }
}
