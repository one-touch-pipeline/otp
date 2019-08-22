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
import grails.validation.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class CommonNameServiceSpec extends Specification implements UserAndRoles, TaxonomyFactory {

    private static final String NAME = "Mouse"

    @Autowired
    CommonNameService commonNameService

    void setupData() {
        createUserAndRoles()
    }

    void "createCommonName, all fine"() {
        given:
        setupData()
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = commonNameService.createCommonName(NAME)
        }

        then:
        errors == null
    }

    void "createCommonName, name contains invalid characters"() {
        given:
        setupData()
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = commonNameService.createCommonName("${NAME}#@!")
        }

        then:
        errors.allErrors*.codes*.contains('Contains invalid characters')
    }

    @Unroll
    void "createCommonName, name \"#name\" already exists"() {
        given:
        setupData()
        Errors errors

        createCommonName(name: NAME)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            errors = commonNameService.createCommonName(name)
        }

        then:
        errors.allErrors*.codes*.contains('CommonName already exists')

        where:
        name               | _
        NAME               | _
        NAME.toLowerCase() | _
        NAME.toUpperCase() | _
    }

    void "findOrSaveCommonName, commonName found"() {
        given:
        setupData()
        CommonName commonName = createCommonName(name: NAME)
        CommonName foundCommonName

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            foundCommonName = commonNameService.findOrSaveCommonName(name)
        }

        then:
        commonName.id == foundCommonName.id

        where:
        name               | _
        NAME               | _
        NAME.toLowerCase() | _
        NAME.toUpperCase() | _
    }

    void "findOrSaveCommonName, commonName saved"() {
        given:
        setupData()

        expect:
        !CommonName.findByNameIlike(NAME)

        CommonName commonName
        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            commonName = commonNameService.findOrSaveCommonName(NAME)
        }

        then:
        commonName
    }

    void "createAndGetCommonName, valid"() {
        given:
        setupData()

        expect:
        !CommonName.findByNameIlike(NAME)

        CommonName commonName
        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            commonName = commonNameService.createAndGetCommonName(NAME)
        }

        then:
        commonName
    }

    void "createAndGetCommonName, invalid"() {
        given:
        setupData()

        expect:
        !CommonName.findByNameIlike(NAME)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            commonNameService.createAndGetCommonName("${NAME}#@!")
        }

        then:
        thrown(ValidationException)
    }
}
