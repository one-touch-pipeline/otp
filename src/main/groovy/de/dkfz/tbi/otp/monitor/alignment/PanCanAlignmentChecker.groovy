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
package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

class PanCanAlignmentChecker extends AbstractRoddyAlignmentChecker {

    static final String HEADER_EXOME_WITHOUT_LIBRARY_PREPERATION_KIT =
            'The following SeqTracks have no library preperation kit'

    static final String HEADER_EXOME_NO_BEDFILE =
            'The following SeqTracks have no bedfile kit'

    @Override
    String getWorkflowName() {
        return 'PanCanWorkflow'
    }

    @Override
    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.PANCAN_ALIGNMENT
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqTypeService.wholeGenomePairedSeqType, SeqTypeService.exomePairedSeqType, SeqTypeService.chipSeqPairedSeqType]
    }

    @Override
    List<SeqTrack> filter(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        String libraryPreperationKitMissing = 'libraryPreperationKitMissing'
        String bedFileMissing = 'bedFileMissing'
        String ok = 'ok'

        Map groupedSeqTracks = seqTracks.groupBy { SeqTrack seqTrack ->
            if (seqTrack.seqType.isExome()) {
                if (!seqTrack.libraryPreparationKit) {
                    return libraryPreperationKitMissing
                }
                ReferenceGenome referenceGenome = seqTrack.configuredReferenceGenome
                BedFile bedFile = CollectionUtils.atMostOneElement(
                        BedFile.findAllByReferenceGenomeAndLibraryPreparationKit(referenceGenome, seqTrack.libraryPreparationKit))
                if (!bedFile) {
                    return bedFileMissing
                }
            }
            return ok
        }
        output.showList(HEADER_EXOME_WITHOUT_LIBRARY_PREPERATION_KIT, groupedSeqTracks[libraryPreperationKitMissing])
        output.showList(HEADER_EXOME_NO_BEDFILE, groupedSeqTracks[bedFileMissing])

        return groupedSeqTracks[ok]
    }
}
