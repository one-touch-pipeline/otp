package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.util.Environment
import org.junit.After
import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(AlignmentPassService)
@TestMixin(GrailsUnitTestMixin)
@Mock([
    AlignmentPass,
    AlignmentPassService,
    DataFile,
    Individual,
    ProcessedBamFile,
    Project,
    Realm,
    ReferenceGenome,
    ReferenceGenomeProjectSeqType,
    Run,
    Sample,
    SampleType,
    SeqCenter,
    SeqPlatform,
    SeqTrack,
    SeqType,
    SoftwareTool,
])
class AlignmentPassServiceUnitTests {

    AlignmentPassService alignmentPassService
    SeqTrack seqTrack
    AlignmentPass alignmentPass
    ReferenceGenome referenceGenome
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    File directory
    File file
    String referenceGenomePath

    @Before
    void setUp() {
        alignmentPassService = new AlignmentPassService()
        alignmentPassService.qualityAssessmentPassService = new QualityAssessmentPassService()
        alignmentPassService.referenceGenomeService = new ReferenceGenomeService()
        alignmentPassService.referenceGenomeService.configService = new ConfigService()

        referenceGenomePath = "/tmp/reference_genomes/referenceGenome/"

        directory = new File(referenceGenomePath)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        file = new File("${referenceGenomePath}prefixName.fa")
        if (!file.exists()) {
            file.createNewFile()
            file << "test"
        }

        Realm realm = new Realm()
        realm.name = "def"
        realm.env = Environment.getCurrent().getName()
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.cluster = Realm.Cluster.DKFZ
        realm.rootPath = ""
        realm.processingRootPath = "tmp"
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000000
        realm.pbsOptions = ""
        realm.save(flush : true)

        Project project = new Project()
        project.name = "SOME_PROJECT"
        project.dirName = "/tmp/alignmentPassService/"
        project.realmName = realm.name
        project.save(flush: true)

        Individual individual = new Individual()
        individual.pid = "SOME_PATIENT_ID"
        individual.mockPid = "PUBLIC_PID"
        individual.mockFullName = "PUBLIC_NAME"
        individual.type = Individual.Type.REAL
        individual.project = project
        individual.save(flush: true)

        SampleType sampleType = new SampleType()
        sampleType.name = "TUMOR"
        sampleType.save(flush: true)

        Sample sample = new Sample()
        sample.individual = individual
        sample.sampleType = sampleType
        sample.save(flush: true)

        SeqType seqType = new SeqType()
        seqType.name = "WHOLE_GENOME"
        seqType.libraryLayout = "SINGLE"
        seqType.dirName = "whole_genome_sequencing"
        seqType.save(flush: true)

        SeqCenter seqCenter = new SeqCenter()
        seqCenter.name = "DKFZ"
        seqCenter.dirName = "core"
        seqCenter.save(flush: true)

        SeqPlatform seqPlatform = new SeqPlatform()
        seqPlatform.name = "solid"
        seqPlatform.model = "4"
        seqPlatform.save(flush: true)

        Run run = new Run()
        run.name = "testname"
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        run.save(flush: true)

        SoftwareTool softwareTool = new SoftwareTool()
        softwareTool.programName = "SOLID"
        softwareTool.programVersion = "0.4.8"
        softwareTool.qualityCode = null
        softwareTool.type = SoftwareTool.Type.ALIGNMENT
        softwareTool.save(flush: true)

        seqTrack = new SeqTrack()
        seqTrack.laneId = "123"
        seqTrack.seqType = seqType
        seqTrack.sample = sample
        seqTrack.run = run
        seqTrack.seqPlatform = seqPlatform
        seqTrack.pipelineVersion = softwareTool
        seqTrack.save(flush: true)

        alignmentPass = new AlignmentPass()
        alignmentPass.identifier = 2
        alignmentPass.seqTrack = seqTrack
        alignmentPass.description = "test"
        alignmentPass.save(flush: true)

        referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.save(flush: true)

        referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType()
        referenceGenomeProjectSeqType.project = project
        referenceGenomeProjectSeqType.seqType = seqType
        referenceGenomeProjectSeqType.referenceGenome = referenceGenome
        referenceGenomeProjectSeqType.save(flush: true)
    }

