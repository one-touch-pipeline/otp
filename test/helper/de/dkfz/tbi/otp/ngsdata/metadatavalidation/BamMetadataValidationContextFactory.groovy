package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.*

class BamMetadataValidationContextFactory {

    static BamMetadataValidationContext createContext(String document, Map properties = [:]) {
        assert !properties.containsKey('document')
        return createContext(properties + [document: document])
    }

    static BamMetadataValidationContext createContext(Map properties = [:]) {
        return new BamMetadataValidationContext(
                properties.metadataFile ?: Paths.get(properties.testDirectory ?: TestCase.uniqueNonExistentPath.path, "run${HelperUtils.uniqueString}" as String, 'metadata_fastq.tsv'),
                properties.metadataFileMd5sum ?: HelperUtils.randomMd5sum,
                properties.spreadsheet ?: new Spreadsheet(properties.document ?: 'I am header!\nI am data!', properties.delimiter ?: '\t' as char),
                properties.problems ?: new Problems(),
                properties.content ?: ''.bytes,
                properties.fileSystem ?: FileSystems.default,
        )
    }
}
