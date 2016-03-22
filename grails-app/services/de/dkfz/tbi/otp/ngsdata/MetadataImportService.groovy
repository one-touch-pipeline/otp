package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import groovy.transform.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*
import org.springframework.security.access.prepost.*

import java.util.logging.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * Metadata import 2.0 (OTP-34)
 */
class MetadataImportService {

    static final String AUTO_DETECT_DIRECTORY_STRUCTURE_NAME = ''
    static final String DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME = 'dataFilesInSameDirectory'

    @Autowired
    ApplicationContext applicationContext

    RunSubmitService runSubmitService

    /**
     * @return A collection of descriptions of the validations which are performed
     */
    Collection<String> getImplementedValidations() {
        return metadataValidators.sum { it.descriptions }
    }

    /**
     * @return Names (keys) and descriptions (values) of directory structures
     */
    Map<String, String> getSupportedDirectoryStructures() {
        Map<String, String> directoryStructures = new TreeMap<String, String>()
        directoryStructures.put(AUTO_DETECT_DIRECTORY_STRUCTURE_NAME, 'detect automatically')
        applicationContext.getBeansOfType(DirectoryStructure).each { String name, DirectoryStructure directoryStructure ->
            directoryStructures.put(name, directoryStructure.description)
        }
        return directoryStructures
    }

    /**
     * @param directoryStructureName As returned by {@link #getSupportedDirectoryStructures()}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")  // TODO: OTP-1908: Relax this restriction
    MetadataValidationContext validate(File metadataFile, String directoryStructureName) {
        MetadataValidationContext context = MetadataValidationContext.createFromFile(metadataFile,
                getDirectoryStructure(getDirectoryStructureBeanName(directoryStructureName, metadataFile)))
        if (context.spreadsheet) {
            metadataValidators*.validate(context)
        }
        return context
    }

    /**
     * @param directoryStructureName As returned by {@link #getSupportedDirectoryStructures()}
     * @param previousValidationMd5sum May be {@code null}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ValidateAndImportResult validateAndImport(File metadataFile, String directoryStructureName, boolean align, boolean ignoreWarnings, String previousValidationMd5sum) {
        MetadataValidationContext context = validate(metadataFile, directoryStructureName)
        Long runId = null
        if (mayImport(context, ignoreWarnings, previousValidationMd5sum)) {
            runId = doImport(context, align)
        }
        return new ValidateAndImportResult(context, runId)
    }

    protected Collection<MetadataValidator> getMetadataValidators() {
        return applicationContext.getBeansOfType(MetadataValidator).values().sort { it.getClass().name }
    }

    protected static String getDirectoryStructureBeanName(String directoryStructureName, File metadataFile) {
        if (directoryStructureName == AUTO_DETECT_DIRECTORY_STRUCTURE_NAME) {
            // TODO: Really do auto-detection based on the metadata file path when more directories structures are supported
            return DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME
        } else {
            return directoryStructureName
        }
    }

    protected DirectoryStructure getDirectoryStructure(String directoryStructureBeanName) {
        return applicationContext.getBean(directoryStructureBeanName, DirectoryStructure)
    }

    protected static boolean mayImport(MetadataValidationContext context, boolean ignoreWarnings, String previousValidationMd5sum) {
        Level maxLevel = context.maximumProblemLevel
        if (maxLevel.intValue() < Level.WARNING.intValue()) {
            return true
        } else if (maxLevel == Level.WARNING && ignoreWarnings) {
            if (context.metadataFileMd5sum.equalsIgnoreCase(previousValidationMd5sum)) {
                return true
            } else {
                context.addProblem(Collections.emptySet(), Level.INFO, 'Not ignoring warnings, because the metadata file has changed since the previous validation.')
            }
        }
        return false
    }

    // TODO: rewrite this method to trigger the metadata import 2.0
    private long doImport(MetadataValidationContext context, boolean align) {
        Closure uniqueColumnValue = { MetaDataColumn column ->
            return exactlyOneElement(context.spreadsheet.dataRows*.getCellByColumnTitle(column.name())*.text.unique())
        }
        return runSubmitService.submit(
                uniqueColumnValue(MetaDataColumn.RUN_ID),
                SeqPlatformService.findSeqPlatform(
                        uniqueColumnValue(MetaDataColumn.INSTRUMENT_PLATFORM),
                        uniqueColumnValue(MetaDataColumn.INSTRUMENT_MODEL),
                        uniqueColumnValue(MetaDataColumn.SEQUENCING_KIT) ?: null,
                ).id.toString(),
                uniqueColumnValue(MetaDataColumn.CENTER_NAME),
                context.metadataFile.parentFile.parentFile.path,
                align,
        )
    }

    public static ExtractedValue extractBarcode(Row row) {
        String barcode = null
        Set<Cell> cells = [] as Set

        Cell barcodeCell = row.getCellByColumnTitle(BARCODE.name())
        if (barcodeCell) {
            cells.add(barcodeCell)
            barcode = barcodeCell.text ?: null
        }

        Cell filenameCell = row.getCellByColumnTitle(FASTQ_FILE.name())
        if (filenameCell) {
            String barcodeFromFilename = MultiplexingService.barcode(filenameCell.text)
            if (barcodeFromFilename) {
                if (!barcode?.contains(barcodeFromFilename)) {
                    cells.add(filenameCell)
                    if (!barcode) {
                        barcode = barcodeFromFilename
                    } else {
                        // Yes, this is what the metadata import currently does
                        barcode = MultiplexingService.combineLaneNumberAndBarcode(barcode, barcodeFromFilename)
                    }
                }
            } else if (!barcode) {
                cells.add(filenameCell)
            }
        }

        if (cells) {
            return new ExtractedValue(barcode, cells)
        } else {
            return null
        }
    }

    public static ExtractedValue extractMateNumber(Row row) {
        Cell libraryLayoutCell = row.getCellByColumnTitle(LIBRARY_LAYOUT.name())
        if (libraryLayoutCell && libraryLayoutCell.text == LibraryLayout.SINGLE.name()) {
            return new ExtractedValue('1', [libraryLayoutCell] as Set)
        }
        Cell filenameCell = row.getCellByColumnTitle(FASTQ_FILE.name())
        if (!filenameCell) {
            return null
        }
        int mateNumber
        try {
            mateNumber = MetaDataService.findOutMateNumber(filenameCell.text)
        } catch (RuntimeException e) {
            if (e.message == "cannot find mateNumber for ${filenameCell.text}".toString()) {
                return null
            } else {
                throw e
            }
        }
        return new ExtractedValue(Integer.toString(mateNumber), [filenameCell] as Set)
    }
}

@TupleConstructor
class ExtractedValue {

    final String value

    /**
     * The cells the value has been extracted from
     */
    final Set<Cell> cells

}

@TupleConstructor
class ValidateAndImportResult {

    final MetadataValidationContext context

    /**
     * {@code null} if the import has been rejected
     */
    final Long runId
}
