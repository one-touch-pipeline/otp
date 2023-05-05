/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class SearchSeqTrackServiceSpec extends HibernateSpec implements ServiceUnitTest<SearchSeqTrackService>, WorkflowSystemDomainFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                AbstractBamFile,
                RoddyBamFile,
                DataFile,
                Project,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                ExternallyProcessedBamFile,
                ExternalMergingWorkPackage,
        ]
    }

    void "run getAllSeqTracksByProjectAndSeqTypes, should return the expected seqTracks"() {
        given:
        final SeqType st1 = createSeqTypePaired()
        final SeqType st2 = createSeqTypePaired()
        final SeqType st3 = createSeqTypePaired()

        final Project project = createProject()
        final Individual individual = createIndividual(project: project)
        final SeqTrack seqTrack1 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st1,
        ])
        final SeqTrack seqTrack2 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st2,
        ])

        createSeqTrackWithTwoDataFile([
                sample: createSample(),
                seqType: st2,
        ])

        final SeqTrack seqTrack4 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st3,
        ])

        Set<SeqTrack> result = [seqTrack1, seqTrack2, seqTrack4] as Set

        when:
        Set<SeqTrack> seqTracks = service.getAllSeqTracksByProjectAndSeqTypes(
                project,
                [st1, st2, st3] as Set
        )

        then:
        TestCase.assertContainSame(seqTracks, result)
    }

    void "run getAllSeqTracksByIndividualsAndSeqTypes, should return the expected seqTracks"() {
        given:
        final SeqType st1 = createSeqTypePaired()
        final SeqType st2 = createSeqTypePaired()

        Project project = createProject()
        Individual individual1 = createIndividual([
                project: project,
                pid: 'ind_1',
        ])
        Individual individual2 = createIndividual([
                project: project,
                pid: 'ind_2',
        ])
        Individual individual3 = createIndividual([
                project: project,
                pid: 'ind_3',
        ])
        SeqTrack seqTrack1 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual1),
                seqType: st1,
        ])
        SeqTrack seqTrack2 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual2),
                seqType: st2,
        ])

        // this seqTrack below shouldn't be in the found list
        createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual3),
                seqType: st2,
        ])

        when:
        Set<SeqTrack> seqTracks = service.getAllSeqTracksByIndividualsAndSeqTypes(
                [individual1, individual2] as Set,
                [st1, st2] as Set,
        )

        then:
        seqTracks.size() == 2
        TestCase.assertContainSame(seqTracks, [seqTrack1, seqTrack2])
    }

    void "run getAllSeqTracksByLaneIds, should return the expected seqTracks"() {
        given:
        final SeqType st1 = createSeqTypePaired(name: SeqTypeNames.EXOME.seqTypeName)
        final SeqType st2 = createSeqTypePaired(name: SeqTypeNames.WHOLE_GENOME.seqTypeName)

        Project project = createProject()
        Individual individual = createIndividual([
                project: project,
        ])
        SeqTrack seqTrack1 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st1,
        ])
        SeqTrack seqTrack2 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st2,
        ])

        // this seqTrack below shouldn't be in the found list
        createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st2,
        ])

        when:
        Set<SeqTrack> seqTracks = service.getAllSeqTracksByLaneIds([seqTrack1.id, seqTrack2.id] as Set)

        then:
        seqTracks.size() == 2
        TestCase.assertContainSame(seqTracks, [seqTrack1, seqTrack2])
    }

    void "run getAllSeqTracksByIlseSubmissions, should return the expected seqTracks"() {
        given:
        final SeqType st1 = createSeqTypePaired(name: SeqTypeNames.EXOME.seqTypeName)
        final SeqType st2 = createSeqTypePaired(name: SeqTypeNames.WHOLE_GENOME.seqTypeName)

        final IlseSubmission ilseSubmission1 = createIlseSubmission(ilseNumber: 1234)
        final IlseSubmission ilseSubmission2 = createIlseSubmission(ilseNumber: 2345)
        final IlseSubmission ilseSubmission3 = createIlseSubmission(ilseNumber: 3456)

        Project project = createProject()
        Individual individual = createIndividual([
                project: project,
        ])
        SeqTrack seqTrack1 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st1,
                ilseSubmission: ilseSubmission1,
        ])
        SeqTrack seqTrack2 = createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st2,
                ilseSubmission: ilseSubmission2,
        ])

        // this seqTrack below shouldn't be in the found list
        createSeqTrackWithTwoDataFile([
                sample: createSample(individual: individual),
                seqType: st2,
                ilseSubmission: ilseSubmission3,
        ])

        when:
        Set<SeqTrack> seqTracks = service.getAllSeqTracksByIlseSubmissions([ilseSubmission1, ilseSubmission2] as Set)

        then:
        seqTracks.size() == 2
        TestCase.assertContainSame(seqTracks, [seqTrack1, seqTrack2])
    }
}
