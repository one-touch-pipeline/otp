/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.gui

import geb.spock.GebSpec
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Ignore

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.User

@Rollback
@Integration
class HomeSpec extends GebSpec implements DomainFactoryCore {

    void "test the info page renders correctly"() {
        when:
        go '/'

        then: "The title is correct"
        $('#content h1').text() == "About OTP"

        then: "The links are correct"
        $('#infoMenu a')[0].getAttribute('href').endsWith("/")
        $('#infoMenu a')[1].getAttribute('href').endsWith("/info/numbers")
        $('#infoMenu a')[2].getAttribute('href').endsWith("/info/contact")
        $('#infoMenu a')[3].getAttribute('href').endsWith("/info/imprint")
        $('#infoMenu a')[4].getAttribute('href').endsWith("/info/templates")
    }

    void "test the home page renders correctly without projects"() {
        when:
        go 'home/index'

        then: "The titles are correct"
        $('.body').text() == "No project exists yet, click here to create a project"
    }

    @Ignore
    void "test the home page renders correctly with project"() {
        given: "create project"
        User user = DomainFactory.createUser([
                username: 'otp'
        ])
        DomainFactory.createUserProjectRole([
                user: user
        ])

        when:
        go 'home/index'

        then: "The titles are correct"
        $('.body h2')[0].text() == "Your projects"
        $('.body h2')[1].text() == "General statistics (for selected project group)"
    }
}
