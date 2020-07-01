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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.Md5sumFormatValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.validation.Level

import java.nio.file.Path
import java.security.MessageDigest

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.HelperUtils.byteArrayToHexString

@Rollback
@Integration
class BamMetadataImportServiceIntegrationSpec extends Specification implements RoddyPancanFactory, UserAndRoles {

    @Autowired
    BamMetadataImportService bamMetadataImportService

    void 'getBamMetadataValidators returns BamMetadataValidators'() {
        expect:
        bamMetadataImportService.bamMetadataValidators.find { it instanceof Md5sumFormatValidator }
    }

    class DataBamImportRow implements DomainFactoryCore {

        TemporaryFolder temporaryFolder

        Sample sample = createSample()

        SeqType seqType = createSeqType()

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        ReferenceGenome referenceGenome = createReferenceGenome()

        File bamFilesDir

        File bamFile

        File baiFile

        File insertSizeFile

        File qualityControlFile

        Map<BamMetadataColumn, String> metaData

        DataBamImportRow(TemporaryFolder temporaryFolder, Map<BamMetadataColumn, String> metaDataChanges = [:]) {
            this.temporaryFolder = temporaryFolder
            DomainFactory.createExternallyProcessedPipelineLazy()

            bamFilesDir = temporaryFolder.newFolder("path-to-bam-files")
            bamFile = CreateFileHelper.createFile(new File(bamFilesDir, "bamFile.bam"))
            baiFile = CreateFileHelper.createFile(new File(bamFilesDir, "bamFile.bai"))
            insertSizeFile = CreateFileHelper.createFile(new File(bamFilesDir, "insertSize.txt"))

            qualityControlFile = CreateFileHelper.createFile(new File(bamFilesDir, "qualityControl.json"), """\
{ "all":{"insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157,  "properlyPaired": 2050531101 }}
""")

            metaData = [
                    (BAM_FILE_PATH)          : bamFile.absolutePath,
                    (PROJECT)                : project.name,
                    (INDIVIDUAL)             : individual.pid,
                    (SAMPLE_TYPE)            : sampleType.name,
                    (SEQUENCING_TYPE)        : seqType.name,
                    (SEQUENCING_READ_TYPE)   : seqType.libraryLayout.toString(),
                    (LIBRARY_PREPARATION_KIT): libraryPreparationKit?.name,
                    (REFERENCE_GENOME)       : referenceGenome.name,
                    (MD5)                    : HelperUtils.randomMd5sum,
                    (COVERAGE)               : nextId,
                    (MAXIMAL_READ_LENGTH)    : nextId,
                    (INSERT_SIZE_FILE)       : insertSizeFile.name,
                    (QUALITY_CONTROL_FILE)   : qualityControlFile.name,
            ] + metaDataChanges
        }

        List<File> createDirectories(String... directories) {
            return directories.collect {
                CreateFileHelper.createFile(new File(new File(bamFilesDir, it), 'someFile'))
            }
        }

        List<File> createFiles(String... files) {
            return files.collect {
                CreateFileHelper.createFile(new File(bamFilesDir, it))
            }
        }

        SampleType getSampleType() {
            return sample.sampleType
        }

        Individual getIndividual() {
            sample.individual
        }

        Project getProject() {
            individual.project
        }

        void assertBamFile(ExternallyProcessedMergedBamFile epmbf) {
            assert epmbf.individual == individual
            assert epmbf.referenceGenome == referenceGenome
            assert epmbf.project == project
            assert epmbf.seqType == seqType
            assert epmbf.sampleType == sampleType
            assert epmbf.importedFrom == bamFile.absolutePath
        }
    }

    class DataBamImportMetaData implements DomainFactoryCore {

        TemporaryFolder temporaryFolder

        List<DataBamImportRow> rows

        Path metadataFile

        String md5sum

