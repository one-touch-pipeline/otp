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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ProjectClosedValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingOption,
                Project,
                ProjectCategory,
                Realm,
                SampleIdentifier,
        ]
    }

    void 'validate metadata, when project column exist but project is not closed'() {
        given:
        Project project = DomainFactory.createProject()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${project.name}\n"
        )

        when:
        new ProjectClosedValidator().validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate metadata, when project column exist but project is closed, add problem'() {
        given:
        Project project = DomainFactory.createProject([closed: true])
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${project.name}\n"
        )

        when:
        new ProjectClosedValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The project '${project.name}' is closed.")
    }

    void 'validate metadata, when sample ID column exist but project is closed, add problem'() {
        given:
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier()
        sampleIdentifier.sample.project.closed = true
        sampleIdentifier.sample.project.save(flush: true)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_ID}\n" +
                        "${sampleIdentifier.name}\n"
        )

        when:
        new ProjectClosedValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The project '${sampleIdentifier.project}' is closed.")
    }

    void 'validate metadata, when sample ID is unknown and project is not given'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_ID}\n" +
                        "asdf\n"
        )

        when:
        new ProjectClosedValidator().validate(context)

        then:
        context.problems.isEmpty()
    }


    void 'validate metadata, when sample ID is unknown and project does not exist'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_ID}\t${MetaDataColumn.PROJECT}\n" +
                        "asdf\tasdf\n"
        )

        when:
        new ProjectClosedValidator().validate(context)

        then:
        context.problems.isEmpty()
    }
}
