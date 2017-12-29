package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Component
class SampleProjectValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Override
    Collection<String> getDescriptions() {
        return ["The value in the column '${PROJECT}' should be consistent with the sample identifier."]
    }
    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_ID.name(), PROJECT.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == PROJECT.name()) {
            optionalColumnMissing(context, columnTitle)
        }
        return false
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String sampleId = it.getValue(SAMPLE_ID.name())
            String project = it.getValue(PROJECT.name())
            SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
            if (sampleIdentifier) {
                if (![sampleIdentifier.project.name, sampleIdentifier.project.nameInMetadataFiles].contains(project)) {
                    context.addProblem(it.cells, Level.WARNING, "Sample identifier '${sampleId}' is already registered in OTP with project '${sampleIdentifier.project.name}', not with project '${project}'. If you ignore this warning, OTP will keep the assignment of the sample identifier to project '${sampleIdentifier.project.name}' and ignore the value '${project}' in the '${PROJECT}' column.", "At least one sample identifier is already registered in OTP but with another project.")
                }
            } else {
                ParsedSampleIdentifier parsedIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleId)
                if (parsedIdentifier && project != parsedIdentifier.projectName &&
                        project != atMostOneElement(Project.findAllByName(parsedIdentifier.projectName))?.nameInMetadataFiles) {
                    context.addProblem(it.cells, Level.WARNING, "Sample identifier '${sampleId}' looks like it belongs to project '${parsedIdentifier.projectName}', not to project '${project}'. If you ignore this warning, OTP will assign the sample to project '${parsedIdentifier.projectName}' and ignore the value '${project}' in the '${PROJECT}' column.", "At least one sample identifier looks like is belongs to another project than in the '${PROJECT}' column.")
                }
            }
        }
    }
}
