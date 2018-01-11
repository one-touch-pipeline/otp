package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*
import org.springframework.security.access.prepost.*

import java.nio.file.*

class BamMetadataImportService {

    @Autowired
    ApplicationContext applicationContext

    FileSystemService fileSystemService

    /**
     * @return A collection of descriptions of the validations which are performed
     */
    Collection<String> getImplementedValidations() {
        return (Collection<String>) bamMetadataValidators.sum { it.descriptions }
    }

    protected Collection<BamMetadataValidator> getBamMetadataValidators() {
        return applicationContext.getBeansOfType(BamMetadataValidator).values().sort { it.getClass().name }
    }

    BamMetadataValidationContext validate(String metadataFile, List<String> furtherFiles) {
        FileSystem fileSystem = fileSystemService.getFilesystemForBamImport()
        BamMetadataValidationContext context = BamMetadataValidationContext.createFromFile(
                fileSystem.getPath(metadataFile),
                furtherFiles,
                fileSystem,
        )
        if (context.spreadsheet) {
            bamMetadataValidators*.validate(context)
        }
        return context
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map validateAndImport(String metadataFile, boolean ignoreWarnings, String previousValidationMd5sum, boolean replaceWithLink,
                          boolean triggerSnv, boolean triggerIndel, boolean triggerAceseq, List<String> furtherFiles) {
        Project outputProject
        ImportProcess importProcess = new ImportProcess(externallyProcessedMergedBamFiles: [])
        BamMetadataValidationContext context = validate(metadataFile, furtherFiles)
        if (MetadataImportService.mayImport(context, ignoreWarnings, previousValidationMd5sum)) {
            context.spreadsheet.dataRows.each { Row row ->
                String referenceGenome = uniqueColumnValue(row, BamMetadataColumn.REFERENCE_GENOME)
                String seqType = uniqueColumnValue(row, BamMetadataColumn.SEQUENCING_TYPE)
                String bamFilePath = uniqueColumnValue(row, BamMetadataColumn.BAM_FILE_PATH)
                String _sampleType = uniqueColumnValue(row, BamMetadataColumn.SAMPLE_TYPE)
                String _individual = uniqueColumnValue(row, BamMetadataColumn.INDIVIDUAL)
                String libraryLayout = uniqueColumnValue(row, BamMetadataColumn.LIBRARY_LAYOUT)
                String _project = uniqueColumnValue(row, BamMetadataColumn.PROJECT)
                String coverage = uniqueColumnValue(row, BamMetadataColumn.COVERAGE)
                String md5sum = uniqueColumnValue(row, BamMetadataColumn.MD5)

                Sample sample = Sample.createCriteria().get {
                    individual {
                        and {
                            or {
                                eq('pid', _individual)
                                eq('mockPid', _individual)
                                eq('mockFullName', _individual)
                            }
                            project {
                                eq('name', _project)
                            }
                        }
                    }
                    sampleType {
                        eq('name', _sampleType)
                    }
                }

                ExternalMergingWorkPackage emwp = new ExternalMergingWorkPackage(
                        referenceGenome: ReferenceGenome.findByName(referenceGenome),
                        sample         : sample,
                        seqType        : SeqType.findByNameAndLibraryLayout(seqType, libraryLayout),
                        pipeline       : Pipeline.findByNameAndType(Pipeline.Name.EXTERNALLY_PROCESSED, Pipeline.Type.ALIGNMENT)
                )
                assert emwp.save(flush:true)

                ExternallyProcessedMergedBamFile epmbf = new ExternallyProcessedMergedBamFile(
                        workPackage         : emwp,
                        importedFrom        : bamFilePath,
                        fileName            : getNameFromPath(bamFilePath),
                        coverage            : coverage ? Double.parseDouble(coverage) : null,
                        md5sum              : md5sum ?: null,
                        furtherFiles        : [] as Set
                ).save()

                File bamFileParent = new File(epmbf.importedFrom).parentFile

                furtherFiles.findAll().findAll { String path ->
                    new File (bamFileParent, path).exists()
                }.each {
                        epmbf.furtherFiles.add(it)
                }

                emwp.bamFileInProjectFolder = null
                assert epmbf.save(flush:true)
                importProcess.externallyProcessedMergedBamFiles.add(epmbf)
            }

            importProcess.state = ImportProcess.State.NOT_STARTED
            importProcess.replaceSourceWithLink = replaceWithLink
            importProcess.triggerSnv = triggerSnv
            importProcess.triggerIndel = triggerIndel
            importProcess.triggerAceseq = triggerAceseq
            assert importProcess.save(flush:true)

            outputProject = importProcess.externallyProcessedMergedBamFiles.first().project
        }

        return [
                context : context,
                importProcess: importProcess,
                project : outputProject
        ]
    }

    private static String getNameFromPath(String path) {
        return new File(path).name
    }

    static String uniqueColumnValue(Row row, BamMetadataColumn column) {
        return row.getCell(row.spreadsheet.getColumn(column.name()))?.text
    }
}
