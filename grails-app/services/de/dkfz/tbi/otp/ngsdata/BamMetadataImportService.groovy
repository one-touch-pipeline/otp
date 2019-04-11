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

package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.util.spreadsheet.Row

import java.nio.file.*

@Transactional
class BamMetadataImportService {

    @Autowired
    ApplicationContext applicationContext

    SamplePairDeciderService samplePairDeciderService

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
                          boolean triggerAnalysis, List<String> furtherFiles) {
        FileSystem fileSystem = fileSystemService.getFilesystemForBamImport()
        Project outputProject
        ImportProcess importProcess = new ImportProcess(externallyProcessedMergedBamFiles: [])
        BamMetadataValidationContext context = validate(metadataFile, furtherFiles)
        if (MetadataImportService.mayImport(context, ignoreWarnings, previousValidationMd5sum)) {
            context.spreadsheet.dataRows.each { Row row ->
                String _referenceGenome = uniqueColumnValue(row, BamMetadataColumn.REFERENCE_GENOME)
                String _seqType = uniqueColumnValue(row, BamMetadataColumn.SEQUENCING_TYPE)
                String bamFilePath = uniqueColumnValue(row, BamMetadataColumn.BAM_FILE_PATH)
                String _sampleType = uniqueColumnValue(row, BamMetadataColumn.SAMPLE_TYPE)
                String _individual = uniqueColumnValue(row, BamMetadataColumn.INDIVIDUAL)
                String libraryLayout = uniqueColumnValue(row, BamMetadataColumn.LIBRARY_LAYOUT)
                String _project = uniqueColumnValue(row, BamMetadataColumn.PROJECT)
                String coverage = uniqueColumnValue(row, BamMetadataColumn.COVERAGE)
                String md5sum = uniqueColumnValue(row, BamMetadataColumn.MD5)
                String insertSizeFile = uniqueColumnValue(row, BamMetadataColumn.INSERT_SIZE_FILE)
                String qualityControlFile = uniqueColumnValue(row, BamMetadataColumn.QUALITY_CONTROL_FILE)
                String libraryPreparationKit = uniqueColumnValue(row, BamMetadataColumn.LIBRARY_PREPARATION_KIT)

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
                assert sample: "No sample found for ${_individual} and ${_sampleType} in ${_project}"

                SeqType seqType = SeqType.findByNameAndLibraryLayoutAndSingleCell(_seqType, libraryLayout, false)
                assert seqType : "No seqtype found for ${_seqType}, ${libraryLayout} and bulk"

                ReferenceGenome referenceGenome = ReferenceGenome.findByName(_referenceGenome)
                assert referenceGenome: "no reference genom found for ${_referenceGenome}"

                ExternalMergingWorkPackage emwp = new ExternalMergingWorkPackage(
                        referenceGenome         : referenceGenome,
                        sample                  : sample,
                        seqType                 : seqType,
                        pipeline                : Pipeline.findByNameAndType(Pipeline.Name.EXTERNALLY_PROCESSED, Pipeline.Type.ALIGNMENT),
                        libraryPreparationKit   : LibraryPreparationKit.findByName(libraryPreparationKit)
                )
                assert emwp.save(flush:true)

                ExternallyProcessedMergedBamFile epmbf = new ExternallyProcessedMergedBamFile(
                        workPackage         : emwp,
                        importedFrom        : bamFilePath,
                        fileName            : getNameFromPath(bamFilePath),
                        coverage            : coverage ? Double.parseDouble(coverage) : null,
                        md5sum              : md5sum ?: null,
                        furtherFiles        : [] as Set,
                        insertSizeFile      : insertSizeFile
                ).save(flush: true)

                Path bamFileParent = fileSystem.getPath(epmbf.importedFrom).parent

                furtherFiles.findAll().findAll { String path ->
                    Files.exists(bamFileParent.resolve(path))
                }.each {
                        epmbf.furtherFiles.add(it)
                }

                if (insertSizeFile) {
                    Path insertSizeFilePath = bamFileParent.resolve(insertSizeFile)
                    if (!epmbf.furtherFiles.find {
                        Path furtherPath = bamFileParent.resolve(it)
                        insertSizeFilePath.startsWith(furtherPath)
                    }) {
                        epmbf.furtherFiles.add(insertSizeFile)
                    }
                }

                if (qualityControlFile) {
                    Path qualityControlFilePath = bamFileParent.resolve(qualityControlFile)

                    def qcValues = new JsonSlurper().parse(qualityControlFilePath.bytes)

                    new ExternalProcessedMergedBamFileQualityAssessment(
                            properlyPaired                  : qcValues.all.properlyPaired,
                            pairedInSequencing              : qcValues.all.pairedInSequencing,
                            insertSizeMedian                : qcValues.all.insertSizeMedian,
                            insertSizeCV                    : qcValues.all.insertSizeCV,
                            qualityAssessmentMergedPass     : new QualityAssessmentMergedPass([
                                    abstractMergedBamFile   : epmbf,
                            ]).save(flush: true),
                    ).save(flush: true)

                    if (!epmbf.furtherFiles.find {
                        Path furtherPath = bamFileParent.resolve(it)
                        qualityControlFilePath.startsWith(furtherPath)
                    }) {
                        epmbf.furtherFiles.add(qualityControlFile)
                    }
                }

                emwp.bamFileInProjectFolder = null
                assert epmbf.save(flush:true)
                importProcess.externallyProcessedMergedBamFiles.add(epmbf)
            }

            importProcess.state = ImportProcess.State.NOT_STARTED
            importProcess.replaceSourceWithLink = replaceWithLink
            importProcess.triggerAnalysis = triggerAnalysis
            assert importProcess.save(flush:true)

            if (importProcess.triggerAnalysis) {
                samplePairDeciderService.findOrCreateSamplePairs(importProcess.externallyProcessedMergedBamFiles*.workPackage)
            }


            outputProject = importProcess.externallyProcessedMergedBamFiles.first().project
        }

        return [
                context : context,
                importProcess: importProcess,
                project : outputProject,
        ]
    }

    private static String getNameFromPath(String path) {
        return new File(path).name
    }

    static String uniqueColumnValue(Row row, BamMetadataColumn column) {
        return row.getCell(row.spreadsheet.getColumn(column.name()))?.text
    }
}
