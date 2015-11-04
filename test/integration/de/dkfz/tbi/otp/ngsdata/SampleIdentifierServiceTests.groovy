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
    void testParseSampleIdentifier_WithDeepSampleNameAndDefaultParsers_ShouldReturnDeepSampleIdentifier() {
        assert sampleIdentifierService.parseSampleIdentifier('41_Hf01_BlAd_CD_WGBS_S_1').projectName == 'DEEP'
    }
}
