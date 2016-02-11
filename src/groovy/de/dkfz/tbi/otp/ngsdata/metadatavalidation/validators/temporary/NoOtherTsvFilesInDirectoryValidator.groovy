package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.temporary

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.Level

@Component
class NoOtherTsvFilesInDirectoryValidator implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The directory containing the metadata file contains no other *fastq*.tsv or *align*.tsv file. (This restriction is planned to be removed in the future.)']
    }

    @Override
    void validate(MetadataValidationContext context) {
        if (!context.metadataFile.name.contains('fastq')) {
            context.addProblem(Collections.emptySet(), Level.ERROR, "The file name of '${context.metadataFile}' does not contain 'fastq'.")
        } else {
            Collection<File> files = context.metadataFile.parentFile.listFiles().findAll {
                it.name.endsWith('.tsv') && (it.name.contains('fastq') || it.name.contains('align'))
            }
            assert files.contains(context.metadataFile)
            if (files.size() > 1) {
                context.addProblem(Collections.emptySet(), Level.WARNING, "Directory '${context.metadataFile.parentFile}' contains multiple files which would be imported:\n'${files*.name.join("'\n'")}'")
            }
        }
    }
}
