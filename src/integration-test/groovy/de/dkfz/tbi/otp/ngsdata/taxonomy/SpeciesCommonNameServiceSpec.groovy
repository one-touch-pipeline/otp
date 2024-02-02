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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.validation.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class SpeciesCommonNameServiceSpec extends Specification implements UserAndRoles, TaxonomyFactory {

    private static final String NAME = "Mouse"

    @Autowired
    SpeciesCommonNameService speciesCommonNameService

    void setupData() {
        createUserAndRoles()
    }

    void "createSpeciesCommonName, all fine"() {
        given:
        setupData()
        Errors errors

        when:
        errors = doWithAuth(OPERATOR) {
            speciesCommonNameService.createSpeciesCommonName(NAME)
        }

        then:
        errors == null
    }

    void "createSpeciesCommonName, name contains invalid characters"() {
        given:
        setupData()
        Errors errors

        when:
        errors = doWithAuth(OPERATOR) {
            speciesCommonNameService.createSpeciesCommonName("${NAME}#@!")
        }

        then:
        errors.allErrors*.codes*.contains('Contains invalid characters')
    }

    @Unroll
    void "createSpeciesCommonName, name \"#name\" already exists"() {
        given:
        setupData()
        Errors errors

        createSpeciesCommonName(name: NAME)

        when:
        errors = doWithAuth(OPERATOR) {
            speciesCommonNameService.createSpeciesCommonName(name)
        }

        then:
        errors.allErrors*.codes*.contains('SpeciesCommonName already exists')

        where:
        name               | _
        NAME               | _
        NAME.toLowerCase() | _
        NAME.toUpperCase() | _
    }

    void "findOrSaveSpeciesCommonName, speciesCommonName found"() {
        given:
        setupData()
        SpeciesCommonName speciesCommonName = createSpeciesCommonName(name: NAME)
        SpeciesCommonName foundSpeciesCommonName

        when:
        foundSpeciesCommonName = doWithAuth(OPERATOR) {
            speciesCommonNameService.findOrSaveSpeciesCommonName(name)
        }

        then:
        speciesCommonName.id == foundSpeciesCommonName.id

        where:
        name               | _
        NAME               | _
        NAME.toLowerCase() | _
        NAME.toUpperCase() | _
    }

    void "findOrSaveSpeciesCommonName, speciesCommonName saved"() {
        given:
        setupData()

        expect:
        !CollectionUtils.atMostOneElement(SpeciesCommonName.findAllByNameIlike(NAME))

        SpeciesCommonName speciesCommonName
        when:
        speciesCommonName = doWithAuth(OPERATOR) {
            speciesCommonNameService.findOrSaveSpeciesCommonName(NAME)
        }

        then:
        speciesCommonName
    }

    void "createAndGetSpeciesCommonName, valid"() {
        given:
        setupData()

        expect:
        !CollectionUtils.atMostOneElement(SpeciesCommonName.findAllByNameIlike(NAME))

        SpeciesCommonName speciesCommonName
        when:
        speciesCommonName = doWithAuth(OPERATOR) {
            speciesCommonNameService.createAndGetSpeciesCommonName(NAME)
        }

        then:
        speciesCommonName
    }

    void "createAndGetSpeciesCommonName, invalid"() {
        given:
        setupData()

        expect:
        !CollectionUtils.atMostOneElement(SpeciesCommonName.findAllByNameIlike(NAME))

        when:
        doWithAuth(OPERATOR) {
            speciesCommonNameService.createAndGetSpeciesCommonName("${NAME}#@!")
        }

        then:
        thrown(ValidationException)
    }
}
