/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.parser.hipo.Hipo2SampleIdentifierParser
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class SampleIdentifierServiceTests implements DomainFactoryCore {

    SampleIdentifierService sampleIdentifierService

    void setup() {
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.HIPO_PARSER_MAPPING,
                value: '{\n' +
                        '   "H021":"hipo_021",\n' +
                        '   "H123":"hipo_123",\n' +
                        '}',
        )
        Hipo2SampleIdentifierParser parser = sampleIdentifierService.getSampleIdentifierParser(SampleIdentifierParserBeanName.HIPO2) as Hipo2SampleIdentifierParser
        parser.processingOptionService = new ProcessingOptionService()
    }

    @Test
    void testParseAndFindOrSaveSampleIdentifier_WithHipoSampleNameAndDefaultParsers_ShouldReturnHipoSampleIdentifier() {
        Project project = createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        assert sampleIdentifierService.parseAndFindOrSaveSampleIdentifier('H123-ABCDEF-T1-D1', project).project == project
    }

    @Test
    void testParseSampleIdentifier_WithHipoSampleNameAndDefaultParsers_ShouldReturnHipoSampleIdentifier() {
        Project project = createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        assert sampleIdentifierService.parseSampleIdentifier('H123-ABCDEF-T1-D1', project).projectName == 'hipo_123'
    }

    @Test
    void 'testParseSampleIdentifier, when HIPO1 sample name, uses HIPO1 parser'() {
        Project project = createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        assert sampleIdentifierService.parseSampleIdentifier('H12A-ABCDEF-T1-D1', project).sampleTypeDbName == 'tumor'
    }

    @Test
    void 'testParseSampleIdentifier, when HIPO2 sample name, uses HIPO2 parser'() {
        setup()
        Project project = createProject(name: 'hipo_123', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO2)
        assert sampleIdentifierService.parseSampleIdentifier('H123-ABCDEF-T1-D1', project).sampleTypeDbName == 'tumor1-01'
        assert sampleIdentifierService.parseSampleIdentifier('H123-ABCDEF-T1-D1', project).projectName == 'hipo_123'
    }

    @Test
    void testParseSampleIdentifier_WithDeepSampleNameAndDefaultParsers_ShouldReturnDeepSampleIdentifier() {
        Project project = createProject(name: 'DEEP', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP)
        assert sampleIdentifierService.parseSampleIdentifier('41_Hf01_BlAd_CD_WGBS_S_1', project).projectName == 'DEEP'
    }

    @Test
    void testParseSampleIdentifier_WithInformSampleNameAndDefaultParsers_ShouldReturnInformSampleIdentifier() {
        Project project = createProject(name: 'INFORM1', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.INFORM)
        assert sampleIdentifierService.parseSampleIdentifier('I123_456_1T3_D1', project).projectName == 'INFORM1'
    }
}
