package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

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
            context.addProblem(cells, Level.ERROR, "Pipeline version '${pipelineVersion}' is not registered in the OTP database.", "At least one pipeline version is not registered in the OTP database.")
        }
    }
}
