package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*

@Component
class SampleValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Override
    Collection<String> getDescriptions() {
        return [
                'The sample identifier must be registered in OTP or parsable using a pattern known to OTP.',
                'If the sample identifier can be parsed and is already registered in the OTP database, the parsed values should match those in the database.',
                'All sample identifiers in the metadata file should belong to the same project.',
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_ID.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        Collection<String> missingIdentifiers = []
        Map<String, Collection<ValueTuple>> byProjectName = valueTuples.groupBy {
            String sampleId = it.getValue(SAMPLE_ID.name())
            ParsedSampleIdentifier parsedIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleId)
            SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
            if (!parsedIdentifier && !sampleIdentifier) {
                context.addProblem(it.cells, Level.ERROR, "Sample identifier '${sampleId}' is neither registered in OTP nor matches a pattern known to OTP.")
                missingIdentifiers.add(sampleId)
            }
            if (parsedIdentifier && !sampleIdentifier) {
                if (!atMostOneElement(Project.findAllByName(parsedIdentifier.projectName))) {
                    context.addProblem(it.cells, Level.ERROR, "Sample identifier '${sampleId}' is not registered in OTP. It looks like it belongs to project '${parsedIdentifier.projectName}', but no project with that name is registered in OTP.")
                }
                Individual individual = atMostOneElement(Individual.findAllByPid(parsedIdentifier.pid))
                if (individual && individual.project.name != parsedIdentifier.projectName) {
                    context.addProblem(it.cells, Level.ERROR, "Sample identifier '${sampleId}' is not registered in OTP. It looks like it belongs to project '${parsedIdentifier.projectName}' and individual '${parsedIdentifier.pid}', but individual '${parsedIdentifier.pid}' is already registered in OTP with project '${individual.project.name}'.")
                }
            }
            if (parsedIdentifier && sampleIdentifier) {
                if (sampleIdentifier.project.name != parsedIdentifier.projectName) {
                    context.addProblem(it.cells, Level.WARNING, "Sample identifier '${sampleId}' looks like it belongs to project '${parsedIdentifier.projectName}', but it is already registered in OTP with project '${sampleIdentifier.project.name}'. If you ignore this warning, OTP will keep the assignment of the sample identifier to project '${sampleIdentifier.project.name}'.")
                }
                if (sampleIdentifier.individual.pid != parsedIdentifier.pid) {
                    context.addProblem(it.cells, Level.WARNING, "Sample identifier '${sampleId}' looks like it belongs to individual '${parsedIdentifier.pid}', but it is already registered in OTP with individual '${sampleIdentifier.individual.pid}'. If you ignore this warning, OTP will keep the assignment of the sample identifier to individual '${sampleIdentifier.individual.pid}'.")
                }
                if (sampleIdentifier.sampleType.name != parsedIdentifier.sampleTypeDbName) {
                    context.addProblem(it.cells, Level.WARNING, "Sample identifier '${sampleId}' looks like it belongs to sample type '${parsedIdentifier.sampleTypeDbName}', but it is already registered in OTP with sample type '${sampleIdentifier.sampleType.name}' If you ignore this warning, OTP will keep the assignment of the sample identifier to sample type '${sampleIdentifier.sampleType.name}'.")
                }
            }
            return sampleIdentifier?.project?.name ?: parsedIdentifier?.projectName
        }
        if (!missingIdentifiers.isEmpty()) {
            context.addProblem(Collections.emptySet(), Level.INFO, "All sample identifiers which are neither registered in OTP nor match a pattern known to OTP:\n${missingIdentifiers.sort().join('\n')}")
        }
        if (byProjectName.size() == 1 && context.spreadsheet.getColumn(PROJECT.name()) == null) {
            String projectName = exactlyOneElement(byProjectName.keySet())
            if (projectName != null) {
                context.addProblem((Set)byProjectName.values().sum()*.cells.sum(), Level.INFO,
                        "All sample identifiers belong to project '${projectName}'.")
            }
        }
        byProjectName.remove(null)
        if (byProjectName.size() > 1) {
            context.addProblem((Set)byProjectName.values().sum()*.cells.sum(), Level.WARNING,
                    'The sample identifiers belong to different projects:\n' +
                            byProjectName.collect { projectName, valueTuplesOfProject ->
                                return "Project '${projectName}': ${valueTuplesOfProject.collect { "'${it.getValue(SAMPLE_ID.name())}'" }.sort().join(', ')}"
                            }.join('\n'))
        }
    }
}
