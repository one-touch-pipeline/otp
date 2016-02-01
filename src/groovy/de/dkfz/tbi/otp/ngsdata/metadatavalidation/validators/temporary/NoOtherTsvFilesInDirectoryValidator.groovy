package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.temporary

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.Level

@Component
class NoOtherTsvFilesInDirectoryValidator implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The directory containing the metadata file contains only one *.tsv file. (This restriction is planned to be removed in the future.)']
    }

    @Override
    void validate(MetadataValidationContext context) {
        if (context.metadataFile.parentFile.listFiles().find { it.name.endsWith('.tsv') && it != context.metadataFile }) {
            context.addProblem(Collections.emptySet(), Level.ERROR, "Directory '${context.metadataFile.parentFile}' contains other *.tsv files than '${context.metadataFile.name}'. This is currently not supported.")
        }
    }
}
