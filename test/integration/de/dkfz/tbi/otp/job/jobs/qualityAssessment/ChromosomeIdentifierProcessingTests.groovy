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
    ProcessedBamFile processedBamFile
    File root
    File directory
    File fileInput
    File fileOutput

    @Before
    void setUp() {

        directory = new File("/tmp/results_per_pid/PID/alignment/testname_123/pass2/QualityAssessment/")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        fileInput = new File("/tmp/results_per_pid/PID/alignment/testname_123/pass2/QualityAssessment/tumor_testname_s_123_SINGLE.sorted_coverage.tsv")
        if (!fileInput.exists()) {
            fileInput.createNewFile()
            fileInput << "chr1\t0\t0\nchr1\t1000\t5\nchr1\t2000\t7\n"
            fileInput << "chr10\t0\t0\nchr10\t1000\t1\nchr10\t6000\t55\n"
            fileInput << "chr11\t0\t0\nchr11\t5000\t0\nchr11\t6000\t55\n"
            fileInput << "chr12\t1000\t1\nchr12\t2000\t44\n"
            fileInput << "chr13\t0\t0\nchr13\t4000\t5\nchr13\t6000\t55\n"
            fileInput << "chr14\t0\t0\nchr14\t6000\t55\n"
            fileInput << "chr15\t0\t0\nchr15\t6000\t55\n"
            fileInput << "chr16\t0\t1\nchr16\t1000\t1\n"
            fileInput << "chr17\t0\t0\nchr17\t1000\t3\nchr17\t2000\t44\n"
            fileInput << "chr18\t0\t0\nchr18\t6000\t55\n"
            fileInput << "chr19\t0\t0\n"
            fileInput << "chr2\t0\t0\nchr2\t6000\t55\n"
            fileInput << "chr20\t5000\t0\nchr20\t6000\t25\n"
            fileInput << "chr21\t4000\t5\nchr21\t6000\t55\n"
            fileInput << "chr22\t5000\t0\nchr22\t6000\t55\n"
            fileInput << "chr3\t0\t0\nchr3\t1000\t1\n"
            fileInput << "chr4\t0\t33\nchr4\t1000\t1\n"
            fileInput << "chr5\t6000\t55\n"
            fileInput << "chrM\t0\t48\nchrM\t1000\t3\n"
            fileInput << "chr*\t1000\t1\nchr*\t3000\t23\nchr*\t4000\t5\nchr*\t5000\t0\nchr*\t6000\t55\n"
            fileInput << "chr6\t1000\t1\nchr6\t6000\t55\n"
            fileInput << "chr7\t0\t0\nchr7\t1000\t1\nchr7\t2000\t44\n"
            fileInput << "chr8\t6000\t55\n"
            fileInput << "chr9\t0\t34\nchr9\t3000\t23\nchr9\t4000\t5\n"
            fileInput << "chrX\t4000\t5\nchrX\t5000\t0\nchrX\t6000\t55\n"
            fileInput << "chrY\t0\t0\nchrY\t1000\t1\n"
        }

        fileOutput = new File("/tmp/results_per_pid/PID/alignment/testname_123/pass2/QualityAssessment/tumor_testname_s_123_SINGLE.sorted_filtered_and_sorted_coverage.tsv")
        if (!fileOutput.exists()) {
            fileOutput.createNewFile()
        }

        Project project = new Project()
        project.name = "SOME_PROJECT"
        project.dirName = "/tmp/"
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

        processedBamFile = new ProcessedBamFile()
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
    }

    @After
    void tearDown() {
        processedBamFile = null
        fileInput.delete()
    }

    /**
     * tests if the method execute (including the services mapping, sorting, filtering) works
     */
    @Test
    void testExecution() {


        assertTrue(chromosomeIdentifierProcessingService.execute(processedBamFile))
    }
}
