package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

class MergingPassServiceTests {

    MergingPassService mergingPassService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testCreateMergingSet() {
        MergingPass mergingPass = mergingPassService.create()
        assertNull(mergingPass)

        MergingSet mergingSet = createMergingSet("1")
        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(0, mergingPass.identifier)
        assertEquals(MergingPass.State.PROCESSING, mergingPass.status)

        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(1, mergingPass.identifier)
        assertEquals(MergingPass.State.PROCESSING, mergingPass.status)

        mergingSet.status = MergingSet.State.PROCESSED
        mergingSet.save([flush: true, failOnError: true])
        mergingPass = mergingPassService.create()
        assertNull(mergingPass)

        mergingSet = createMergingSet("2")
        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(0, mergingPass.identifier)
        assertEquals(MergingPass.State.PROCESSING, mergingPass.status)

        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(1, mergingPass.identifier)
        assertEquals(MergingPass.State.PROCESSING, mergingPass.status)
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
