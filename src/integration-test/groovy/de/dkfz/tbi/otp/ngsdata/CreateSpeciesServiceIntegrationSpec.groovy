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

package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors
import spock.lang.Specification

import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class CreateSpeciesServiceIntegrationSpec extends Specification implements UserAndRoles {

    @Autowired
    CreateSpeciesService createSpeciesService

    void setupData() {
        createUserAndRoles()
    }

    void "test createSpecies, all fine"() {
        given:
        setupData()
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = createSpeciesService.createSpecies('test', 'test')
        }

        then:
        errors == null
    }

    void "test createSpecies add expected errors, returns errors"() {
        given:
        setupData()
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = createSpeciesService.createSpecies(commonName, scientificName)
        }

        then:
        errors.allErrors*.codes*.contains('Contains invalid characters')

        where:
        commonName | scientificName
        'test('    | 'test'
        'test'     | 'test)'
        'test('    | 'test)'
    }

}
