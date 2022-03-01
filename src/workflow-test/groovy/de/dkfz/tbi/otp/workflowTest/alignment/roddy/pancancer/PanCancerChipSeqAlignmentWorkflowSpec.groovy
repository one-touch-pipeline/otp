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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.pancancer

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg38

class PanCancerChipSeqAlignmentWorkflowSpec extends AbstractPanCancerWorkflowSpec implements ReferenceGenomeHg38 {

    //@Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(PanCancerChipSeqAlignmentWorkflowSpec)

    @Override
    SeqType findSeqType() {
        return SeqTypeService.chipSeqPairedSeqType
    }

    @Override
    void setup() {
        log.debug("Start setup ${this.class.simpleName}")
        SessionUtils.withNewSession {
            antibodyTarget = createAntibodyTarget()
        }
        log.debug("Finish setup ${this.class.simpleName}")
    }

    void "test align lanes only, no base bam file exists, two lanes, same antibody, all fine"() {
        given:
        SeqTrack firstSeqTrack
        SeqTrack secondSeqTrack

        SessionUtils.withNewSession {
            firstSeqTrack = createSeqTrack("readGroup1")
            secondSeqTrack = createSeqTrack("readGroup2")
            decide(6, 1)
        }

        when:
        execute(1, 4)

        then:
        verify_alignLanesOnly_NoBaseBamExist_TwoLanes(firstSeqTrack, secondSeqTrack)
    }

    void "test align lanes only, no base bam file exists, two lanes, different antibody, all fine"() {
        given:
        SeqTrack firstSeqTrack
        SeqTrack secondSeqTrack

        SessionUtils.withNewSession {
            firstSeqTrack = createSeqTrack("readGroup1")
            secondSeqTrack = createSeqTrack("readGroup2", [
                    antibodyTarget: createAntibodyTarget()
            ])

            decide(6, 2)
        }

        when:
        execute(2, 4)

        then:
        SessionUtils.withNewSession {
            List<RoddyBamFile> bamFiles = RoddyBamFile.list([sort: 'id'])
            assert bamFiles.size() == 2

            checkLatestBamFileState(bamFiles[0], null, [mergingWorkPackage: bamFiles[0].workPackage, seqTracks: [firstSeqTrack], containedSeqTracks: [firstSeqTrack], identifier: 0L,])
            checkLatestBamFileState(bamFiles[1], null, [mergingWorkPackage: bamFiles[1].workPackage, seqTracks: [secondSeqTrack], containedSeqTracks: [secondSeqTrack], identifier: 0L,])

            bamFiles.each { RoddyBamFile bamFile ->
                assert !bamFile.mergingWorkPackage.needsProcessing

                assertBamFileFileSystemPropertiesSet(bamFile)

                assertBaseFileSystemState(bamFile)

                checkQC(bamFile)
            }
            return true
        }
    }
}
