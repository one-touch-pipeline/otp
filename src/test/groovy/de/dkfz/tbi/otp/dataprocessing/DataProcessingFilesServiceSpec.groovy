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
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.DataProcessingFilesService.OutputDirectories
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*

class DataProcessingFilesServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                Project,
                Realm,
        ]
    }

    @Unroll
    void "getOutputDirectory, if input is #outputDirectories, then create correct path"() {
        given:
        DataProcessingFilesService dataProcessingFilesService = new DataProcessingFilesService()
        ConfigService configService = new TestConfigService()

        Individual individual = createIndividual()
        Project project = individual.project

        String pid = individual.pid
        String projectDir = project.dirName
        String rootDir = configService.getProcessingRootPath()

        String expectedPath = "${rootDir}/${projectDir}/results_per_pid/${pid}/${lastPath}"

        when:
        String actualPath = dataProcessingFilesService.getOutputDirectory(individual, outputDirectories)

        then:
        expectedPath == actualPath

        where:
        outputDirectories           | lastPath
        null                        | ''
        OutputDirectories.BASE      | ''
        OutputDirectories.ALIGNMENT | 'alignment/'
        OutputDirectories.FASTX_QC  | 'fastx_qc/'
    }
}
