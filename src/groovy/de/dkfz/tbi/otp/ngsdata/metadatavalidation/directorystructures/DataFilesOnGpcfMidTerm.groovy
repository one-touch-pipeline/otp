package de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import java.util.regex.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*


@Component
class DataFilesOnGpcfMidTerm implements DirectoryStructure {

    @Override
    String getDescription() {
        return 'data files on GPCF ILSe MidTerm'
    }

    @Override
    List<String> getColumnTitles() {
        return [
                FASTQ_FILE.name(),
                RUN_ID.name(),
        ]
    }

    @Override
    File getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
        String fileName = valueTuple.getValue(FASTQ_FILE.name())
        String runId = valueTuple.getValue(RUN_ID.name())
        Matcher matcher = fileName =~ /^(.*)_R[12]\.fastq\.gz$/
        if (!OtpPath.isValidPathComponent(fileName) || !OtpPath.isValidPathComponent(runId) || !matcher || !OtpPath.isValidPathComponent(matcher.group(1))) {
            context.addProblem(valueTuple.cells, Level.ERROR, "Cannot construct a valid GPCF midterm storage path from run name '${runId}' and filename '${fileName}'.")
            return null
        } else {
            String dir = matcher.group(1)
            return new File("${context.metadataFile.parentFile}/${runId}/${dir}/fastq/${fileName}")
        }
    }
}
