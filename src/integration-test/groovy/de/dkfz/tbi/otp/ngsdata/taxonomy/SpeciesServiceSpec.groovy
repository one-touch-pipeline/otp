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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.SessionUtils

@Rollback
@Integration
class SpeciesServiceSpec extends Specification implements UserAndRoles, TaxonomyFactory {

    private static final String MUS = "mus musculus"
    private static final String MOUSE = "Mouse"

    @Autowired
    SpeciesService speciesService

    void setupData() {
        createUserAndRoles()
    }

    void "createSpeciesAndCommonName, all fine"() {
        given:
        setupData()
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = speciesService.createSpeciesAndCommonName(MOUSE, MUS)
        }

        then:
        errors == null
    }

    @Unroll
    void "createSpeciesAndCommonName, fails on invalid commonName \"#commonName\""() {
        given:
        setupData()
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = speciesService.createSpeciesAndCommonName(commonName, MUS)
        }

        then:
        errors.allErrors*.codes*.contains(expectedError)
        SessionUtils.withNewSession {
            CommonName.list() == []
            Species.list() == []
        }

        where:
        commonName    || expectedError
        ""            || "cannot be blank"
        "${MOUSE}#@!" || "Contains invalid characters"
    }

    @Unroll
    void "createSpecies, fails on invalid scientificName \"#scientificName\""() {
        given:
        setupData()
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = speciesService.createSpeciesAndCommonName(MOUSE, scientificName)
        }

        then:
        errors.allErrors*.codes*.contains(expectedError)
        SessionUtils.withNewSession {
            CommonName.list() == []
            Species.list() == []
        }

        where:
        scientificName || expectedError
        ""             || "cannot be blank"
        "${MUS}#@!"    || "Contains invalid characters"
    }
}
