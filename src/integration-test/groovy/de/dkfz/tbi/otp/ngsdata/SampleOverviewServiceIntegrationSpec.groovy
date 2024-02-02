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

    @SuppressWarnings("SpaceAfterOpeningBrace")
    void "test abstractBamFilesInProjectFolder with abstractBamFile in Project"() {
        given:
        AbstractBamFile bamFile = abstractBamFile()
        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        when:
        List result = sampleLaneService.abstractBamFilesInProjectFolder(bamFile.project)

        then:
        bamFile == CollectionUtils.exactlyOneElement(result)

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
        List result = sampleLaneService.abstractBamFilesInProjectFolder(bamFile.project)

        then:
        bamFile == CollectionUtils.exactlyOneElement(result)

        where:
        finishedAbstractBamFile                                        | unfinishedAbstractBamFile
        ({ createBamFile() })                                          | ({ createBamFile(workPackage: it) })
        ({ DomainFactory.createFinishedExternallyProcessedBamFile() }) | ({ DomainFactory.createExternallyProcessedBamFile(workPackage: it) })
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
