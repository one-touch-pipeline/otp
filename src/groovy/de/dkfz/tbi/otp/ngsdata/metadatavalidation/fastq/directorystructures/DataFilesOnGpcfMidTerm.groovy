package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import java.nio.file.Path
import java.util.regex.Matcher

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID

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
    Path getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
        String fileName = valueTuple.getValue(FASTQ_FILE.name())
        String runId = valueTuple.getValue(RUN_ID.name())
        Matcher matcher = fileName =~ /^(.*)_R[12]\.fastq\.gz$/
        if (!OtpPath.isValidPathComponent(fileName) || !OtpPath.isValidPathComponent(runId) || !matcher || !OtpPath.isValidPathComponent(matcher.group(1))) {
            context.addProblem(valueTuple.cells, Level.ERROR, "Cannot construct a valid GPCF midterm storage path from run name '${runId}' and filename '${fileName}'.", "Cannot construct a valid GPCF midterm storage path for all rows.")
            return null
        } else {
            String dir = matcher.group(1)
            return context.metadataFile.resolveSibling("${runId}/${dir}/fastq/${fileName}")
        }
    }
}
