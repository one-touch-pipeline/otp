package de.dkfz.tbi.otp.dataprocessing

import org.junit.*

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class MergingSetServiceTests {

    TestData testData = new TestData()
    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SeqTrack seqTrack
    SeqTrack seqTrack2

    @Before
    void setUp() {
        Project project = DomainFactory.createProject(
                        name: "name_1",
                        dirName: "dirName",
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "pid_1",
                        mockPid: "mockPid_1",
                        mockFullName: "mockFullName_1",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "name-1"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        seqType = DomainFactory.createSeqType()

        seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        Run run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        )
        assertNotNull(run.save([flush: true, failOnError: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))
        seqTrack2 = new SeqTrack(
                        laneId: "laneId2",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        sample = null
        seqType = null
        seqTrack = null
    }

    @Test(expected = AssertionError)
    void testNextIdentifierIdentifierNull() {
        MergingSet.nextIdentifier(null)
    }

    @Test
    void testNextIdentifier() {
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        assertEquals(0, MergingSet.nextIdentifier(mergingWorkPackage))
        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        assertEquals(1, MergingSet.nextIdentifier(mergingWorkPackage))
        MergingSet mergingSet2 = new MergingSet(
                        identifier: 1,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet2.save([flush: true, failOnError: true]))
        assertEquals(2, MergingSet.nextIdentifier(mergingWorkPackage))
    }

    private MergingWorkPackage createMergingWorkPackage() {
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqType)

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage(MergingWorkPackage.getMergingProperties(seqTrack) + [pipeline: DomainFactory.createDefaultOtpPipeline()])
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))
        return mergingWorkPackage
    }

}
