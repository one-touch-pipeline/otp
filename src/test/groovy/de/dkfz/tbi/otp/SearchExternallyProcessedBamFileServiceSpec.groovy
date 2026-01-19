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
package de.dkfz.tbi.otp

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryBam
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class SearchExternallyProcessedBamFileServiceSpec extends HibernateSpec implements ServiceUnitTest<SearchExternallyProcessedBamFileService>, ExternalBamFactoryBam {

    @Override
    List<Class> getDomainClasses() {
        return [
                AbstractBamFile,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                FastqFile,
                Individual,
                MergingWorkPackage,
                Project,
                RawSequenceFile,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                Sample,
                SampleType,
                SeqTrack,
                SeqType,
        ]
    }

    void "run getAllExternallyProcessedBamFilesByProjectAndSeqTypes, should return the expected bamFiles"() {
        given:
        final SeqType st1 = createSeqTypePaired()
        final SeqType st2 = createSeqTypePaired()
        final SeqType st3 = createSeqTypePaired()

        final Project project = createProject()
        final Individual individual = createIndividual(project: project)

        final ExternallyProcessedBamFile externallyProcessedBamFile1 = createBamFile(
                workPackage: createMergingWorkPackage([
                        sample : createSample(individual: individual),
                        seqType: st1,
                ]))
        final ExternallyProcessedBamFile externallyProcessedBamFile2 = createBamFile(
                workPackage: createMergingWorkPackage([
                        sample : createSample(individual: individual),
                        seqType: st2,
                ]))

        createBamFile(
                workPackage: createMergingWorkPackage([
                        sample : createSample(),
                        seqType: st2,
                ]))

        final ExternallyProcessedBamFile externallyProcessedBamFile4 = createBamFile(
                workPackage: createMergingWorkPackage([
                        sample : createSample(individual: individual),
                        seqType: st3,
                ]))

        Set<ExternallyProcessedBamFile> result = [externallyProcessedBamFile1, externallyProcessedBamFile2, externallyProcessedBamFile4] as Set

        when:
        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles = service.getAllExternallyProcessedBamFilesByProjectAndSeqTypes(
                project,
                [st1, st2, st3] as Set
        )

        then:
        TestCase.assertContainSame(externallyProcessedBamFiles, result)
    }

    void "run getAllExternallyProcessedBamFilesByIndividualsAndSeqTypes, should return the expected bamFiles"() {
        given:
        final SeqType st1 = createSeqTypePaired()
        final SeqType st2 = createSeqTypePaired()

        Project project = createProject()
        Individual individual1 = createIndividual([
                project: project,
                pid    : 'ind_1',
        ])
        Individual individual2 = createIndividual([
                project: project,
                pid    : 'ind_2',
        ])
        Individual individual3 = createIndividual([
                project: project,
                pid    : 'ind_3',
        ])
        final ExternallyProcessedBamFile externallyProcessedBamFile1 = createBamFile(
                workPackage: createMergingWorkPackage([
                        sample : createSample(individual: individual1),
                        seqType: st1,
                ]))
        final ExternallyProcessedBamFile externallyProcessedBamFile2 = createBamFile(
                workPackage: createMergingWorkPackage([
                        sample : createSample(individual: individual2),
                        seqType: st2,
                ]))

        // this bamFile below shouldn't be in the found list
        createBamFile(
                workPackage: createMergingWorkPackage([
                        sample : createSample(individual: individual3),
                        seqType: st2,
                ]))

        when:
        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles = service.getAllExternallyProcessedBamFilesByIndividualsAndSeqTypes(
                [individual1, individual2] as Set,
                [st1, st2] as Set
        )

        then:
        TestCase.assertContainSame(externallyProcessedBamFiles, [externallyProcessedBamFile1, externallyProcessedBamFile2])
    }

    void "run getExternallyProcessedBamFilesByMultiInput, should return the expected bamFiles"() {
        given:
        service.seqTypeService = new SeqTypeService()
        SequencingReadType readType = SequencingReadType.PAIRED
        String readTypeName = readType.name()

        Boolean singleCell = true

        Individual individual = createIndividual()
        String pid = individual.pid

        SampleType sampleType = createSampleType()
        String sampleTypeName = sampleType.name
        Sample sample = createSample([individual: individual, sampleType: sampleType])
        String name1 = "name1"
        String name2 = "name2"
        String name3 = "name3"

        SeqType seqType1 = DomainFactory.createSeqType([name: name1, libraryLayout: readType, singleCell: singleCell])
        SeqType seqType2 = DomainFactory.createSeqType([displayName: name2, libraryLayout: readType, singleCell: singleCell])
        SeqType seqType3 = DomainFactory.createSeqType([importAlias: [name3, 'alias2'], libraryLayout: readType, singleCell: singleCell])

        SeqType seqType4 = DomainFactory.createSeqType([libraryLayout: SequencingReadType.SINGLE, singleCell: singleCell])

        ExternallyProcessedBamFile externallyProcessedBamFile1 = createBamFile(
                workPackage: createMergingWorkPackage([sample: sample, seqType: seqType1]))
        ExternallyProcessedBamFile externallyProcessedBamFile2 = createBamFile(
                workPackage: createMergingWorkPackage([sample: sample, seqType: seqType2]))
        ExternallyProcessedBamFile externallyProcessedBamFile3 = createBamFile(
                workPackage: createMergingWorkPackage([sample: sample, seqType: seqType3]))
        createBamFile(workPackage: createMergingWorkPackage([sample: sample, seqType: seqType4]))

        when:
        Set<ExternallyProcessedBamFile> result1 = service.getExternallyProcessedBamFilesByMultiInput(pid, sampleTypeName, name1, readTypeName, singleCell)

        then:
        TestCase.assertContainSame(result1, [externallyProcessedBamFile1])

        when:
        Set<ExternallyProcessedBamFile> result2 = service.getExternallyProcessedBamFilesByMultiInput(pid, sampleTypeName, name2, readTypeName, singleCell)

        then:
        TestCase.assertContainSame(result2, [externallyProcessedBamFile2])

        when:
        Set<ExternallyProcessedBamFile> result3 = service.getExternallyProcessedBamFilesByMultiInput(pid, sampleTypeName, name3, readTypeName, singleCell)

        then:
        TestCase.assertContainSame(result3, [externallyProcessedBamFile3])
    }
}
