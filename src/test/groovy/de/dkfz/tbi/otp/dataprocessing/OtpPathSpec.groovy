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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project

class OtpPathSpec extends Specification implements DataTest, DomainFactoryCore {


    void "relativePath"() {
        given:
        Project project = new Project()

        when:
        OtpPath path0 = new OtpPath(project, 'first', 'second', 'third')

        then:
        project == path0.project
        new File('first/second/third') == path0.relativePath

        when:
        OtpPath path1 = new OtpPath(path0, 'fourth')

        then:
        project == path1.project
        new File('first/second/third/fourth') == path1.relativePath
    }

    void "getAbsoluteDataProcessingPath, when not absolute, then throw exception"() {
        given:
        new TestConfigService([
                (OtpProperty.PATH_PROCESSING_ROOT): 'processing_root'
        ])
        OtpPath otpPath = new OtpPath(new Project(), 'child')

        when:
        otpPath.absoluteDataProcessingPath

        then:
        RuntimeException e = thrown()
        e.message == 'processing_root is not absolute.'
    }

    void "getAbsoluteDataProcessingPath, when absolute, then return absolute file"() {
        given:
        new TestConfigService([
                (OtpProperty.PATH_PROCESSING_ROOT): '/processing_root'
        ])
        OtpPath otpPath = new OtpPath(new Project(), 'child')

        when:
        File path = otpPath.absoluteDataProcessingPath

        then:
        path == new File('/processing_root/child')
    }

    void "absoluteDataManagementPath, when not absolute, then throw exception"() {
        given:
        new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): 'root_path'
        ])
        OtpPath otpPath = new OtpPath(new Project(), 'child')

        when:
        otpPath.absoluteDataManagementPath

        then:
        RuntimeException e = thrown()
        e.message == 'root_path is not absolute.'
    }

    void "absoluteDataManagementPath, when absolute, then return absolute file"() {
        given:
        new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): '/root_path'
        ])
        OtpPath otpPath = new OtpPath(new Project(), 'child')

        when:
        File path = otpPath.absoluteDataManagementPath

        then:
        path == new File('/root_path/child')
    }
}
