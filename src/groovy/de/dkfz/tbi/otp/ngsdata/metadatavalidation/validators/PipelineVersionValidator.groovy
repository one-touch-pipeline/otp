package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.SoftwareToolService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PIPELINE_VERSION

@Component
class PipelineVersionValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The pipeline version is registered in the OTP database or empty.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return PIPELINE_VERSION.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String pipelineVersion, Set<Cell> cells) {
        if (pipelineVersion && !SoftwareToolService.getBaseCallingTool(pipelineVersion)) {
            context.addProblem(cells, Level.ERROR, "Pipeline version '${pipelineVersion}' is not registered in the OTP database.")
        }
    }
}
