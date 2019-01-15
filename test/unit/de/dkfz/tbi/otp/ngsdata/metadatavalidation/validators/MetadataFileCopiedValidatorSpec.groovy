package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Mock([SeqCenter])
class MetadataFileCopiedValidatorSpec extends Specification {

    MetadataFileCopiedValidator metadataFileCopiedValidator

    static final String CONTEXT = "${CENTER_NAME}\t${ILSE_NO}\nCenter\t1234"

    @Rule
    TemporaryFolder temporaryFolder

    Path seqCenterMetadataFile

    void setup() {
        Path seqCenterDir = temporaryFolder.newFolder('seqCenterInbox').toPath()

        seqCenterMetadataFile = seqCenterDir.resolve('metadata_fastq.tsv')
        seqCenterMetadataFile.text = CONTEXT

        metadataFileCopiedValidator = new MetadataFileCopiedValidator()
        metadataFileCopiedValidator.metadataImportService = Spy(MetadataImportService) {
            getIlseFolder('1234', _) >> seqCenterDir
            getIlseFolder('invalid', _) >> null
        }
    }

    @Unroll
    void 'validate, when seqCenter contextString = #contextString and copyMetadataFile = #copyMetadataFile, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(contextString, copyMetadataFile)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty

        where:
        contextString                                   | copyMetadataFile
        CONTEXT                                         | false
        CONTEXT                                         | true
        "${ILSE_NO}\t1234"                              | true
        "${PROJECT}\nProject"                           | true
        "${CENTER_NAME}\tCenter"                        | true
        "${CENTER_NAME}\t${ILSE_NO}\nCenter\tinvalid"   | true
    }

    void 'validate, when seqCenterInbox metadataFile is not there, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        Files.delete(seqCenterMetadataFile)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when seqCenterInbox metadataFile is a directorys, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        Files.delete(seqCenterMetadataFile)
        Files.createDirectory(seqCenterMetadataFile)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when seqCenterInbox metadataFile is not readable, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        Files.setPosixFilePermissions(seqCenterMetadataFile, [] as Set)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when seqCenterInbox metadataFile is empty, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        seqCenterMetadataFile.bytes = []

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("There is already a file in the seqcenter inbox but it is different from this metadata file.")
    }

    void 'validate, when contents are different, adds Error'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT + "\nCenter\t1234")

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("There is already a file in the seqcenter inbox but it is different from this metadata file.")
    }

    private MetadataValidationContext createContextAndSeqCenter(String contextString, copyMetadataFile = true) {
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                contextString,
                [
                        content: contextString.bytes,
                ]
        )
        DomainFactory.createSeqCenter(
                name: 'Center',
                copyMetadataFile: copyMetadataFile,
        )
        return context
    }
}
