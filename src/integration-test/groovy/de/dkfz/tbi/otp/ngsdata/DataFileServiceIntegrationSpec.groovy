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
        } as List<DataFile>

        then:
        dataFilesContentIsEqual(result[0], expected[0])
        dataFilesContentIsEqual(result[1], expected[1])
    }

    private boolean dataFilesContentIsEqual(DataFile file1, DataFile file2) {
        return file1.withdrawnComment == file2.withdrawnComment &&
                file1.fileWithdrawn == file2.fileWithdrawn &&
                file1.seqTrack.sampleIdentifier == file2.seqTrack.sampleIdentifier &&
                file1.seqTrack.sample.individual.id == file2.seqTrack.sample.individual.id &&
                file1.seqTrack.sample.individual.mockFullName == file2.seqTrack.sample.individual.mockFullName &&
                file1.seqTrack.sample.sampleType.id == file2.seqTrack.sample.sampleType.id &&
                file1.seqTrack.sample.sampleType.name == file2.seqTrack.sample.sampleType.name &&
                file1.seqTrack.seqType.displayNameWithLibraryLayout == file2.seqTrack.seqType.displayNameWithLibraryLayout &&
                file1.seqTrack.seqType.libraryLayout == file2.seqTrack.seqType.libraryLayout &&
                file1.seqTrack.seqType.singleCell == file2.seqTrack.seqType.singleCell &&
                file1.seqTrack.seqType.displayName == file2.seqTrack.seqType.displayName &&
                file1.seqTrack.seqType.id == file2.seqTrack.seqType.id
    }
}
