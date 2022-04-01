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

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class AlignmentValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Autowired
    ProjectService projectService

    @Autowired
    MetadataImportService metadataImportService

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
        return [BASE_MATERIAL, PROJECT, SAMPLE_NAME]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        allValueTuples.groupBy { metadataImportService.getSeqTypeFromMetadata(it) }.each { SeqType seqType, List<ValueTuple> valueTuplesSameSeqType ->
            if (seqType) {
                valueTuplesSameSeqType.groupBy { MetadataImportService.getProjectFromMetadata(it) }.each { Project project, List<ValueTuple> valueTuplesSameProject ->
                    if (project) {
                        if (seqType in SeqTypeService.roddyAlignableSeqTypes) {
                            Pipeline pipeline = CollectionUtils.atMostOneElement(Pipeline.findAllByNameAndType(Pipeline.Name.forSeqType(seqType), Pipeline.Type.ALIGNMENT))
                            if (!RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)) {
                                context.addProblem(Collections.emptySet(), LogLevel.WARNING, "${pipeline.name.name()} is not configured for Project '${project}' and SeqType '${seqType}'", "At least one Alignment is not configured.")
                            }
                        } else if (seqType in SeqTypeService.cellRangerAlignableSeqTypes) {
                            if (!projectService.getLatestCellRangerConfig(project, seqType)) {
                                context.addProblem(Collections.emptySet(), LogLevel.WARNING, "CellRanger is not configured for Project '${project}' and SeqType '${seqType}'", "At least one Alignment is not configured.")
                            }
                        } else {
                            context.addProblem(Collections.emptySet(), LogLevel.INFO, "Alignment for SeqType ${seqType} is not supported")
                        }
                    }
                }
            }
        }
    }
}
