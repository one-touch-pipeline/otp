package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
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
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE.name(), LIB_PREP_KIT.name(), PROJECT.name(), LIBRARY_LAYOUT.name(), BASE_MATERIAL.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle in [SEQUENCING_TYPE, PROJECT, LIBRARY_LAYOUT]*.name()) {
            return false
        }
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            String baseMaterial = valueTuple.getValue(BASE_MATERIAL.name())
            boolean singleCell = SeqTypeService.isSingleCell(baseMaterial)
            if (seqTypeName.isEmpty()) {
                return
            }
            LibraryLayout libraryLayout = LibraryLayout.findByName(valueTuple.getValue(LIBRARY_LAYOUT.name()))
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

            if (seqType in SeqTypeService.getRoddyAlignableSeqTypes() &&
                    project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT &&
                    config?.adapterTrimmingNeeded) {
                if (!seqType.isRna() && !kit.adapterFile) {
                    context.addProblem(valueTuple.cells, Level.WARNING, "Adapter trimming is requested but adapter file for library preparation kit '${kit}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing.")
                }
                if (seqType.isRna() && !kit.reverseComplementAdapterSequence) {
                    context.addProblem(valueTuple.cells, Level.WARNING, "Adapter trimming is requested but reverse complement adapter sequence for library preparation kit '${kit}' is missing.", "Adapter trimming is requested but the reverse complement adapter sequence for at least one library preparation kit is missing.")
                }
            }
        }
    }
}
