package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CUSTOMER_LIBRARY
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT

class LibraryProjectValidatorIntegrationSpec extends Specification {

    void 'validate, adds expected warnings,succeeds'() {
        given:
        setUpSeqTracks()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\t${PROJECT}\n" +
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
        new LibraryProjectValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[8].cells) as Set, Level.WARNING,
                        "In project 'project01' the following library names which look similar to '1' are already registered: 'lib1', 'library1'.", "For at least one project library names which look similar to entries in the metadata file are already registered."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "In project 'project02' the following library names which look similar to '1' are already registered: 'lib1'.", "For at least one project library names which look similar to entries in the metadata file are already registered."),
                new Problem((context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[2].cells) as Set, Level.WARNING,
                        "All rows for project 'project02' which look similar to '1' should have the same value in column '${CUSTOMER_LIBRARY}'.", "All rows for one project which have a similar customer library should have the same value in column 'CUSTOMER_LIBRARY'."),
                new Problem((context.spreadsheet.dataRows[3].cells + context.spreadsheet.dataRows[4].cells) as Set, Level.WARNING,
                        "All rows for project 'project01' which look similar to '5' should have the same value in column '${CUSTOMER_LIBRARY}'.", "All rows for one project which have a similar customer library should have the same value in column 'CUSTOMER_LIBRARY'."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, missing column PROJECT, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        "lib1\n"
        )

        when:
        new LibraryProjectValidator().validate(context)

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
        new LibraryProjectValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, missing column CUSTOMER_LIBRARY and PROJECT, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new LibraryProjectValidator().validate(context)

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
