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
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ProjectInternalNoteValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    protected final static String INTERNAL_NOTES = 'notes'

    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingOption,
                Project,
                Realm,
                SampleIdentifier,
        ]
    }

    void 'validate metadata, when project column does not exist, no problem'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new ProjectInternalNoteValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate metadata, when project column exist but is empty or unknown project, no problem'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "\n" +
                        "unknown"
        )

        when:
        new ProjectInternalNoteValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate metadata, when project column exist but project has no internal note, no problem'() {
        given:
        Project project = createProject()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${project.name}\n"
        )

        when:
        new ProjectInternalNoteValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate metadata, when project column exist and project has internal note, add info'() {
        given:
        Project project = createProject(internalNotes: INTERNAL_NOTES)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${project.name}\n" +
                        "${project.name}\n"
        )

        when:
        new ProjectInternalNoteValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.INFO
        containSame(problem.affectedCells*.cellAddress, Collections.emptySet())
        problem.message.contains("Internal notes for project '${project.name}': ${project.internalNotes}")
    }

    void 'validate metadata, when project column exists, one project has internal note another one does not, add info for the one that has'() {
        given:
        Project project = createProject(internalNotes: INTERNAL_NOTES)
        Project project2 = createProject()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${project.name}\n" +
                        "${project2.name}\n"
        )

        when:
        new ProjectInternalNoteValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.INFO
        containSame(problem.affectedCells*.cellAddress, Collections.emptySet())
        problem.message.contains("Internal notes for project '${project.name}': ${project.internalNotes}")
    }

    void 'validate metadata, when project column exists, two projects each one with there own note, add infos'() {
        given:
        Project project = createProject(internalNotes: INTERNAL_NOTES)
        Project project2 = createProject(internalNotes: INTERNAL_NOTES)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${project.name}\n" +
                        "${project2.name}\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.INFO, "Internal notes for project '${project.name}': ${project.internalNotes}", "A Project has internal notes"),
                new Problem(Collections.emptySet(), Level.INFO, "Internal notes for project '${project2.name}': ${project2.internalNotes}", "A Project has internal notes"),
        ]

        when:
        new ProjectInternalNoteValidator().validate(context)

        then:
        CollectionUtils.containSame(context.problems,  expectedProblems)
    }

    void 'validate metadata, when sample name column exist for project with internal note, adds info'() {
        given:
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier()
        sampleIdentifier.project.internalNotes = INTERNAL_NOTES
        sampleIdentifier.project.save(flush: true)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_NAME}\n" +
                        "${sampleIdentifier.name}\n"
        )

        when:
        new ProjectInternalNoteValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.INFO
        containSame(problem.affectedCells*.cellAddress, Collections.emptySet())
        problem.message.contains("Internal notes for project '${sampleIdentifier.project.name}': ${sampleIdentifier.project.internalNotes}")
    }
}
