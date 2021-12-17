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

import de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LibPrepKitAdapterValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ["If the sample is configured for adapter trimming the library preparation kit must contain adapter information."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE, PROJECT, SEQUENCING_READ_TYPE]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [LIB_PREP_KIT, BASE_MATERIAL]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            String baseMaterial = valueTuple.getValue(BASE_MATERIAL.name())
            boolean singleCell = SeqTypeService.isSingleCell(baseMaterial)
            if (seqTypeName.isEmpty()) {
                return
            }
            SequencingReadType libraryLayout = SequencingReadType.findByName(valueTuple.getValue(SEQUENCING_READ_TYPE.name()))
            if (!libraryLayout) {
                return
            }
            SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libraryLayout, singleCell: singleCell])
            LibraryPreparationKit kit
            if (valueTuple.getValue(LIB_PREP_KIT.name())) {
                kit = libraryPreparationKitService.findByNameOrImportAlias(valueTuple.getValue(LIB_PREP_KIT.name()))
            }
            if (!seqType || !kit) {
                return
            }
            Pipeline pipeline = seqType.isRna() ? Pipeline.findByName(Pipeline.Name.RODDY_RNA_ALIGNMENT) : Pipeline.findByName(Pipeline.Name.PANCAN_ALIGNMENT)
            String projectName = valueTuple.getValue(PROJECT.name())
            Project project = Project.getByNameOrNameInMetadataFiles(projectName)
            if (!project) {
                return
            }
            RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)

            if (seqType in SeqTypeService.roddyAlignableSeqTypes &&
                    project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT &&
                    config?.adapterTrimmingNeeded) {
                if (!seqType.isRna() && !kit.adapterFile) {
                    context.addProblem(valueTuple.cells, LogLevel.WARNING, "Adapter trimming is requested but adapter file for library preparation kit '${kit}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing.")
                }
                if (seqType.isRna() && !kit.reverseComplementAdapterSequence) {
                    context.addProblem(valueTuple.cells, LogLevel.WARNING, "Adapter trimming is requested but reverse complement adapter sequence for library preparation kit '${kit}' is missing.", "Adapter trimming is requested but the reverse complement adapter sequence for at least one library preparation kit is missing.")
                }
            }
        }
    }
}
