import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
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
                        if (instance.processingState == AnalysisProcessingStates.IN_PROGRESS) {
                            instance.withdrawn = true
                            instance.save(flush: true, failOnError: true)
                            actions << "withdraw: ${instance.class.simpleName}:${instance.id}"
                        }
                    }

                    actions << "${processingStatusName}: ${samplePair."$processingStatusName"} -> ${SamplePair.ProcessingStatus.NEEDS_PROCESSING}"
                    samplePair."$processingStatusName" = SamplePair.ProcessingStatus.NEEDS_PROCESSING
                    samplePair.save(flush: true, failOnError: true)
                }
                output << sprintf("  * %-10s : %s", [analysis, actions.join('; ')])
            }
            println("\n${samplePair}\n${output.join('\n')}")
        }
        assert false : "Failed intentionally, remove this to assert to start reprocessing"
    }
})