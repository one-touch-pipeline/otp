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
import grails.gorm.transactions.Rollback
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class SeqTrackServiceTests {

    SeqTrackService seqTrackService

    TestData testData

    File dataPath
    File mdPath
    SeqType alignableSeqType

    void setupData() {
        dataPath = TestCase.uniqueNonExistentPath
        mdPath = TestCase.uniqueNonExistentPath
        testData = new TestData()
        alignableSeqType = DomainFactory.createDefaultOtpAlignableSeqTypes().first()
    }

    @After
    void tearDown() {
        TestCase.cleanTestDirectory()
        testData = null
    }

    @Test
    void testDecideAndPrepareForAlignment_noAlignmentDecider_shouldReturnEmptyList() {
        setupData()
        SeqTrack seqTrack = setupSeqTrackProjectAndDataFile()

        Collection<MergingWorkPackage> workPackages = seqTrackService.decideAndPrepareForAlignment(seqTrack)

        assert workPackages.empty
    }

    @Test
    void testReturnExternallyProcessedBamFiles_InputIsNull_ShouldFail() {
        setupData()
        TestCase.shouldFail(IllegalArgumentException) {
            seqTrackService.returnExternallyProcessedBamFiles(null)
        }
    }

    @Test
    void testReturnExternallyProcessedBamFiles_InputIsEmpty_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            seqTrackService.returnExternallyProcessedBamFiles([])
        }
    }

    @Test
    void testReturnExternallyProcessedBamFiles_NoExternalBamFileAttached_AllFine() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        assert seqTrackService.returnExternallyProcessedBamFiles([seqTrack]).isEmpty()
    }

    @Test
    void testReturnExternallyProcessedBamFiles_ExternalBamFileAttached_AllFine() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage(
                seqType: seqTrack.seqType,
                sample:  seqTrack.sample,
        )
        ExternallyProcessedBamFile bamFile = DomainFactory.createExternallyProcessedBamFile(
                workPackage: externalMergingWorkPackage,
        ).save(flush: true)
        assert [bamFile] == seqTrackService.returnExternallyProcessedBamFiles([seqTrack])
    }

    private static SeqTrack setupSeqTrackProjectAndDataFile() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                seqType: DomainFactory.createDefaultOtpAlignableSeqTypes().first(),
        )

        SeqPlatform sp = seqTrack.seqPlatform
        sp.save(flush: true)

        DomainFactory.createMergingCriteriaLazy(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        )

        DomainFactory.createReferenceGenomeProjectSeqType(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        ).save(flush: true)

        DomainFactory.createSequenceDataFile(
                seqTrack: seqTrack,
                fileWithdrawn: false,
                fileExists: true,
                fileSize: 1L,
        ).save(flush: true)

        Project project = seqTrack.project
        project.save(flush: true)
        return seqTrack
    }
}
