/*
 * Copyright 2011-2020 The OTP authors
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
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.project.Project

/**
 * Search for analyses run with a specific version and list these, other analyses run for the sample pair and a list of pids.
 * The pid list can be used for trigger the analyses with the script RetriggerAnalysesForPids.
 *
 * The input is:
 * - a project
 * - a plugin with version
 *
 * The scripts produce three list:
 * 1. All found instances
 * 2. All other run analyses of the same plugin, but in other version
 * 3. All effected pids. That can be used as input for RetriggerAnalysisForPids
 */

//--------------------------
//input

String projectName = ''

/**
 * the plugin and version, for example
 * - SophiaWorkflow:2.2.2
 * - IndelCallingWorkflow:2.4.1
 * - SNVCallingWorkflow:1.2.166-3
 * - ACEseqWorkflow:1.2.8-4
 */
String plugInAndVersion = ''

//-------------------------
//work

assert projectName: "No project name given"
assert plugInAndVersion: "No plugin version given"

assert Project.findAllByName(projectName): "No project with name '${projectName}' exist"
assert ConfigPerProjectAndSeqType.findAllByProgramVersion(plugInAndVersion): "'${plugInAndVersion}' is unknown"

List<BamFilePairAnalysis> bamFilePairAnalysisList = BamFilePairAnalysis.withCriteria {
    samplePair {
        mergingWorkPackage1 {
            sample {
                individual {
                    project {
                        eq('name', projectName)
                    }
                }
            }
        }
    }
    config {
        eq('programVersion', plugInAndVersion)
    }
}

println "found analysis: ${bamFilePairAnalysisList.size()}"
println([
        'pid',
        'seqType',
        'sampleType1',
        'sampleTYpe2',
        'withdrawn',
        'instance list',
].join(','))
println bamFilePairAnalysisList.collect {
    [
            it.samplePair.individual.pid,
            it.seqType.displayNameWithLibraryLayout,
            it.samplePair.sampleType1.name,
            it.samplePair.sampleType2.name,
            it.withdrawn ? '(withdrawn)' : '',
            it.instanceName,
    ].join(',')
}.sort().join('\n')

Map<SamplePair, List<BamFilePairAnalysis>> bamFilePairAnalysisFiltered = bamFilePairAnalysisList.collectEntries { analysis ->
    [(analysis.samplePair): analysis.class.withCriteria {
        eq('samplePair', analysis.samplePair)
        config {
            ne('programVersion', plugInAndVersion)
        }
        eq('withdrawn', false)
    }
    ]
}

println "\n\nother analysis version of the sample pair (ignoring withdrawn): ${bamFilePairAnalysisFiltered.size()}"
println([
        'pid',
        'seqType',
        'sampleType1',
        'sampleTYpe2',
        'analyses count',
        'config version list',
        'instance list',
].join(','))
println bamFilePairAnalysisFiltered.collect { SamplePair samplePair, List<BamFilePairAnalysis> analysis ->
    [
            samplePair.individual.pid,
            samplePair.seqType.displayNameWithLibraryLayout,
            samplePair.sampleType1.name,
            samplePair.sampleType2.name,
            analysis.size(),
            analysis*.config*.programVersion.unique().sort().join(';'),
            analysis*.instanceName.unique().sort().join(';'),
    ].join(',')
}.sort().join('\n')

List<String> pids = bamFilePairAnalysisList.collect {
    it.samplePair.individual.pid
}.sort().unique()
println "\n\npids: ${pids.size()}"
println pids.join('\n')
