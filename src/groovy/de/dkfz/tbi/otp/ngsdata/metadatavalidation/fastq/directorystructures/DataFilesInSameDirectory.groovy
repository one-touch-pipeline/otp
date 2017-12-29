package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

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
    File getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
        String fileName = valueTuple.getValue(FASTQ_FILE.name())
        if (OtpPath.isValidPathComponent(fileName)) {
            return new File(context.metadataFile.parentFile, fileName)
        } else {
            context.addProblem(valueTuple.cells, Level.ERROR, "'${fileName}' is not a valid file name.", "At least one file name is not a valid file name.")
            return null
        }
    }
}
