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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.job.processing.FileSystemService

@CompileDynamic
@PreAuthorize("hasRole('ROLE_OPERATOR')")
@Transactional
class WithdrawService {

    FileSystemService fileSystemService

    @Autowired
    List<AbstractWithdrawBamFileService<?>> withdrawBamFileServices
    WithdrawAnalysisService withdrawAnalysisService
    WithdrawHelperService withdrawHelperService

    String withdraw(WithdrawParameters withdrawParameters) {
        WithdrawStateHolder withdrawStateHolder = new WithdrawStateHolder([
                withdrawParameters: withdrawParameters,
                remoteFileSystem  : fileSystemService.remoteFileSystem,
        ])
        Map<AbstractWithdrawBamFileService, List<AbstractBamFile>> bamFileMap = withdrawBamFileServices.collectEntries {
            [(it), it.collectObjects(withdrawStateHolder.seqTracks)]
        }
        withdrawStateHolder.bamFiles = bamFileMap.values().flatten()

        withdrawStateHolder.analysis = withdrawAnalysisService.collectObjects(withdrawStateHolder.bamFiles)

        withdrawHelperService.with {
            checkArchivedProject(withdrawStateHolder)
            createOverviewSummary(withdrawStateHolder)
            checkNonExistingRawSequenceFiles(withdrawStateHolder)
            checkForAlreadyWithdrawnRawSequenceFiles(withdrawStateHolder)

            handleAnalysis(withdrawStateHolder)

            handleBamFiles(withdrawStateHolder, bamFileMap)

            handleRawSequenceFiles(withdrawStateHolder)

            createAndWriteBashScript(withdrawStateHolder)
        }
        return withdrawStateHolder.summary.join('\n')
    }
}
