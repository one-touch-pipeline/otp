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
package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class DataFileServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    @Unroll
    void "getAllDataFilesOfProject, returns DataFiles of given Project"() {
        given:
        createUserAndRoles()
        DataFileService dataFileService = new DataFileService()

        Closure<DataFile> createDataFileForProject = { Project project ->
            return createDataFile(
                    seqTrack: createSeqTrack(
                            sample: createSample(
                                    individual: createIndividual(
                                            project: project,
                                    ),
                            ),
                    ),
            )
        }

        Project project = createProject()
        List<DataFile> expected = [
            createDataFileForProject(project),
            createDataFileForProject(project),
        ]

        createDataFile()

        when:
        List<DataFile> result = SpringSecurityUtils.doWithAuth(ADMIN) {
            dataFileService.getAllDataFilesOfProject(project)
        }

        then:
        TestCase.assertContainSame(expected, result)
    }
}
