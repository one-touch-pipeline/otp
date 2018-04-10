package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*


@Component
class LibPrepKitAdapterValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator{

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
        return [SEQUENCING_TYPE.name(), LIB_PREP_KIT.name(), SAMPLE_ID.name(), LIBRARY_LAYOUT.name(), BASE_MATERIAL.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle in [SEQUENCING_TYPE, SAMPLE_ID, LIBRARY_LAYOUT]*.name()) {
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
            SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: valueTuple.getValue(LIBRARY_LAYOUT.name()), singleCell: singleCell])
            LibraryPreparationKit kit
            if (valueTuple.getValue(LIB_PREP_KIT.name())) {
                kit = libraryPreparationKitService.findByNameOrImportAlias(valueTuple.getValue(LIB_PREP_KIT.name()))
            }
            if (!seqType || ! kit) {
                return
            }
            Pipeline pipeline = seqType.isRna() ? Pipeline.findByName(Pipeline.Name.RODDY_RNA_ALIGNMENT) : Pipeline.findByName(Pipeline.Name.PANCAN_ALIGNMENT)
            String sampleId = valueTuple.getValue(SAMPLE_ID.name())
            Individual individual
            Project project
            RoddyWorkflowConfig config
            SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
            if (sampleIdentifier) {
                project = sampleIdentifier.project
                individual = sampleIdentifier.individual
                config = RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline)
            } else {
                ParsedSampleIdentifier identifier = sampleIdentifierService.parseSampleIdentifier(sampleId)
                project = Project.findByName(identifier?.projectName)
                if (!project) {
                    return
                }
                config = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
            }
            if (seqType in SeqType.getRoddyAlignableSeqTypes() &&
                    project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean &&
                    config?.adapterTrimmingNeeded) {
                if (!seqType.isRna() && !kit.adapterFile) {
                    context.addProblem(valueTuple.cells, Level.WARNING, "Adapter trimming is requested but adapter file for library preparation kit '${kit}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing.")
                }
                if (seqType.isRna() && !kit.reverseComplementAdapterSequence) {
                    context.addProblem(valueTuple.cells, Level.WARNING, "Adapter trimming is requested but reverse complement adapter sequence for library preparation kit '${kit}' is missing.","Adapter trimming is requested but the reverse complement adapter sequence for at least one library preparation kit is missing.")
                }
            }
        }
    }
}
