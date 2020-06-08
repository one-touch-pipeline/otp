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


import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Path
import java.nio.file.Paths

//input area
//************ Select patients ************//
String patients = """
#patient1
#patient2

"""

//************ Path to copy files. Underneath, 'PID folders' will be created. (absolute path) ************//
String targetOutputFolder = "/..."

//************ Select whether FASTQ files should be copied (true/false) ************//
boolean copyFastqFiles = false

//************ Select whether BAM files should be copied (true/false) ************//
boolean copyBamFiles = false

//************ Check whether BAM or FASTQ files exist (true/false) ************//
boolean checkFileStatusFirst = true


//************ Select seq types ************//
def seqTypesToCopy = [
        //SeqTypeService.wholeGenomePairedSeqType,
        //SeqTypeService.exomePairedSeqType,
        //SeqTypeService.wholeGenomeBisulfitePairedSeqType,
        //SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
        //SeqTypeService.rnaPairedSeqType,
        //SeqTypeService.chipSeqPairedSeqType,
        //SeqType.findByNameAndLibraryLayoutAndSingleCell("ATAC", LibraryLayout.PAIRED, false)
]


// work area
StringBuilder output = new StringBuilder()

Path targetFolder = Paths.get(targetOutputFolder)

assert targetFolder.absolute : "targetOutputFolder is no absolute path"
assert seqTypesToCopy : "no seq type selected"

def addToOutput = { String s ->
    output.append("${s}\n")
}

if (!checkFileStatusFirst) {
    addToOutput("#!/bin/bash\n\nset -e\n")
}

patients.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.each { String pid ->
    if (checkFileStatusFirst) {
        println "${pid}"
    }
    if (copyFastqFiles) {
        List<SeqTrack> seqTrackList = SeqTrack.createCriteria().list {
            sample {
                individual {
                    eq("pid", pid)
                }
            }
            'in'("seqType", seqTypesToCopy)
        }

        if (checkFileStatusFirst) {
            println "Found lanes ${seqTrackList.size()}"
            seqTrackList.each { seqTrack ->
                println seqTrack
            }
        }

        seqTrackList.each { seqTrack ->
            String seqTrackPid = seqTrack.individual.pid
            String seqType = seqTrack.seqType.dirName
            String sampleType = seqTrack.sampleType.dirName
            seqTrack.dataFiles.findAll { !it.fileWithdrawn }.each { DataFile dataFile ->
                Path currentFile = Paths.get(ctx.lsdfFilesService.getFileFinalPath(dataFile))
                if (currentFile.toFile().exists()) {
                    if (!checkFileStatusFirst) {
                        Path targetFastqFolder = Paths.get(
                                targetFolder.toString(),
                                seqTrackPid,
                                seqType,
                                sampleType + (seqTrack.seqType.hasAntibodyTarget ? "-${seqTrack.antibodyTarget.name}" : "" )
                        )
                        addToOutput("echo ${currentFile}")
                        addToOutput("mkdir -p ${targetFastqFolder}")
                        addToOutput("cp -uv ${currentFile}* ${targetFastqFolder}")
                    }
                } else {
                    if (checkFileStatusFirst) {
                        println("WARNING: FastQ file ${currentFile} for ${seqTrackPid} ${sampleType} ${seqType} doesn't exist")
                    }
                }
            }
        }
        addToOutput("")
    }

    if (copyBamFiles) {
        addToOutput("\n")
        List<AbstractMergedBamFile> bamFiles = AbstractMergedBamFile.createCriteria().list {
            eq("withdrawn", false)
            eq("fileOperationStatus",
                    AbstractMergedBamFile.FileOperationStatus.PROCESSED)
            workPackage {
                sample {
                    individual {
                        eq("pid", pid)
                    }
                }
                'in'("seqType", seqTypesToCopy)
            }
        }.findAll { it.isMostRecentBamFile() }

        if (checkFileStatusFirst) {
            println "Found BAM files ${bamFiles.size()}"
            bamFiles.each { bamFile ->
                println bamFile.toString()
            }
        }

        bamFiles.each { bamFile ->
            Path sourceBam = Paths.get(bamFile.getWorkDirectory().getAbsolutePath(), bamFile.getBamFileName())
            Path targetBamFolder = Paths.get(
                    targetFolder.toString(),
                    bamFile.individual.pid,
                    bamFile.seqType.dirName,
                    bamFile.sampleType.getDirName() + (bamFile.workPackage.seqType.hasAntibodyTarget ? "-${bamFile.workPackage.antibodyTarget.name}" : "")
            )
            if (sourceBam.toFile().exists()) {
                if (!checkFileStatusFirst) {
                    addToOutput("echo ${sourceBam}")
                    addToOutput("mkdir -p ${targetBamFolder}")
                    addToOutput("cp -uv ${sourceBam}* ${targetBamFolder}")
                }
            } else {
                if (checkFileStatusFirst) {
                    println "WARNING: BAM File ${sourceBam} for ${bamFile.individual.pid} ${bamFile.sampleType.getDirName()} ${bamFile.seqType.dirName} doesn't exist"
                }
            }
        }
        addToOutput("")
    }
}

println "----------- output script -----------"
println output
