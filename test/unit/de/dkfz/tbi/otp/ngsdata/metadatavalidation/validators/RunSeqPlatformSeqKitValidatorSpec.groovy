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
        SequencingKitLabel,
])
class RunSeqPlatformSeqKitValidatorSpec extends Specification {

    @Unroll
    void 'validate #identifier in the run name against the platform and kit combination, adds expected problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CENTER_NAME}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${RUN_ID}\n" +
                        "${center}\tPlatform\t${platformName}\t${kitName}\t${runName}\n"
        )

        SeqPlatformModelLabel model = DomainFactory.createSeqPlatformModelLabel(name: platformName, alias: [platformName])
        SequencingKitLabel kit = DomainFactory.createSequencingKitLabel(name: kitName, alias: [kitName])
        SeqPlatform platform = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'Platform',
                seqPlatformModelLabel: model,
                sequencingKitLabel: kit,
                identifierInRunName: identifier
        )
        DomainFactory.createRun(name: runName, seqPlatform: platform)

        when:
        new RunSeqPlatformSeqKitValidator().validate(context)

        then:
        if (result) {
            Problem problem = exactlyOneElement(context.problems)
            assert problem.level == Level.WARNING
            assert containSame(problem.affectedCells, context.spreadsheet.dataRows[0].cells)
            assert problem.message.contains("The run name ${runName} does not contain the sequencing kit and sequencing platform specific run identifier ${identifier}.")
        } else {
            assert context.problems.isEmpty()
        }

        where:
        center | platformName | kitName | runName | identifier || result
        "DKFZ" | "HiSeq 2000" | "V3" | "someACXXRunName" | null || null
        "DKFZ" | "HiSeq 2000" | "V3" | "someACXXRunName" |  "ACXX" || null
        "DKFZ" | "HiSeq 2000" | "V3" | "someACXXRunName" |  "AAAA" || "error"
        "otherCenter" | "HiSeq 2000" | "V3" | "someACXXRunName" |  "ACXX" || null
        "otherCenter" | "HiSeq 2000" | "V3" | "someACXXRunName" |  "AAAA" || null
    }
}
