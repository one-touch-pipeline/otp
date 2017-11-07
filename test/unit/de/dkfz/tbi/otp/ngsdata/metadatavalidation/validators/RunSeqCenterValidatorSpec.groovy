package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        Run,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
])
class RunSeqCenterValidatorSpec extends Specification {

    void 'validate adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CENTER_NAME}\t${RUN_ID}\n" +
                        "Center1\tInconsistentMetadata\n" +
                        "Center2\tInconsistentMetadata\n" +
                        "Center2\tInconsistentDatabaseAndMetadata\n" +
                        "Center1\tConsistentDatabaseAndMetadata\n" +
                        "Center1\tRunNotRegistered\n")
        SeqCenter center1 = DomainFactory.createSeqCenter(name: 'Center1')
        DomainFactory.createRun(name: 'InconsistentDatabaseAndMetadata', seqCenter: center1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadata', seqCenter: center1)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "All entries for run 'InconsistentMetadata' must have the same value in the column '${CENTER_NAME}'."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "Run 'InconsistentDatabaseAndMetadata' is already registered in the OTP database with sequencing center 'Center1', not with 'Center2'."),
        ]

        when:
        new RunSeqCenterValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
