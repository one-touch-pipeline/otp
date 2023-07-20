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
package de.dkfz.tbi.otp.withdraw

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackWithComment

import java.nio.file.FileSystems

class WithdrawServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
        ]
    }

    //disable UnnecessaryGetter, since for mock the method the method call is required
    @SuppressWarnings('UnnecessaryGetter')
    void "withdraw, when called then call helper methods in correct order"() {
        SeqTrack seqTrack1 = Mock(SeqTrack)
        SeqTrack seqTrack2 = Mock(SeqTrack)
        RoddyBamFile roddyBamFile1 = Mock(RoddyBamFile)
        RoddyBamFile roddyBamFile2 = Mock(RoddyBamFile)
        SingleCellBamFile singleCellBamFile1 = Mock(SingleCellBamFile)
        SingleCellBamFile singleCellBamFile2 = Mock(SingleCellBamFile)
        BamFilePairAnalysis analysis1 = Mock(BamFilePairAnalysis)
        BamFilePairAnalysis analysis2 = Mock(BamFilePairAnalysis)

        List<SeqTrack> seqTracks = [
                seqTrack1,
                seqTrack2,
        ]
        List<RoddyBamFile> roddyBamFiles = [
                roddyBamFile1,
                roddyBamFile2,
        ]
        List<SingleCellBamFile> singleCellBamFiles = [
                singleCellBamFile1,
                singleCellBamFile2,
        ]
        List<AbstractBamFile> bamFiles = [
                roddyBamFile1,
                roddyBamFile2,
                singleCellBamFile1,
                singleCellBamFile2,
        ]
        List<BamFilePairAnalysis> bamFilePairAnalyses = [
                analysis1,
                analysis2,
        ]

        FileSystemService fileSystemService = Mock(FileSystemService)
        RoddyBamFileWithdrawService roddyBamFileWithdrawService = Mock(RoddyBamFileWithdrawService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = Mock(CellRangerBamFileWithdrawService)
        WithdrawAnalysisService withdrawAnalysisService = Mock(WithdrawAnalysisService)
        WithdrawHelperService withdrawHelperService = Mock(WithdrawHelperService)

        WithdrawService service = new WithdrawService([
                fileSystemService      : fileSystemService,
                withdrawBamFileServices: [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
                withdrawAnalysisService: withdrawAnalysisService,
                withdrawHelperService  : withdrawHelperService,
        ])

        WithdrawParameters withdrawParameters = new WithdrawParameters([
                seqTracksWithComments: [
                        new SeqTrackWithComment(seqTracks[0], "WithdrawnComment1\nover multiple lines"),
                        new SeqTrackWithComment(seqTracks[1], "WithdrawnComment2\nwith multiple lines"),
                ],
        ])

        when:
        service.withdraw(withdrawParameters)

        then:
        1 * fileSystemService.remoteFileSystemOnDefaultRealm >> FileSystems.default

        then:
        1 * roddyBamFileWithdrawService.collectObjects(seqTracks) >> roddyBamFiles
        1 * cellRangerBamFileWithdrawService.collectObjects(seqTracks) >> singleCellBamFiles

        then:
        1 * withdrawAnalysisService.collectObjects(bamFiles) >> bamFilePairAnalyses

        then:
        1 * withdrawHelperService.createOverviewSummary(_)

        then:
        1 * withdrawHelperService.checkNonExistingRawSequenceFiles(_)

        then:
        1 * withdrawHelperService.checkForAlreadyWithdrawnRawSequenceFiles(_)

        then:
        1 * withdrawHelperService.handleAnalysis(_)

        then:
        1 * withdrawHelperService.handleBamFiles(_, _)

        then:
        1 * withdrawHelperService.handleRawSequenceFiles(_)

        then:
        1 * withdrawHelperService.createAndWriteBashScript(_)
    }
}
