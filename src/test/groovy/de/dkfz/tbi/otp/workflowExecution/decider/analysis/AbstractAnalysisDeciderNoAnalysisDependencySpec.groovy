/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.decider.analysis

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.workflowExecution.AnalysisArtefactService

@Transactional
@Slf4j
abstract class AbstractAnalysisDeciderNoAnalysisDependencySpec<T extends BamFilePairAnalysis> extends AbstractAnalysisDeciderSpec<T> {

    void "fetchAdditionalArtefacts"() {
        given:
        RoddyBamFile bamFile1 = createBamFile()
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1], [], [:])

        RoddyBamFile bamFile2 = createBamFile()
        AnalysisBamFileArtefactData artefactData2 = createAnalysisBamFileArtefactData(bamFile2)
        AnalysisAnalysisArtefactData<T> analysisArtefactData = createAnalysisAnalysisArtefactData(createAnalysisInstance())

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([bamFile1]) >> [artefactData2]
            1 * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], decider.instanceClasses) >> [analysisArtefactData]
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == [artefactData2]
        dataList2.alreadyRunAnalysisDataList == [analysisArtefactData]
        dataList2.dependingAnalysisDataList == [:]
    }

    void "fetchAdditionalArtefacts, if input is empty, then return object with empty list"() {
        given:
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([], [], [:])

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([]) >> []
            1 * fetchRelatedAnalysisArtefactsForBamFiles([], decider.instanceClasses) >> []
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == []
        dataList2.alreadyRunAnalysisDataList == []
        dataList2.dependingAnalysisDataList == [:]
    }
}
