/*
 * Copyright 2011-2024 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_LIBRARY
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT

@Rollback
@Integration
class TagmentationLibraryProjectValidatorIntegrationSpec extends Specification {

    void 'validate, adds expected warnings,succeeds'() {
        given:
        setUpSeqTracks()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\t${PROJECT}\n" +
                        "lib1\tproject01\n" +
                        "lib1\tproject02\n" +
                        "library1\tproject02\n" +
                        "lib5\tproject01\n" +
                        "library5\tproject01\n" +
                        "lib2\tproject01\n" +
                        "lib2\tproject01\n" +
                        "lib2\tproject01\n" +
                        "lib1\tproject01\n" +
                        "\tproject02\n"
        )

        when:
        new TagmentationLibraryProjectValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[8].cells) as Set, LogLevel.WARNING,
                        "In project 'project01' the following tagmentation library names which look similar to '1' are already registered: 'lib1', 'library1'.", "For at least one project tagmentation library names which look similar to entries in the metadata file are already registered."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.WARNING,
                        "In project 'project02' the following tagmentation library names which look similar to '1' are already registered: 'lib1'.", "For at least one project tagmentation library names which look similar to entries in the metadata file are already registered."),
                new Problem((context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[2].cells) as Set, LogLevel.WARNING,
                        "All rows for project 'project02' which look similar to '1' should have the same value in column '${TAGMENTATION_LIBRARY}'.", "All rows for one project which have a similar tagmentation library should have the same value in column 'TAGMENTATION_LIBRARY'."),
                new Problem((context.spreadsheet.dataRows[3].cells + context.spreadsheet.dataRows[4].cells) as Set, LogLevel.WARNING,
                        "All rows for project 'project01' which look similar to '5' should have the same value in column '${TAGMENTATION_LIBRARY}'.", "All rows for one project which have a similar tagmentation library should have the same value in column 'TAGMENTATION_LIBRARY'."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, missing column PROJECT, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\n" +
                        "lib1\n"
        )

        when:
        new TagmentationLibraryProjectValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, missing column CUSTOMER_LIBRARY, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${PROJECT}\n" +
                        "project01\n"
        )

        when:
        new TagmentationLibraryProjectValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, missing column CUSTOMER_LIBRARY and PROJECT, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new TagmentationLibraryProjectValidator().validate(context)

        then:
        context.problems.empty
    }

    private void setUpSeqTracks() {
        Project project01 = DomainFactory.createProject(name: "project01")
        Project project02 = DomainFactory.createProject(name: "project02")
        Individual individual01 = DomainFactory.createIndividual(project: project01)
        Individual individual02 = DomainFactory.createIndividual(project: project01)
        Individual individual03 = DomainFactory.createIndividual(project: project01)
        Individual individual04 = DomainFactory.createIndividual(project: project02)
        Sample sample01 = DomainFactory.createSample(individual: individual01)
        Sample sample02 = DomainFactory.createSample(individual: individual02)
        Sample sample03 = DomainFactory.createSample(individual: individual03)
        Sample sample04 = DomainFactory.createSample(individual: individual04)
        DomainFactory.createSampleIdentifier(name: 'sample01', sample: sample01)
        DomainFactory.createSampleIdentifier(name: 'sample02', sample: sample02)
        DomainFactory.createSampleIdentifier(name: 'sample03', sample: sample03)
        DomainFactory.createSampleIdentifier(name: 'sample04', sample: sample04)

        DomainFactory.createSeqTrack(libraryName: 'lib1', normalizedLibraryName: SeqTrack.normalizeLibraryName('lib1'), sample: sample01)
        DomainFactory.createSeqTrack(libraryName: 'lib1', normalizedLibraryName: SeqTrack.normalizeLibraryName('lib1'), sample: sample02)
        DomainFactory.createSeqTrack(libraryName: 'lib2', normalizedLibraryName: SeqTrack.normalizeLibraryName('lib2'), sample: sample02)
        DomainFactory.createSeqTrack(libraryName: 'library1', normalizedLibraryName: SeqTrack.normalizeLibraryName('library1'), sample: sample03)
        DomainFactory.createSeqTrack(libraryName: 'lib1', normalizedLibraryName: SeqTrack.normalizeLibraryName('lib1'), sample: sample04)
        DomainFactory.createSeqTrack(libraryName: null, normalizedLibraryName: SeqTrack.normalizeLibraryName(null), sample: sample04)
        DomainFactory.createSeqTrack(libraryName: '', normalizedLibraryName: SeqTrack.normalizeLibraryName(''), sample: sample04)
    }
}
