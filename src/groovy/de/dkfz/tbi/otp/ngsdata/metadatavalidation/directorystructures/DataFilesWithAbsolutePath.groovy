package de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

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
    File getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
        String fileName = valueTuple.getValue(FASTQ_FILE.name())
        if (OtpPath.isValidAbsolutePath(fileName)) {
            return new File(fileName)
        } else {
            context.addProblem(valueTuple.cells, Level.ERROR, "'${fileName}' is not a valid absolute path.", "At least one file path is not a valid absolute path.")
            return null
        }
    }
}
