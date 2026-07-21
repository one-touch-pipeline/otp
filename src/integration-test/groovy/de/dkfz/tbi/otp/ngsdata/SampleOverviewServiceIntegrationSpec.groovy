/*
 * Copyright 2011-2026 The OTP authors
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
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class SampleOverviewServiceIntegrationSpec extends Specification implements UserAndRoles, IsRoddy {

    @Autowired
    SampleOverviewService sampleLaneService

    void "test abstractBamFilesInProjectFolder without bamFiles in Project"() {
        given:
        Project project = DomainFactory.createProject()

        expect:
        sampleLaneService.abstractBamFilesInProjectFolder(project) == []
    }

    void "test abstractBamFilesInProjectFolder returns empty list for null project"() {
        expect:
        sampleLaneService.abstractBamFilesInProjectFolder(null) == []
    }

    @SuppressWarnings("SpaceAfterOpeningBrace")
    void "test abstractBamFilesInProjectFolder projects the row of an abstractBamFile in Project"() {
        given:
        AbstractBamFile bamFile = abstractBamFile()
        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        when:
        List<SampleOverviewBamFileRow> result = sampleLaneService.abstractBamFilesInProjectFolder(bamFile.project)
        SampleOverviewBamFileRow row = CollectionUtils.exactlyOneElement(result)

        then:
        row.pid == bamFile.individual.pid
        row.sampleTypeName == bamFile.sampleType.name
        row.seqTypeId == bamFile.seqType.id
        row.pipelineId == bamFile.pipeline.id
        row.numberOfMergedLanes == bamFile.numberOfMergedLanes
        row.coverage == bamFile.coverage
        row.withdrawn == bamFile.withdrawn

        where:
        abstractBamFile                                                | _
        ({ createBamFile() })                                          | _
        ({ DomainFactory.createFinishedExternallyProcessedBamFile() }) | _
    }

    @SuppressWarnings("SpaceAfterOpeningBrace")
    void "test abstractBamFilesInProjectFolder with abstractBamFile in different Project"() {
        given:
        Project project = DomainFactory.createProject()
        abstractBamFile()

        when:
        List result = sampleLaneService.abstractBamFilesInProjectFolder(project)

        then:
        result.isEmpty()

        where:
        abstractBamFile                                                | _
        ({ createBamFile() })                                          | _
        ({ DomainFactory.createFinishedExternallyProcessedBamFile() }) | _
    }

    @SuppressWarnings("SpaceAfterOpeningBrace")
    void "test abstractBamFilesInProjectFolder with abstractBamFiles in Project but without MergingWorkPackage"() {
        given:
        AbstractBamFile bamFile = abstractBamFile()
        AbstractMergingWorkPackage workPackage = bamFile.workPackage
        workPackage.bamFileInProjectFolder = null
        assert workPackage.save(flush: true)

        when:
        List result = sampleLaneService.abstractBamFilesInProjectFolder(bamFile.project)

        then:
        result.isEmpty()

        where:
        abstractBamFile                                                | _
        ({ createBamFile() })                                          | _
        ({ DomainFactory.createFinishedExternallyProcessedBamFile() }) | _
    }

    @SuppressWarnings("SpaceAfterOpeningBrace")
    void "test abstractBamFilesInProjectFolder with two abstractBamFiles in Project but one not Finished yet"() {
        given:
        AbstractBamFile bamFile = finishedAbstractBamFile()
        unfinishedAbstractBamFile(bamFile.mergingWorkPackage)
        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        when:
        List<SampleOverviewBamFileRow> result = sampleLaneService.abstractBamFilesInProjectFolder(bamFile.project)
        SampleOverviewBamFileRow row = CollectionUtils.exactlyOneElement(result)

        then:
        row.pid == bamFile.individual.pid
        row.seqTypeId == bamFile.seqType.id
        row.pipelineId == bamFile.pipeline.id

        where:
        finishedAbstractBamFile                                        | unfinishedAbstractBamFile
        ({ createBamFile() })                                          | ({ createBamFile(workPackage: it) })
        ({ DomainFactory.createFinishedExternallyProcessedBamFile() }) | ({ DomainFactory.createExternallyProcessedBamFile(workPackage: it) })
    }

    void "test laneCountForSeqtypesPerPatientAndSampleType sums lanes per pid, sampleType and seqType and projects seqTypeId"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType()
        DomainFactory.createAggregateSequences([projectId: project.id, pid: "patient1", sampleTypeName: "sample-type-1", seqTypeId: seqType.id, laneCount: 3])
        DomainFactory.createAggregateSequences([projectId: project.id, pid: "patient1", sampleTypeName: "sample-type-1", seqTypeId: seqType.id, laneCount: 5])
        // a different project must not leak into the result
        DomainFactory.createAggregateSequences([projectId: DomainFactory.createProject().id, pid: "patient1", sampleTypeName: "sample-type-1", seqTypeId: seqType.id, laneCount: 99])

        when:
        List<SampleOverviewRegisteredLaneCountRow> result = sampleLaneService.laneCountForSeqtypesPerPatientAndSampleType(project)

        then:
        CollectionUtils.exactlyOneElement(result) == new SampleOverviewRegisteredLaneCountRow("patient1", "sample-type-1", seqType.id, 8L)
    }

    void "test laneCountForSeqtypesPerPatientAndSampleType without sequences in Project"() {
        expect:
        sampleLaneService.laneCountForSeqtypesPerPatientAndSampleType(DomainFactory.createProject()) == []
    }

    void "test withdrawnLaneCountForSeqTypesPerPatientAndSampleType counts only withdrawn lanes and projects seqTypeId"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType()
        // Sequence has a composite id (seqTrackId, seqTypeId, ...); a distinct seqTrackId keeps each row unique
        DomainFactory.createSequence([seqTrackId: 1, projectId: project.id, pid: "patient1", sampleTypeName: "sample-type-1", seqTypeId: seqType.id, fileWithdrawn: true])
        DomainFactory.createSequence([seqTrackId: 2, projectId: project.id, pid: "patient1", sampleTypeName: "sample-type-1", seqTypeId: seqType.id, fileWithdrawn: true])
        // not withdrawn -> excluded
        DomainFactory.createSequence([seqTrackId: 3, projectId: project.id, pid: "patient1", sampleTypeName: "sample-type-1", seqTypeId: seqType.id, fileWithdrawn: false])
        // different project -> excluded
        DomainFactory.createSequence([seqTrackId: 4, projectId: DomainFactory.createProject().id, pid: "patient1", sampleTypeName: "sample-type-1", seqTypeId: seqType.id, fileWithdrawn: true])

        when:
        List<SampleOverviewWithdrawnLaneCountRow> result = sampleLaneService.withdrawnLaneCountForSeqTypesPerPatientAndSampleType(project)

        then:
        CollectionUtils.exactlyOneElement(result) == new SampleOverviewWithdrawnLaneCountRow("patient1", "sample-type-1", seqType.id, 2L)
    }

    void "test samplesOfProject projects pid and sampleTypeName for each sample of the project"() {
        given:
        Project project = DomainFactory.createProject()
        Individual individual = DomainFactory.createIndividual([project: project])
        SampleType sampleType = DomainFactory.createSampleType()
        DomainFactory.createSample([individual: individual, sampleType: sampleType])
        // sample of a different project must not be returned
        DomainFactory.createSample()

        when:
        List<SampleOverviewSampleRow> result = sampleLaneService.samplesOfProject(project)

        then:
        CollectionUtils.exactlyOneElement(result) == new SampleOverviewSampleRow(individual.pid, sampleType.name)
    }

    void "test samplesOfProject returns empty list for project without samples"() {
        expect:
        sampleLaneService.samplesOfProject(DomainFactory.createProject()) == []
    }

    void "test samplesOfProject returns empty list for null project"() {
        expect:
        sampleLaneService.samplesOfProject(null) == []
    }

    void "test sampleTypeByProject without SampleTypes in Project"() {
        given:
        Project project = DomainFactory.createProject()

        expect:
        sampleLaneService.sampleTypeByProject(project) == []
    }

    void "test sampleTypeByProject with one SampleType in Project"() {
        given:
        Project project = DomainFactory.createProject()
        SampleType sampleType = DomainFactory.createSampleType()
        createAggregateSequences(project, sampleType)

        when:
        List<String> results = sampleLaneService.sampleTypeByProject(project)

        then:
        sampleType.name == CollectionUtils.exactlyOneElement(results)
    }

    void "test sampleTypeByProject with multiple SampleType-Project combinations"() {
        given:
        Project project = DomainFactory.createProject()
        SampleType sampleType1 = DomainFactory.createSampleType()
        SampleType sampleType2 = DomainFactory.createSampleType()
        SampleType sampleType3 = DomainFactory.createSampleType()
        SampleType sampleType4 = DomainFactory.createSampleType()
        createAggregateSequences(project, sampleType1)
        createAggregateSequences(project, sampleType2)
        createAggregateSequences(project, sampleType3)
        createAggregateSequences(DomainFactory.createProject(), sampleType4)

        when:
        List<String> results = sampleLaneService.sampleTypeByProject(project)

        then:
        TestCase.assertContainSame(results, [sampleType1.name, sampleType2.name, sampleType3.name])
    }

    private void createAggregateSequences(Project project, SampleType sampleType) {
        DomainFactory.createAggregateSequences([projectId: project.id, sampleTypeName: sampleType.name, sampleTypeId: sampleType.id])
    }
}
