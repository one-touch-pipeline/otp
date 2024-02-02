/*
 * Copyright 2011-2024 The OTP authors
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

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@CompileDynamic
@Component
class SampleValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    static final String ERROR_NEITHER_REGISTERED_NOR_PARSEABLE = "At least one sample name is neither registered in OTP nor can be parsed."
    static final String ERROR_PARSED_PROJECT_UNKNOWN = "At least for one sample name the parsed project is not registered in OTP"
    static final String ERROR_PARSED_PROJECT_DIFFERS = "At least for one sample name the parsed project does not match the project in the metadata column."
    static final String ERROR_PARSED_INDIVIDUAL_KNOWN_IN_OTHER_PROJECT = "At least one sample name refers to an existing pid connected not to the parsed project."
    static final String ERROR_PARSED_AND_FOUND_PROJECT_DIFFER = "At least one sample name looks like it belongs to a specific project, but it is already " +
            "registered in OTP with another project."
    static final String ERROR_PARSED_AND_FOUND_INDIVIDUAL_DIFFER = "At least one sample name looks like it belongs to a specific individual, but it is " +
            "already registered in OTP with another individual."
    static final String ERROR_PARSED_AND_FOUND_SAMPLE_TYPE_DIFFER = "At least one sample name looks like it belongs to a specific sample type, but it is " +
            "already registered in OTP with another sample type."

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Override
    Collection<String> getDescriptions() {
        return [
                'The sample name must be registered in OTP or parsable by the parser defined by the project.',
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
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    @SuppressWarnings("Indentation")
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        Collection<String> missingIdentifiersWithProject = []
        Collection<String> parsedSampleIdentifiers = []

        Map<String, Collection<ValueTuple>> byProjectName = valueTuples.groupBy {
            return checkValueTupleAndReportProjectAndFillLists(it, context, missingIdentifiersWithProject, parsedSampleIdentifiers)
        }

        if (parsedSampleIdentifiers) {
            context.addProblem(Collections.emptySet(), LogLevel.INFO, "The following Samples will be created:\n" +
                    "${SampleIdentifierService.BulkSampleCreationHeader.headers}\n" +
                    "${parsedSampleIdentifiers.sort().join('\n')}")
        }

        if (missingIdentifiersWithProject) {
            context.addProblem(Collections.emptySet(), LogLevel.INFO,
                    "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n" +
                            "${SampleIdentifierService.BulkSampleCreationHeader.headers}\n" +
                            missingIdentifiersWithProject.collect {
                                it.substring(0, it.length() - it.reverse().indexOf("\t\t") - 2)
                            }.sort().join('\n'),
                    "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n" +
                            "${SampleIdentifierService.BulkSampleCreationHeader.summaryHeaders}\n" +
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
                                String sampleNames = valueTuplesOfProject.collect {
                                    "\n        '${it.getValue(SAMPLE_NAME.name())}'"
                                }.sort().join('')
                                return "Project '${projectName}':${sampleNames}"
                            }.join('\n'))
        }
    }

    private String checkValueTupleAndReportProjectAndFillLists(ValueTuple valueTuple, MetadataValidationContext context,
                                                               List<String> missingIdentifiersWithProject, List<String> parsedSampleIdentifiers) {
        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        String projectName = valueTuple.getValue(PROJECT.name()) ?: ''
        String pid = valueTuple.getValue(PATIENT_ID.name()) ?: ''
        String sampleType = valueTuple.getValue(SAMPLE_TYPE.name()) ?: ''
        String antibodyTarget = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ''

        Project project = ProjectService.findByNameOrNameInMetadataFiles(projectName)
        ParsedSampleIdentifier parsedIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (!parsedIdentifier && !sampleIdentifier) {
            checkNeitherKnownSampleIdentifierNorParsable(project, context, valueTuple, sampleName, projectName)
            missingIdentifiersWithProject.add("${projectName}\t${pid}\t${sampleType}\t${sampleName}\t\t${antibodyTarget}")
        }
        if (parsedIdentifier && !sampleIdentifier) {
            if (!checkParsableButUnknownSampleIdentfier(parsedIdentifier, context, valueTuple, sampleName, project)) {
                parsedSampleIdentifiers <<
                        "${parsedIdentifier.projectName}\t${parsedIdentifier.pid}\t${parsedIdentifier.sampleTypeDbName}\t${parsedIdentifier.fullSampleName}"
            }
        }
        if (parsedIdentifier && sampleIdentifier) {
            checkKnownSampleIdentifierAndParsable(parsedIdentifier, sampleIdentifier, context, valueTuple, sampleName)
        }
        return sampleIdentifier?.project?.name ?: parsedIdentifier?.projectName
    }

    private void checkNeitherKnownSampleIdentifierNorParsable(Project project, MetadataValidationContext context, ValueTuple valueTuple, String sampleName,
                                                              String projectName) {
        if (project && project.sampleIdentifierParserBeanName != SampleIdentifierParserBeanName.NO_PARSER) {
            context.addProblem(valueTuple.cells, LogLevel.ERROR,
                    "Sample name '${sampleName}' is neither registered in OTP nor can be parsed by the parser " +
                            "'${project.sampleIdentifierParserBeanName}' (specified by project '${projectName}')",
                    ERROR_NEITHER_REGISTERED_NOR_PARSEABLE)
        } else {
            context.addProblem(valueTuple.cells, LogLevel.ERROR,
                    "Sample name '${sampleName}' is not registered in OTP and for the project no parser is defined.",
                    ERROR_NEITHER_REGISTERED_NOR_PARSEABLE)
        }
    }

    private boolean checkParsableButUnknownSampleIdentfier(ParsedSampleIdentifier parsedIdentifier, MetadataValidationContext context,
                                                           ValueTuple valueTuple, String sampleName, Project project) {
        boolean error = false
        Project parsedProject = ProjectService.findByNameOrNameInMetadataFiles(parsedIdentifier.projectName)
        if (!parsedProject) {
            context.addProblem(valueTuple.cells, LogLevel.ERROR,
                    "The parsed project '${parsedIdentifier.projectName}' of the sample name '${sampleName}' could not be found in the database.",
                    ERROR_PARSED_PROJECT_UNKNOWN)
            error = true
        } else if (project != parsedProject) {
            context.addProblem(valueTuple.cells, LogLevel.ERROR,
                    "The parsed project '${parsedIdentifier.projectName}' of the sample name '${sampleName}' does not match the project in " +
                            "the metadata column '${project.name}'.",
                    ERROR_PARSED_PROJECT_DIFFERS)
            error = true
        }
        Individual individual = atMostOneElement(Individual.findAllByPid(parsedIdentifier.pid))
        if (individual && individual.project != parsedProject) {
            context.addProblem(valueTuple.cells, LogLevel.ERROR,
                    "The parsed pid '${parsedIdentifier.pid}' of the sample name '${sampleName}' already exist, but belongs to the project " +
                            "'${individual.project.name}' and not the parsed project '${parsedIdentifier.projectName}'.",
                    ERROR_PARSED_INDIVIDUAL_KNOWN_IN_OTHER_PROJECT)
            error = true
        }

        return error
    }

    private void checkKnownSampleIdentifierAndParsable(ParsedSampleIdentifier parsedIdentifier, SampleIdentifier sampleIdentifier,
                                                       MetadataValidationContext context, ValueTuple valueTuple, String sampleName) {
        Project parsedProject = ProjectService.findByNameOrNameInMetadataFiles(parsedIdentifier.projectName)
        if (sampleIdentifier.project != parsedProject) {
            context.addProblem(valueTuple.cells, LogLevel.WARNING,
                    "Sample name '${sampleName}' looks like it belongs to project '${parsedIdentifier.projectName}', but it is already " +
                            "registered in OTP with project '${sampleIdentifier.project.name}'. If you ignore this warning, " +
                            "OTP will keep the assignment of the sample name to project '${sampleIdentifier.project.name}'.",
                    ERROR_PARSED_AND_FOUND_PROJECT_DIFFER)
        }
        if (sampleIdentifier.individual.pid != parsedIdentifier.pid) {
            context.addProblem(valueTuple.cells, LogLevel.WARNING,
                    "Sample name '${sampleName}' looks like it belongs to individual '${parsedIdentifier.pid}', but it is already " +
                            "registered in OTP with individual '${sampleIdentifier.individual.pid}'. If you ignore this warning, " +
                            "OTP will keep the assignment of the sample name to individual '${sampleIdentifier.individual.pid}'.",
                    ERROR_PARSED_AND_FOUND_INDIVIDUAL_DIFFER)
        }
        if (sampleIdentifier.sampleType.name != parsedIdentifier.sampleTypeDbName) {
            context.addProblem(valueTuple.cells, LogLevel.WARNING,
                    "Sample name '${sampleName}' looks like it belongs to sample type '${parsedIdentifier.sampleTypeDbName}', but it is already " +
                            "registered in OTP with sample type '${sampleIdentifier.sampleType.name}'. If you ignore this warning, " +
                            "OTP will keep the assignment of the sample name to sample type '${sampleIdentifier.sampleType.name}'.",
                    ERROR_PARSED_AND_FOUND_SAMPLE_TYPE_DIFFER)
        }
    }
}
