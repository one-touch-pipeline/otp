package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

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


    void 'validate, when no ANTIBODY column exists and seqType is ChIP seq, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\n" +
                        "ChIP Seq\tsome_antibody_target"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        assertContainSame(context.problems, [])
    }


    void 'validate, when ANTIBODY column is empty and seqType is ChIP seq, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\t${ANTIBODY}\n" +
                        "ChIP Seq\tsome_antibody_target\t"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        assertContainSame(context.problems, [])
    }


    void 'validate, when no ANTIBODY_TARGET column exists and seqType is ChIP seq, adds error'() {
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


    void 'validate, when ANTIBODY_TARGET column is empty and seqType is ChIP seq, adds error'() {
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


    void 'validate, when ANTIBODY_TARGET and ANTIBODY column exist and seqType is not ChIP seq, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${ANTIBODY_TARGET}\t${ANTIBODY}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "some seqtype\tsome_antibody_target\tsome_antibody\t\n" +
                        "ChIP Seq\tsome_antibody_target\tsome_antibody\t\n" +
                        "ChIP Seq\tsome_antibody_target\tsome_antibody\ttrue"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "Antibody target ('some_antibody_target') and/or antibody ('some_antibody') are/is provided although data is no ChIP seq data. OTP will ignore the values.", "Antibody target and/or antibody are/is provided although data is no ChIP seq data. OTP will ignore the values."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING, "Antibody target ('some_antibody_target') and/or antibody ('some_antibody') are/is provided although data is no ChIP seq data. OTP will ignore the values.", "Antibody target and/or antibody are/is provided although data is no ChIP seq data. OTP will ignore the values."),
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
