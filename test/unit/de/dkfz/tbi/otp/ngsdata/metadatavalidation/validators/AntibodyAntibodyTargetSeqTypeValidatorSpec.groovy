package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import spock.lang.Specification

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY_TARGET
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE

class AntibodyAntibodyTargetSeqTypeValidatorSpec extends Specification {


    void 'validate, when no ANTIBODY_TARGET and ANTIBODY column exist and seqType is not ChIP seq, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                "some seqType"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when no ANTIBODY column exists and seqType is ChIP seq, adds warnings'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\n" +
                        "ChIP Seq\tsome_antibody_target"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "Antibody is not provided although data is ChIP seq data."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when ANTIBODY column is empty and seqType is ChIP seq, adds warnings'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\t${ANTIBODY}\n" +
                        "ChIP Seq\tsome_antibody_target\t"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "Antibody is not provided although data is ChIP seq data."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when no ANTIBODY_TARGET column exists and seqType is ChIP seq, adds errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY}\n" +
                        "ChIP Seq\tsome_antibody"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR, "Antibody target is not provided although data is ChIP seq data."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when ANTIBODY_TARGET column is empty and seqType is ChIP seq, adds errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\t${ANTIBODY}\n" +
                        "ChIP Seq\t\tsome_antibody"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR, "Antibody target is not provided although data is ChIP seq data."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when ANTIBODY_TARGET and ANTIBODY column exist and seqType is not ChIP seq, adds warnings'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\t${ANTIBODY}\n" +
                        "some seqtype\tsome_antibody_target\tsome_antibody"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "Antibody target ('some_antibody_target') and/or antibody ('some_antibody') are/is provided although data is no ChIP seq data. OTP will ignore the values."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when ANTIBODY_TARGET and ANTIBODY column exist and seqType is ChIP seq, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\t${ANTIBODY}\n" +
                        "ChIP Seq\tsome_antibody_target\tsome_antibody"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }

}
