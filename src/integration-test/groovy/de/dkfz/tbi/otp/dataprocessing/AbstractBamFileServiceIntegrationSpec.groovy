/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

@Rollback
@Integration
class AbstractBamFileServiceIntegrationSpec extends Specification implements RoddyPancanFactory {

    AbstractBamFileService abstractBamFileService

    void "test calculateCoverageWithN, when needsBedFile is true, return null"() {
        given:
        SeqType seqType = DomainFactory.createExomeSeqType()
        SeqTrack seqTrack = createSeqTrack(seqType: seqType)

        AbstractBamFile abstractBamFile = createBamFile(seqTracks: [seqTrack] as Set)
        abstractBamFileService = new AbstractBamFileService()

        expect:
        abstractBamFileService.calculateCoverageWithN(abstractBamFile) == null
    }

    void "test calculateCoverageWithN, when needsBedFile is false, return result"() {
        given:
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqTrack seqTrack = createSeqTrack(seqType: seqType)

        AbstractBamFile abstractBamFile = createBamFile(seqTracks: [seqTrack] as Set)
        abstractBamFile.workPackage.referenceGenome.length = 10
        abstractBamFile.workPackage.referenceGenome.save(flush: true)
        DomainFactory.createRoddyMergedBamQa(abstractBamFile, [chromosome: RoddyQualityAssessment.ALL, qcBasesMapped: 2])
        abstractBamFileService = new AbstractBamFileService()

        expect:
        abstractBamFileService.calculateCoverageWithN(abstractBamFile) == 0.2
    }
}
