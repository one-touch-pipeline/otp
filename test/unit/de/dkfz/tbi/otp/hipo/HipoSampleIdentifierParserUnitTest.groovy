package de.dkfz.tbi.otp.hipo

import org.junit.Test

import static junit.framework.TestCase.*

class HipoSampleIdentifierParserUnitTest {

    HipoSampleIdentifierParser parser = new HipoSampleIdentifierParser()

    @Test
    void testTryParse() {
        String sampleName = "H004-ABCD-T1"
        assertNull(parser.tryParse(sampleName))

        sampleName = ""
        assertNull(parser.tryParse(sampleName))

        sampleName = null
        assertNull(parser.tryParse(sampleName))

        sampleName = "H456-ABCD-T3-D1"
        HipoSampleIdentifier identifier = parser.tryParse(sampleName)
        assertNotNull(identifier)
        assertEquals("456", identifier.projectNumber)
        assertEquals("H456-ABCD", identifier.pid)
        assertEquals(HipoTissueType.TUMOR, identifier.tissueType)
        assertEquals('3', identifier.sampleNumber)
        assertEquals("D1", identifier.analyteTypeAndNumber)
        assertEquals(sampleName, identifier.fullSampleName)
        assertEquals(sampleName, identifier.toString())

        sampleName = "C026-EFGH-M2-D1"
        identifier = parser.tryParse(sampleName)
        assertNotNull(identifier)
        assertEquals("C026", identifier.projectNumber)
        assertEquals("C026-EFGH", identifier.pid)
        assertEquals(HipoTissueType.METASTASIS, identifier.tissueType)
        assertEquals('2', identifier.sampleNumber)
        assertEquals("D1", identifier.analyteTypeAndNumber)
        assertEquals(sampleName, identifier.fullSampleName)
        assertEquals(sampleName, identifier.toString())


        sampleName = "H035-IJKLMM-B1-D1"
        identifier = parser.tryParse(sampleName)
        assertNotNull(identifier)
        assertEquals("035", identifier.projectNumber)
        assertEquals("H035-IJKLMM", identifier.pid)
        assertEquals(HipoTissueType.BLOOD, identifier.tissueType)
        assertEquals('1', identifier.sampleNumber)
        assertEquals("D1", identifier.analyteTypeAndNumber)
        assertEquals(sampleName, identifier.fullSampleName)
        assertEquals(sampleName, identifier.toString())


        // Not testing sampleTypeDbName here because there is a dedicated test method for it.

        sampleName = "H004-ABCD-T1-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H004-BPF4-A4-D1"
        assertNull(parser.tryParse(sampleName))

        sampleName = "P021-EFGH"
        assertNull(parser.tryParse(sampleName))

        sampleName = "P021-EFGH-T1-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H035-BPD1-B3-D1"
        assertNull(parser.tryParse(sampleName))

        sampleName = "H035-BPDM-T3-D1"
        assertNull(parser.tryParse(sampleName))

        sampleName = "H035-BPDM-B3-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H035-BPDM-C4-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H035-BPDK-B1-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H035-BPDK-C8-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-A1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-M1"
        assertNull(parser.tryParse(sampleName))

        sampleName = "H00B-BPD1-T1-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H00A-BPD1-T1-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-D01"
        assertNull(parser.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-C1"
        assertNull(parser.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-C10"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-C02"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H003-BPDK-C80-C02"
        assertNull(parser.tryParse(sampleName))

        sampleName = "H059-BPDK-C80-C02"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H032-PX6D42-M2-D1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H032-PX6D42-T2-W1"
        assertNotNull(parser.tryParse(sampleName))

        sampleName = "H032-PX6D42-T2-Y1"
        assertNotNull(parser.tryParse(sampleName))
    }

    @Test
    void testSampleTypeDbName() {
        String sampleName = "H004-ABCD-T1-D1"
        String tissueTypeExp = "TUMOR"
        String tissueTypeAct = parser.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H456-ABCD-T3-D1"
        tissueTypeExp = "TUMOR03"
        tissueTypeAct = parser.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-B3-D1"
        tissueTypeExp = "BLOOD03"
        tissueTypeAct = parser.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-B1-D1"
        tissueTypeExp = "BLOOD01"
        tissueTypeAct = parser.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-C1-D1"
        tissueTypeExp = "CELL01"
        tissueTypeAct = parser.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-IJKLMM-B1-D1"
        tissueTypeExp = "BLOOD01"
        tissueTypeAct = parser.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)
    }

    @Test
    void testSampleType_checkTissueType_PLASMA() {
        String sampleName = "H001-BPDK-L8-C02"
        String tissueTypeExp = "PLASMA08"

        HipoSampleIdentifier identifier = parser.tryParse(sampleName)
        assert null != identifier
        assert tissueTypeExp == identifier.sampleTypeDbName
    }

    @Test
    void testSampleType_checkTissueType_NORMAL_SORTED_CELLS() {
        String sampleName = "H001-BPDK-Z8-C02"
        String tissueTypeExp = "NORMAL_SORTED_CELLS08"

        HipoSampleIdentifier identifier = parser.tryParse(sampleName)
        assert null != identifier
        assert tissueTypeExp == identifier.sampleTypeDbName
    }

    @Test
    void testSampleType_checkTissueType_TUMOR_INTERVAL_DEBULKING_SURGERY() {
        String sampleName = "H003-BPDK-E8-C02"
        String tissueTypeExp = "TUMOR_INTERVAL_DEBULKING_SURGERY08"

        HipoSampleIdentifier identifier = parser.tryParse(sampleName)
        assert null != identifier
        assert tissueTypeExp == identifier.sampleTypeDbName
    }
}
