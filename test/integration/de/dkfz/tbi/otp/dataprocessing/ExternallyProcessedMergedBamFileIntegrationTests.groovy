package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class ExternallyProcessedMergedBamFileIntegrationTests {

    Sample sample
    SeqType seqType
    MergingSet mergingSet
    Project project
    Individual individual
    SampleType sampleType
    SoftwareTool softwareTool
    SeqPlatform seqPlatform
    Run run
    ExternallyProcessedMergedBamFile bamFile

    @Before
    void setUp() {
        project = TestData.createProject(
                        name: "project",
                        dirName: "project-dir",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

        individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))
        seqType.refresh()

        softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        seqPlatform = SeqPlatform.build()

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: Run.StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))

        Realm realm1 = Realm.build(
                name: 'DKFZ',
                cluster: Realm.Cluster.DKFZ,
                env: 'test',
                operationType: Realm.OperationType.DATA_MANAGEMENT,
        )

        Realm realm2 = Realm.build(
                name: 'DKFZ',
                cluster: Realm.Cluster.DKFZ,
                env: 'test',
                operationType: Realm.OperationType.DATA_PROCESSING,
                stagingRootPath: new File(TestCase.uniqueNonExistentPath, 'staging_root_path').path
        )

        ReferenceGenome refGenome = ReferenceGenome.build(
                name: "REF_GEN",
                length: 1000,
                lengthWithoutN: 10000000,
                lengthRefChromosomes: 32,
                lengthRefChromosomesWithoutN: 7,
        )

        SeqTrack seqTrack = createSeqTrack()

        FastqSet fastqSet = FastqSet.build(
                seqTracks: [seqTrack]
        )

        bamFile = ExternallyProcessedMergedBamFile.build(
                type: AbstractBamFile.BamType.SORTED,
                fastqSet: fastqSet,
                referenceGenome: refGenome,
                fileName: "FILE_NAME",
                source: "SOURCE",
        )
    }

    @After
    void tearDown() {
        sample = null
        seqType = null
        mergingSet = null
        project = null
        individual = null
        sampleType = null
        softwareTool = null
        seqPlatform = null
        run = null
    }


    @Test
    void testToString() {
        String expected = /id: \d+ \(external\) <br>sample: mockPid sample-type seqType: seq-type library <br>project: project/
        String actual = bamFile.toString()
        assertTrue("Expected string matching '" + expected + "'. Got: " + actual, actual.matches(expected))
    }

    @Test
    void testGetFilePath() {
        OtpPath otpPath = bamFile.getFilePath()
        assert otpPath.project == project
        assert otpPath.relativePath == new File("project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/nonOTP/SOURCE_REF_GEN/FILE_NAME")
    }

    SeqTrack createSeqTrack(String laneId = "laneId") {
        SeqTrack seqTrack = new SeqTrack(
                        laneId: laneId,
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))
        return seqTrack
    }
}
