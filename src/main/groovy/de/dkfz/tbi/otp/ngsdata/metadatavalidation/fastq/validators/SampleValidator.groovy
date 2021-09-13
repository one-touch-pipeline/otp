/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Component
class SampleValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Override
    Collection<String> getDescriptions() {
        return [
                'The sample name must be registered in OTP or parsable using a pattern known to OTP.',
                'If the sample name can be parsed and is already registered in the OTP database,' +
                        ' the current parsed values should match those already registered in the database.',
                'All sample names in the metadata file should belong to the same project.',
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_NAME]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [PROJECT, PATIENT_ID, SAMPLE_TYPE, ANTIBODY_TARGET]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        Collection<String> missingIdentifiersWithProject = []
        Collection<String> parsedSampleIdentifiers = []

        Map<String, Collection<ValueTuple>> byProjectName = valueTuples.groupBy {
            String sampleName = it.getValue(SAMPLE_NAME.name())
            String projectName = it.getValue(PROJECT.name()) ?: ''
            String pid = it.getValue(PATIENT_ID.name()) ?: ''
            String sampleType = it.getValue(SAMPLE_TYPE.name()) ?: ''
            String antibodyTarget = it.getValue(ANTIBODY_TARGET.name()) ?: ''

            Project project = Project.getByNameOrNameInMetadataFiles(projectName)
            ParsedSampleIdentifier parsedIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
            SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
            if (!parsedIdentifier && !sampleIdentifier) {
                context.addProblem(it.cells, LogLevel.ERROR, "Sample name '${sampleName}' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample name is neither registered in OTP nor matches a pattern known to OTP.")
                missingIdentifiersWithProject.add("${projectName}\t${pid}\t${sampleType}\t${sampleName}\t\t${antibodyTarget}")
            }
            if (parsedIdentifier && !sampleIdentifier) {
                boolean error = false
                if (!atMostOneElement(Project.findAllByName(parsedIdentifier.projectName))) {
                    context.addProblem(it.cells, LogLevel.ERROR, "Sample name '${sampleName}' is not registered in OTP. It looks like it belongs to project '${parsedIdentifier.projectName}', but no project with that name is registered in OTP.", "At least one sample name is not registered in OTP. It looks like it belongs to a project not registered in OTP.")
                    error = true
                }
                Individual individual = atMostOneElement(Individual.findAllByPid(parsedIdentifier.pid))
                if (individual && individual.project.name != parsedIdentifier.projectName) {
                    context.addProblem(it.cells, LogLevel.ERROR, "Sample name '${sampleName}' is not registered in OTP. It looks like it belongs to project '${parsedIdentifier.projectName}' and individual '${parsedIdentifier.pid}', but individual '${parsedIdentifier.pid}' is already registered in OTP with project '${individual.project.name}'.", "At least one sample name is not registered in OTP. It looks like it belongs to a specific project and individual, but this individual is already registered in OTP with another project.")
                    error = true
                }
                if (!error) {
                    parsedSampleIdentifiers.add("${parsedIdentifier.projectName}\t${parsedIdentifier.pid}\t${parsedIdentifier.sampleTypeDbName}\t${parsedIdentifier.fullSampleName}")
                }
            }
            if (parsedIdentifier && sampleIdentifier) {
                if (sampleIdentifier.project.name != parsedIdentifier.projectName) {
                    context.addProblem(it.cells, LogLevel.WARNING, "Sample name '${sampleName}' looks like it belongs to project '${parsedIdentifier.projectName}', but it is already registered in OTP with project '${sampleIdentifier.project.name}'. If you ignore this warning, OTP will keep the assignment of the sample name to project '${sampleIdentifier.project.name}'.", "At least one sample name looks like it belongs to a specific project, but it is already registered in OTP with another project.")
                }
                if (sampleIdentifier.individual.pid != parsedIdentifier.pid) {
                    context.addProblem(it.cells, LogLevel.WARNING, "Sample name '${sampleName}' looks like it belongs to individual '${parsedIdentifier.pid}', but it is already registered in OTP with individual '${sampleIdentifier.individual.pid}'. If you ignore this warning, OTP will keep the assignment of the sample name to individual '${sampleIdentifier.individual.pid}'.", "At least one sample name looks like it belongs to a specific individual, but it is already registered in OTP with another individual.")
                }
                if (sampleIdentifier.sampleType.name != parsedIdentifier.sampleTypeDbName) {
                    context.addProblem(it.cells, LogLevel.WARNING, "Sample name '${sampleName}' looks like it belongs to sample type '${parsedIdentifier.sampleTypeDbName}', but it is already registered in OTP with sample type '${sampleIdentifier.sampleType.name}' If you ignore this warning, OTP will keep the assignment of the sample name to sample type '${sampleIdentifier.sampleType.name}'.", "At least one sample name looks like it belongs to a specific sample type, but it is already registered in OTP with another sample type.")
                }
            }
            return sampleIdentifier?.project?.name ?: parsedIdentifier?.projectName
        }

        if (parsedSampleIdentifiers) {
            context.addProblem(Collections.emptySet(), LogLevel.INFO, "The following Samples will be created:\n" +
                    "${SampleIdentifierService.BulkSampleCreationHeader.getHeaders()}\n" +
                    "${parsedSampleIdentifiers.sort().join('\n')}")
        }

        if (missingIdentifiersWithProject) {
            context.addProblem(Collections.emptySet(), LogLevel.INFO,
                    "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n" +
                            "${SampleIdentifierService.BulkSampleCreationHeader.getHeaders()}\n" +
                            "${missingIdentifiersWithProject.collect { it.substring(0, it.length() - it.reverse().indexOf("\t\t") - 2) }.sort().join('\n')}",
                    "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n" +
                            "${SampleIdentifierService.BulkSampleCreationHeader.getSummaryHeaders()}\n" +
                            "${missingIdentifiersWithProject.sort().join('\n')}")
        }
        if (byProjectName.size() == 1 && context.spreadsheet.getColumn(PROJECT.name()) == null) {
            String projectName = exactlyOneElement(byProjectName.keySet())
            if (projectName != null) {
                context.addProblem((Set) byProjectName.values().sum()*.cells.sum(), LogLevel.INFO,
                        "All sample names belong to project '${projectName}'.")
            }
        }
        byProjectName.remove(null)
        if (byProjectName.size() > 1) {
            context.addProblem((Set) byProjectName.values().sum()*.cells.sum(), LogLevel.WARNING,
                    'The sample names belong to different projects:\n' +
                            byProjectName.collect { projectName, valueTuplesOfProject ->
                                return "Project '${projectName}':\n        ${valueTuplesOfProject.collect { "'${it.getValue(SAMPLE_NAME.name())}'" }.sort().join('\n        ')}"
                            }.join('\n'))
        }
    }
}
