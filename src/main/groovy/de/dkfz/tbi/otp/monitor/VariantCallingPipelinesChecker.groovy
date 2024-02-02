/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair

class VariantCallingPipelinesChecker extends PipelinesChecker<AbstractBamFile> {

    @Override
    List handle(List<AbstractBamFile> bamFiles, MonitorOutputCollector output) {
        if (!bamFiles) {
            return []
        }

        // 'tier 1' workflows, directly on bams
        List<SamplePair> samplePairs = new SamplePairChecker().handle(bamFiles, output)
        List<BamFilePairAnalysis> snvFinished = new SnvCallingPipelineChecker().handle(samplePairs, output)
        List<BamFilePairAnalysis> indelFinished = new IndelCallingPipelineChecker().handle(samplePairs, output)
        List<BamFilePairAnalysis> sophiaFinished = new SophiaCallingPipelineChecker().handle(samplePairs, output)

        // 'tier 2' workflows, requiring tier-1 results.
        List<BamFilePairAnalysis> aceseqFinished = new AceseqCallingPipelineChecker().handle(sophiaFinished*.samplePair, output)
        List<BamFilePairAnalysis> runYapsaFinished = new RunYapsaPipelineChecker().handle(snvFinished*.samplePair, output)

        // samplepairs that are COMPLETELY done.
        // CAVEAT: if one of these workflows is not configured to run at all (e.g. disabled for project)
        // this will never return anything.
        List<SamplePair> finished = samplePairs.intersect(snvFinished*.samplePair)
                .intersect(indelFinished*.samplePair)
                .intersect(sophiaFinished*.samplePair)
                .intersect(aceseqFinished*.samplePair)
                .intersect(runYapsaFinished*.samplePair)
        return finished
    }
}
