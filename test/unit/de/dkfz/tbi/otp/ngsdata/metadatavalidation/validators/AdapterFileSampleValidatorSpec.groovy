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
        AdapterFile,
])
class AdapterFileSampleValidatorSpec extends Specification {

    static final String adapterName = "adapter"

    static final String VALID_METADATA =
        "${SEQUENCING_TYPE.name()}\t${SAMPLE_SUBMISSION_TYPE.name()}\t${ADAPTER_FILE.name()}\t${TAGMENTATION_BASED_LIBRARY}\n" +
        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tSample\t\t\n" +
        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tSample\t\ttrue\n" +
        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tSample\t${adapterName}\t\n" +
        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tasdf\t${adapterName}\t\n"

    void setup() {
        DomainFactory.createAdapterFile(fileName: adapterName).save(flush: true, failOnError: true)
    }


    void 'validate, when all data is valid'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        AdapterFileSampleValidator validator = new AdapterFileSampleValidator()
        validator.adapterFileService = new AdapterFileService()
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when columns are missing, is valid'(String document) {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(document)

        when:
        AdapterFileValidator validator = new AdapterFileValidator()
        validator.adapterFileService = new AdapterFileService()
        validator.validate(context)

        then:
        context.problems.empty

        where:
        document                                                                                | _
        "${SEQUENCING_TYPE.name()}\t${SAMPLE_SUBMISSION_TYPE.name()}\t${ADAPTER_FILE.name()}\n" | _
        "${SEQUENCING_TYPE.name()}\t${SAMPLE_SUBMISSION_TYPE.name()}\n"                         | _
        "${SEQUENCING_TYPE.name()}\t${ADAPTER_FILE.name()}\n"                                   | _
        "${SEQUENCING_TYPE.name()}\n"                                                           | _
        "${SAMPLE_SUBMISSION_TYPE.name()}\t${ADAPTER_FILE.name()}\n"                            | _
        "${SAMPLE_SUBMISSION_TYPE.name()}\n"                                                    | _
        "${ADAPTER_FILE.name()}\n"                                                              | _
        "other_column\n"                                                                        | _
    }

    void 'validate, when adapter file is required and missing, is not valid'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tasdf\t\n"
        )

        when:
        AdapterFileSampleValidator validator = new AdapterFileSampleValidator()
        validator.adapterFileService = new AdapterFileService()
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        containSame(problem.affectedCells*.cellAddress, ['A6', 'B6', 'C6', 'D6'])
        problem.message.contains("There should be an entry in the ADAPTER_FILE column for sequencing type '${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}'")
    }
}
