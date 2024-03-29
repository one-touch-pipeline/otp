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
package de.dkfz.tbi.otp.ngsdata.taxonomy

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class SpeciesWithStrainService {

    List<SpeciesWithStrain> list() {
        return SpeciesWithStrain.list()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Species> allSpecies() {
        return Species.list()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createSpeciesWithStrain(Species species, Strain strain) {
        SpeciesWithStrain speciesWithStrain = new SpeciesWithStrain(
                species: species,
                strain: strain
        )
        try {
            speciesWithStrain.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    SpeciesWithStrain getByAlias(String importAlias) {
        return CollectionUtils.atMostOneElement(SpeciesWithStrain.list().findAll {
            it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase())
        })
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void changeLegacyState(SpeciesWithStrain domainObject, boolean legacy) {
        domainObject.legacy = legacy
        assert domainObject.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addNewAlias(Long id, String importAlias) {
        assert id: "the input ID must not be null"
        assert importAlias: "the input importAlias must not be null"

        SpeciesWithStrain instance = SpeciesWithStrain.get(id)
        assert instance: "could not get an instance of type SpeciesWithStrain with ID ${id}"

        SpeciesWithStrain duplicateImportAlias = getByAlias(importAlias)
        assert !duplicateImportAlias: "importAlias ${importAlias} already exists for ${duplicateImportAlias}"

        assert !instance.importAlias.contains(importAlias): "the importAlias was already created"
        instance.importAlias.add(importAlias)
        assert instance.save(flush: true)
    }
}
