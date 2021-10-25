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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

@Rollback
@Integration
class WgbsAlignmentStartJobIntegrationTests {

    @Autowired
    WgbsAlignmentStartJob wgbsAlignmentStartJob


    @Test
    void "test findUsableBaseBamFile when bamFileInProjectFolder is usable should return null"() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                seqType        : DomainFactory.createWholeGenomeBisulfiteSeqType(),
                needsProcessing: true,
                pipeline       : DomainFactory.createPanCanPipeline(),
        ])
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mergingWorkPackage,
        ])
        mergingWorkPackage.bamFileInProjectFolder = bamFile
        assert mergingWorkPackage.save(flush: true)

        assert null == wgbsAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void "test that getSeqType contains expected SeqTypes"() {
        List<SeqType> expected = [
                DomainFactory.createWholeGenomeBisulfiteSeqType(),
                DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType(),
        ]

        TestCase.assertContainSame(expected, wgbsAlignmentStartJob.seqTypes)
    }
}
