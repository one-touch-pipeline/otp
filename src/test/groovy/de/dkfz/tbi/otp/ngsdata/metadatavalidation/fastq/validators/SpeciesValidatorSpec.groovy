/*
 * Copyright 2011-2021 The OTP authors
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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.taxonomy.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class SpeciesValidatorSpec extends Specification implements DataTest, DomainFactoryCore, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                SampleIdentifier,
                SpeciesWithStrain,
        ]
    }

    void "test validate"() {
        given:
        SpeciesValidator validator = new SpeciesValidator()
        validator.speciesWithStrainService = new SpeciesWithStrainService()
        validator.sampleIdentifierService = new SampleIdentifierService()
        validator.validatorHelperService = new ValidatorHelperService()
        validator.sampleService = new SampleService()

        Strain strain = createStrain(name: "No strain available")
        SpeciesWithStrain human = createSpeciesWithStrain(importAlias: ["human"] as Set,
                species: createSpecies(scientificName: "Homo sapiens", speciesCommonName: createSpeciesCommonName(name: "Human")), strain: strain)
        SpeciesWithStrain mouse = createSpeciesWithStrain(importAlias: ["mouse"] as Set,
                species: createSpecies(scientificName: "Mus musculus", speciesCommonName: createSpeciesCommonName(name: "Mouse")), strain: strain)
        SpeciesWithStrain dolphin = createSpeciesWithStrain(importAlias: ["dolphin"] as Set,
                species: createSpecies(scientificName: "Delphinus delphis", speciesCommonName: createSpeciesCommonName(name: "Dolphin")), strain: strain)

        // correct metadata
        createSampleIdentifier(name: "sample", sample: createSample(individual: createIndividual(species: human), mixedInSpecies: []))
        createSampleIdentifier(name: "sampleWithMixedInSpecies", sample: createSample(individual: createIndividual(species: human), mixedInSpecies: [mouse, dolphin]))
        createSampleIdentifier(name: "existingSampleWithoutData1", sample: createSample(individual: createIndividual(species: null), mixedInSpecies: []))
        createSampleIdentifier(name: "existingSampleWithoutData2", sample: createSample(individual: createIndividual(species: null), mixedInSpecies: []))

        // incorrect metadata
        createSampleIdentifier(name: "distinctSpecies", sample: createSample(individual: createIndividual(pid: "1", species: mouse)))
        createSampleIdentifier(name: "existingDifferentSpecies1", sample: createSample(individual: createIndividual(pid: "2", species: dolphin)))
        createSampleIdentifier(name: "existingDifferentSpecies2", sample: createSample(individual: createIndividual(pid: "3", species: null), seqTracks: [createSeqTrack()]))

        createSampleIdentifier(name: "distinctMixin", sample: createSample(individual: createIndividual(pid: "4", species: human), sampleType: createSampleType(name: "a")))
        createSampleIdentifier(name: "existingDifferentMixin1", sample: createSample(individual: createIndividual(pid: "5", species: human), sampleType: createSampleType(name: "b"), mixedInSpecies: [mouse]))
        createSampleIdentifier(name: "existingDifferentMixin2", sample: createSample(individual: createIndividual(pid: "6", species: human), sampleType: createSampleType(name: "c"), mixedInSpecies: [], seqTracks: [createSeqTrack()]))
        createSampleIdentifier(name: "existingDifferentMixin3", sample: createSample(individual: createIndividual(pid: "7", species: human), sampleType: createSampleType(name: "d"), mixedInSpecies: [human, mouse]))

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SAMPLE_NAME}\t${SPECIES}\t${PROJECT}
sample\tHUMAN\t
sample\thuman\t
sample\thUmAn\t
sampleWithMixedInSpecies\thuman+mouse+dolphin\t
sampleWithMixedInSpecies\thuman+dolphin+mouse\t
existingSampleWithoutData1\tHUMAN\t
existingSampleWithoutData2\tHUMAN+MOUSE\t
\t\t
\tCERBERUS\t
\tHUMAN+PEGASUS+MEDUSA\t
distinctSpecies\tHUMAN\t
distinctSpecies\tMOUSE\t
existingDifferentSpecies1\tHUMAN\t
existingDifferentSpecies2\tHUMAN\t
distinctMixin\tHUMAN+MOUSE\t
distinctMixin\tHUMAN+DOLPHIN\t
existingDifferentMixin1\tHUMAN\t
existingDifferentMixin2\tHUMAN+MOUSE\t
existingDifferentMixin3\tHUMAN+DOLPHIN+MOUSE\t
unknownSampleName\tHUMAN\t
unknownSampleName\tMOUSE\t
""")

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[7].cells as Set, LogLevel.ERROR,
                        "The field 'SPECIES' must not be empty.",
                        "The field 'SPECIES' must not be empty."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, LogLevel.ERROR,
                        "The species 'CERBERUS' is not known to OTP.",
                        "Each value of the + separated list in field 'SPECIES' has to be known to OTP."),
                new Problem(context.spreadsheet.dataRows[9].cells as Set, LogLevel.ERROR,
                        "The species 'PEGASUS' is not known to OTP.",
                        "Each value of the + separated list in field 'SPECIES' has to be known to OTP."),
                new Problem(context.spreadsheet.dataRows[9].cells as Set, LogLevel.ERROR,
                        "The species 'MEDUSA' is not known to OTP.",
                        "Each value of the + separated list in field 'SPECIES' has to be known to OTP."),

                new Problem(context.spreadsheet.dataRows[10, 11].collectMany { it.cells } as Set, LogLevel.ERROR,
                        "PID '1' has distinct species 'Human (Homo sapiens) [No strain available]', 'Mouse (Mus musculus) [No strain available]'.",
                        "All entries of a PID must have the same main species."),
                new Problem(context.spreadsheet.dataRows[12].cells as Set, LogLevel.ERROR,
                        "Species 'Human (Homo sapiens) [No strain available]' for PID '2' doesn't match with existing PID with species 'Dolphin (Delphinus delphis) [No strain available]'.",
                        "If the PID already exists, the main species has to match."),
                new Problem(context.spreadsheet.dataRows[13].cells as Set, LogLevel.ERROR,
                        "Species 'Human (Homo sapiens) [No strain available]' for PID '3' doesn't match with existing PID with species 'null'.",
                        "If the PID already exists, the main species has to match."),

                new Problem(context.spreadsheet.dataRows[14, 15].collectMany { it.cells } as Set, LogLevel.ERROR,
                        "Sample '4 a' has distinct mixed-in species 'Mouse (Mus musculus) [No strain available]', 'Dolphin (Delphinus delphis) [No strain available]'.",
                        "All entries of a sample must have the same mixed-in species."),
                new Problem(context.spreadsheet.dataRows[16].cells as Set, LogLevel.ERROR,
                        "No mixed-in species for sample '5 b' doesn't match with existing sample with mixed-in species 'Mouse (Mus musculus) [No strain available]'.",
                        "If the sample already exists, the mixed-in species have to match."),
                new Problem(context.spreadsheet.dataRows[17].cells as Set, LogLevel.ERROR,
                        "Mixed-in species 'Mouse (Mus musculus) [No strain available]' for sample '6 c' doesn't match with existing sample without mixed-in species.",
                        "If the sample already exists, the mixed-in species have to match."),
                new Problem(context.spreadsheet.dataRows[18].cells as Set, LogLevel.ERROR,
                        "Mixed-in species 'Dolphin (Delphinus delphis) [No strain available],Mouse (Mus musculus) [No strain available]' for sample '7 d' doesn't match with existing sample with mixed-in species 'Human (Homo sapiens) [No strain available],Mouse (Mus musculus) [No strain available]'.",
                        "If the sample already exists, the mixed-in species have to match."),
        ]
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void "test validate that species is set for project"() {
        given:
        SpeciesValidator validator = new SpeciesValidator()
        validator.speciesWithStrainService = new SpeciesWithStrainService()
        validator.sampleIdentifierService = new SampleIdentifierService()
        validator.validatorHelperService = new ValidatorHelperService()
        validator.sampleService = new SampleService()

        Strain strain = createStrain(name: "No strain available")
        SpeciesWithStrain human = createSpeciesWithStrain(importAlias: ["human"] as Set,
                species: createSpecies(scientificName: "Homo sapiens", speciesCommonName: createSpeciesCommonName(name: "Human")), strain: strain)
        SpeciesWithStrain mouse = createSpeciesWithStrain(importAlias: ["mouse"] as Set,
                species: createSpecies(scientificName: "Mus musculus", speciesCommonName: createSpeciesCommonName(name: "Mouse")), strain: strain)

        createSampleIdentifier(name: "sampleHuman", sample: createSample(individual: createIndividual(species: human), mixedInSpecies: []))
        createSampleIdentifier(name: "sampleMouse", sample: createSample(individual: createIndividual(species: mouse), mixedInSpecies: []))

        Project projectForSpeciesHuman = createProject(name: 'projectForSpeciesHuman', speciesWithStrains: [human] as Set)
        Project projectWithoutSpecies = createProject(name: 'projectWithoutSpecies')

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SAMPLE_NAME}\t${SPECIES}\t${PROJECT}
sampleHuman\tHUMAN\t${projectForSpeciesHuman.name}
sampleHuman\tHUMAN\t${projectWithoutSpecies.name}
sampleMouse\tmouse\t${projectForSpeciesHuman.name}
""")
        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        "No allowed species are set for project 'projectWithoutSpecies'.",
                        "The main species must match the species set for the project."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.ERROR,
                        "The main species 'Mouse (Mus musculus) [No strain available]' doesn't match the species 'Human (Homo sapiens) [No strain available]' set for the project 'projectForSpeciesHuman'.",
                        "The main species must match the species set for the project."),
        ]
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}
