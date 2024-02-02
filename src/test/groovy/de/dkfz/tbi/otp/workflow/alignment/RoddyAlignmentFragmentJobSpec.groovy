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
package de.dkfz.tbi.otp.workflow.alignment

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.PanCancerWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.SingleSelectSelectorExtendedCriteria
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class RoddyAlignmentFragmentJobSpec extends Specification implements DataTest, PanCancerWorkflowDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                FileType,
                MergingWorkPackage,
                RoddyBamFile,
                WorkflowStep,
        ]
    }

    private RoddyAlignmentFragmentJob job
    private WorkflowStep workflowStep
    private LibraryPreparationKit seqTrackLibPrepKit
    private LibraryPreparationKit workPackageLibPrepKit
    private MergingWorkPackage workPackage
    private RoddyBamFile roddyBamFile

    private void setupData(boolean workPackageLibIsNull, boolean seqTrackLibIsNull) {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: createPanCancerWorkflowVersion(),
                ]),
        ])

        seqTrackLibPrepKit = seqTrackLibIsNull ? null : createLibraryPreparationKit()
        workPackageLibPrepKit = workPackageLibIsNull ? null : seqTrackLibPrepKit

        workPackage = createMergingWorkPackage([
                libraryPreparationKit: workPackageLibPrepKit,
        ])

        createMergingCriteria([
                project      : workPackage.project,
                seqType      : workPackage.seqType,
                useLibPrepKit: !workPackageLibIsNull,
        ])

        job = new RoddyAlignmentFragmentJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> { roddyBamFile }
            0 * _
        }
    }

    @Unroll
    void "fetchSelectors, when #name, then return only one criteria"() {
        given:
        setupData(workPackageLibIsNull, seqTrackLibIsNull)

        List<SeqTrack> seqTracks = (1..countOfSeqTracks).collect {
            DomainFactory.createSeqTrackWithTwoFastqFiles(workPackage, [
                    libraryPreparationKit: seqTrackLibPrepKit,
            ])
        }
        roddyBamFile = createBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
                workPackage        : workPackage,
                seqTracks          : seqTracks as Set,
        ])

        when:
        List<SingleSelectSelectorExtendedCriteria> criteriaList = job.fetchSelectors(workflowStep)

        then:
        criteriaList.size() == 1
        SingleSelectSelectorExtendedCriteria criteria = criteriaList.first()

        criteria.workflow == workflowStep.workflowRun.workflow
        criteria.workflowVersion == workflowStep.workflowRun.workflowVersion
        criteria.project == workPackage.project
        criteria.seqType == workPackage.seqType
        criteria.referenceGenome == workPackage.referenceGenome
        criteria.libraryPreparationKit == seqTrackLibPrepKit

        where:
        name                                                                       | workPackageLibIsNull | seqTrackLibIsNull | countOfSeqTracks
        'lib is null and single seqTrack'                                          | true                 | true              | 1
        'lib is not null and single seqTrack'                                      | false                | false             | 1
        'lib is not null for workPackage but not for seqTrack and single seqTrack' | true                 | false             | 1
        'lib is null and two seqTrack'                                             | true                 | true              | 2
        'lib is not null and two seqTrack'                                         | false                | false             | 2
        'lib is not null for workPackage but not for seqTrack and two seqTrack'    | true                 | false             | 2
        'lib is null and 5 seqTrack'                                               | true                 | true              | 5
        'lib is not null and 5 seqTrack'                                           | false                | false             | 5
        'lib is not null for workPackage but not for seqTrack and 5 seqTrack'      | true                 | false             | 5
    }

    @Unroll
    void "fetchSelectors, when #name, then return multiple criteria"() {
        given:
        setupData(true, oneSeqTrackIsNull)

        List<SeqTrack> seqTracks = [
                DomainFactory.createSeqTrackWithFastqFiles(workPackage, [
                        libraryPreparationKit: seqTrackLibPrepKit,
                ]),
        ] + (2..countOfSeqTracks).collect {
            DomainFactory.createSeqTrackWithFastqFiles(workPackage, [
                    libraryPreparationKit: createLibraryPreparationKit(),
            ])
        }
        roddyBamFile = createBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
                workPackage        : workPackage,
                seqTracks          : seqTracks as Set,
        ])

        when:
        List<SingleSelectSelectorExtendedCriteria> criteriaList = job.fetchSelectors(workflowStep)

        then:
        criteriaList.size() == countOfSeqTracks
        criteriaList.each { SingleSelectSelectorExtendedCriteria criteria ->
            assert criteria.workflow == workflowStep.workflowRun.workflow
            assert criteria.workflowVersion == workflowStep.workflowRun.workflowVersion
            assert criteria.project == workPackage.project
            assert criteria.seqType == workPackage.seqType
            assert criteria.referenceGenome == workPackage.referenceGenome
        }

        List<LibraryPreparationKit> libraryPreparationKits = criteriaList*.libraryPreparationKit
        TestCase.assertContainSame(libraryPreparationKits, seqTracks*.libraryPreparationKit)

        where:
        name                                         | oneSeqTrackIsNull | countOfSeqTracks
        '2 seqTracks with two different libs'        | false             | 2
        '5 seqTracks with 5 different libs'          | false             | 5
        '2 seqTracks, one is null and the other not' | true              | 2
    }
}
