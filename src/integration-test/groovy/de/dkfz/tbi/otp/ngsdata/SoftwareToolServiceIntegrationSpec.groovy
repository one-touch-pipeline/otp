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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.validation.ValidationException
import org.springframework.validation.Errors
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class SoftwareToolServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    SoftwareToolService softwareToolService

    void "getBaseCallingTool returns the same object for names only differing in case"() {
        given:
        SoftwareToolIdentifier softwareToolIdentifier = createSoftwareToolIdentifier(name: "unknown")

        expect:
        ["unknown", "UNKNOWN", "uNkNoWN"].every {
            return softwareToolService.getBaseCallingTool(it) == softwareToolIdentifier
        }
    }

    void "getBaseCallingTool only considers BASECALLING"() {
        given:
        SoftwareToolIdentifier softwareToolIdentifier = createSoftwareToolIdentifier(
                name        : "test",
                softwareTool: createSoftwareTool(type: type),
        )

        SoftwareToolIdentifier expected = (type == SoftwareTool.Type.BASECALLING) ? softwareToolIdentifier : null

        when:
        SoftwareToolIdentifier result = softwareToolService.getBaseCallingTool("test")

        then:
        result == expected

        where:
        type                          | _
        SoftwareTool.Type.BASECALLING | _
        SoftwareTool.Type.ALIGNMENT   | _
    }

    private List<SoftwareToolIdentifier> createIdentifierForSoftwareTool(SoftwareTool softwareTool, List<String> identifierNames) {
        return identifierNames.collect { String identifierName ->
            return createSoftwareToolIdentifier(name: identifierName, softwareTool: softwareTool)
        }
    }

    void "getIdentifiersPerSoftwareTool, returns all identifiers per software tool"() {
        given:
        createUserAndRoles()

        List<List<String>> listOfIdentifierNames = [
                ["A2", "A1"],
                ["B1"],
                ["C2", "C3", "C1"],
        ]

        Map<SoftwareTool, List<SoftwareToolIdentifier>> expected = listOfIdentifierNames.collectEntries { List<String> identifierNames ->
            SoftwareTool softwareTool = createSoftwareTool()
            [(softwareTool): createIdentifierForSoftwareTool(softwareTool, identifierNames).sort { it.name }]
        }

        Map<SoftwareTool, List<SoftwareToolIdentifier>> result

        when:
        result = doWithAuth(OPERATOR) {
            softwareToolService.identifiersPerSoftwareTool()
        }

        then:
        expected == result
    }

    void "test getSoftwareTollPerProgramName, groups all BASECALLING groups of software tools by name and sorts them by version"() {
        given:
        createUserAndRoles()
        String programName1 = "ProgramName1"
        String programName2 = "ProgramName2"
        String programName3 = "ProgramName3"

        List<SoftwareTool> softwareToolsA = ["B", "C", "A"].collect { String programVersion ->
            return createSoftwareTool(programName: programName1, programVersion: programVersion, type: SoftwareTool.Type.BASECALLING)
        }

        List<SoftwareTool> softwareToolsB = ["E", "D"].collect { String programVersion ->
            return createSoftwareTool(programName: programName2, programVersion: programVersion, type: SoftwareTool.Type.BASECALLING)
        }

        ["G", "F"].collect { String programVersion ->
            return createSoftwareTool(programName: programName3, programVersion: programVersion, type: SoftwareTool.Type.ALIGNMENT)
        }

        Map<String, List<SoftwareTool>> result = [:]

        when:
        result = doWithAuth(OPERATOR) {
            softwareToolService.softwareToolsPerProgramName()
        }

        then:
        result.size() == 2
        result[programName1] == softwareToolsA[2, 0, 1]
        result[programName2] == softwareToolsB[1, 0]
        result[programName3] == null
    }

    void "test updateSoftwareTool, updates a old Softwaretool"() {
        given:
        createUserAndRoles()
        String newVersion = "1.1"
        SoftwareTool softwareTool = createSoftwareTool()

        when:
        doWithAuth(OPERATOR) {
            softwareToolService.updateSoftwareTool(softwareTool.id, newVersion)
        }

        then:
        softwareTool.programVersion == newVersion
    }

    void "test updateSoftwareTool, throws exception if unique constraints does not break"() {
        given:
        createUserAndRoles()
        SoftwareTool softwareToolA = createSoftwareTool()
        SoftwareTool softwareToolB = createSoftwareTool(programName: softwareToolA.programName)

        when:
        doWithAuth(OPERATOR) {
            softwareToolService.updateSoftwareTool(softwareToolB.id, softwareToolA.programVersion)
        }

        then:
        ValidationException e = thrown()
        e.message.contains("softwareTool.programName.unique")
    }

    void "test createSoftwareTool, creates a software tool for the given properties"() {
        given:
        createUserAndRoles()

        Errors result

        when:
        result = doWithAuth(OPERATOR) {
            softwareToolService.createSoftwareTool("Test", "1.0", SoftwareTool.Type.BASECALLING)
        }

        then:
        result == null
        SoftwareTool.findAllByProgramNameAndProgramVersionAndType("Test", "1.0", SoftwareTool.Type.BASECALLING).size() == 1
    }

    void "test updateSoftwareToolIdentifier custom validator for case insensitive unique"() {
        given:
        createUserAndRoles()
        SoftwareToolIdentifier softwareToolIdentifier = createSoftwareToolIdentifier([
                name: "unknown",
        ])

        when:
        doWithAuth(OPERATOR) {
            softwareToolService.updateSoftwareToolIdentifier(softwareToolIdentifier.id, newIdentifier)
        }

        then:
        ValidationException e = thrown()
        e.message.contains("default.not.unique.message")

        where:
        newIdentifier << ["UNKNOWN", "unKnown", "UnKnown"]
    }

    void "test createSoftwareToolIdentifier, create a software tool identifier for the given properties"() {
        given:
        createUserAndRoles()
        String alias = "Test"
        SoftwareTool softwareTool = createSoftwareTool()

        SoftwareToolIdentifier result

        when:
        result = doWithAuth(OPERATOR) {
            softwareToolService.createSoftwareToolIdentifier(softwareTool, alias)
        }

        then:
        result == CollectionUtils.atMostOneElement(SoftwareToolIdentifier.findAllBySoftwareToolAndName(softwareTool, alias))
    }

    void "test createSoftwareToolIdentifier, create a software tool identifier with underscore"() {
        given:
        createUserAndRoles()
        String alias = "Test 123"
        String alias2 = "Test_123"
        SoftwareTool softwareTool = createSoftwareTool()

        SoftwareToolIdentifier result

        when:
        result = doWithAuth(OPERATOR) {
            softwareToolService.createSoftwareToolIdentifier(softwareTool, alias)
            softwareToolService.createSoftwareToolIdentifier(softwareTool, alias2)
        }

        then:
        result == CollectionUtils.atMostOneElement(SoftwareToolIdentifier.findAllBySoftwareToolAndName(softwareTool, alias2))
    }
}
