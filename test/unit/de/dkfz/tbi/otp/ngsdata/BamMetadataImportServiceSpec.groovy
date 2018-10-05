package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import org.springframework.context.*
import spock.lang.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        ExternalMergingWorkPackage,
        ExternallyProcessedMergedBamFile,
        ImportProcess,
        Individual,
        Project,
        Pipeline,
        Realm,
        ReferenceGenome,
        Sample,
        SampleType,
        SeqType,
        LibraryPreparationKit
])
class BamMetadataImportServiceSpec extends Specification {

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
        File qualityDirectory = new File(testDirectory,"quality")
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

        Path metadataFile = temporaryFolder.newFile("bamMetadata.tsv").toPath()
        metadataFile.bytes = ("""\
${REFERENCE_GENOME},${SEQUENCING_TYPE},${BAM_FILE_PATH},${SAMPLE_TYPE},${INDIVIDUAL},${LIBRARY_LAYOUT},${PROJECT},${COVERAGE},${INSERT_SIZE_FILE}
refGen1,seqType1,${bamFilesDir}/bamfile1_merged.mdup.bam,sampleType1,individual1,SINGLE,project_01,,insertSize.txt
refGen2,seqType2,${bamFilesDir}/bamfile2_merged.mdup.bam,sampleType2,individual2,SINGLE,project_01,,qualityDir/insertSize.txt
refGen3,seqType3,${bamFilesDir}/bamfile3_merged.mdup.bam,sampleType3,individual3,SINGLE,project_01,,qualityDirinsertSize.txt
refGen4,seqType4,${bamFilesDir}/bamfile4_merged.mdup.bam,sampleType4,individual4,SINGLE,project_01,,qualityFileinsertSize.txt
""".replaceAll(',', '\t')).getBytes(BamMetadataValidationContext.CHARSET)

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(metadataFile: metadataFile)

        BamMetadataImportService service = new BamMetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(BamMetadataValidator) >> [:]
        }
        service.fileSystemService = new TestFileSystemService()
        service.samplePairDeciderService = Mock(SamplePairDeciderService) {
            1 * findOrCreateSamplePairs(_)
        }

        when:
        Map results = service.validateAndImport(metadataFile.toString(), true, context.metadataFileMd5sum, false, true, furtherFiles)

        then:
        results.context.metadataFile == metadataFile
        results.context.problems.empty
        results.project?.name == "project_01"
        results.importProcess.externallyProcessedMergedBamFiles.size() == 4

        (1..4).each {
            String pid = "individual${it}"
            ExternallyProcessedMergedBamFile epmbf = results.importProcess.externallyProcessedMergedBamFiles.find  {
                it.individual.mockPid == pid
            }
            assert epmbf : "${pid} not found in the result"
            assert epmbf.referenceGenome.name == "refGen${it}"
            assert epmbf.project.name == "project_01"
            assert epmbf.seqType.libraryLayout == "SINGLE"
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
                    assert epmbf.furtherFiles.size() == 3
                    break
            }
        }
    }
}
