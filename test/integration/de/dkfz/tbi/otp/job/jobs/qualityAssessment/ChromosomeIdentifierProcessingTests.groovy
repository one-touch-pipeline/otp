package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import static org.junit.Assert.*
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class ChromosomeIdentifierProcessingTests {

    ChromosomeIdentifierProcessingService chromosomeIdentifierProcessingService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    /**
     * tests if the method execute (including the services mapping, sorting, filtering) works
     */
    @Test
    void testExecution() {
        Project project = new Project()
        project.name = "SOME_PROJECT"
        project.dirName = "/some/relative/path"
        project.realmName = "def"
        project.save(flush: true)
        assertTrue(project.validate())

        Individual individual = new Individual()
        individual.pid = "SOME_PATIENT_ID"
        individual.mockPid = "PUBLIC_PID"
        individual.mockFullName = "PUBLIC_NAME"
        individual.type = Individual.Type.REAL
        individual.project = project
        individual.save(flush: true)
        assertTrue(individual.validate())

        SampleType sampleType = new SampleType()
        sampleType.name = "TUMOR"
        sampleType.save(flush: true)
        assertTrue(sampleType.validate())

        Sample sample = new Sample()
        sample.individual = individual
        sample.sampleType = sampleType
        sample.save(flush: true)
        assertTrue(sample.validate())

        SeqType seqType = new SeqType()
        seqType.name = "WHOLE_GENOME"
        seqType.libraryLayout = "SINGLE"
        seqType.dirName = "whole_genome_sequencing"
        seqType.save(flush: true)
        assertTrue(seqType.validate())

        SeqCenter seqCenter = new SeqCenter()
        seqCenter.name = "DKFZ"
        seqCenter.dirName = "core"
        seqCenter.save(flush: true)
        assertTrue(seqCenter.validate())

        SeqPlatform seqPlatform = new SeqPlatform()
        seqPlatform.name = "solid"
        seqPlatform.model = "4"
        seqPlatform.save(flush: true)
        assertTrue(seqPlatform.validate())

        Run run = new Run()
        run.name = "testname"
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        run.save(flush: true)
        assertTrue(run.validate())

        SoftwareTool softwareTool = new SoftwareTool()
        softwareTool.programName = "SOLID"
        softwareTool.programVersion = "0.4.8"
        softwareTool.qualityCode = null
        softwareTool.type = SoftwareTool.Type.ALIGNMENT
        softwareTool.save(flush: true)
        assertTrue(softwareTool.validate())

        SeqTrack seqTrack = new SeqTrack()
        seqTrack.laneId = "123"
        seqTrack.run = run
        seqTrack.sample = sample
        seqTrack.seqType = seqType
        seqTrack.seqPlatform = seqPlatform
        seqTrack.pipelineVersion = softwareTool
        seqTrack.save(flush: true)
        assertTrue(seqTrack.validate())

        AlignmentPass alignmentPass = new AlignmentPass()
        alignmentPass.identifier = 2
        alignmentPass.seqTrack = seqTrack
        alignmentPass.description = "test"
        alignmentPass.save(flush: true)
        assertTrue(alignmentPass.validate())

        ProcessedBamFile processedBamFile = new ProcessedBamFile()
        processedBamFile.type = AbstractBamFile.BamType.SORTED
        processedBamFile.fileExists = true
        processedBamFile.dateFromFileSystem = new Date()
        processedBamFile.alignmentPass = alignmentPass
        processedBamFile.save(flush: true)
        assertTrue(processedBamFile.validate())


        ReferenceGenome referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.filePath = "bla"
        referenceGenome.save(flush: true)
        assertTrue(referenceGenome.validate())


        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType()
        referenceGenomeProjectSeqType.project = project
        referenceGenomeProjectSeqType.seqType = seqType
        referenceGenomeProjectSeqType.referenceGenome = referenceGenome
        referenceGenomeProjectSeqType.save(flush: true)
        assertTrue(referenceGenomeProjectSeqType.validate())


        Realm realm = new Realm()
        realm.name = "def"
        realm.env = "test"
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.cluster = Realm.Cluster.DKFZ
        realm.rootPath = ""
        realm.processingRootPath = ""
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000000
        realm.pbsOptions = ""
        realm.save(flush : true)
        assertTrue(realm.validate())

        assertTrue(chromosomeIdentifierProcessingService.execute(processedBamFile))
    }
}
