package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.*

class MergingWorkPackageServiceTests {

    MergingWorkPackageService mergingWorkPackageService

    SeqPlatform seqPlatform
    SeqType seqType
    Sample sample

    @Before
    void setUp() {
        Project project = new Project(
                        name: "name",
                        dirName: "dirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "pid",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: de.dkfz.tbi.otp.ngsdata.Individual.Type.REAL,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "name"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        seqType = new SeqType(
                        name: "name",
                        libraryLayout: "library",
                        dirName: "dirName"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        seqPlatform = new SeqPlatform(
                        name: "name",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        seqPlatform = null
        seqType = null
        sample = null
    }

    @Test
    void testCreateWorkPackageAllCorrect() {
        MergingWorkPackage mergingWorkPackage = mergingWorkPackageService.createWorkPackage(sample, seqType, MergingCriteria.DEFAULT)
        assertNotNull(mergingWorkPackage)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateWorkPackageSampleNull() {
        sample = null
        mergingWorkPackageService.createWorkPackage(sample, seqType, MergingCriteria.DEFAULT)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateWorkPackageSeqTypeNull() {
        seqType = null
        mergingWorkPackageService.createWorkPackage(sample, seqType, MergingCriteria.DEFAULT)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateWorkPackageMergingCriteriaNull() {
        mergingWorkPackageService.createWorkPackage(sample, seqType, null)
    }
}
