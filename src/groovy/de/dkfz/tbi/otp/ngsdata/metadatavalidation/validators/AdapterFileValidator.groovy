package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*


@Component
class AdapterFileValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    AdapterFileService adapterFileService

    @Override
    Collection<String> getDescriptions() {
        return [
                "All adapter file names must be registered in OTP.",
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return ADAPTER_FILE.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        if(value && !adapterFileService.findByFileName(value)) {
            context.addProblem(cells, Level.ERROR, "Adapter file name '${value}' is not registered in OTP.")
        }
    }
}
