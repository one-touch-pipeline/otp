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
        hipoIndividualService = new HipoIndividualService()
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
    void testCreateHipoIndividualNewIndividual() {
        String sampleName = "H456-ABCD-T3-D1"

    }

    @Test
    void testCreateHipoIndividualExistingIndividual() {
        String sampleName = "H456-ABCD-T3-D1"

    }

    @Test
    void testCreateHipoIndividualNewIndividualHipo35() {
        String sampleName = "H035-BPDK-C3-D1"

    }

    @Test
    void testCreateHipoIndividualExistingIndividualHipo35() {
        String sampleName = "H035-BPDM-B1-D1"

    }

    @Test
    void testCheckIfHipoName() {
        String sampleName = "H004-ABCD-T1"
        assertFalse(hipoIndividualService.checkIfHipoName(sampleName))

        sampleName = ""
        assertFalse(hipoIndividualService.checkIfHipoName(sampleName))

        sampleName = null
        assertFalse(hipoIndividualService.checkIfHipoName(sampleName))

        sampleName = "H456-ABCD-T3-D1"
        assertTrue(hipoIndividualService.checkIfHipoName(sampleName))

        sampleName = "H004-ABCD-T1-D1"
        assertTrue(hipoIndividualService.checkIfHipoName(sampleName))

        sampleName = "H004-BPF4-L4-D1"
        assertFalse(hipoIndividualService.checkIfHipoName(sampleName))

        sampleName = "P021-EFGH"
        assertFalse(hipoIndividualService.checkIfHipoName(sampleName))

        sampleName = "P021-EFGH-T1-D1"
        assertTrue(hipoIndividualService.checkIfHipoName(sampleName))
    }

    @Test
    void testCheckIfHipo35Name() {
        String sampleName = "H456-ABCD-T3-D1"
        assertFalse(hipoIndividualService.checkIfHipo35Name(sampleName))

        sampleName = "H035-BPD1-B3-D1"
        assertFalse(hipoIndividualService.checkIfHipo35Name(sampleName))

        sampleName = "H035-BPDM-T3-D1"
        assertFalse(hipoIndividualService.checkIfHipo35Name(sampleName))

        sampleName = "H035-BPDM-B3-D1"
        assertTrue(hipoIndividualService.checkIfHipo35Name(sampleName))

        sampleName = "H035-BPDM-C4-D1"
        assertTrue(hipoIndividualService.checkIfHipo35Name(sampleName))

        sampleName = "H035-BPDK-B1-D1"
        assertTrue(hipoIndividualService.checkIfHipo35Name(sampleName))

        sampleName = "H035-BPDK-C8-D1"
        assertTrue(hipoIndividualService.checkIfHipo35Name(sampleName))
    }

    @Test
    void testAssureSampleType() {
        String sampleName = "H001-BPDK-B1-D1"
        int projectNumber = 1
        assertEquals(0, SampleType.list().size())
        hipoIndividualService.assureSampleType(sampleName, projectNumber)
        assertEquals(1, SampleType.list().size())

        sampleName = "H035-BPDK-B1-D1"
        projectNumber = 35
        hipoIndividualService.assureSampleType(sampleName, projectNumber)
        assertEquals(2, SampleType.list().size())
    }

    @Test
    void testTissueType() {
        String sampleName = "H004-ABCD-T1-D1"
        String tissueTypeExp = "TUMOR"
        int projectNumber = 004
        project.name = "hipo_004"
        String tissueTypeAct = hipoIndividualService.tissueType(sampleName, projectNumber)
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H456-ABCD-T3-D1"
        tissueTypeExp = "TUMOR03"
        tissueTypeAct = hipoIndividualService.tissueType(sampleName, projectNumber)
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-B3-D1"
        tissueTypeExp = "BLOOD03"
        projectNumber = 35
        project.name = "PROJECT_NAME"
        tissueTypeAct = hipoIndividualService.tissueType(sampleName, projectNumber)
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-B1-D1"
        tissueTypeExp = "BLOOD01"
        tissueTypeAct = hipoIndividualService.tissueType(sampleName, projectNumber)
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-C1-D1"
        tissueTypeExp = "CELL01"
        tissueTypeAct = hipoIndividualService.tissueType(sampleName, projectNumber)
        assertEquals(tissueTypeExp, tissueTypeAct)
    }

    @Test
    void testAssureProject() {
        String sampleName = "H004-ABCD-T1-D1"
        project.name = "hipo_004"
        hipoIndividualService.assureProject(sampleName)

        sampleName = "H035-BPD1-T1-D1"
        project.name = "PROJECT_NAME"
        hipoIndividualService.assureProject(sampleName)
    }

    @Test(expected = Exception)
    void testAssureProjectNoProjectForName() {
        String sampleName = "H004-ABCD-T1-D1"
        project.name = "PROJECT_NAME"
        hipoIndividualService.assureProject(sampleName)
    }

    @Test
    void testFindProject() {
        String sampleName = "H004-ABCD-T1-D1"
        project.name = "hipo_004"
        Project projectAct = hipoIndividualService.findProject(sampleName)
        assertEquals(project, projectAct)

        project.name = "PROJECT_NAME"
        projectAct = hipoIndividualService.findProject(sampleName)
        assertNull(projectAct)

        sampleName = "H035-BPDM-B1-D1"
        projectAct = hipoIndividualService.findProject(sampleName)
        assertEquals(project, projectAct)
    }

    @Test
    void testCreateOrReturnIndividual() {
        String sampleName = "H004-ABCD-T1-D1"
        project.name = "hipo_004"
        assertEquals(0, Individual.list().size())
        Individual individualAct = hipoIndividualService.createOrReturnIndividual(sampleName)
        assertEquals(1, Individual.list().size())
        assertEquals([individualAct], Individual.list())
    }

    @Test
    void testAddSample() {
        String sampleName = "H004-ABCD-T1-D1"
        String pid = "H004-ABCD"
        project.name = "hipo_004"
        int projectNumber = 004
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
        hipoIndividualService.addSample(ind, sampleName, projectNumber)
        assertEquals(1, Sample.list().size())
        assertEquals(1, SampleIdentifier.list().size())
        assertEquals(ind, Sample.list().first().individual)
    }

    @Test
    void testAddSampleHipo35() {
        String sampleName = "H035-BPDK-B1-D1"
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
        hipoIndividualService.addSample(ind, sampleName, projectNumber)
        assertEquals(1, Sample.list().size())
        assertEquals(1, SampleIdentifier.list().size())
        assertEquals(ind, Sample.list().first().individual)
    }
}
