/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.withdraw

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack

/**
 * Internal service for simplify {@link WithdrawHelperService}.
 *
 * The service should used outside.
 */
@PreAuthorize("hasRole('ROLE_OPERATOR')")
@Transactional
class WithdrawDisplayDomainService {

    String analysisInfo(BamFilePairAnalysis analysis) {
        return [
                analysis.id,
                analysis.instanceName,
                analysis.project,
                analysis.individual.pid,
                analysis.sampleType1BamFile.sampleType.name,
                analysis.sampleType2BamFile.sampleType.name,
                analysis.seqType.displayNameWithLibraryLayout,
                analysis.withdrawn ? "withdrawn" : '',
        ].join('\t')
    }

    String bamFileInfo(AbstractMergedBamFile bamFile) {
        return [
                bamFile.id,
                bamFile.project,
                bamFile.individual.pid,
                bamFile.sampleType.name,
                bamFile.seqType.displayNameWithLibraryLayout,
                bamFile.workPackage.antibodyTarget ?: '',
                bamFile.withdrawn ? "withdrawn" : '',
        ].join('\t')
    }

    String seqTrackInfo(SeqTrack seqTrack) {
        return [
                seqTrack.project,
                seqTrack.individual.pid,
                seqTrack.sampleType.name,
                seqTrack.seqType.displayNameWithLibraryLayout,
                seqTrack.antibodyTarget ?: '',
                seqTrack.run,
                seqTrack.laneId,
        ].join('\t')
    }

    String dataFileInfo(DataFile dataFile, boolean includeWithdrawnColumns = false) {
        List<String> info = [
                seqTrackInfo(dataFile.seqTrack),
                dataFile.mateNumber.toString(),
                dataFile.fileName,
        ]
        if (includeWithdrawnColumns) {
            info << dataFile.fileWithdrawn
            info << dataFile.withdrawnDate
            info << dataFile.withdrawnComment?.replace('[\n\r]+', ' ')
        }
        return info.join('\t')
    }
}
