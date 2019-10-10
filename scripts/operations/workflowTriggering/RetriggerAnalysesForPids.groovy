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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.utils.logging.*

// INPUT
def samplePairs = SamplePair.withCriteria {
    mergingWorkPackage1 {
        sample {
            individual {
                'in'('pid', [

                ])
            }
        }
    }
}

List<String> analyses = [
        //"snv",
        //"indel",
        //"sophia",
        //"aceseq",
        //"runYapsa",
]


// OVERVIEW
println "Affected analyses: (${analyses.size()})"
analyses.each { println " - ${it}" }
println "\nAffected SamplePairs: (${samplePairs.size()})"
samplePairs.each { println " - ${it}" }
println("\n")


// CONFIGURATION
// this maps the different names and objects each analysis needs to its name
Map<String, Map<String, Object>> analysesObjectMapping = [
        snv: [
                processingStatusName: "snvProcessingStatus",
                instanceClass:        RoddySnvCallingInstance,
        ],
        indel: [
                processingStatusName: "indelProcessingStatus",
                instanceClass:        IndelCallingInstance,
        ],
        sophia: [
                processingStatusName: "sophiaProcessingStatus",
                instanceClass:        SophiaInstance,
        ],
        aceseq: [
                processingStatusName: "aceseqProcessingStatus",
                instanceClass:        AceseqInstance,
        ],
        runYapsa: [
                processingStatusName: "runYapsaProcessingStatus",
                instanceClass:        RunYapsaInstance,
        ],
]


// PROCESSING
LogThreadLocal.withThreadLog(System.out, {
    SamplePair.withTransaction {
        samplePairs.each { SamplePair samplePair ->
            List<String> output = []
            analyses.each { String analysis ->
                List<String> actions = []
                Map<String, Object> analysisObjects = analysesObjectMapping[analysis]
                String processingStatusName = analysisObjects['processingStatusName']
                Class instanceClass = analysisObjects['instanceClass']

                if (samplePair."$processingStatusName" == SamplePair.ProcessingStatus.DISABLED) {
                    actions << "${processingStatusName} is ${SamplePair.ProcessingStatus.DISABLED} -> ignore"
                } else {
                    instanceClass.findAllBySamplePair(samplePair).each { BamFilePairAnalysis instance ->
                        // only withdraw instances IN_PROGRESS (still running or failed), keep FINISHED
                        if (instance.processingState == AnalysisProcessingStates.IN_PROGRESS && !instance.withdrawn) {
                            instance.withdrawn = true
                            instance.save(flush: true)
                            actions << "withdraw: ${instance.class.simpleName}:${instance.id}"
                        }
                    }

                    actions << "${processingStatusName}: ${samplePair."$processingStatusName"} -> ${SamplePair.ProcessingStatus.NEEDS_PROCESSING}"
                    samplePair."$processingStatusName" = SamplePair.ProcessingStatus.NEEDS_PROCESSING
                    samplePair.save(flush: true)
                }
                output << sprintf("  * %-10s : %s", [analysis, actions.join('; ')])
            }
            println("\n${samplePair}\n${output.join('\n')}")
        }
        assert false : "Failed intentionally, remove this to assert to start reprocessing"
    }
})
