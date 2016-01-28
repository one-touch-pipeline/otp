package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import java.util.logging.Level

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import groovy.transform.TupleConstructor

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
        Map<String, String> directoryStructures = [(AUTO_DETECT_DIRECTORY_STRUCTURE_NAME): 'detect automatically']
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
        return applicationContext.getBeansOfType(MetadataValidator).values()
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
}

@TupleConstructor
class ValidateAndImportResult {

    final MetadataValidationContext context

    /**
     * {@code null} if the import has been rejected
     */
    final Long runId
}
