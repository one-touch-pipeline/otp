/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY_TARGET
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class AntibodyTargetValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AntibodyTarget,
        ]
    }

    void 'validate, when no ANTIBODY_TARGET column exists in metadata file, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new AntibodyTargetValidator([
                antibodyTargetService: new AntibodyTargetService(),
        ]).validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when ANTIBODY_TARGET column is empty, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        ""
        )

        when:
        new AntibodyTargetValidator([
                antibodyTargetService: new AntibodyTargetService(),
        ]).validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when ANTIBODY_TARGET entry is not registered in database, adds errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "some_antibody_target"
        )

        when:
        new AntibodyTargetValidator([
                antibodyTargetService: new AntibodyTargetService(),
        ]).validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The antibody target 'some_antibody_target' is not registered in OTP.")
    }

    void 'validate, when ANTIBODY_TARGET entry is registered in database, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "some_antibody_target\n" +
                        "Some_Antibody_Target"
        )
        DomainFactory.createAntibodyTarget(name: "some_antibody_target")

        when:
        new AntibodyTargetValidator([
                antibodyTargetService: new AntibodyTargetService(),
        ]).validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when ANTIBODY_TARGET entry is registered in database as alias, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "some_antibody_target\n" +
                        "Some_Antibody_Target"
        )
        DomainFactory.createAntibodyTarget(name: "antibody_target", importAlias: ['some_antibody_target'])

        when:
        new AntibodyTargetValidator([
                antibodyTargetService: new AntibodyTargetService(),
        ]).validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when ANTIBODY_TARGET entry is a shortcut of a registered entry in database, adds errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "target"
        )
        DomainFactory.createAntibodyTarget(name: "some_antibody_target")

        when:
        new AntibodyTargetValidator([
                antibodyTargetService: new AntibodyTargetService(),
        ]).validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The antibody target 'target' is not registered in OTP.")
    }

    void 'validate, when ANTIBODY_TARGET entry contains special character, adds errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "some_%_target"
        )
        DomainFactory.createAntibodyTarget(name: "some_antibody_target")

        when:
        new AntibodyTargetValidator([
                antibodyTargetService: new AntibodyTargetService(),
        ]).validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The antibody target 'some_%_target' is not registered in OTP.")
    }
}
