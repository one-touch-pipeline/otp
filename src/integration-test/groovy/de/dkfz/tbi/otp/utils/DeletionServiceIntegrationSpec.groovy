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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*

@Rollback
@Integration
class DeletionServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    DeletionService deletionService

    void setupData() {
        deletionService = new DeletionService()
        deletionService.lsdfFilesService = Mock(LsdfFilesService)
        deletionService.dataSwapService = Mock(DataSwapService)
        deletionService.commentService = Mock(CommentService)
        deletionService.fastqcDataFilesService = Mock(FastqcDataFilesService)
        deletionService.dataProcessingFilesService = Mock(DataProcessingFilesService)
        deletionService.seqTrackService = Mock(SeqTrackService)
        deletionService.analysisDeletionService = new AnalysisDeletionService()
        deletionService.fileService = new FileService()
    }

    void "test delete project content"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        createDataFile([seqTrack: seqTrack])
        createDataFile([seqTrack: seqTrack])

        when:
        deletionService.deleteProjectContent(project)

        then:
        DataFile.count() == 0
        Individual.count() == 0
        Project.count() == 1
    }

    void "test delete project content without any content"() {
        given:
        setupData()
        Project project = createProject()

        when:
        deletionService.deleteProjectContent(project)

        then:
        Project.count() == 1
    }

    void "test delete project"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        createDataFile([seqTrack: seqTrack])
        createDataFile([seqTrack: seqTrack])

        when:
        deletionService.deleteProject(project)

        then:
        DataFile.count() == 0
        Individual.count() == 0
        Project.count() == 0
    }

    void "test delete project without any content"() {
        given:
        setupData()
        Project project = createProject()

        when:
        deletionService.deleteProject(project)

        then:
        Project.count() == 0
    }

    void "test delete project with config per project and seq type"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
                obsoleteDate: new Date(),
        ])

        DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
                previousConfig: config1,
        ])

        when:
        deletionService.deleteProject(project)

        then:
        ConfigPerProjectAndSeqType.count() == 0
        Project.count() == 0
    }

    void "test delete project with config per project and seq type reverse"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
        ])

        RoddyWorkflowConfig config2 = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
                obsoleteDate: new Date(),
        ])

        config1.previousConfig = config2
        config1.save(flush: true)

        when:
        deletionService.deleteProject(project)

        then:
        ConfigPerProjectAndSeqType.count() == 0
        Project.count() == 0
    }
}
