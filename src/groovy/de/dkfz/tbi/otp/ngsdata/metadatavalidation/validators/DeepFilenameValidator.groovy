package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*


@Component
class DeepFilenameValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Override
    Collection<String> getDescriptions() {
        return ["In the DEEP project, FASTQ filenames should start with the sample ID."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [FASTQ_FILE.name(), SAMPLE_ID.name()]
    }

    @Override
    boolean columnsMissing(MetadataValidationContext context, Collection<String> columnTitles) {
        return false
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String sampleId = valueTuple.getValue(SAMPLE_ID.name())
            if((atMostOneElement(SampleIdentifier.findAllByName(sampleId))?.project?.name ?:
                    sampleIdentifierService.parseSampleIdentifier(sampleId)?.projectName == "DEEP") &&
                    !valueTuple.getValue(FASTQ_FILE.name()).startsWith(valueTuple.getValue(SAMPLE_ID.name()))) {
                context.addProblem(valueTuple.cells, Level.WARNING, "In the DEEP project, name of FASTQ file should start with sample ID.")
            }
        }
    }
}
