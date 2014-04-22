package de.dkfz.tbi.otp.hipo

import grails.test.mixin.*

class HipoSampleIdentifierUnitTest {

    void testTryParse() {
        String sampleName = "H004-ABCD-T1"
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = ""
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = null
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H456-ABCD-T3-D1"
        HipoSampleIdentifier identifier = HipoSampleIdentifier.tryParse(sampleName)
        assertNotNull(identifier)

        assertEquals("456", identifier.projectNumber)
        assertEquals("H456-ABCD", identifier.pid)
        assertEquals(HipoTissueType.TUMOR, identifier.tissueType)
        assertEquals(3, identifier.sampleNumber)
        assertEquals("D1", identifier.analyteTypeAndNumber)
        assertEquals(sampleName, identifier.fullSampleName)
        assertEquals(sampleName, identifier.toString())
        // Not testing sampleTypeDbName here because there is a dedicated test method for it.

        sampleName = "H004-ABCD-T1-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H004-BPF4-A4-D1"
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "P021-EFGH"
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "P021-EFGH-T1-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H035-BPD1-B3-D1"
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H035-BPDM-T3-D1"
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H035-BPDM-B3-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H035-BPDM-C4-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H035-BPDK-B1-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H035-BPDK-C8-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-A1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H003-BPDK-C8-M1"
        assertNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H00B-BPD1-T1-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))

        sampleName = "H00A-BPD1-T1-D1"
        assertNotNull(HipoSampleIdentifier.tryParse(sampleName))
    }

    void testSampleTypeDbName() {
        String sampleName = "H004-ABCD-T1-D1"
        String tissueTypeExp = "TUMOR"
        String tissueTypeAct = HipoSampleIdentifier.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H456-ABCD-T3-D1"
        tissueTypeExp = "TUMOR03"
        tissueTypeAct = HipoSampleIdentifier.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-B3-D1"
        tissueTypeExp = "BLOOD03"
        tissueTypeAct = HipoSampleIdentifier.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-B1-D1"
        tissueTypeExp = "BLOOD01"
        tissueTypeAct = HipoSampleIdentifier.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)

        sampleName = "H035-BPDM-C1-D1"
        tissueTypeExp = "CELL01"
        tissueTypeAct = HipoSampleIdentifier.tryParse(sampleName).sampleTypeDbName
        assertEquals(tissueTypeExp, tissueTypeAct)
    }

}
