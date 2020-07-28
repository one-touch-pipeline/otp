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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class SeqPlatformValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SequencingKitLabel,
        ]
    }

    SeqPlatformModelLabel model1
    SeqPlatformModelLabel model2
    SeqPlatformModelLabel model3
    SeqPlatformModelLabel model4
    SequencingKitLabel kit1
    SequencingKitLabel kit2
    SequencingKitLabel kit3
    SequencingKitLabel kit4
    SeqPlatform platformName1Label1Kit1
    SeqPlatform platformName3Label3KitX

    private void createPlatforms() {
        model1 = DomainFactory.createSeqPlatformModelLabel(name: 'Model1', importAlias: ['Model1ImportAlias'])
        model2 = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', importAlias: ['Model2ImportAlias'])
        model3 = DomainFactory.createSeqPlatformModelLabel(name: 'Model3', importAlias: ['Model3ImportAlias'])
        model4 = DomainFactory.createSeqPlatformModelLabel(name: 'Model4', importAlias: ['Model4ImportAlias'])
        kit1 = DomainFactory.createSequencingKitLabel(name: 'Kit1', importAlias: ['Kit1ImportAlias'])
        kit2 = DomainFactory.createSequencingKitLabel(name: 'Kit2', importAlias: ['Kit2ImportAlias'])
        kit3 = DomainFactory.createSequencingKitLabel(name: 'Kit3', importAlias: ['Kit3ImportAlias'])
        kit4 = DomainFactory.createSequencingKitLabel(name: 'Kit4', importAlias: ['Kit4ImportAlias'])
        platformName1Label1Kit1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform1',
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit1,
        )
        platformName3Label3KitX = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform3',
                seqPlatformModelLabel: model3,
                sequencingKitLabel: null,
        )
    }

    void 'with kit column, validate adds expected problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\n" +
                        "Platform1\tModel1\tKit1\n" +
                        "Platform1\tModel1ImportAlias\tKit1\n" +
                        "Platform1\tModel1\tKit1ImportAlias\n" +
                        "Platform1\tModel1ImportAlias\tKit1ImportAlias\n" +
                        "Platform2\tModel1\tKit1\n" +
                        "Platform1\tModel2\tKit1\n" +
                        "Platform1\tModel1\tKit2\n" +
                        "Platform1\tModel1\n" +
                        "Platform3\tModel3\n" +
                        "Platform3\tModel3ImportAlias\n" +
                        "Platform4\tModel3\n" +
                        "Platform3\tModel4\n" +
                        "Platform3\tModel3\tKit1\n" +
                        "Platform5\tModel1\tKit1\n" +
                        "Platform1\tModel5\tKit1\n" +
                        "Platform1\tModel1\tKit5\n" +
                        "Platform5\tModel3\n" +
                        "Platform3\tModel5\n")
        createPlatforms()
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform2', instrument model 'Model1' and sequencing kit 'Kit1' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model2' and sequencing kit 'Kit1' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit 'Kit2' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[10].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform4', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[11].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model4' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[12].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model3' and sequencing kit 'Kit1' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[13].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform5', instrument model 'Model1' and sequencing kit 'Kit1' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[14].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model5' and sequencing kit 'Kit1' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[15].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit 'Kit5' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[16].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform5', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[17].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model5' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
        ]

        when:
        SeqPlatformValidator validator = new SeqPlatformValidator()
        validator.seqPlatformService = Mock(SeqPlatformService) {
            1 * findSeqPlatform("Platform1", "Model1", "Kit1") >> platformName1Label1Kit1
            1 * findSeqPlatform("Platform1", "Model1ImportAlias", "Kit1") >> platformName1Label1Kit1
            1 * findSeqPlatform("Platform1", "Model1", "Kit1ImportAlias") >> platformName1Label1Kit1
            1 * findSeqPlatform("Platform1", "Model1ImportAlias", "Kit1ImportAlias") >> platformName1Label1Kit1
            1 * findSeqPlatform("Platform2", "Model1", "Kit1") >> null
            1 * findSeqPlatform("Platform1", "Model2", "Kit1") >> null
            1 * findSeqPlatform("Platform1", "Model1", "Kit2") >> null
            1 * findSeqPlatform("Platform1", "Model1", null) >> null
            1 * findSeqPlatform("Platform3", "Model3", null) >> platformName3Label3KitX
            1 * findSeqPlatform("Platform3", "Model3ImportAlias", null) >> platformName3Label3KitX
            1 * findSeqPlatform("Platform4", "Model3", null) >> null
            1 * findSeqPlatform("Platform3", "Model4", null) >> null
            1 * findSeqPlatform("Platform3", "Model3", "Kit1") >> null
            1 * findSeqPlatform("Platform5", "Model1", "Kit1") >> null
            1 * findSeqPlatform("Platform1", "Model5", "Kit1") >> null
            1 * findSeqPlatform("Platform1", "Model1", "Kit5") >> null
            1 * findSeqPlatform("Platform5", "Model3", null) >> null
            1 * findSeqPlatform("Platform3", "Model5", null) >> null
        }
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'without kit column, validate adds expected problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\n" +
                        "Platform1\tModel1\n" +
                        "Platform2\tModel1\n" +
                        "Platform1\tModel2\n" +
                        "Platform3\tModel3\n" +
                        "Platform3\tModel3ImportAlias\n" +
                        "Platform4\tModel3\n" +
                        "Platform3\tModel4\n" +
                        "Platform5\tModel3\n" +
                        "Platform3\tModel5\n")
        createPlatforms()
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "Optional column '${SEQUENCING_KIT}' is missing."),
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform2', instrument model 'Model1' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model2' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform4', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model4' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform5', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model5' and sequencing kit '' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database."),
        ]

        when:
        SeqPlatformValidator validator = new SeqPlatformValidator()
        validator.seqPlatformService = Mock(SeqPlatformService) {
            findSeqPlatform("Platform1", "Model1", null) >> null
            findSeqPlatform("Platform2", "Model1", null) >> null
            findSeqPlatform("Platform1", "Model2", null) >> null
            findSeqPlatform("Platform3", "Model3", null) >> platformName3Label3KitX
            findSeqPlatform("Platform3", "Model3ImportAlias", null) >> platformName3Label3KitX
            findSeqPlatform("Platform4", "Model3", null) >> null
            findSeqPlatform("Platform3", "Model4", null) >> null
            findSeqPlatform("Platform5", "Model3", null) >> null
            findSeqPlatform("Platform3", "Model5", null) >> null
        }
        validator.validate(context)
        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
