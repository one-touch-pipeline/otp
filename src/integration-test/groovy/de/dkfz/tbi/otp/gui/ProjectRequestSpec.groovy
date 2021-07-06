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

/**
 * This test isn't finished yet and may not working.
 */

@Ignore
@Rollback
@Integration
class ProjectRequestSpec extends GebSpec {

    @Ignore
    void "Test the project request page"() {
        when:
        go 'projectRequest/index'

        then: "check page"
        $('.body h1').text() == "Project requests"

        then: "fill in"
        $('#name').value("new_project")
        $('#description').value("description")
        $('#keyword').value("keyword")
        $('#organizationalUnit').value("organizationalUnit")
        $('#storagePeriod').value("TEN_YEARS")
        $('#pi').value("pi")

        then: "submit"
        $('#Submit').click()
    }
}
