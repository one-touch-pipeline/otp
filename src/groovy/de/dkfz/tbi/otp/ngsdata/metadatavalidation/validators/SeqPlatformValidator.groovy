package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqPlatformService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqPlatformValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqPlatformService seqPlatformService

    @Override
    Collection<String> getDescriptions() {
        return ['The combination of instrument platform, instrument model and sequencing kit is registered in the OTP database.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [INSTRUMENT_PLATFORM.name(),
                INSTRUMENT_MODEL.name(),
                SEQUENCING_KIT.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == SEQUENCING_KIT.name()) {
            optionalColumnMissing(context, columnTitle)
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        allValueTuples.each {
            if (!seqPlatformService.findSeqPlatform(it.getValue(INSTRUMENT_PLATFORM.name()), it.getValue(INSTRUMENT_MODEL.name()), it.getValue(SEQUENCING_KIT.name()) ?: null)) {
                context.addProblem(it.cells, Level.ERROR, "The combination of instrument platform '${it.getValue(INSTRUMENT_PLATFORM.name())}', instrument model '${it.getValue(INSTRUMENT_MODEL.name())}' and sequencing kit '${it.getValue(SEQUENCING_KIT.name()) ?: ''}' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database.")
            }
        }
    }
}
