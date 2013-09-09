package de.dkfz.tbi.otp.hipo

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

@TestFor(HipoIndividualService)
@TestMixin(GrailsUnitTestMixin)
class HipoIndividualServiceTests {

    HipoIndividualService hipoIndividualService

    @Before
    void setUp() {
        hipoIndividualService = new HipoIndividualService()
    }

    @After
    void tearDown() {
        hipoIndividualService = null
    }

    @Test
    void testTissueType() {
        String sampleName = "H004-ABCD-T1-D1"
        String tissueTypeExp = "TUMOR"
        String tissueTypeAct = hipoIndividualService.tissueType(sampleName)
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H456-ABCD-T3-D1"
        tissueTypeExp = "TUMOR03"
        tissueTypeAct = hipoIndividualService.tissueType(sampleName)
        assertEquals(tissueTypeExp, tissueTypeAct)
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
}