    @Test
    void testFindNotStartedSeqTrackWithNonWithdrawnDataFile() {
        assertNull(alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        seqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        assertNull(alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        DataFile dataFile1 = new DataFile()
        dataFile1.seqTrack = seqTrack
        dataFile1.fileWithdrawn = false
        assertNotNull(dataFile1.save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        dataFile1.fileWithdrawn = true
        assertNotNull(dataFile1.save(flush: true))
        assertNull(alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        dataFile1.fileWithdrawn = false
        assertNotNull(dataFile1.save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        seqTrack.alignmentState = SeqTrack.DataProcessingState.IN_PROGRESS
        assertNull(alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        DataFile dataFile2 = new DataFile()
        dataFile2.seqTrack = seqTrack
        dataFile2.fileWithdrawn = false
        assertNotNull(dataFile2.save(flush: true))
        assertNull(alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        seqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        assertEquals(seqTrack, alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        dataFile2.fileWithdrawn = true
        assertNotNull(dataFile2.save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
        dataFile1.fileWithdrawn = true
        assertNotNull(dataFile1.save(flush: true))
        assertNull(alignmentPassService.findNotStartedSeqTrackWithNonWithdrawnDataFile())
    }

    @Test
    void testCreateAlignmentPass() {
        assertNull(alignmentPassService.createAlignmentPass())
        seqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        DataFile dataFile1 = new DataFile()
        dataFile1.seqTrack = seqTrack
        dataFile1.fileWithdrawn = false
        assertNotNull(dataFile1.save(flush: true))
        AlignmentPass pass1 = alignmentPassService.createAlignmentPass()
        assertNotNull(pass1)
        assertEquals(seqTrack, pass1.seqTrack)
        assertEquals(1, pass1.identifier)  // Should actually be 3, because the maximum existing
                                           // identifier is already 2 (created in the setup()
                                           // method of this test class).
        AlignmentPass pass2 = alignmentPassService.createAlignmentPass()
        assertNotNull(pass2)
        assertEquals(seqTrack, pass2.seqTrack)
        assertEquals(2, pass2.identifier)
        dataFile1.fileWithdrawn = true
        assertNotNull(dataFile1.save(flush: true))
        assertNull(alignmentPassService.createAlignmentPass())
    }

    @After
    void tearDown() {
        alignmentPass = null
        referenceGenome = null
        referenceGenomeProjectSeqType = null
        alignmentPassService = null
        directory.deleteOnExit()
        file.deleteOnExit()
    }

    @Test(expected = RuntimeException)
    void testReferenceGenomeNoReferenceGenomeForProjectAndSeqType() {
        Project project2 = new Project()
        project2.name = "test"
        project2.dirName = "/tmp/test"
        project2.realmName = "test"
        project2.save(flush: true)
        referenceGenomeProjectSeqType.project = project2
        referenceGenomeProjectSeqType.save(flush: true)
        alignmentPassService.referenceGenomePath(alignmentPass)
    }

    @Test
    void testReferenceGenomePathAllCorrect() {
        String pathExp = "${referenceGenomePath}prefixName.fa"
        String pathAct = alignmentPassService.referenceGenomePath(alignmentPass)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testAlignmentPassFinishedNull() {
        alignmentPass = null
        alignmentPassService.alignmentPassFinished(alignmentPass)
    }

    @Test
    void testAlignmentPassFinished() {
        ProcessedBamFile bamFile = new ProcessedBamFile(
                        type: BamType.SORTED,
                        alignmentPass: alignmentPass,
                        withdrawn: false,
                        qualityAssessmentStatus: QaProcessingStatus.NOT_STARTED
                        )
        bamFile.save(flush: true)
        alignmentPassService.alignmentPassFinished(alignmentPass)
        assertEquals(SeqTrack.DataProcessingState.FINISHED, alignmentPass.seqTrack.alignmentState)
        assertEquals(AbstractBamFile.QaProcessingStatus.NOT_STARTED, bamFile.qualityAssessmentStatus)
    }
}
