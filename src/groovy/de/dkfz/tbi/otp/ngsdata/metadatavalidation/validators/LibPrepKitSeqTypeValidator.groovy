package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*

@Component
class LibPrepKitSeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator{

    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is ${SeqType.seqTypesRequiredLibPrepKit*.nameWithLibraryLayout.join(' or ')}, the library preparation kit must not be empty."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE.name(), LIBRARY_LAYOUT.name(), LIB_PREP_KIT.name(), TAGMENTATION_BASED_LIBRARY.name(), BASE_MATERIAL.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == SEQUENCING_TYPE.name() || columnTitle == LIBRARY_LAYOUT.name()) {
            return false
        }
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        List<SeqType> seqTypes = SeqType.getSeqTypesRequiredLibPrepKit()
        valueTuples.each { ValueTuple valueTuple ->
            String seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            String libLayout = valueTuple.getValue(LIBRARY_LAYOUT.name())
            String baseMaterial = valueTuple.getValue(BASE_MATERIAL.name())
            boolean singleCell = SeqTypeService.isSingleCell(baseMaterial)
            if (seqTypeName.isEmpty()) {
                return
            }
            SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libLayout, singleCell: singleCell])
            if (seqType in seqTypes && !valueTuple.getValue(LIB_PREP_KIT.name())) {
                context.addProblem(valueTuple.cells, Level.ERROR, "If the sequencing type is '${seqType.nameWithLibraryLayout}'" +
                        ", the library preparation kit must be given.")
            }
        }
    }
}
