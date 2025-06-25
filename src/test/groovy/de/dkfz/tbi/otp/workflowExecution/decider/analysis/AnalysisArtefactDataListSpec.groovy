/*
 * Copyright 2011-2025 The OTP authors
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

import spock.lang.Specification
import spock.lang.Unroll

class AnalysisArtefactDataListSpec extends Specification {

    @Unroll
    void "isEmpty, when #caseName, then should return #expectedValue"() {
        given:
        AnalysisArtefactDataList analysisArtefactDataList = new AnalysisArtefactDataList(bamFileDataList, alreadyRunAnalysisDataList, dependingAnalysisDataList)

        expect:
        analysisArtefactDataList.isEmpty() == expectedValue

        where:
        caseName                                                       | bamFileDataList                     | alreadyRunAnalysisDataList           | dependingAnalysisDataList                        || expectedValue
        "list and map are empty"                                       | []                                  | []                                   | [:]                                              || true
        "list are empty and mapContains only empty list"               | []                                  | []                                   | [a: [], b: []]                                   || true
        "bamFileDataList is not empty"                                 | [Mock(AnalysisBamFileArtefactData)] | []                                   | [a: [], b: []]                                   || false
        "alreadyRunAnalysisDataList is not empty"                      | []                                  | [Mock(AnalysisAnalysisArtefactData)] | [a: [], b: []]                                   || false
        "dependingAnalysisDataList contains not empty list"            | []                                  | []                                   | [a: [Mock(AnalysisAnalysisArtefactData)], b: []] || false
        "bamFileDataList and alreadyRunAnalysisDataList are not empty" | [Mock(AnalysisBamFileArtefactData)] | [Mock(AnalysisAnalysisArtefactData)] | [a: [], b: []]                                   || false
    }
}