        DataBamImportMetaData(TemporaryFolder temporaryFolder, List<DataBamImportRow> rows) {
            createUserAndRoles()
            this.temporaryFolder = temporaryFolder
            this.rows = rows
            writeMetadata()
            md5sum = byteArrayToHexString(MessageDigest.getInstance('MD5').digest(metadataFile.bytes))
        }

        private void writeMetadata() {
            List<Map<BamMetadataColumn, String>> metaData = rows*.metaData
            List<BamMetadataColumn> keys = metaData*.keySet().flatten().unique()
            List<String> content = []
            content << keys.join('\t')
            metaData.each { Map<BamMetadataColumn, String> map ->
                content << keys.collect { BamMetadataColumn key ->
                    map[key] ?: ''
                }.join('\t')
            }
            metadataFile = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), 'bamImport.tsv'), content.join('\n')).toPath()
        }
    }

    @Rule
    TemporaryFolder temporaryFolder

    void assertResultNoError(Map results, DataBamImportRow dataBamImportRow, Path metadataFile) {
        assert results.context.metadataFile == metadataFile
        assert results.context.problemsObject.maximumProblemLevel.intValue() < Level.ERROR.intValue()
        assert results.importProcess
        assert results.importProcess.externallyProcessedMergedBamFiles.size() == 1
        dataBamImportRow.assertBamFile(results.importProcess.externallyProcessedMergedBamFiles[0])
        assert results.project?.name == dataBamImportRow.project.name
    }

    void assertResultError(Map results, Path metadataFile, List<String> expectedErrorMessages) {
        assert results.context.metadataFile == metadataFile
        assert results.context.problemsObject.maximumProblemLevel.intValue() == Level.ERROR.intValue()
        assert results.importProcess == null
        assert results.project == null

        Collection<String> foundErrorMessages = results.context.problemsObject.problems.findAll {
            it.level == Level.ERROR
        }*.message
        TestCase.assertContainSame(expectedErrorMessages, foundErrorMessages)
    }

    void "validateAndImport, if all values given, then all values should be set"() {
        given:
        DataBamImportRow dataBamImportRow = new DataBamImportRow(temporaryFolder, [:])
        DataBamImportMetaData dataBamImportMetaData = new DataBamImportMetaData(temporaryFolder, [dataBamImportRow])
        Map results

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            results = bamMetadataImportService.validateAndImport(dataBamImportMetaData.metadataFile.toString(), true,
                    dataBamImportMetaData.md5sum, ImportProcess.LinkOperation.COPY_AND_KEEP, false, [])
        }

        then:
        assertResultNoError(results, dataBamImportRow, dataBamImportMetaData.metadataFile)

        and:
        ExternallyProcessedMergedBamFile bamFile = results.importProcess.externallyProcessedMergedBamFiles[0]
        bamFile.md5sum
        bamFile.coverage
        bamFile.maximumReadLength
        bamFile.insertSizeFile
        bamFile.overallQualityAssessment
        bamFile.mergingWorkPackage.libraryPreparationKit
    }

    @Unroll
    void "validateAndImport, case #name with operation #linkOperation, then create importProcess without any errors"() {
        given:
        DataBamImportRow dataBamImportRow = new DataBamImportRow(temporaryFolder, updateMap)
        DataBamImportMetaData dataBamImportMetaData = new DataBamImportMetaData(temporaryFolder, [dataBamImportRow])
        Map results

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            results = bamMetadataImportService.validateAndImport(dataBamImportMetaData.metadataFile.toString(), true,
                    dataBamImportMetaData.md5sum, linkOperation, false, [])
        }

        then:
        assertResultNoError(results, dataBamImportRow, dataBamImportMetaData.metadataFile)

        where:
        name                         | linkOperation                             | updateMap
        //copy and keep
        'all given'                  | ImportProcess.LinkOperation.COPY_AND_KEEP | [:]
        'no md5sum'                  | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.MD5): null]
        'no coverage'                | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.COVERAGE): null]
        'no maximal read length'     | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.MAXIMAL_READ_LENGTH): null]
        'no insert size file'        | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.INSERT_SIZE_FILE): null]
        'no quality control file'    | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.QUALITY_CONTROL_FILE): null]
        'no library preperation kit' | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.LIBRARY_PREPARATION_KIT): null]
        //copy and link
        'all given'                  | ImportProcess.LinkOperation.COPY_AND_LINK | [:]
        'no md5sum'                  | ImportProcess.LinkOperation.COPY_AND_LINK | [(BamMetadataColumn.MD5): null]
        'no coverage'                | ImportProcess.LinkOperation.COPY_AND_LINK | [(BamMetadataColumn.COVERAGE): null]
        'no maximal read length'     | ImportProcess.LinkOperation.COPY_AND_LINK | [(BamMetadataColumn.MAXIMAL_READ_LENGTH): null]
        'no insert size file'        | ImportProcess.LinkOperation.COPY_AND_LINK | [(BamMetadataColumn.INSERT_SIZE_FILE): null]
        'no quality control file'    | ImportProcess.LinkOperation.COPY_AND_LINK | [(BamMetadataColumn.QUALITY_CONTROL_FILE): null]
        'no library preperation kit' | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.LIBRARY_PREPARATION_KIT): null]
        //link source
        'all given'                  | ImportProcess.LinkOperation.LINK_SOURCE   | [:]
        'no coverage'                | ImportProcess.LinkOperation.LINK_SOURCE   | [(BamMetadataColumn.COVERAGE): null]
        'no insert size file'        | ImportProcess.LinkOperation.LINK_SOURCE   | [(BamMetadataColumn.INSERT_SIZE_FILE): null]
        'no quality control file'    | ImportProcess.LinkOperation.LINK_SOURCE   | [(BamMetadataColumn.QUALITY_CONTROL_FILE): null]
        'no library preperation kit' | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.LIBRARY_PREPARATION_KIT): null]
    }

    @Unroll
    void "validateAndImport, case #name with operation #linkOperation, then create no importProcess and return errors"() {
        given:
        DataBamImportRow dataBamImportRow = new DataBamImportRow(temporaryFolder, updateMap)
        DataBamImportMetaData dataBamImportMetaData = new DataBamImportMetaData(temporaryFolder, [dataBamImportRow])
        Map results

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            results = bamMetadataImportService.validateAndImport(dataBamImportMetaData.metadataFile.toString(), true,
                    dataBamImportMetaData.md5sum, linkOperation, false, [])
        }
        then:
        assertResultError(results, dataBamImportMetaData.metadataFile, errorText)

        where:
        name                                    | linkOperation                             | updateMap                                                                                                                                         || errorText
        'no md5sum given'                       | ImportProcess.LinkOperation.LINK_SOURCE   | [(BamMetadataColumn.MD5): null,]                                                                                                                  || ["The md5sum is required, if the files should only be linked"]
        'no maximal read length given'          | ImportProcess.LinkOperation.LINK_SOURCE   | [(BamMetadataColumn.MAXIMAL_READ_LENGTH): null,]                                                                                                  || ["The maximalReadLength is required, if the files should only be linked"]
        'bam file does not ends with bam'       | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.BAM_FILE_PATH): 'test', (BamMetadataColumn.INSERT_SIZE_FILE): null, (BamMetadataColumn.QUALITY_CONTROL_FILE): null,]          || ["Filename 'test' does not end with '.bam'.", "The path 'test' is no absolute path."]
        'bam file path is no absolute path'     | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.BAM_FILE_PATH): 'test.bam', (BamMetadataColumn.INSERT_SIZE_FILE): null, (BamMetadataColumn.QUALITY_CONTROL_FILE): null,]      || ["The path 'test.bam' is no absolute path."]
        'bam file does not exist'               | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.BAM_FILE_PATH): '/tmp/test.bam', (BamMetadataColumn.INSERT_SIZE_FILE): null, (BamMetadataColumn.QUALITY_CONTROL_FILE): null,] || ["'/tmp/test.bam' does not exist or cannot be accessed by OTP."]
        'no project given'                      | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.PROJECT): null,]                                                                                                              || ["The project '' is not registered in OTP."]
        'no individual and sample type  given'  | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.INDIVIDUAL): null, (BamMetadataColumn.SAMPLE_TYPE): null,]                                                                    || ["The individual '' is not registered in OTP.", "The sample as combination of the individual '' and the sample type '' is not registered in OTP.", "The sample type '' is not registered in OTP."]
        'no sequencing type given'              | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.SEQUENCING_TYPE): null,]                                                                                                      || ["No seqType is given."]
        'sequencing type is unknown'            | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.SEQUENCING_TYPE): 'unknownSeqType',]                                                                                          || ["The sequencing type 'unknownSeqType' is not registered in OTP."]
        'no sequencing read type given'         | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.SEQUENCING_READ_TYPE): null,]                                                                                                 || ["Library layout '' is not registered in OTP."]
        'sequencing read type is unknown'       | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.SEQUENCING_READ_TYPE): 'unknownSequencingRead',]                                                                              || ["Library layout 'unknownSequencingRead' is not registered in OTP."]
        'no reference genome given'             | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.REFERENCE_GENOME): null,]                                                                                                     || ["The reference genome '' is not registered in OTP."]
        'unknown reference genome'              | ImportProcess.LinkOperation.COPY_AND_KEEP | [(BamMetadataColumn.REFERENCE_GENOME): 'unknownReferenceGenome',]                                                                                 || ["The reference genome 'unknownReferenceGenome' is not registered in OTP."]
    }

    void "validateAndImport, when further files given, then imported bam files has them if they exist on file system"() {
        given:
        DataBamImportRow dataBamImportRow = new DataBamImportRow(temporaryFolder, [:])
        DataBamImportMetaData dataBamImportMetaData = new DataBamImportMetaData(temporaryFolder, [dataBamImportRow])

        String qualityDirExistAndCopy = "dirExistAndCopy"
        String qualityDirExistAndNotCopy = "DirExistAndNotCopy"
        String qualityDirNotExistAndCopy = "DirNotExistAndCopy"

        String qualityFileExistAndCopy = "dir1/FileExistAndCopy"
        String qualityFileExistAndNotCopy = "dir2/FileExistAndNotCopy"
        String qualityFileNotExistAndCopy = "dir3/FileNotExistAndCopy"

        List<String> furtherFiles = [
                qualityDirExistAndCopy,
                qualityDirNotExistAndCopy,
                qualityFileExistAndCopy,
                qualityFileNotExistAndCopy,
        ]

        List<String> expected = [
                qualityDirExistAndCopy,
                qualityFileExistAndCopy,
                dataBamImportRow.insertSizeFile.name,
                dataBamImportRow.qualityControlFile.name,
        ]

        dataBamImportRow.createDirectories(qualityDirExistAndCopy, qualityDirExistAndNotCopy)
        dataBamImportRow.createFiles(qualityFileExistAndCopy, qualityFileExistAndNotCopy)

        Map results

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            results = bamMetadataImportService.validateAndImport(dataBamImportMetaData.metadataFile.toString(), true,
                    dataBamImportMetaData.md5sum, ImportProcess.LinkOperation.COPY_AND_KEEP, false, furtherFiles)
        }

        then:
        assertResultNoError(results, dataBamImportRow, dataBamImportMetaData.metadataFile)
        TestCase.assertContainSame(results.importProcess.externallyProcessedMergedBamFiles[0].furtherFiles, expected)
        ExternalProcessedMergedBamFileQualityAssessment.findAll().size() == 1
        ExternalProcessedMergedBamFileQualityAssessment.findAll().get(0).insertSizeCV == 23
    }
}
