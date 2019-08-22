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


import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.util.spreadsheet.validation.Level

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class BamMetadataImportServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ExternalMergingWorkPackage,
                ExternallyProcessedMergedBamFile,
                ExternalProcessedMergedBamFileQualityAssessment,
                QualityAssessmentMergedPass,
                ImportProcess,
                Individual,
                Project,
                Pipeline,
                Realm,
                ReferenceGenome,
                Sample,
                SampleType,
                SeqType,
                LibraryPreparationKit,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    void "getImplementedValidations returns descriptions of validations"() {
        given:
        BamMetadataImportService service = new BamMetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(BamMetadataValidator) >>
                    [
                            'validator1': [
                                    getDescriptions: { ['description1', 'description2'] }
                            ] as BamMetadataValidator,
                            'validator2': [
                                    getDescriptions: { ['description3'] }
                            ] as BamMetadataValidator,
                    ]
        }

        expect:
        containSame(service.getImplementedValidations(), ['description1', 'description2', 'description3'])
    }

    void "validate creates context and calls validators"() {
        given:
        File testDirectory = TestCase.createEmptyTestDirectory()
        Path metadataFile = Paths.get(testDirectory.path, 'bamMetadata.tsv')
        metadataFile.bytes = 'Header\nI am metadata!'.getBytes(BamMetadataValidationContext.CHARSET)
        File qualityDirectory = new File(testDirectory, "quality")
        assert qualityDirectory.mkdirs()
        new File(qualityDirectory, "file.qc")
        List<String> furtherFiles = ["/quality"]

        BamMetadataImportService service = new BamMetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(BamMetadataValidator) >>
                    [
                            'validator1': [
                                    validate: { BamMetadataValidationContext context ->
                                        context.addProblem(Collections.emptySet(), Level.ERROR, 'message1')
                                        context.addProblem(Collections.emptySet(), Level.ERROR, 'message2')
                                    }
                            ] as BamMetadataValidator,
                            'validator2': [
                                    validate: { BamMetadataValidationContext context ->
                                        context.addProblem(Collections.emptySet(), Level.ERROR, 'message3')
                                    }
                            ] as BamMetadataValidator,
                    ]
        }
        service.fileSystemService = new TestFileSystemService()

        when:
        BamMetadataValidationContext context = service.validate(metadataFile.toString(), furtherFiles)

        then:
        containSame(context.problems*.message, ['message1', 'message2', 'message3'])
        context.metadataFile == metadataFile
        context.spreadsheet.header.cells[0].text == 'Header'
        context.spreadsheet.dataRows[0].cells[0].text == 'I am metadata!'

        cleanup:
        testDirectory.deleteDir()
    }

    void "validateAndImport, when there are no problems, returns an importProcess object"() {
        given:
        Project project = DomainFactory.createProject(name: "project_01")
        DomainFactory.createExternallyProcessedPipelineLazy()
        File bamFilesDir = temporaryFolder.newFolder("path-to-bam-files")
        File qualityDirectory = temporaryFolder.newFolder("path-to-bam-files", "qualityDir")
        CreateFileHelper.createFile(new File(bamFilesDir, "qualityFile"))
        CreateFileHelper.createFile(new File(qualityDirectory, "file.qc"))
        File qualityControl = CreateFileHelper.createFile(new File(bamFilesDir, "qualityControl.json"))
        qualityControl.bytes = ("""\
        { "all":{"insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157,  "properlyPaired": 2050531101 }}
        """).getBytes(BamMetadataValidationContext.CHARSET)

        (1..4).each {
            DomainFactory.createReferenceGenome(name: "refGen${it}")
            Individual individual = DomainFactory.createIndividual(mockPid: "individual${it}", project: project)
            SampleType sampleType = DomainFactory.createSampleType(name: "sampleType${it}")
            DomainFactory.createSeqType(name: "seqType${it}", libraryLayout: LibraryLayout.SINGLE)
            DomainFactory.createSample(individual: individual, sampleType: sampleType)
        }

        List<String> furtherFiles = [
                "qualityDir",
                "qualityFile",
        ]

        Path metadataFile = metaDataFileForTestValidateAndImport(bamFilesDir, qualityControl)

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(metadataFile: metadataFile)

        BamMetadataImportService service = new BamMetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(BamMetadataValidator) >> [:]
        }
        service.fileSystemService = new TestFileSystemService()
        service.seqTypeService = new SeqTypeService()
        service.samplePairDeciderService = Mock(SamplePairDeciderService) {
            1 * findOrCreateSamplePairs(_)
        }

        when:
        Map results = service.validateAndImport(metadataFile.toString(), true, context.metadataFileMd5sum,
                false, true, furtherFiles)

        then:
        results.context.metadataFile == metadataFile
        results.context.problems.empty
        results.project?.name == "project_01"
        results.importProcess.externallyProcessedMergedBamFiles.size() == 4

        (1..4).each {
            String pid = "individual${it}"
            ExternallyProcessedMergedBamFile epmbf = results.importProcess.externallyProcessedMergedBamFiles.find {
                it.individual.mockPid == pid
            }
            assert epmbf: "${pid} not found in the result"
            assert epmbf.referenceGenome.name == "refGen${it}"
            assert epmbf.project.name == "project_01"
            assert epmbf.seqType.libraryLayout == LibraryLayout.SINGLE
            assert epmbf.seqType.name == "seqType${it}"
            assert epmbf.sampleType.name == "sampleType${it}"
            assert epmbf.importedFrom == new File(bamFilesDir, "bamfile${it}_merged.mdup.bam").path
            assert epmbf.furtherFiles.contains('qualityDir')
            assert epmbf.furtherFiles.contains('qualityFile')
            switch (it) {
                case 1:
                    assert epmbf.insertSizeFile == "insertSize.txt"
                    assert epmbf.furtherFiles.contains('insertSize.txt')
                    assert epmbf.furtherFiles.size() == 3
                    break
                case 2:
                    assert epmbf.insertSizeFile == "qualityDir/insertSize.txt"
                    assert epmbf.furtherFiles.size() == 2
                    break
                case 3:
                    assert epmbf.insertSizeFile == "qualityDirinsertSize.txt"
                    assert epmbf.furtherFiles.contains('qualityDirinsertSize.txt')
                    assert epmbf.furtherFiles.size() == 3
                    break
                case 4:
                    assert epmbf.insertSizeFile == "qualityFileinsertSize.txt"
                    assert epmbf.furtherFiles.contains('qualityFileinsertSize.txt')
                    assert epmbf.furtherFiles.contains(qualityControl.name)
                    assert epmbf.furtherFiles.size() == 4
                    assert ExternalProcessedMergedBamFileQualityAssessment.findAll().size() == 1
                    assert ExternalProcessedMergedBamFileQualityAssessment.findAll().get(0).insertSizeCV == 23
                    break
            }
        }
    }

    private Path metaDataFileForTestValidateAndImport(File bamFilesDir, File qualityControl) {
        Path metadataFile = temporaryFolder.newFile("bamMetadata.tsv").toPath()
        metadataFile.bytes = ("""\
${REFERENCE_GENOME}\t${SEQUENCING_TYPE}\t${BAM_FILE_PATH}\t${SAMPLE_TYPE}\t${INDIVIDUAL}\t${LIBRARY_LAYOUT}\t${
            PROJECT
        }\t${COVERAGE}\t${INSERT_SIZE_FILE}\t${QUALITY_CONTROL_FILE}
refGen1\tseqType1\t${bamFilesDir}/bamfile1_merged.mdup.bam\tsampleType1\tindividual1\t${LibraryLayout.SINGLE}\tproject_01\t\tinsertSize.txt
refGen2\tseqType2\t${bamFilesDir}/bamfile2_merged.mdup.bam\tsampleType2\tindividual2\t${LibraryLayout.SINGLE}\tproject_01\t\tqualityDir/insertSize.txt
refGen3\tseqType3\t${bamFilesDir}/bamfile3_merged.mdup.bam\tsampleType3\tindividual3\t${LibraryLayout.SINGLE}\tproject_01\t\tqualityDirinsertSize.txt
refGen4\tseqType4\t${bamFilesDir}/bamfile4_merged.mdup.bam\tsampleType4\tindividual4\t${LibraryLayout.SINGLE}\tproject_01\t\tqualityFileinsertSize.txt\t${qualityControl.name}
""").getBytes(BamMetadataValidationContext.CHARSET)
        return metadataFile
    }
}
