/*
 * Copyright 2011-2024 The OTP authors
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
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class RawSequenceFileServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    @Unroll
    void "getAllRawSequenceFilesOfProject, returns DataFiles of given Project"() {
        given:
        createUserAndRoles()
        RawSequenceFileService rawSequenceFileService = new RawSequenceFileService()

        Closure<RawSequenceFile> createRawSequenceFileForProject = { Project project ->
            return createFastqFile(
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
        List<RawSequenceFile> expected = [
            createRawSequenceFileForProject(project),
            createRawSequenceFileForProject(project),
        ]

        createFastqFile()

        when:
        List<RawSequenceFile> result = doWithAuth(ADMIN) {
            rawSequenceFileService.getAllRawSequenceFilesOfProject(project)
        } as List<RawSequenceFile>

        then:
        rawSequenceFilesContentIsEqual(result[0], expected[0])
        rawSequenceFilesContentIsEqual(result[1], expected[1])
    }

    private boolean rawSequenceFilesContentIsEqual(RawSequenceFile file1, RawSequenceFile file2) {
        return file1.withdrawnComment == file2.withdrawnComment &&
                file1.fileWithdrawn == file2.fileWithdrawn &&
                file1.seqTrack.sampleIdentifier == file2.seqTrack.sampleIdentifier &&
                file1.seqTrack.sample.individual.id == file2.seqTrack.sample.individual.id &&
                file1.seqTrack.sample.individual.pid == file2.seqTrack.sample.individual.pid &&
                file1.seqTrack.sample.sampleType.id == file2.seqTrack.sample.sampleType.id &&
                file1.seqTrack.sample.sampleType.name == file2.seqTrack.sample.sampleType.name &&
                file1.seqTrack.seqType.displayNameWithLibraryLayout == file2.seqTrack.seqType.displayNameWithLibraryLayout &&
                file1.seqTrack.seqType.libraryLayout == file2.seqTrack.seqType.libraryLayout &&
                file1.seqTrack.seqType.singleCell == file2.seqTrack.seqType.singleCell &&
                file1.seqTrack.seqType.displayName == file2.seqTrack.seqType.displayName &&
                file1.seqTrack.seqType.id == file2.seqTrack.seqType.id
    }
}
