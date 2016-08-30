package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        Comment,
        IlseSubmission,
])
class IlseNumberBlacklistedValidatorSpec extends Specification {

    void 'validate, when column does not exist, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
NoIlseColumn
abc
""")

        when:
        new IlseNumberBlacklistedValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata fields contain not blacklisted ILSe number, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.ILSE_NO}
5464
""")

        when:
        new IlseNumberBlacklistedValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata fields contain blacklisted ILSe number without warning, succeeds'() {
        given:
        int ilse = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.ILSE_NO}
${ilse}
""")
        DomainFactory.createIlseSubmission([ilseNumber: ilse, warning: false])

        when:
        new IlseNumberBlacklistedValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata fields contain blacklisted ILSe number, adds warnings'() {
        given:
        int ilse = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.ILSE_NO}
${ilse}
""")
        IlseSubmission ilseSubmission = DomainFactory.createIlseSubmission([ilseNumber: ilse, warning: true])


        when:
        new IlseNumberBlacklistedValidator().validate(context)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "The ilse ${ilse} is blacklisted:\n${ilseSubmission.comment.displayString()}."),
        ]

        then:
        containSame(context.problems, expectedProblems)
    }
}
