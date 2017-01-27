package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

@Component
class ProjectValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The project is registered in OTP or the column ${MetaDataColumn.PROJECT} does not exist."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.PROJECT.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {}


    @Override
    void validateValue(MetadataValidationContext context, String projectName, Set<Cell> cells) {
        if (!(CollectionUtils.atMostOneElement(Project.findAllByNameOrNameInMetadataFiles(projectName, projectName)))) {
            context.addProblem(cells, Level.WARNING, "The project '${projectName}' is not registered in OTP.")
        }
    }
}
