package de.dkfz.tbi.otp.hipo

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SampleType

class HipoIndividualServiceTests {

    HipoIndividualService hipoIndividualService
    Project project

    @Before
    void setUp() {
        project = new Project(
                        name: "projectName",
                        dirName: "dirName",
                        realmName: "DKFZ")
        project.save(flush: true)
    }

    @After
    void tearDown() {
        hipoIndividualService = null
        project = null
    }

    @Test
    void testCreateHipoIndividualInvalidIdentifier() {
        String sampleName = "something_random"
        assertEquals(0, Individual.count)
        assertEquals(0, Sample.count)
        assertEquals(0, SampleIdentifier.count)
        assertNull(hipoIndividualService.createHipoIndividual(sampleName))
        assertEquals(0, Individual.count)
        assertEquals(0, Sample.count)
        assertEquals(0, SampleIdentifier.count)
    }

    @Test(expected = RuntimeException)
    void testCreateHipoIndividualWithoutCreatingProject() {
        String sampleName = "H456-ABCD-T3-D1"
        assertEquals(0, Individual.count)
        assertEquals(0, Sample.count)
        assertEquals(0, SampleIdentifier.count)
        hipoIndividualService.createHipoIndividual(sampleName)
    }

    @Test
    void testCreateHipoIndividual() {
        String sampleName = "H456-ABCD-T3-D1"
        project.name = "hipo_004"
        createIndividual(sampleName)
    }

    @Test
    void testCreateHipoIndividual35() {
        String sampleName = "H035-BPDK-C3-D1"
        project.name = "PROJECT_NAME"
        createIndividual(sampleName)
    }

    private createIndividual(String sampleName) {
        assertEquals(0, Individual.count)
        assertEquals(0, Sample.count)
        assertEquals(0, SampleIdentifier.count)
        assertNotNull(hipoIndividualService.createHipoIndividual(sampleName))
        assertEquals(1, Individual.count)
        assertEquals(1, Sample.count)
        assertEquals(1, SampleIdentifier.count)
        assertNotNull(hipoIndividualService.createHipoIndividual(sampleName))
        assertEquals(1, Individual.count)
        assertEquals(1, Sample.count)
        assertEquals(1, SampleIdentifier.count)
    }

    @Test
    void testCreateHipoExistingSample() {
        String sampleName1 = "H456-ABCD-T3-D1"
        String sampleName2 = "H004-ABCD-T4-D1"
        String sampleName3 = "H004-ABCD-T4-D2"
        project.name = "hipo_004"
        assertEquals(0, Individual.count)
        assertEquals(0, Sample.count)
        assertEquals(0, SampleIdentifier.count)
        assertNotNull(hipoIndividualService.createHipoIndividual(sampleName1))
        assertEquals(1, Individual.count)
        assertEquals(1, Sample.count)
        assertEquals(1, SampleIdentifier.count)
        assertNotNull(hipoIndividualService.createHipoIndividual(sampleName2))
        assertEquals(1, Individual.count)
        assertEquals(2, Sample.count)
        assertEquals(2, SampleIdentifier.count)
        assertNotNull(hipoIndividualService.createHipoIndividual(sampleName3))
        assertEquals(1, Individual.count)
        assertEquals(2, Sample.count)
        assertEquals(3, SampleIdentifier.count)
    }

    @Test
    void testAssureSampleType() {
        String sampleName = "H001-BPDK-B1-D1"
        HipoSampleIdentifier sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        assertEquals(0, SampleType.list().size())
        hipoIndividualService.assureSampleType(sampleIdentifier)
        assertEquals(1, SampleType.list().size())

        sampleName = "H035-BPDK-B1-D1"
        sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        hipoIndividualService.assureSampleType(sampleIdentifier)
        assertEquals(2, SampleType.list().size())

        sampleName = "H001-BPDK-B2-D1"
        sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        hipoIndividualService.assureSampleType(sampleIdentifier)
        assertEquals(3, SampleType.list().size())

        sampleName = "H035-BPDK-B2-D1"
        sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        hipoIndividualService.assureSampleType(sampleIdentifier)
        assertEquals(3, SampleType.list().size())
    }

