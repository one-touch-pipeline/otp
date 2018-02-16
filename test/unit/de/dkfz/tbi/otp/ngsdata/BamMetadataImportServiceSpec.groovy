package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
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
        SeqType
])
class BamMetadataImportServiceSpec extends Specification {

    @Rule
    public TemporaryFolder temporaryFolder

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
        File qualityControlFile = new File(qualityDirectory, "file.qc")
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

        DomainFactory.createReferenceGenome(name: "refGen1")
        Individual individual1 = DomainFactory.createIndividual(mockPid: "individual1", project: project)
        SampleType sampleType1 = DomainFactory.createSampleType(name: "sampleType1")
        DomainFactory.createSeqType(name: "seqType1", libraryLayout: LibraryLayout.SINGLE)
        DomainFactory.createSample(individual: individual1, sampleType: sampleType1)
        DomainFactory.createExternallyProcessedPipelineLazy()

        DomainFactory.createReferenceGenome(name: "refGen2")
        Individual individual2 = DomainFactory.createIndividual(mockPid: "individual2", project: project)
        SampleType sampleType2 = DomainFactory.createSampleType(name: "sampleType2")
        DomainFactory.createSeqType(name: "seqType2", libraryLayout: LibraryLayout.SINGLE)
        DomainFactory.createSample(individual: individual2, sampleType: sampleType2)
        DomainFactory.createExternallyProcessedPipelineLazy()

        File bamFilesDir = temporaryFolder.newFolder("path-to-bam-files")
        File pseudoBamFile1 = new File(bamFilesDir, "control_123456_merged.mdup.bam")
        File pseudoBamFile2 = new File(bamFilesDir, "tumor_456789_merged.mdup.bam")
        File qualityDirectory = temporaryFolder.newFolder("quality")
        File qualityControlFile = new File(qualityDirectory, "file.qc")
        List<String> furtherFiles = ["/quality"]

        Path metadataFile = temporaryFolder.newFile("bamMetadata.tsv").toPath()
        metadataFile.bytes = (
                "${REFERENCE_GENOME}\t${SEQUENCING_TYPE}\t${BAM_FILE_PATH}\t${SAMPLE_TYPE}\t${INDIVIDUAL}\t${LIBRARY_LAYOUT}\t${PROJECT}\t${COVERAGE}\n" +
                        "refGen1\tseqType1\t${pseudoBamFile1.path}\tsampleType1\tindividual1\tSINGLE\tproject_01\t\n" +
                        "refGen2\tseqType2\t${pseudoBamFile2.path}\tsampleType2\tindividual2\tSINGLE\tproject_01\t\n"
        ).getBytes(BamMetadataValidationContext.CHARSET)

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
        results.project.name == "project_01"
        assert results.importProcess.externallyProcessedMergedBamFiles.find {
            (
                    it.referenceGenome.name == "refGen1" &&
                    it.individual.mockPid == "individual1" &&
                    it.project.name == "project_01" &&
                    it.seqType.libraryLayout == "SINGLE" &&
                    it.seqType.name == "seqType1" &&
                    it.sampleType.name == "sampleType1" &&
                    it.importedFrom == pseudoBamFile1.absolutePath &&
                    it.fileName == "control_123456_merged.mdup.bam"
            )
        } && results.importProcess.externallyProcessedMergedBamFiles.find {
            (
                    it.referenceGenome.name == "refGen2" &&
                    it.individual.mockPid == "individual2" &&
                    it.project.name == "project_01" &&
                    it.seqType.libraryLayout == "SINGLE" &&
                    it.seqType.name == "seqType2" &&
                    it.sampleType.name == "sampleType2" &&
                    it.importedFrom == pseudoBamFile2.absolutePath &&
                    it.fileName == "tumor_456789_merged.mdup.bam"
            )
        }
    }
}
