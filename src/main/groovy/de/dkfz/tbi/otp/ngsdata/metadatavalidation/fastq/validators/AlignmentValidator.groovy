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
import groovy.transform.TupleConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.ReferenceGenomeSelectorService
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersionSelectorService
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class AlignmentValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Autowired
    ProjectService projectService

    @Autowired
    MetadataImportService metadataImportService

    @Autowired
    ValidatorHelperService validatorHelperService

    @Autowired
    WorkflowVersionSelectorService workflowVersionSelectorService

    @Autowired
    ReferenceGenomeSelectorService referenceGenomeSelectorService

    @Override
    Collection<String> getDescriptions() {
        return [
                "Alignment should be configured",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_READ_TYPE, SEQUENCING_TYPE]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [BASE_MATERIAL, PROJECT, SAMPLE_NAME, SPECIES]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        List<SeqType> alignAbleSeqTypes = seqTypeService.findAlignAbleSeqTypes()
        List<SeqType> seqTypesNewSystem = seqTypeService.seqTypesNewWorkflowSystem
        List<SeqType> seqTypesOldSystem = alignAbleSeqTypes - seqTypesNewSystem

        allValueTuples.groupBy {
            new ProjectSeqTypeSpecies(
                    validatorHelperService.getProjectFromMetadata(it),
                    validatorHelperService.getSeqTypeFromMetadata(it),
                    validatorHelperService.getSpeciesFromMetadata(it) ?: [],
            )
        }.findAll {
            it.key.valuesGiven()
        }.each { ProjectSeqTypeSpecies projectSeqTypeSpecies, List<ValueTuple> valueTuplesSameSeqType ->
            checkSingleTuple(projectSeqTypeSpecies, seqTypesOldSystem, context, seqTypesNewSystem)
        }
    }

    @CompileDynamic
    private void checkSingleTuple(ProjectSeqTypeSpecies projectSeqTypeSpecies, List<SeqType> seqTypesOldSystem, MetadataValidationContext context, List<SeqType> seqTypesNewSystem) {
        Project project = projectSeqTypeSpecies.project
        SeqType seqType = projectSeqTypeSpecies.seqType
        List<SpeciesWithStrain> species = projectSeqTypeSpecies.speciesWithStrains
        if (seqType in seqTypesOldSystem) {
            Pipeline pipeline = CollectionUtils.atMostOneElement(Pipeline.findAllByNameAndType(Pipeline.Name.forSeqType(seqType), Pipeline.Type.ALIGNMENT))
            if (!RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)) {
                createWarningMessage(context, "${pipeline.name.name()} is not configured for Project '${project}' and SeqType '${seqType}'")
            }
        } else if (seqType in seqTypesNewSystem) {
            if (!workflowVersionSelectorService.hasAlignmentConfigForProjectAndSeqType(project, seqType)) {
                createWarningMessage(context, "Alignment is not configured for Project '${project}' and SeqType '${seqType}'")
            }
            else if (!referenceGenomeSelectorService.hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies(project, seqType, species)) {
                createWarningMessage(context, "Reference Genome is not configured for Project '${project}', SeqType '${seqType}' and Species '${species.join(' + ')}'")
            }
        } else if (seqType in SeqTypeService.cellRangerAlignableSeqTypes) {
            if (!projectService.getLatestCellRangerConfig(project, seqType)) {
                createWarningMessage(context, "CellRanger is not configured for Project '${project}' and SeqType '${seqType}'")
            }
        } else {
            context.addProblem(Collections.emptySet(), LogLevel.INFO, "Alignment for SeqType ${seqType} is not supported")
        }
    }

    private static void createWarningMessage(MetadataValidationContext context, String message) {
        context.addProblem(Collections.emptySet(), LogLevel.WARNING, message, "At least one Alignment or Reference Genome is not configured.")
    }

    @TupleConstructor
    static private class ProjectSeqTypeSpecies {
        final Project project
        final SeqType seqType
        final List<SpeciesWithStrain> speciesWithStrains

        boolean valuesGiven() {
            return project && seqType && speciesWithStrains
        }
    }
}
