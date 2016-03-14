package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator
import org.springframework.stereotype.Component


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
