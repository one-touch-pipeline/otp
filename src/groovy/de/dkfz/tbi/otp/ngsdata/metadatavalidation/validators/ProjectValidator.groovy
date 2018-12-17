package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

@Component
class ProjectValidator extends SingleValueValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The project is registered in OTP or the column ${MetaDataColumn.PROJECT} does not exist."]
    }

    @Override
    String getColumnTitle(AbstractMetadataValidationContext context) {
        return MetaDataColumn.PROJECT.name()
    }

    @Override
    void columnMissing(AbstractMetadataValidationContext context) {
        if (context instanceof BamMetadataValidationContext) {
            mandatoryColumnMissing(context, BamMetadataColumn.PROJECT.name())
        }
    }

    @Override
    void validateValue(AbstractMetadataValidationContext context, String projectName, Set<Cell> cells) {
        if (!(CollectionUtils.atMostOneElement(Project.findAllByNameOrNameInMetadataFiles(projectName, projectName)))) {
            def level
            if (context instanceof BamMetadataValidationContext) {
                level = Level.ERROR
            } else {
                level = Level.WARNING
            }
            context.addProblem(cells, level, "The project '${projectName}' is not registered in OTP.","At least one project is not registered in OTP.")
        }
    }
}
