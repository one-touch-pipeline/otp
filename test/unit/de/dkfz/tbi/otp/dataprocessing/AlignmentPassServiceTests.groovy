package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.util.Environment

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(AlignmentPassService)
@TestMixin(GrailsUnitTestMixin)
@Mock([Realm, SeqPlatform, SeqCenter, SoftwareTool, SoftwareTool, Run, Project, AlignmentPassService, Individual, Sample, SeqType, SeqTrack, AlignmentPass, ReferenceGenome, ReferenceGenomeProjectSeqType, SampleType])
class AlignmentPassServiceTests {

    AlignmentPassService alignmentPassService
    AlignmentPass alignmentPass
    ReferenceGenome referenceGenome
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    @Before
    void setUp() {
        alignmentPassService = new AlignmentPassService()
        alignmentPassService.referenceGenomeService = new ReferenceGenomeService()
        alignmentPassService.referenceGenomeService.configService = new ConfigService()

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

        SeqTrack seqTrack = new SeqTrack()
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

    @After
    void tearDown() {
        alignmentPass = null
        referenceGenome = null
        referenceGenomeProjectSeqType = null
        alignmentPassService = null
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
        String pathExp = "/tmp/reference_genomes/referenceGenome/prefixName"
        String pathAct = alignmentPassService.referenceGenomePath(alignmentPass)
        assertEquals(pathExp, pathAct)
    }
}
