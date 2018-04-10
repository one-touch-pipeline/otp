package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqTypeLibraryLayoutValidator extends ValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ['The combination of sequencing type, library layout and single cell is registered in the OTP database.']
    }

    @Override
    List<String> getColumnTitles(AbstractMetadataValidationContext context) {
        List<String> columns = [SEQUENCING_TYPE.name(), LIBRARY_LAYOUT.name()]
        if (context instanceof BamMetadataValidationContext) {
            return columns
        } else {
            columns.add(BASE_MATERIAL.name())
            columns.add(TAGMENTATION_BASED_LIBRARY.name())
            return columns
        }
    }

    @Override
    boolean columnMissing(AbstractMetadataValidationContext context, String columnTitle) {
        if (columnTitle in [TAGMENTATION_BASED_LIBRARY.name(), BASE_MATERIAL.name()]) {
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String seqTypeName
            if (context instanceof BamMetadataValidationContext) {
                seqTypeName = it.getValue(SEQUENCING_TYPE.name())
            } else {
                seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(it)
            }
            String libraryLayoutName = it.getValue(LIBRARY_LAYOUT.name())
            String baseMaterial = it.getValue(BASE_MATERIAL.name())
            boolean isSingleCell = SeqTypeService.isSingleCell(baseMaterial)
            if (seqTypeName &&
                    libraryLayoutName &&
                    seqTypeService.findByNameOrImportAlias(seqTypeName) &&
                    SeqType.findByLibraryLayout(libraryLayoutName) &&
                    !seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libraryLayoutName, singleCell: isSingleCell])) {
                if (isSingleCell)
                    context.addProblem(it.cells, Level.ERROR, "The combination of sequencing type '${seqTypeName}' and library layout '${libraryLayoutName}' and Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and Single Cell is not registered in the OTP database.")
                else
                    context.addProblem(it.cells, Level.ERROR, "The combination of sequencing type '${seqTypeName}' and library layout '${libraryLayoutName}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database.")
            }
        }
    }
}
