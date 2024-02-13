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
package de.dkfz.tbi.otp.domainFactory.taxonomy

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.taxonomy.*

trait TaxonomyFactory implements DomainFactoryCore {

    Species createSpecies(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Species, [
                speciesCommonName: { createSpeciesCommonName() },
                scientificName   : "scientificName ${nextId}",
        ], properties, saveAndValidate)
    }

    SpeciesCommonName createSpeciesCommonName(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(SpeciesCommonName, [
                name: "name ${nextId}",
        ], properties, saveAndValidate)
    }

    Strain createStrain(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Strain, [
                name: "name_${nextId}",
        ], properties, saveAndValidate)
    }

    SpeciesWithStrain createSpeciesWithStrain(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(SpeciesWithStrain, [
                species: { createSpecies() },
                strain : { createStrain() },
        ], properties, saveAndValidate)
    }

    SpeciesCommonName findOrCreateSpeciesCommonName(String name, boolean saveAndValidate = true) {
        return findOrCreateDomainObject(SpeciesCommonName, [name: name], [:], [:], saveAndValidate)
    }

    Strain findOrCreateStrain(String name, boolean saveAndValidate = true) {
        return findOrCreateDomainObject(Strain, [name: name], [:], [:], saveAndValidate)
    }

    Species findOrCreateSpecies(SpeciesCommonName speciesCommonName, String scientificName, boolean saveAndValidate = true) {
        return findOrCreateDomainObject(Species, [
                speciesCommonName: speciesCommonName,
                scientificName   : scientificName,
        ], [:], [:], saveAndValidate)
    }

    SpeciesWithStrain findOrCreateSpeciesWithStrain(Species species, Strain strain, Set<String> importAlias, boolean saveAndValidate = true) {
        return findOrCreateDomainObject(SpeciesWithStrain, [
                species: species,
                strain : strain,
        ], [:], [importAlias: importAlias,], saveAndValidate)
    }

    SpeciesWithStrain findOrCreateHumanSpecies() {
        SpeciesCommonName human = findOrCreateSpeciesCommonName("Human")
        Species homoSapiens = findOrCreateSpecies(human, "homo sapiens")
        Strain strain = findOrCreateStrain("No strain available")
        Set<String> importAlias = ["human"]

        return findOrCreateSpeciesWithStrain(homoSapiens, strain, importAlias)
    }

    SpeciesWithStrain findOrCreateMouseSpecies() {
        SpeciesCommonName mouse = findOrCreateSpeciesCommonName("Mouse")
        Species musMusculus = findOrCreateSpecies(mouse, "mus musculus")
        Strain strain = findOrCreateStrain("unknown")
        Set<String> importAlias = ["mouse"]

        return findOrCreateSpeciesWithStrain(musMusculus, strain, importAlias)
    }
}

class TaxonomyFactoryInstance implements TaxonomyFactory {

    final static TaxonomyFactoryInstance INSTANCE = new TaxonomyFactoryInstance()
}