    @Test
    void testAssureProject() {
        String sampleName = "H004-ABCD-T1-D1"
        project.name = "hipo_004"
        assertEquals(project,
                hipoIndividualService.assureProject(HipoSampleIdentifier.tryParse(sampleName)))

        sampleName = "H035-BPDM-B3-D2"
        project.name = "PROJECT_NAME"
        assertEquals(project,
                hipoIndividualService.assureProject(HipoSampleIdentifier.tryParse(sampleName)))
    }

    @Test(expected = RuntimeException)
    void testAssureProjectNoProjectForName() {
        String sampleName = "H004-ABCD-T1-D1"
        project.name = "PROJECT_NAME"
        hipoIndividualService.assureProject(HipoSampleIdentifier.tryParse(sampleName))
    }

    @Test
    void testFindProject() {
        String sampleName = "H004-ABCD-T1-D1"
        HipoSampleIdentifier sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        project.name = "hipo_004"
        assertEquals(project, hipoIndividualService.findProject(sampleIdentifier))

        project.name = "PROJECT_NAME"
        assertNull(hipoIndividualService.findProject(sampleIdentifier))

        sampleName = "H035-BPDM-B1-D1"
        sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        assertEquals(project, hipoIndividualService.findProject(sampleIdentifier))
    }

    @Test
    void testCreateOrReturnIndividual() {
        String sampleName = "H004-ABCD-T1-D1"
        HipoSampleIdentifier sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        project.name = "hipo_004"
        assertEquals(0, Individual.list().size())
        Individual individualAct = hipoIndividualService.createOrReturnIndividual(sampleIdentifier)
        assertEquals(1, Individual.list().size())
        assertEquals([individualAct], Individual.list())
    }

    @Test
    void testAddSample() {
        String sampleName = "H004-ABCD-T1-D1"
        HipoSampleIdentifier sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        String pid = "H004-ABCD"
        project.name = "hipo_004"
        Individual ind = new Individual(
                        pid: pid,
                        mockPid: pid,
                        mockFullName: pid,
                        project: project,
                        type: Individual.Type.REAL
                        )
        ind.save(flush: true)

        SampleType sampleType = new SampleType(
                        name: "TUMOR"
                        )
        sampleType.save(flush: true)

        assertEquals(0, Sample.list().size())
        assertEquals(0, SampleIdentifier.list().size())
        hipoIndividualService.addSample(ind, sampleIdentifier)
        assertEquals(1, Sample.list().size())
        assertEquals(1, SampleIdentifier.list().size())
        assertEquals(ind, Sample.list().first().individual)
    }

    @Test
    void testAddSampleHipo35() {
        String sampleName = "H035-BPDK-B1-D1"
        HipoSampleIdentifier sampleIdentifier = HipoSampleIdentifier.tryParse(sampleName)
        String pid = "H035-BPDK"
        project.name = "PROJECT_NAME"
        int projectNumber = 35
        Individual ind = new Individual(
                        pid: pid,
                        mockPid: pid,
                        mockFullName: pid,
                        project: project,
                        type: Individual.Type.REAL
                        )
        ind.save(flush: true)

        SampleType sampleType = new SampleType(
                        name: "BLOOD01"
                        )
        sampleType.save(flush: true)

        assertEquals(0, Sample.list().size())
        assertEquals(0, SampleIdentifier.list().size())
        println "Individual " + ind
        println "sampleName " + sampleName
        println "projectNumber " + projectNumber
        hipoIndividualService.addSample(ind, sampleIdentifier)
        assertEquals(1, Sample.list().size())
        assertEquals(1, SampleIdentifier.list().size())
        assertEquals(ind, Sample.list().first().individual)
    }
}
