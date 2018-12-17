package de.dkfz.tbi.otp.ngsdata

import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.hipo.HipoSampleIdentifierParser

class SampleIdentifierServiceTests {

    SampleIdentifierService sampleIdentifierService

    HipoSampleIdentifierParser hipoSampleIdentifierParser

    @Test
    void testParseAndFindOrSaveSampleIdentifier_WithHipoSampleNameAndDefaultParsers_ShouldReturnHipoSampleIdentifier() {
        Project project = DomainFactory.createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        assert sampleIdentifierService.parseAndFindOrSaveSampleIdentifier('H123-ABCDEF-T1-D1', project).project == project
    }

    @Test
    void testParseSampleIdentifier_WithHipoSampleNameAndDefaultParsers_ShouldReturnHipoSampleIdentifier() {
        Project project = DomainFactory.createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        assert sampleIdentifierService.parseSampleIdentifier('H123-ABCDEF-T1-D1', project).projectName == 'hipo_123'
    }

    @Test
    void 'testParseSampleIdentifier, when HIPO1 sample identifier, uses HIPO1 parser'() {
        Project project = DomainFactory.createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        assert sampleIdentifierService.parseSampleIdentifier('H12A-ABCDEF-T1-D1', project).sampleTypeDbName == 'TUMOR'
    }

    @Test
    void 'testParseSampleIdentifier, when HIPO2 sample identifier, uses HIPO2 parser'() {
        Project project = DomainFactory.createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO2)
        assert sampleIdentifierService.parseSampleIdentifier('K12A-ABCDEF-T1-D1', project).sampleTypeDbName == 'TUMOR1'
    }

    @Test
    void testParseSampleIdentifier_WithDeepSampleNameAndDefaultParsers_ShouldReturnDeepSampleIdentifier() {
        Project project = DomainFactory.createProject(name: 'DEEP', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP)
        assert sampleIdentifierService.parseSampleIdentifier('41_Hf01_BlAd_CD_WGBS_S_1', project).projectName == 'DEEP'
    }

    @Test
    void testParseSampleIdentifier_WithInformSampleNameAndDefaultParsers_ShouldReturnInformSampleIdentifier() {
        Project project = DomainFactory.createProject(name: 'INFORM', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.INFORM)
        assert sampleIdentifierService.parseSampleIdentifier('I123_456_1T3_D1', project).projectName == 'INFORM'
    }
}
