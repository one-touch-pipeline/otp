package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ProjectCategory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import grails.test.mixin.Mock
import spock.lang.Specification


@Mock([
        DataFile,
        FileType,
        Project,
        ProjectCategory,
])
class Md5sumUniqueValidatorSpec extends Specification {

    void 'validate, when column is missing, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Mandatory column 'MD5' is missing.")
    }

    void 'validate, all are fine'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MD5}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
""")

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, adds expected errors'() {

        given:
        String md5sum1 = HelperUtils.getRandomMd5sum()
        String md5sum2 = HelperUtils.getRandomMd5sum()
        String md5sum3 = HelperUtils.getRandomMd5sum()
        String md5sum4 = HelperUtils.getRandomMd5sum()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MD5}
${md5sum1}
${md5sum2}
${md5sum3}
${md5sum2}
""")
        DomainFactory.createDataFile(md5sum: md5sum3)
        DomainFactory.createDataFile(md5sum: md5sum4)

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[3].cells as Set, Level.WARNING,
                        "The MD5 sum '${md5sum2}' is not unique in the metadata file."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "A fastq file with the MD5 sum '${md5sum3}' is already registered in OTP."),
        ]

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}
