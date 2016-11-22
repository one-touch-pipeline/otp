package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.hipo.HipoSampleIdentifierParser
import org.junit.Test

class SampleIdentifierServiceTests {

    SampleIdentifierService sampleIdentifierService

    HipoSampleIdentifierParser hipoSampleIdentifierParser

    @Test
    void testGetSampleIdentifierParsers_ShouldReturnHipoParser() {
        assert sampleIdentifierService.getSampleIdentifierParsers().contains(hipoSampleIdentifierParser)
    }

    @Test
    void testParseAndFindOrSaveSampleIdentifier_WithHipoSampleNameAndDefaultParsers_ShouldReturnHipoSampleIdentifier() {
        Project project = DomainFactory.createProject(name: 'hipo_123')
        assert sampleIdentifierService.parseAndFindOrSaveSampleIdentifier('H123-ABCDEF-T1-D1').project == project
    }

    @Test
    void testParseSampleIdentifier_WithHipoSampleNameAndDefaultParsers_ShouldReturnHipoSampleIdentifier() {
        assert sampleIdentifierService.parseSampleIdentifier('H123-ABCDEF-T1-D1').projectName == 'hipo_123'
    }

    @Test
    void 'testParseSampleIdentifier, when HIPO1 sample identifier, uses HIPO1 parser'() {
        assert sampleIdentifierService.parseSampleIdentifier('H12A-ABCDEF-T1-D1').sampleTypeDbName == 'TUMOR'
    }

    @Test
    void 'testParseSampleIdentifier, when HIPO2 sample identifier, uses HIPO2 parser'() {
        assert sampleIdentifierService.parseSampleIdentifier('K12A-ABCDEF-T1-D1').sampleTypeDbName == 'TUMOR1'
    }

    @Test
    void testParseSampleIdentifier_WithDeepSampleNameAndDefaultParsers_ShouldReturnDeepSampleIdentifier() {
        assert sampleIdentifierService.parseSampleIdentifier('41_Hf01_BlAd_CD_WGBS_S_1').projectName == 'DEEP'
    }

    @Test
    void testParseSampleIdentifier_WithInformSampleNameAndDefaultParsers_ShouldReturnInformSampleIdentifier() {
        assert sampleIdentifierService.parseSampleIdentifier('I123_456_1T3_D1').projectName == 'INFORM'
    }
}
