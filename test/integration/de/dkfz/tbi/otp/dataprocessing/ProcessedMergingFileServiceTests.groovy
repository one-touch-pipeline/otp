package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.util.Environment
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.junit.*

class ProcessedMergingFileServiceTests {

    ProcessedMergingFileService processedMergingFileService

    Realm realm

    final static String directory = "/tmp/otp-unit-test/pmfs/processing/project-dir/results_per_pid/patient/merging//sample-type/seq-type/library/DEFAULT/0/pass0"
    final static String baseFile = "sample-type_patient_seq-type_library_merged.mdup"
    final static String basePath = directory + "/" + baseFile

    @Before
    void setUp() {
        realm = new Realm()
        realm.cluster = Cluster.DKFZ
        realm.rootPath = "/tmp/otp-unit-test/pmfs/root"
        realm.processingRootPath = "/tmp/otp-unit-test/pmfs/processing"
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000
        realm.pbsOptions = ""
        realm.name = "realmName"
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.env = Environment.getCurrent().getName()
        realm.save([flush: true, failOnError: true])
    }

    @After
    void tearDown() {
        realm = null
    }

    @Test(expected = IllegalArgumentException)
    void testDirectoryByProcessedMergedBamFileBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergingFileService.directory(processedMergedBamFile)
    }

    @Test
    void testDirectoryByProcessedMergedBamFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String pathExp = directory
        String pathAct = processedMergingFileService.directory(processedMergedBamFile)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testDirectoryByMergingPassMergingPassIsNull() {
        MergingPass mergingPass = null
        processedMergingFileService.directory(mergingPass)
    }

    @Test
    void testDirectoryByMergingPass() {
        MergingPass mergingPass = createMergingPass()
        String pathExp = directory
        String pathAct = processedMergingFileService.directory(mergingPass)
        assertEquals(pathExp, pathAct)
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass) {
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP
                        )
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))
        return processedMergedBamFile
    }

    private MergingPass createMergingPass() {
        Project project = new Project(
                        name: "project",
                        dirName: "project-dir",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))
        return mergingPass
    }
}
