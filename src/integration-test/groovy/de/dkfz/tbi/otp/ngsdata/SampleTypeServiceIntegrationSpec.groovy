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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class SampleTypeServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    void "findUsedSampleTypesForProject, if project is empty, return nothing"() {
        given:
        SampleTypeService service = new SampleTypeService()

        Project project = createProject()
        DomainFactory.createAllAnalysableSeqTypes()

        when:
        List ret = service.findUsedSampleTypesForProject(project)

        then:
        ret.empty
    }

    void "findUsedSampleTypesForProject, if project has only seqTracks of not analysable SeqTypes, return an empty list"() {
        given:
        SampleTypeService service = new SampleTypeService()

        DomainFactory.createAllAnalysableSeqTypes()
        SeqTrack seqTrack = createSeqTrack()

        when:
        List ret = service.findUsedSampleTypesForProject(seqTrack.project)

        then:
        ret.empty
    }

    void "findUsedSampleTypesForProject, if project has a SeqTrack of an analysable SeqType, return the SampleType of this SeqTrack"() {
        given:
        SampleTypeService service = new SampleTypeService()

        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()
        SeqTrack seqTrack = createSeqTrack([
                seqType: seqTypes.first()
        ])

        when:
        List ret = service.findUsedSampleTypesForProject(seqTrack.project)

        then:
        ret == [seqTrack.sampleType]
    }

    void "findUsedSampleTypesForProject, if project has only AbstractMergingWorkPackage of not analysable SeqTypes, return an empty list"() {
        given:
        SampleTypeService service = new SampleTypeService()

        DomainFactory.createAllAnalysableSeqTypes()
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage()

        when:
        List ret = service.findUsedSampleTypesForProject(externalMergingWorkPackage.project)

        then:
        ret.empty
    }

    void "findUsedSampleTypesForProject, if project has a AbstractMergingWorkPackage of an analysable SeqType, return the SampleType of this AbstractMergingWorkPackage"() {
        given:
        SampleTypeService service = new SampleTypeService()

        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage([
                seqType: seqTypes.first()
        ])

        when:
        List ret = service.findUsedSampleTypesForProject(externalMergingWorkPackage.project)

        then:
        ret == [externalMergingWorkPackage.sampleType]
    }

    void "findUsedSampleTypesForProject, if sampleType is used in SeqTrack and AbstractMergingWorkPackage with an analysable SeqTypes, return the sampleType only once"() {
        given:
        SampleTypeService service = new SampleTypeService()

        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()
        SeqTrack seqTrack = createSeqTrack([
                seqType: seqTypes.first()
        ])
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage([
                seqType: seqTypes.first(),
                sample : seqTrack.sample,
        ])

        when:
        List ret = service.findUsedSampleTypesForProject(externalMergingWorkPackage.project)

        then:
        ret == [externalMergingWorkPackage.sampleType]
    }

    void "test getSeqTracksWithoutSampleCategory"() {
        given:
        SeqType seqType = DomainFactory.createAllAnalysableSeqTypes().first()
        SeqTrack st1 = createSeqTrack([
                seqType: seqType,
        ])
        SeqTrack st2 = createSeqTrack([
                seqType: seqType,
        ])
        SeqTrack st3 = createSeqTrack()
        DomainFactory.createSampleTypePerProject(project: st2.project, sampleType: st2.sampleType)
        SampleTypeService service = new SampleTypeService()

        expect:
        [st1] == service.getSeqTracksWithoutSampleCategory([st1, st2, st3])
    }
}
