/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.utils.validation

import spock.lang.*

class OtpPathValidatorSpec extends Specification {

    @Shared
    String originalOsName

    @Shared
    String originalGrailsEnv

    void setupSpec() {
        originalOsName = System.getProperty("os.name")
        originalGrailsEnv = System.getProperty("grails.env")
        System.setProperty("grails.env", "test")
    }

    void cleanup() {
        System.setProperty("os.name", originalOsName)
        System.setProperty("grails.env", originalGrailsEnv)
    }

    @Unroll
    void "test isValidPathComponent with '#input' should return #expected"() {
        expect:
        OtpPathValidator.isValidPathComponent(input) == expected

        where:
        input           | expected
        "validName"     | true
        "valid_name"    | true
        "valid-name"    | true
        "valid.name"    | true
        "valid123"      | true
        "123valid"      | true
        ""              | false
        "invalid/name"  | false
        "invalid\\name" | false
        "invalid name"  | false
        "."             | false
        ".."            | false
    }

    void "test isValidRelativePath with Windows paths"() {
        given: "Simulate Windows environment"
        System.setProperty("os.name", "Windows 10")

        expect: "Windows environment should be detected"
        OtpPathValidator.isWindows()

        and: "Windows relative paths should be valid"
        OtpPathValidator.isValidRelativePath("path\\to\\file")
        OtpPathValidator.isValidRelativePath("simple")
        OtpPathValidator.isValidRelativePath("path\\wi.th\\dot")

        and: "Unix relative paths should be rejected"
        !OtpPathValidator.isValidRelativePath("path/to/file")

        and: "paths with dots should be rejected"
        !OtpPathValidator.isValidRelativePath("path\\with\\..\\dots")
        !OtpPathValidator.isValidRelativePath("path\\with\\.\\dot")
    }

    void "test isValidRelativePath with Unix paths"() {
        given: "Mock the system property to simulate Unix"
        System.setProperty("os.name", "Linux")
        System.setProperty("grails.env", "test")

        expect: "Unix relative paths should be valid"
        OtpPathValidator.isValidRelativePath("path/to/file")
        OtpPathValidator.isValidRelativePath("simple")
        OtpPathValidator.isValidRelativePath("path/wi.th/dot")

        and: "Windows relative paths should be rejected"
        !OtpPathValidator.isValidRelativePath("path\\to\\file")

        and: "paths with dots should be rejected"
        !OtpPathValidator.isValidRelativePath("path/with/../dots")
        !OtpPathValidator.isValidRelativePath("path/with/./dot")
    }

    void "test isValidAbsolutePath with Windows paths"() {
        given: "Mock the system property to simulate Windows"
        System.setProperty("os.name", "Windows 10")
        System.setProperty("grails.env", "test")

        expect: "UNC paths should be valid"
        OtpPathValidator.isValidAbsolutePath("\\absolute\\path")
        !OtpPathValidator.isValidAbsolutePath("relative\\path")

        and: "Unix paths should be rejected"
        !OtpPathValidator.isValidAbsolutePath("/absolute/path")

        and: "drive letter paths should be valid"
        OtpPathValidator.isValidAbsolutePath("C:\\absolute\\path")
        OtpPathValidator.isValidAbsolutePath("D:\\some\\other\\path")
        OtpPathValidator.isValidAbsolutePath("C:\\")
        OtpPathValidator.isValidAbsolutePath("C:")
        OtpPathValidator.isValidAbsolutePath("Z:\\single")

        and: "invalid drive letter paths should be rejected"
        !OtpPathValidator.isValidAbsolutePath("C:/absolute/path")
        !OtpPathValidator.isValidAbsolutePath("1:\\invalid")
        !OtpPathValidator.isValidAbsolutePath("CC:\\invalid")
    }

    void "test isValidAbsolutePath with Unix paths"() {
        given: "Mock the system property to simulate Unix"
        System.setProperty("os.name", "Linux")
        System.setProperty("grails.env", "test")

        expect: "valid Unix paths should be accepted"
        OtpPathValidator.isValidAbsolutePath("/absolute/path")
        !OtpPathValidator.isValidAbsolutePath("relative/path")

        and: "Windows UNC paths should be rejected"
        !OtpPathValidator.isValidAbsolutePath("\\absolute\\path")

        and: "Windows drive letter paths should be rejected"
        !OtpPathValidator.isValidAbsolutePath("C:\\absolute\\path")
        !OtpPathValidator.isValidAbsolutePath("D:\\some\\other\\path")
        !OtpPathValidator.isValidAbsolutePath("C:\\")
        !OtpPathValidator.isValidAbsolutePath("C:")
        !OtpPathValidator.isValidAbsolutePath("Z:\\single")
    }

    void "test isValidAbsolutePathContainingVariable with Windows paths"() {
        given: "Mock the system property to simulate Windows"
        System.setProperty("os.name", "Windows 10")
        System.setProperty("grails.env", "test")

        expect: "UNC paths with variables should be valid"
        OtpPathValidator.isValidAbsolutePathContainingVariable('\\path\\\${variable}\\file') /* codenarc-disable-line GStringExpressionWithinString */
        OtpPathValidator.isValidAbsolutePathContainingVariable("\\path\\normal\\file")

        and: "Unix paths should be rejected"
        !OtpPathValidator.isValidAbsolutePathContainingVariable('/path/\${variable}/file') /* codenarc-disable-line GStringExpressionWithinString */

        and: "drive letter paths with variables should be valid"
        OtpPathValidator.isValidAbsolutePathContainingVariable('C:\\path\\\${variable}\\file') /* codenarc-disable-line GStringExpressionWithinString */
        OtpPathValidator.isValidAbsolutePathContainingVariable('D:\\some\\\${VAR}\\path') /* codenarc-disable-line GStringExpressionWithinString */
        OtpPathValidator.isValidAbsolutePathContainingVariable("C:\\path\\normal\\file")
    }

    void "test isValidAbsolutePathContainingVariable with Unix paths"() {
        given: "Mock the system property to simulate Unix"
        System.setProperty("os.name", "Linux")
        System.setProperty("grails.env", "test")

        expect: "valid Unix paths with variables should be accepted"
        OtpPathValidator.isValidAbsolutePathContainingVariable('/path/\${variable}/file') /* codenarc-disable-line GStringExpressionWithinString */
        OtpPathValidator.isValidAbsolutePathContainingVariable("/path/normal/file")

        and: "Windows UNC paths should be rejected"
        !OtpPathValidator.isValidAbsolutePathContainingVariable('\\path\\\${variable}\\file') /* codenarc-disable-line GStringExpressionWithinString */

        and: "Windows drive letter paths should be rejected"
        !OtpPathValidator.isValidAbsolutePathContainingVariable('C:\\path\\\${variable}\\file') /* codenarc-disable-line GStringExpressionWithinString */
        !OtpPathValidator.isValidAbsolutePathContainingVariable('D:\\some\\\${VAR}\\path') /* codenarc-disable-line GStringExpressionWithinString */
        !OtpPathValidator.isValidAbsolutePathContainingVariable("C:\\path\\normal\\file")
    }

    void "test production environment always uses Linux paths"() {
        given:
        System.setProperty("os.name", "Windows 10")
        System.setProperty("grails.env", "production")

        expect: "Unix paths should be valid in production"
        OtpPathValidator.isValidAbsolutePath("/linux/path")

        and: "Windows paths should be rejected in production"
        !OtpPathValidator.isValidAbsolutePath("\\windows\\path")
        !OtpPathValidator.isValidAbsolutePath("C:\\windows\\path")
    }
}
