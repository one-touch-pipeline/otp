package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

class BamMetadataValidationContext extends AbstractMetadataValidationContext{

    private BamMetadataValidationContext(File metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, byte[] content) {
        super(metadataFile, metadataFileMd5sum, spreadsheet, problems, content)
    }

    static BamMetadataValidationContext createFromFile(File metadataFile) {

        Map parametersForFile = readAndCheckFile(metadataFile)

        return new BamMetadataValidationContext(metadataFile, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, parametersForFile.problems, parametersForFile.bytes)
    }
}
