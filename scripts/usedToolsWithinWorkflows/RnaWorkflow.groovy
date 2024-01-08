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

package usedToolsWithinWorkflows

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.utils.CollectionUtils

/*
Input area
Please provide the name of the project you want to get the version information for
If you have specific PIDs you can also provide those instead of the project name.
Please provide the PIDs in a comma separated list, e.g. ["pid1", "pid2", ...]
 */
def projectName = ""
def pids = []

/*
Input checks
 */
assert (projectName || !pids.empty) : "Please provide either a project name or pids"
if (projectName) {
    assert CollectionUtils.atMostOneElement(Project.findAllByName(projectName)) : "No project with name ${projectName} could be found"
}

if (pids) {
    def noPids = []
    pids.each { pid ->
        if (!Individual.findAllByPid(pid)) {
            noPids << pid
        }
    }
    if (noPids) {
        println "No individual could be found for the following pids: ${noPids.join(',')}"
        assert false
    }
}

/*
Script area
 */
def fnf = new FilenameFilter(){
    boolean accept(File dir,
                   String name) {
        return name.endsWith(".parameters")
    }
}

def fnfOld = new FilenameFilter(){
    boolean accept(File dir,
                   String name) {
        return name.equals("runtimeConfig.sh")
    }
}

def toolNames = [
        "PYTHON_VERSION",
        "STAR_VERSION",
        "FEATURECOUNTS_BINARY",
        "SAMBAMBA_BINARY",
        "SAMTOOLS_BINARY",
        "RNASEQC_BINARY",
        "KALLISTO_BINARY",
        "QUALIMAP_BINARY",
        "ARRIBA_BINARY",
        "SUBREAD_VERSION",
        "SAMBAMBA_VERSION",
        "SAMTOOLS_VERSION",
        "HTSLIB_VERSION",
        "RNASEQC_VERSION",
        "KALLISTO_VERSION",
        "QUALIMAP_VERSION",
        "ARRIBA_VERSION",
        "R_VERSION"
]

RnaRoddyBamFile.createCriteria().list {
    eq("withdrawn", false)
    eq("fileOperationStatus", RoddyBamFile.FileOperationStatus.PROCESSED)
    workPackage {
        seqType {
            eq("name", SeqTypeNames.RNA.seqTypeName)
        }
        sample {
            if (pids) {
                individual {
                    'in'("pid", pids)
                }
            } else {
                individual {
                    project {
                        eq("name", projectName)
                    }
                }
            }
        }
    }
}.each { RnaRoddyBamFile bam ->
    println ""
    println "${bam.sample} ${bam.seqType}"
    boolean correctEnvironment = false
    List versions = []
    versions << bam.config.programVersion
    List<File> finalExecutionDirectories = bam.finalExecutionDirectories
    if (!finalExecutionDirectories) {
        println "!!!! No execution directory known for '${bam} !!!!"
        return
    }
    File dir = finalExecutionDirectories.first()
    if (bam.config.programVersion == "RNAseqWorkflow:1.0.22-1") {
        correctEnvironment = true
        def parameterFiles = dir.list(fnfOld)
        if (parameterFiles) {
            def file = new File(dir, parameterFiles.first())
            file.each { line ->
                if (toolNames.any { line.contains(it) }) {
                    versions << line
                }
            }
        }
    } else {
        def parameterFiles = dir.list(fnf)
        if (parameterFiles) {
            def file = new File(dir, parameterFiles.first())
            file.each { line ->
                if (toolNames.any { line.contains(it) }) {
                    versions << line
                }
                if (line.contains("workflowEnvironmentScript=workflowEnvironment_tbiCluster")) {
                    correctEnvironment = true
                }
            }
        }
    }
    if (!correctEnvironment) {
        println "!!!! The script does not support the environment or the version that was used to process the data !!!!"
    } else {
        versions.each {
            println it.replace("declare -x    ", "").replace("<cvalue name=", "").replace(" />", "").replace("/ibios/tbi_cluster/13.1/x86_64/", "")
        }
    }
}

println ""
