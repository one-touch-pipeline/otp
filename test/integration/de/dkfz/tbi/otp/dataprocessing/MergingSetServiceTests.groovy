package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

class MergingSetServiceTests {

    MergingSetService mergingSetService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testGetNextMergingSet() {
        MergingSet next = mergingSetService.getNextMergingSet()
        assertNull(next)

        MergingSet mergingSet = createMergingSet("1")
        next = mergingSetService.getNextMergingSet()
        assertEquals(mergingSet, next)

        MergingSet mergingSet2 = createMergingSet("2")
        MergingSet mergingSet3 = createMergingSet("3")
        next = mergingSetService.getNextMergingSet()
        assertNotNull(next)
        assertTrue(next.equals(mergingSet) || next.equals(mergingSet2) || next.equals(mergingSet3) )

        mergingSet.status = MergingSet.State.INPROGRESS
        mergingSet.save([flush: true, failOnError: true])
        mergingSet2.status = MergingSet.State.INPROGRESS
        mergingSet2.save([flush: true, failOnError: true])
        mergingSet3.status = MergingSet.State.INPROGRESS
        mergingSet3.save([flush: true, failOnError: true])
        next = mergingSetService.getNextMergingSet()
        assertNull(next)

        MergingSet mergingSet4 = createMergingSet("4")
        next = mergingSetService.getNextMergingSet()
        assertNotNull(next)
        assertTrue(next.equals(mergingSet4))
    }

    @Test
    void testBlockForMerging() {
        MergingSet mergingSet = createMergingSet("1")
        mergingSetService.blockForMerging(mergingSet)
        assertEquals(mergingSet.status, MergingSet.State.INPROGRESS)
    }

    private MergingSet createMergingSet(String uniqueId) {
        Project project = new Project(
                name: "name_"+uniqueId,
                dirName: "dirName",
                realmName: "realmName"
                )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                pid: "pid_" + uniqueId,
                mockPid: "mockPid_"+uniqueId,
                mockFullName: "mockFullName_"+ uniqueId,
                type: Individual.Type.UNDEFINED,
                project: project
                )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                name: "name_"+uniqueId
                )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
                individual: individual,
                sampleType: sampleType
                )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                sample: sample
                )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                identifier: 0,
                mergingWorkPackage: mergingWorkPackage
                )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        return mergingSet
    }
}
