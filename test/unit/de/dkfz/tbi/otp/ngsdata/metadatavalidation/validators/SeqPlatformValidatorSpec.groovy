package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Mock([
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SequencingKitLabel,
])
class SeqPlatformValidatorSpec extends Specification {

    SeqPlatformModelLabel model1
    SeqPlatformModelLabel model2
    SeqPlatformModelLabel model3
    SeqPlatformModelLabel model4
    SequencingKitLabel kit1
    SequencingKitLabel kit2
    SequencingKitLabel kit3
    SequencingKitLabel kit4
    SeqPlatform platform_1_1_1
    SeqPlatform platform_3_3_X

    private void createPlatforms() {
        model1 = DomainFactory.createSeqPlatformModelLabel(name: 'Model1', alias: ['Model1Alias'])
        model2 = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', alias: ['Model2Alias'])
        model3 = DomainFactory.createSeqPlatformModelLabel(name: 'Model3', alias: ['Model3Alias'])
        model4 = DomainFactory.createSeqPlatformModelLabel(name: 'Model4', alias: ['Model4Alias'])
        kit1 = DomainFactory.createSequencingKitLabel(name: 'Kit1', alias: ['Kit1Alias'])
        kit2 = DomainFactory.createSequencingKitLabel(name: 'Kit2', alias: ['Kit2Alias'])
        kit3 = DomainFactory.createSequencingKitLabel(name: 'Kit3', alias: ['Kit3Alias'])
        kit4 = DomainFactory.createSequencingKitLabel(name: 'Kit4', alias: ['Kit4Alias'])
        platform_1_1_1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform1',
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit1,
        )
        platform_3_3_X = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
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
                        "Platform1\tModel1Alias\tKit1\n" +
                        "Platform1\tModel1\tKit1Alias\n" +
                        "Platform1\tModel1Alias\tKit1Alias\n" +
                        "Platform2\tModel1\tKit1\n" +
                        "Platform1\tModel2\tKit1\n" +
                        "Platform1\tModel1\tKit2\n" +
                        "Platform1\tModel1\n" +
                        "Platform3\tModel3\n" +
                        "Platform3\tModel3Alias\n" +
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
                        "The combination of instrument platform 'Platform2', instrument model 'Model1' and sequencing kit 'Kit1' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model2' and sequencing kit 'Kit1' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit 'Kit2' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[10].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform4', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[11].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model4' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[12].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model3' and sequencing kit 'Kit1' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[13].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform5', instrument model 'Model1' and sequencing kit 'Kit1' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[14].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model5' and sequencing kit 'Kit1' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[15].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit 'Kit5' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[16].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform5', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[17].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model5' and sequencing kit '' is not registered in the OTP database."),
        ]

        when:
        new SeqPlatformValidator().validate(context)

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
                        "Platform3\tModel3Alias\n" +
                        "Platform4\tModel3\n" +
                        "Platform3\tModel4\n" +
                        "Platform5\tModel3\n" +
                        "Platform3\tModel5\n")
        createPlatforms()
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "Optional column '${SEQUENCING_KIT}' is missing."),
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model1' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform2', instrument model 'Model1' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform1', instrument model 'Model2' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform4', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model4' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform5', instrument model 'Model3' and sequencing kit '' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "The combination of instrument platform 'Platform3', instrument model 'Model5' and sequencing kit '' is not registered in the OTP database."),
        ]

        when:
        new SeqPlatformValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
