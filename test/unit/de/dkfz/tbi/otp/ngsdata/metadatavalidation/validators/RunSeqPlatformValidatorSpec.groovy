package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        Run,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SequencingKitLabel,
])
class RunSeqPlatformValidatorSpec extends Specification {

    SeqPlatformModelLabel model1
    SeqPlatformModelLabel model2
    SequencingKitLabel kit1
    SequencingKitLabel kit2
    SeqPlatform platform_1_1_1
    SeqPlatform platform_2_1_1
    SeqPlatform platform_1_2_1
    SeqPlatform platform_1_1_2
    SeqPlatform platform_2_2_X
    SeqPlatform platform_1_2_X
    SeqPlatform platform_2_1_X

    private void createPlatforms() {
        model1 = DomainFactory.createSeqPlatformModelLabel(name: 'Model1', alias: ['Model1Alias'])
        model2 = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', alias: ['Model2Alias'])
        kit1 = DomainFactory.createSequencingKitLabel(name: 'Kit1', alias: ['Kit1Alias'])
        kit2 = DomainFactory.createSequencingKitLabel(name: 'Kit2', alias: ['Kit2Alias'])
        platform_1_1_1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform1',
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit1,
        )
        platform_2_1_1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform2',
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit1,
        )
        platform_1_2_1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform1',
                seqPlatformModelLabel: model2,
                sequencingKitLabel: kit1,
        )
        platform_1_1_2 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform1',
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit2,
        )
        platform_2_2_X = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform2',
                seqPlatformModelLabel: model2,
                sequencingKitLabel: null,
        )
        platform_1_2_X = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform1',
                seqPlatformModelLabel: model2,
                sequencingKitLabel: null,
        )
        platform_2_1_X = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform2',
                seqPlatformModelLabel: model1,
                sequencingKitLabel: null,
        )
    }

    void 'with kit column, validate adds expected problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${RUN_ID}\n" +
                        "Platform1\tModel1Alias\tKit1Alias\tInconsistentPlatformInMetadata\n" +
                        "Platform2\tModel1Alias\tKit1Alias\tInconsistentPlatformInMetadata\n" +
                        "Platform1\tModel1Alias\tKit1Alias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel2Alias\tKit1Alias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel1Alias\tKit1Alias\tInconsistentKitInMetadata\n" +
                        "Platform1\tModel1Alias\tKit2Alias\tInconsistentKitInMetadata\n" +
                        "Platform2\tModel1Alias\tKit1Alias\tPlatformInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel2Alias\tKit1Alias\tModelInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel1Alias\tKit2Alias\tKitInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel1Alias\tKit1Alias\tConsistentDatabaseAndMetadataWithKit\n" +
                        "Platform2\tModel2Alias\t\tConsistentDatabaseAndMetadataWithoutKit\n" +
                        "Platform1\tModel1Alias\tKit1Alias\tRunNotRegistered\n" +
                        "Platform2\tModel2Alias\tKit2Alias\tPlatformNotRegistered\n")
        createPlatforms()
        DomainFactory.createRun(name: 'PlatformInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'ModelInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'KitInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadataWithKit', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadataWithoutKit', seqPlatform: platform_2_2_X)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentPlatformInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentModelInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'."),
                new Problem(context.spreadsheet.dataRows[4].cells + context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentKitInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "Run 'PlatformInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_1_1.fullName()}', not with '${platform_2_1_1.fullName()}'."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "Run 'ModelInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_1_1.fullName()}', not with '${platform_1_2_1.fullName()}'."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "Run 'KitInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_1_1.fullName()}', not with '${platform_1_1_2.fullName()}'."),
        ]

        when:
        new RunSeqPlatformValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'without kit column, validate adds expected problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${RUN_ID}\n" +
                        "Platform1\tModel1Alias\tInconsistentPlatformInMetadata\n" +
                        "Platform2\tModel1Alias\tInconsistentPlatformInMetadata\n" +
                        "Platform1\tModel1Alias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel2Alias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel2Alias\tPlatformInconsistentInDatabaseAndMetadata\n" +
                        "Platform2\tModel1Alias\tModelInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel2Alias\tKitInconsistentInDatabaseAndMetadata\n" +
                        "Platform2\tModel2Alias\tConsistentDatabaseAndMetadata\n" +
                        "Platform1\tModel1Alias\tRunNotRegistered\n" +
                        "Platform2\tModel2Alias\tPlatformNotRegistered\n")
        createPlatforms()
        DomainFactory.createRun(name: 'PlatformInconsistentInDatabaseAndMetadata', seqPlatform: platform_2_2_X)
        DomainFactory.createRun(name: 'ModelInconsistentInDatabaseAndMetadata', seqPlatform: platform_2_2_X)
        DomainFactory.createRun(name: 'KitInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_2_1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadata', seqPlatform: platform_2_2_X)
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "Optional column '${SEQUENCING_KIT}' is missing."),
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentPlatformInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentModelInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "Run 'PlatformInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_2_2_X.fullName()}', not with '${platform_1_2_X.fullName()}'."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "Run 'ModelInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_2_2_X.fullName()}', not with '${platform_2_1_X.fullName()}'."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "Run 'KitInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_2_1.fullName()}', not with '${platform_1_2_X.fullName()}'."),
        ]

        when:
        new RunSeqPlatformValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
