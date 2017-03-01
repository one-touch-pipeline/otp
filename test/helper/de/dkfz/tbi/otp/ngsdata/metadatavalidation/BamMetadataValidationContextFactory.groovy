package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problems


class BamMetadataValidationContextFactory {

    static BamMetadataValidationContext createContext(String document, Map properties = [:]) {
        assert !properties.containsKey('document')
        return createContext(properties + [document: document])
    }

    static BamMetadataValidationContext createContext(Map properties = [:]) {
        return new BamMetadataValidationContext(
                properties.metadataFile ?: new File(new File(properties.testDirectory ?: TestCase.uniqueNonExistentPath, "run${HelperUtils.uniqueString}"), 'metadata_bam.tsv'),
                properties.metadataFileMd5sum ?: HelperUtils.randomMd5sum,
                properties.spreadsheet ?: new Spreadsheet(properties.document ?: 'I am header!\nI am data!'),
                properties.problems ?: new Problems(),
                properties.content ?: ''.bytes
        )

    }
}
