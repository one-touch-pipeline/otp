package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([SoftwareTool, SoftwareToolIdentifier])
class PipelineVersionValidatorSpec extends Specification {

    static final String PIPELINE_VERSION = "pipeline version"

    static final String METADATA_CONTENT =
            "${MetaDataColumn.PIPELINE_VERSION}\n" +
            "\n" +
            "${PIPELINE_VERSION}\n"

    void 'validate, when metadata file contains valid pipeline version, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                METADATA_CONTENT
        )

        DomainFactory.createSoftwareToolIdentifier(
                name: PIPELINE_VERSION,
                softwareTool: DomainFactory.createSoftwareTool( type: SoftwareTool.Type.BASECALLING )
        )

        when:
        new PipelineVersionValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata file contain invalid pipeline version, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                METADATA_CONTENT
        )

        when:
        new PipelineVersionValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A3'])
        problem.message.contains("Pipeline version '${PIPELINE_VERSION}' is not registered in the OTP database.")
    }
}
