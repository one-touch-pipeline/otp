package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MetadataValidationContext extends AbstractMetadataValidationContext {

    final DirectoryStructure directoryStructure

    private MetadataValidationContext(File metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, DirectoryStructure directoryStructure, byte[] content) {
        super(metadataFile, metadataFileMd5sum, spreadsheet, problems, content)
        this.directoryStructure = directoryStructure
    }

    static MetadataValidationContext createFromFile(File metadataFile, DirectoryStructure directoryStructure) {

        Map parametersForFile = readAndCheckFile(metadataFile, { Row row ->
            !row.getCellByColumnTitle(FASTQ_FILE.name())?.text?.startsWith('Undetermined') ||
                    !row.getCellByColumnTitle(SAMPLE_ID.name())?.text?.startsWith('Undetermined')
        })

        return new MetadataValidationContext(metadataFile, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, parametersForFile.problems, directoryStructure, parametersForFile.bytes)
    }

    List<String> getSummary() {
        return getProblems()*.type.flatten().unique()
    }
}
