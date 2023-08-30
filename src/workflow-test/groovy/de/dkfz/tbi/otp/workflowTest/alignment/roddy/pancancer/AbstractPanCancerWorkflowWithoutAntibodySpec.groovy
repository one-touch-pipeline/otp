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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.pancancer

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg37

import java.nio.file.Path

/**
 * base class for all PanCancer workflow test without antibody target
 */
abstract class AbstractPanCancerWorkflowWithoutAntibodySpec extends AbstractPanCancerWorkflowSpec implements ReferenceGenomeHg37 {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(AbstractPanCancerWorkflowWithoutAntibodySpec)

    /**
     * Path of the test bam file
     */
    protected Path firstBamFilePath

    @Override
    void setup() {
        log.debug("Start setup ${this.class.simpleName}")
        SessionUtils.withTransaction {
            firstBamFilePath = referenceDataDirectory.resolve('bamFiles/wgs/first-bam-file/control_merged.mdup.bam')
        }
        log.debug("Finish setup ${this.class.simpleName}")
    }

    void "test align lanes only, one lane, with fingerPrinting, all fine"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            setUpFingerPrintingFile()
            decide(3, 1)
        }

        when:
        execute(1, 2)

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "test align lanes only, two lanes, all fine"() {
        given:
        SeqTrack firstSeqTrack
        SeqTrack secondSeqTrack

        SessionUtils.withTransaction {
            firstSeqTrack = createSeqTrack("readGroup1")
            secondSeqTrack = createSeqTrack("readGroup2")
            decide(6, 1)
        }

        when:
        execute(1, 4)

        then:
        verify_alignLanesOnly_TwoLanes(firstSeqTrack, secondSeqTrack)
    }
}
