package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*

@Component
class SampleTypeIndividualValidator extends ValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The sample as combination of individual and sample type is registered in OTP.']
    }

    @Override
    List<String> getColumnTitles(BamMetadataValidationContext context) {
        return [INDIVIDUAL.name(), SAMPLE_TYPE.name()]
    }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String individual = it.getValue(INDIVIDUAL.name())
            String sampleType = it.getValue(SAMPLE_TYPE.name())
            if (!Sample.findByIndividualAndSampleType(
                    Individual.findByPidOrMockPidOrMockFullName(individual, individual, individual), SampleType.findByName(sampleType))) {
                context.addProblem(it.cells, Level.ERROR,
                        "The sample as combination of the individual '${individual}' and the sample type '${sampleType}' is not registered in OTP.", "At least one sample as combination of the individual and the sample type is not registered in OTP.")
            }
        }
    }
}
