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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack

abstract class AbstractRoddyAlignmentChecker extends AbstractAlignmentChecker {

    @Override
    List<AbstractMergedBamFile> getBamFileForMergingWorkPackage(List<MergingWorkPackage> mergingWorkPackages, boolean showFinished, boolean showWithdrawn) {
        if (!mergingWorkPackages) {
            return []
        }

        String filterFinished = showFinished ? '' :
                "and bamFile.fileOperationStatus != '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'"
        String filterWithdrawnFinished = showWithdrawn ? '' :
                "and bamFile.withdrawn = false"

        return AbstractMergedBamFile.executeQuery("""
                    select
                        bamFile
                    from
                        AbstractMergedBamFile bamFile
                    where
                        bamFile.workPackage in (:mergingWorkPackage)
                        ${filterFinished}
                        ${filterWithdrawnFinished}
                        and bamFile.workPackage.seqType in (:seqTypes)
                        and bamFile.id = (
                            select
                                max(bamFile1.id)
                            from
                                AbstractMergedBamFile bamFile1
                            where
                                bamFile1.workPackage = bamFile.workPackage
                        )
                """.toString(), [
                mergingWorkPackage: mergingWorkPackages,
                seqTypes          : seqTypes,
        ])
    }

    @Override
    List<SeqTrack> filterWithoutReferenceGenome(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        Map<Boolean, List<SeqTrack>> mapSpecificReferenceGenomeType = seqTracks.groupBy {
            return it.sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.UNKNOWN
        }
        if (mapSpecificReferenceGenomeType[true]) {
            output << "${MonitorOutputCollector.INDENT}${mapSpecificReferenceGenomeType[true].size()} lanes removed, " +
                    "because the used sampleType has not defined the type of reference genome (Project or sample type specific): " +
                    "${mapSpecificReferenceGenomeType[true]*.sampleType*.name.unique().sort()}}"
        }
        Map<Boolean, List<SeqTrack>> mapNoReferenceGenome = (mapSpecificReferenceGenomeType[false] ?: []).groupBy {
            return !it.configuredReferenceGenome
        }
        if (mapNoReferenceGenome[true]) {
            output << "${MonitorOutputCollector.INDENT}${mapNoReferenceGenome[true].size()} lanes removed, " +
                    "because the used project(s) has/have no reference genome(s): " +
                    "${mapNoReferenceGenome[true]*.project.unique().sort { it.name }}"
        }
        return mapNoReferenceGenome[false] ?: []
    }
}
