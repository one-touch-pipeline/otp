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
            ExternallyProcessedMergedBamFile epmbf = results.importProcess.externallyProcessedMergedBamFiles.find {
                it.individual.mockPid == pid
            }
            epmbf:
            "${pid} not found in the result"
            epmbf.referenceGenome.name == "refGen${it}"
            epmbf.project.name == "project_01"
            epmbf.seqType.libraryLayout == LibraryLayout.SINGLE
            epmbf.seqType.name == "seqType${it}"
            epmbf.sampleType.name == "sampleType${it}"
            epmbf.importedFrom == new File(bamFilesDir, "bamfile${it}_merged.mdup.bam").path
            epmbf.furtherFiles.contains('qualityDir')
            epmbf.furtherFiles.contains('qualityFile')
            switch (it) {
                case 1:
                    epmbf.insertSizeFile == "insertSize.txt"
                    epmbf.furtherFiles.contains('insertSize.txt')
                    epmbf.furtherFiles.size() == 3
                    break
                case 2:
                    epmbf.insertSizeFile == "qualityDir/insertSize.txt"
                    epmbf.furtherFiles.size() == 2
                    break
                case 3:
                    epmbf.insertSizeFile == "qualityDirinsertSize.txt"
                    epmbf.furtherFiles.contains('qualityDirinsertSize.txt')
                    epmbf.furtherFiles.size() == 3
                    break
                case 4:
                    epmbf.insertSizeFile == "qualityFileinsertSize.txt"
                    epmbf.furtherFiles.contains('qualityFileinsertSize.txt')
                    epmbf.furtherFiles.contains(qualityControl.name)
                    epmbf.furtherFiles.size() == 4
                    ExternalProcessedMergedBamFileQualityAssessment.findAll().size() == 1
                    ExternalProcessedMergedBamFileQualityAssessment.findAll().get(0).insertSizeCV == 23
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
refGen4\tseqType4\t${bamFilesDir}/bamfile4_merged.mdup.bam\tsampleType4\tindividual4\t${
            LibraryLayout.SINGLE
        }\tproject_01\t\tqualityFileinsertSize.txt\t${qualityControl.name}
""").getBytes(BamMetadataValidationContext.CHARSET)
        return metadataFile
    }
}
