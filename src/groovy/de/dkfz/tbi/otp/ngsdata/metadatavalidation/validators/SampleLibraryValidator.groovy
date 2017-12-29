package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SampleLibraryValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    final String LIB = "lib"

    @Override
    Collection<String> getDescriptions() {
        return ["If '${LIB}' is contained in the ${SAMPLE_ID} then the library should be given."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_ID.name(), CUSTOMER_LIBRARY.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {

        if (columnTitle == SAMPLE_ID.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        } else {
            return true
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String sample = valueTuple.getValue(SAMPLE_ID.name())
            if (sample.toLowerCase(Locale.ENGLISH).contains(LIB) && !valueTuple.getValue(CUSTOMER_LIBRARY.name())) {
                context.addProblem(valueTuple.cells, Level.WARNING, "For sample '${sample}' which contains 'lib', there should be a value in the ${CUSTOMER_LIBRARY} column.", "For samples which contain 'lib', there should be a value in the ${CUSTOMER_LIBRARY} column.")
            }
        }
    }
}
