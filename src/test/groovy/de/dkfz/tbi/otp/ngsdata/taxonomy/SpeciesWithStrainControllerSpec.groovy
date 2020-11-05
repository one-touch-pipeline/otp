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
package de.dkfz.tbi.otp.ngsdata.taxonomy

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory

import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY

class SpeciesWithStrainControllerSpec extends Specification implements ControllerUnitTest<SpeciesWithStrainController>, DataTest, TaxonomyFactory {

    private static final List<String> SORTED_NAMES = ["1", "2", "3", "D", "E", "F", "a", "b", "c"]

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CommonName,
                Species,
                SpeciesWithStrain,
                Strain,
        ]
    }

    void setupData() {
        controller.speciesWithStrainService = new SpeciesWithStrainService()
        controller.speciesService = new SpeciesService()
        controller.speciesService.commonNameService = new CommonNameService()
        controller.commonNameService = new CommonNameService()
        controller.strainService = new StrainService()
    }

    void "index with no data available"() {
        given:
        setupData()

        expect:
        Species.list() == []
        Strain.list() == []
        CommonName.list() == []
        SpeciesWithStrain.list() == []

        when:
        def model = controller.index()

        then:
        model.allSpecies == []
        model.speciesByCommonName == [:]
        model.strains == []
        model.commonNames == []
        model.speciesWithStrainsBySpecies == [:]
    }

    void "index, lists are sorted and grouped"() {
        given:
        setupData()

        CommonName mouse = createCommonName(name: "Mouse")
        CommonName human = createCommonName(name: "Human")

        Species musMusculus = createSpecies(commonName: mouse, scientificName: "mus musculus")
        Species homoSapiens = createSpecies(commonName: human, scientificName: "homo sapiens")

        SpeciesWithStrain mouseStrain1 = createSpeciesWithStrain(species: musMusculus, strain: createStrain(name: "strain 1"))
        SpeciesWithStrain mouseStrain2 = createSpeciesWithStrain(species: musMusculus, strain: createStrain(name: "strain 2"))
        SpeciesWithStrain humanStrainNotAvailable = createSpeciesWithStrain(species: homoSapiens, strain: createStrain(name: "Not available"))

        List<Species> expectedSortedSpecies = [homoSapiens, musMusculus]
        List<Strain> expectedSortedStrains = [humanStrainNotAvailable, mouseStrain1, mouseStrain2]*.strain
        List<CommonName> expectedSortedCommonName = [human, mouse]

        Map<CommonName, List<Species>> expectedSpeciesByCommonName = [
                (mouse): [musMusculus],
                (human): [homoSapiens],
        ]
        Map<Species, List<SpeciesWithStrain>> expectedSpeciesWithStrainBySpecies = [
                (musMusculus): [mouseStrain1, mouseStrain2],
                (homoSapiens): [humanStrainNotAvailable],
        ]

        when:
        def model = controller.index()

        then:
        model.allSpecies == expectedSortedSpecies
        model.speciesByCommonName == expectedSpeciesByCommonName
        model.strains == expectedSortedStrains
        model.commonNames == expectedSortedCommonName
        model.speciesWithStrainsBySpecies == expectedSpeciesWithStrainBySpecies
    }

    void "index, assert sort order of commonName"() {
        given:
        setupData()

        SORTED_NAMES.each { String name ->
            createCommonName(name: name)
        }

        when:
        def model = controller.index()

        then:
        model.commonNames*.name == SORTED_NAMES
    }

    void "index, assert sort order of strains"() {
        given:
        setupData()

        SORTED_NAMES.each { String name ->
            createStrain(name: name)
        }

        when:
        def model = controller.index()

        then:
        model.strains*.name == SORTED_NAMES
    }

    void "index, assert sort order of species"() {
        given:
        setupData()

        List<String> reverseSortedNames = SORTED_NAMES.reverse()

        SORTED_NAMES.eachWithIndex { String name, int i ->
            createSpecies(commonName: createCommonName(name: name), scientificName: reverseSortedNames[i])
        }

        when:
        def model = controller.index()

        then:
        model.allSpecies*.commonName.name == SORTED_NAMES
        model.allSpecies*.scientificName == reverseSortedNames
    }

    void "index, multiple Species get grouped by CommonName"() {
        given:
        setupData()

        CommonName commonName1 = createCommonName()
        CommonName commonName2 = createCommonName()

        Species species1 = createSpecies(commonName: commonName1)
        Species species2 = createSpecies(commonName: commonName1)
        Species species3 = createSpecies(commonName: commonName1)
        Species species4 = createSpecies(commonName: commonName2)

        Map<CommonName, List<Species>> expectedSpeciesByCommonName = [
                (commonName1): [species1, species2, species3],
                (commonName2): [species4],
        ]

        when:
        def model = controller.index()

        then:
        model.speciesByCommonName == expectedSpeciesByCommonName
    }

    void "index, multiple SpeciesWithStrain get grouped by Species"() {
        given:
        setupData()

        Species species1 = createSpecies()
        Species species2 = createSpecies()

        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain(species: species1)
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain(species: species1)
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain(species: species2)

        Map<Species, List<SpeciesWithStrain>> expectedSpeciesWithStrainBySpecies = [
                (species1): [speciesWithStrain1, speciesWithStrain2],
                (species2): [speciesWithStrain3],
        ]

        when:
        def model = controller.index()

        then:
        model.speciesWithStrainsBySpecies == expectedSpeciesWithStrainBySpecies
    }

    void "createSpeciesWithStrain, valid input"() {
        given:
        setupData()

        Species species = createSpecies()
        Strain strain = createStrain()

        expect:
        SpeciesWithStrain.list().isEmpty()

        when:
        controller.request.method = 'POST'
        controller.params.speciesId = species.id
        controller.params.strainId = strain.id
        controller.createSpeciesWithStrain()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/speciesWithStrain/index"

        controller.flash.message.message == "speciesWithStrain.succ"

        SpeciesWithStrain.findBySpeciesAndStrain(species, strain)
    }

    @Unroll
    void "createSpeciesWithStrain, fail on non existing #missingId ID"() {
        given:
        setupData()

        expect:
        SpeciesWithStrain.list().isEmpty()

        when:
        controller.request.method = 'POST'
        controller.params.speciesId = speciesId
        controller.params.strainId = strainId
        controller.createSpeciesWithStrain()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/speciesWithStrain/index"

        controller.flash.message.message == "speciesWithStrain.fail"

        SpeciesWithStrain.list().isEmpty()

        where:
        missingId | strainId              | speciesId
        "strain"  | 1                     | { createSpecies().id }
        "species" | { createStrain().id } | 1
    }

    void "createSpecies, valid input"() {
        given:
        setupData()

        String inputCommonName = "commonName"
        String inputScientificName = "scientificName"

        expect:
        Species.list().isEmpty()

        when:
        controller.request.method = 'POST'
        controller.params.commonNameName = inputCommonName
        controller.params.scientificName = inputScientificName
        controller.createSpecies()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/speciesWithStrain/index"

        controller.flash.message.message == "speciesWithStrain.succ"

        CommonName commonName = CommonName.findByName(inputCommonName)
        commonName != null

        Species.findByCommonNameAndScientificName(commonName, inputScientificName)
    }

    @Unroll
    void "createSpecies, reuse already existing CommonNames (#inputCN)"() {
        given:
        setupData()

        String scientificName = "scientificName"
        createCommonName(name: "Mouse")

        expect:
        Species.list().isEmpty()

        when:
        controller.request.method = 'POST'
        controller.params.commonNameName = inputCN
        controller.params.scientificName = scientificName
        controller.createSpecies()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/speciesWithStrain/index"

        controller.flash.message.message == "speciesWithStrain.succ"

        CommonName.list().size() == expectedNumberOfCommonNames
        Species.findByCommonNameAndScientificName(CommonName.findByNameIlike(inputCN), scientificName)

        where:
        inputCN || expectedNumberOfCommonNames
        "Human" || 2
        "Mouse" || 1
        "mouse" || 1
        "mOuSe" || 1
    }

    void "createSpecies, duplicate scientificNames are invalid input"() {
        given:
        setupData()
        String scientificName = "mus musculus"

        createSpecies(scientificName: scientificName)

        when:
        controller.request.method = 'POST'
        controller.params.scientificName = scientificName
        controller.params.commonName = "commonName"
        controller.createSpeciesWithStrain()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/speciesWithStrain/index"

        controller.flash.message.message == "speciesWithStrain.fail"
    }

    @Unroll
    void "createStrain, valid input (#name)"() {
        given:
        setupData()

        expect:
        Strain.list().isEmpty()

        when:
        controller.request.method = 'POST'
        controller.params.newStrainName = name
        controller.createStrain()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/speciesWithStrain/index"

        controller.flash.message.message == "speciesWithStrain.succ"

        Strain.findByName(name)

        where:
        name              | _
        "Strain"          | _
        "sTrAiN"          | _
        "/*!@#\$%%^&*(\\" | _
    }
}
