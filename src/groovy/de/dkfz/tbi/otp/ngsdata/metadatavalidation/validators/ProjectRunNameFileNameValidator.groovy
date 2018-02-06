package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class ProjectRunNameFileNameValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Override
    Collection<String> getDescriptions() {
        return ["The filename must be unique in combination with run and project"]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [FASTQ_FILE.name(), RUN_ID.name(), SAMPLE_ID.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            validateValueTuple(context, valueTuple)
        }
    }

    void validateValueTuple(MetadataValidationContext context, ValueTuple valueTuple) {
        String sampleId = valueTuple.getValue(SAMPLE_ID.name())
        String runId = valueTuple.getValue(RUN_ID.name())
        String fileName = valueTuple.getValue(FASTQ_FILE.name())

        Project project
        SampleIdentifier sampleIdentifier = SampleIdentifier.findByName(sampleId)
        ParsedSampleIdentifier parsedSampleIdentifier

        if (sampleIdentifier?.project) {
            project = sampleIdentifier.project
        } else {
            parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleId)
            if (parsedSampleIdentifier) {
                project = CollectionUtils.atMostOneElement(Project.findAllByName(parsedSampleIdentifier.projectName))
            }
        }

        if (!project) {
            return
        }


        Integer result = DataFile.createCriteria().count() {
            eq("fileName", fileName)
            seqTrack {
                sample {
                    individual {
                        eq("project", project)
                    }
                }
                run {
                    eq("name", runId)
                }
            }
        }

        if (result > 0) {
            context.addProblem(valueTuple.cells,
                    Level.ERROR, "A file with name '${fileName}' already exists for run '${runId}' and project '${project.name}'","At least one project, run and file combination already exists in OTP")
        }
    }
}
