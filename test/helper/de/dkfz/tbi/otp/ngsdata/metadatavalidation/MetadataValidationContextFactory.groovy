package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problems

class MetadataValidationContextFactory {

    static MetadataValidationContext createContext(String document, Map properties = [:]) {
        assert !properties.containsKey('document')
        return createContext(properties + [document: document])
    }

    static MetadataValidationContext createContext(Map properties = [:]) {
        return new MetadataValidationContext(
                properties.metadataFile ?: new File(new File(properties.testDirectory ?: TestCase.uniqueNonExistentPath, "run${HelperUtils.uniqueString}"), 'metadata_fastq.tsv'),
                properties.metadataFileMd5sum ?: HelperUtils.randomMd5sum,
                properties.spreadsheet ?: new Spreadsheet(properties.document ?: 'I am header!\nI am data!'),
                properties.problems ?: new Problems(),
                properties.directoryStructure ?: [:] as DirectoryStructure)
    }
}
