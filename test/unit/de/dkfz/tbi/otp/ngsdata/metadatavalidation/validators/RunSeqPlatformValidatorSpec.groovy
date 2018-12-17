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
        model1 = DomainFactory.createSeqPlatformModelLabel(name: 'Model1', importAlias: ['Model1ImportAlias'])
        model2 = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', importAlias: ['Model2ImportAlias'])
        kit1 = DomainFactory.createSequencingKitLabel(name: 'Kit1', importAlias: ['Kit1ImportAlias'])
        kit2 = DomainFactory.createSequencingKitLabel(name: 'Kit2', importAlias: ['Kit2ImportAlias'])
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
                        "Platform1\tModel1ImportAlias\tKit1ImportAlias\tInconsistentPlatformInMetadata\n" +
                        "Platform2\tModel1ImportAlias\tKit1ImportAlias\tInconsistentPlatformInMetadata\n" +
                        "Platform1\tModel1ImportAlias\tKit1ImportAlias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel2ImportAlias\tKit1ImportAlias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel1ImportAlias\tKit1ImportAlias\tInconsistentKitInMetadata\n" +
                        "Platform1\tModel1ImportAlias\tKit2ImportAlias\tInconsistentKitInMetadata\n" +
                        "Platform2\tModel1ImportAlias\tKit1ImportAlias\tPlatformInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel2ImportAlias\tKit1ImportAlias\tModelInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel1ImportAlias\tKit2ImportAlias\tKitInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel1ImportAlias\tKit1ImportAlias\tConsistentDatabaseAndMetadataWithKit\n" +
                        "Platform2\tModel2ImportAlias\t\tConsistentDatabaseAndMetadataWithoutKit\n" +
                        "Platform1\tModel1ImportAlias\tKit1ImportAlias\tRunNotRegistered\n" +
                        "Platform2\tModel2ImportAlias\tKit2ImportAlias\tPlatformNotRegistered\n")
        createPlatforms()
        DomainFactory.createRun(name: 'PlatformInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'ModelInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'KitInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadataWithKit', seqPlatform: platform_1_1_1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadataWithoutKit', seqPlatform: platform_2_2_X)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentPlatformInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'.", "All entries for one run must have the same combination of values in the columns 'INSTRUMENT_PLATFORM', 'INSTRUMENT_MODEL' and 'SEQUENCING_KIT'."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentModelInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'.", "All entries for one run must have the same combination of values in the columns 'INSTRUMENT_PLATFORM', 'INSTRUMENT_MODEL' and 'SEQUENCING_KIT'."),
                new Problem(context.spreadsheet.dataRows[4].cells + context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentKitInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'.", "All entries for one run must have the same combination of values in the columns 'INSTRUMENT_PLATFORM', 'INSTRUMENT_MODEL' and 'SEQUENCING_KIT'."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "Run 'PlatformInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_1_1.fullName()}', not with '${platform_2_1_1.fullName()}'.", "At least one run is already registered in the OTP database with another sequencing platform."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "Run 'ModelInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_1_1.fullName()}', not with '${platform_1_2_1.fullName()}'.", "At least one run is already registered in the OTP database with another sequencing platform."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "Run 'KitInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_1_1.fullName()}', not with '${platform_1_1_2.fullName()}'.", "At least one run is already registered in the OTP database with another sequencing platform."),
        ]

        when:
        RunSeqPlatformValidator validator = new RunSeqPlatformValidator()
        validator.seqPlatformService = Mock(SeqPlatformService) {
            2 * findSeqPlatform("Platform1", "Model1ImportAlias", "Kit1ImportAlias") >> platform_1_1_1
            1 * findSeqPlatform("Platform2", "Model1ImportAlias", "Kit1ImportAlias") >> platform_2_1_1
            1 * findSeqPlatform("Platform1", "Model2ImportAlias", "Kit1ImportAlias") >> platform_1_2_1
            1 * findSeqPlatform("Platform1", "Model1ImportAlias", "Kit2ImportAlias") >> platform_1_1_2
            1 * findSeqPlatform("Platform2", "Model2ImportAlias", null) >> platform_2_2_X
            1 * findSeqPlatform("Platform2", "Model2ImportAlias", "Kit2ImportAlias") >> null
        }
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'without kit column, validate adds expected problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${RUN_ID}\n" +
                        "Platform1\tModel1ImportAlias\tInconsistentPlatformInMetadata\n" +
                        "Platform2\tModel1ImportAlias\tInconsistentPlatformInMetadata\n" +
                        "Platform1\tModel1ImportAlias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel2ImportAlias\tInconsistentModelInMetadata\n" +
                        "Platform1\tModel2ImportAlias\tPlatformInconsistentInDatabaseAndMetadata\n" +
                        "Platform2\tModel1ImportAlias\tModelInconsistentInDatabaseAndMetadata\n" +
                        "Platform1\tModel2ImportAlias\tKitInconsistentInDatabaseAndMetadata\n" +
                        "Platform2\tModel2ImportAlias\tConsistentDatabaseAndMetadata\n" +
                        "Platform1\tModel1ImportAlias\tRunNotRegistered\n" +
                        "Platform2\tModel2ImportAlias\tPlatformNotRegistered\n")
        createPlatforms()
        DomainFactory.createRun(name: 'PlatformInconsistentInDatabaseAndMetadata', seqPlatform: platform_2_2_X)
        DomainFactory.createRun(name: 'ModelInconsistentInDatabaseAndMetadata', seqPlatform: platform_2_2_X)
        DomainFactory.createRun(name: 'KitInconsistentInDatabaseAndMetadata', seqPlatform: platform_1_2_1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadata', seqPlatform: platform_2_2_X)
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "Optional column '${SEQUENCING_KIT}' is missing."),
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentPlatformInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'.", "All entries for one run must have the same combination of values in the columns 'INSTRUMENT_PLATFORM', 'INSTRUMENT_MODEL' and 'SEQUENCING_KIT'."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentModelInMetadata' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'.", "All entries for one run must have the same combination of values in the columns 'INSTRUMENT_PLATFORM', 'INSTRUMENT_MODEL' and 'SEQUENCING_KIT'."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "Run 'PlatformInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_2_2_X.fullName()}', not with '${platform_1_2_X.fullName()}'.", "At least one run is already registered in the OTP database with another sequencing platform."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "Run 'ModelInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_2_2_X.fullName()}', not with '${platform_2_1_X.fullName()}'.", "At least one run is already registered in the OTP database with another sequencing platform."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "Run 'KitInconsistentInDatabaseAndMetadata' is already registered in the OTP database with sequencing platform '${platform_1_2_1.fullName()}', not with '${platform_1_2_X.fullName()}'.", "At least one run is already registered in the OTP database with another sequencing platform."),
        ]

        when:
        RunSeqPlatformValidator validator = new RunSeqPlatformValidator()
        validator.seqPlatformService = Mock(SeqPlatformService) {
            1 * findSeqPlatform("Platform1", "Model1ImportAlias", null) >> null
            1 * findSeqPlatform(platform_2_1_X.name, exactlyOneElement(platform_2_1_X.seqPlatformModelLabel.importAlias), null) >> platform_2_1_X
            2 * findSeqPlatform(platform_1_2_X.name, exactlyOneElement(platform_1_2_X.seqPlatformModelLabel.importAlias), null) >> platform_1_2_X
            2 * findSeqPlatform(platform_2_2_X.name, exactlyOneElement(platform_2_2_X.seqPlatformModelLabel.importAlias), null) >> platform_2_2_X
        }
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
