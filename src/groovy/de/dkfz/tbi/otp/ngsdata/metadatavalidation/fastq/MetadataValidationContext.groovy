package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_ID

class MetadataValidationContext extends AbstractMetadataValidationContext {

    final DirectoryStructure directoryStructure

    private MetadataValidationContext(Path metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, DirectoryStructure directoryStructure, byte[] content) {
        super(metadataFile, metadataFileMd5sum, spreadsheet, problems, content)
        this.directoryStructure = directoryStructure
    }

    static MetadataValidationContext createFromFile(Path metadataFile, DirectoryStructure directoryStructure) {

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
