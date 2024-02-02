/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class RunSeqPlatformValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Run,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SequencingKitLabel,
        ]
    }

    static final String SAME_COMBINATION_DESCRIPTION = "All entries for one run must have the same combination of values in the columns 'INSTRUMENT_PLATFORM', 'INSTRUMENT_MODEL' and 'SEQUENCING_KIT'."
    static final String ALREADY_KNOWN_DESCRIPTION = "At least one run is already registered in the OTP database with another sequencing platform."

    static final String PLATFORM_NAME_1 = "Platform1"
    static final String PLATFORM_NAME_2 = "Platform2"

    SeqPlatformModelLabel model1
    SeqPlatformModelLabel model2
    SequencingKitLabel kit1
    SequencingKitLabel kit2
    SeqPlatform platformName1Label1Kit1
    SeqPlatform platformName2Label1Kit1
    SeqPlatform platformName1Label2Kit1
    SeqPlatform platformName1Label1Kit2
    SeqPlatform platformName2Label2KitNull
    SeqPlatform platformName1Label2KitNull
    SeqPlatform platformName2Label1KitNull

    static String getSameCombinationError(String propertyName) {
        return "All entries for run '${propertyName}' must have the same combination of values in " +
                "the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'."
    }

    static String getAlreadyKnownError(String propertyName, SeqPlatform known, SeqPlatform conflicting) {
        return "Run '${propertyName}' is already registered in the OTP database with sequencing " +
                "platform '${known.fullName}', not with '${conflicting.fullName}'."
    }

    private void createPlatforms() {
        model1 = DomainFactory.createSeqPlatformModelLabel(name: 'Model1', importAlias: ['Model1ImportAlias'])
        model2 = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', importAlias: ['Model2ImportAlias'])
        kit1 = DomainFactory.createSequencingKitLabel(name: 'Kit1', importAlias: ['Kit1ImportAlias'])
        kit2 = DomainFactory.createSequencingKitLabel(name: 'Kit2', importAlias: ['Kit2ImportAlias'])
        platformName1Label2KitNull = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME_1,
                seqPlatformModelLabel: model2,
                sequencingKitLabel: null,
        )
        platformName2Label1KitNull = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME_2,
                seqPlatformModelLabel: model1,
                sequencingKitLabel: null,
        )
        platformName1Label1Kit1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME_1,
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit1,
        )
        platformName2Label1Kit1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME_2,
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit1,
        )
        platformName1Label2Kit1 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME_1,
                seqPlatformModelLabel: model2,
                sequencingKitLabel: kit1,
        )
        platformName1Label1Kit2 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME_1,
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit2,
        )
        platformName2Label2KitNull = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME_2,
                seqPlatformModelLabel: model2,
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
        DomainFactory.createRun(name: 'PlatformInconsistentInDatabaseAndMetadata', seqPlatform: platformName1Label1Kit1)
        DomainFactory.createRun(name: 'ModelInconsistentInDatabaseAndMetadata', seqPlatform: platformName1Label1Kit1)
        DomainFactory.createRun(name: 'KitInconsistentInDatabaseAndMetadata', seqPlatform: platformName1Label1Kit1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadataWithKit', seqPlatform: platformName1Label1Kit1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadataWithoutKit', seqPlatform: platformName2Label2KitNull)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        getSameCombinationError("InconsistentPlatformInMetadata"), SAME_COMBINATION_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells as Set, LogLevel.ERROR,
                        getSameCombinationError("InconsistentModelInMetadata"), SAME_COMBINATION_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[4].cells + context.spreadsheet.dataRows[5].cells as Set, LogLevel.ERROR,
                        getSameCombinationError("InconsistentKitInMetadata"), SAME_COMBINATION_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, LogLevel.ERROR,
                        getAlreadyKnownError("PlatformInconsistentInDatabaseAndMetadata", platformName1Label1Kit1, platformName2Label1Kit1), ALREADY_KNOWN_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, LogLevel.ERROR,
                        getAlreadyKnownError("ModelInconsistentInDatabaseAndMetadata", platformName1Label1Kit1, platformName1Label2Kit1), ALREADY_KNOWN_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, LogLevel.ERROR,
                        getAlreadyKnownError("KitInconsistentInDatabaseAndMetadata", platformName1Label1Kit1, platformName1Label1Kit2), ALREADY_KNOWN_DESCRIPTION),
        ]

        when:
        RunSeqPlatformValidator validator = new RunSeqPlatformValidator()
        validator.seqPlatformService = Mock(SeqPlatformService) {
            2 * findSeqPlatform(PLATFORM_NAME_1, "Model1ImportAlias", "Kit1ImportAlias") >> platformName1Label1Kit1
            1 * findSeqPlatform(PLATFORM_NAME_2, "Model1ImportAlias", "Kit1ImportAlias") >> platformName2Label1Kit1
            1 * findSeqPlatform(PLATFORM_NAME_1, "Model2ImportAlias", "Kit1ImportAlias") >> platformName1Label2Kit1
            1 * findSeqPlatform(PLATFORM_NAME_1, "Model1ImportAlias", "Kit2ImportAlias") >> platformName1Label1Kit2
            1 * findSeqPlatform(PLATFORM_NAME_2, "Model2ImportAlias", null) >> platformName2Label2KitNull
            1 * findSeqPlatform(PLATFORM_NAME_2, "Model2ImportAlias", "Kit2ImportAlias") >> null
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
        DomainFactory.createRun(name: 'PlatformInconsistentInDatabaseAndMetadata', seqPlatform: platformName2Label2KitNull)
        DomainFactory.createRun(name: 'ModelInconsistentInDatabaseAndMetadata', seqPlatform: platformName2Label2KitNull)
        DomainFactory.createRun(name: 'KitInconsistentInDatabaseAndMetadata', seqPlatform: platformName1Label2Kit1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadata', seqPlatform: platformName2Label2KitNull)
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.WARNING,
                        "Optional column '${SEQUENCING_KIT}' is missing."),
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        getSameCombinationError("InconsistentPlatformInMetadata"), SAME_COMBINATION_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells as Set, LogLevel.ERROR,
                        getSameCombinationError("InconsistentModelInMetadata"), SAME_COMBINATION_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, LogLevel.ERROR,
                        getAlreadyKnownError("PlatformInconsistentInDatabaseAndMetadata", platformName2Label2KitNull, platformName1Label2KitNull), ALREADY_KNOWN_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, LogLevel.ERROR,
                        getAlreadyKnownError("ModelInconsistentInDatabaseAndMetadata", platformName2Label2KitNull, platformName2Label1KitNull), ALREADY_KNOWN_DESCRIPTION),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, LogLevel.ERROR,
                        getAlreadyKnownError("KitInconsistentInDatabaseAndMetadata", platformName1Label2Kit1, platformName1Label2KitNull), ALREADY_KNOWN_DESCRIPTION),
        ]

        when:
        RunSeqPlatformValidator validator = new RunSeqPlatformValidator()
        validator.seqPlatformService = Mock(SeqPlatformService) {
            1 * findSeqPlatform(PLATFORM_NAME_1, "Model1ImportAlias", null) >> null
            1 * findSeqPlatform(platformName2Label1KitNull.name, exactlyOneElement(platformName2Label1KitNull.seqPlatformModelLabel.importAlias), null) >> platformName2Label1KitNull
            2 * findSeqPlatform(platformName1Label2KitNull.name, exactlyOneElement(platformName1Label2KitNull.seqPlatformModelLabel.importAlias), null) >> platformName1Label2KitNull
            2 * findSeqPlatform(platformName2Label2KitNull.name, exactlyOneElement(platformName2Label2KitNull.seqPlatformModelLabel.importAlias), null) >> platformName2Label2KitNull
        }
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
