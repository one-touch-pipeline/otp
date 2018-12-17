package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.test.mixin.Mock
import org.joda.time.format.ISODateTimeFormat
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_DATE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

@Mock([
        Run,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
])
class RunRunDateValidatorSpec extends Specification {

    void 'validate adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${RUN_DATE}\t${RUN_ID}\n" +
                        "2016-01-01\tInconsistentMetadata\n" +
                        "2016-01-02\tInconsistentMetadata\n" +
                        "2016-01-02\tInconsistentDatabaseAndMetadata\n" +
                        "2016-01-01\tConsistentDatabaseAndMetadata\n" +
                        "2016-01-01\t160102InconsistentRunName\n" +
                        "2016-01-01\t160101tConsistentDatabaseAndMetadata\n")
        Date date = ISODateTimeFormat.date().parseDateTime("2016-01-01").toDate()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup(name: "Illumina")
        DomainFactory.createRun(name: 'InconsistentDatabaseAndMetadata', dateExecuted: date)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadata', dateExecuted: date)
        DomainFactory.createRun(name: '20160102InconsistentRunName', dateExecuted: date, seqPlatform: seqPlatform)
        DomainFactory.createRun(name: '160101tConsistentDatabaseAndMetadata', dateExecuted: date, seqPlatform: seqPlatform)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentMetadata' must have the same value in the column '${RUN_DATE}'.", "All entries of one run must have the same value in the column 'RUN_DATE'."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "Run 'InconsistentDatabaseAndMetadata' is already registered in the OTP database with run date '2016-01-01', not with '2016-01-02'.", "At least one run is already registered in the OTP database but with another date."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.WARNING,
                        "Run date parsed from run name '160102InconsistentRunName' is not the same as '2016-01-01'. OTP will use the run date from the '${RUN_DATE}' column.", "At least one run date parsed from run name is not the same as in the 'RUN_DATE' column."),
        ]

        RunRunDateValidator validator = new RunRunDateValidator()
        validator.runDateParserService = new RunDateParserService()

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
