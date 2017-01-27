package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import org.joda.time.format.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        Run,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
])
class RunRunDateValidatorSpec extends Specification {

    void 'validate adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${RUN_DATE}\t${RUN_ID}\n" +
                        "2016-01-01\tInconsistentMetadata\n" +
                        "2016-01-02\tInconsistentMetadata\n" +
                        "2016-01-02\tInconsistentDatabaseAndMetadata\n" +
                        "2016-01-01\tConsistentDatabaseAndMetadata\n"+
                        "2016-01-01\t160102InconsistentRunName\n"+
                        "2016-01-01\t160101tConsistentDatabaseAndMetadata\n")
        Date date = ISODateTimeFormat.date().parseDateTime("2016-01-01").toDate()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform(name: "Illumina")
        DomainFactory.createRun(name: 'InconsistentDatabaseAndMetadata', dateExecuted: date)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadata', dateExecuted: date)
        DomainFactory.createRun(name: '20160102InconsistentRunName', dateExecuted: date, seqPlatform: seqPlatform)
        DomainFactory.createRun(name: '160101tConsistentDatabaseAndMetadata', dateExecuted: date, seqPlatform: seqPlatform)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentMetadata' must have the same value in the column '${RUN_DATE}'."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "Run 'InconsistentDatabaseAndMetadata' is already registered in the OTP database with run date '2016-01-01', not with '2016-01-02'."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.WARNING,
                        "Run date parsed from run name '160102InconsistentRunName' is not the same as '2016-01-01'. OTP will use the run date from the '${RUN_DATE}' column."),
        ]

        when:
        new RunRunDateValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
