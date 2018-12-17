package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LibPrepKitSeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is ${SeqTypeService.seqTypesRequiredLibPrepKit*.nameWithLibraryLayout.join(' or ')}, the library preparation kit must not be empty."]
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
        List<SeqType> seqTypes = SeqTypeService.getSeqTypesRequiredLibPrepKit()
        valueTuples.each { ValueTuple valueTuple ->
            String seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            LibraryLayout libraryLayout = LibraryLayout.findByName(valueTuple.getValue(LIBRARY_LAYOUT.name()))
            if (!libraryLayout) {
                return
            }
            String baseMaterial = valueTuple.getValue(BASE_MATERIAL.name())
            boolean singleCell = SeqTypeService.isSingleCell(baseMaterial)
            if (seqTypeName.isEmpty()) {
                return
            }
            SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libraryLayout, singleCell: singleCell])
            if (seqType in seqTypes && !valueTuple.getValue(LIB_PREP_KIT.name())) {
                context.addProblem(valueTuple.cells, Level.ERROR, "If the sequencing type is '${seqType.nameWithLibraryLayout}'" +
                        ", the library preparation kit must be given.")
            }
        }
    }
}
