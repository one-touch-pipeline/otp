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
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*

/**
 * Script to export data (fastq-, bam-file(s) and analysis) to external source.
 *
 * The following options are available:
 *
 *      - targetOutputFolder: absolute path to the desired output location
 *      - copyFastqFiles: enable to copy fastq files
 *      - copyBamFiles: enable to copy bam files
 *      - copyAnalyses: enable to copy the analysis files
 *      - checkFileStatus: if enabled script only checks files and prints information. To get an output script disable this option.
 *      - getFileList: if enabled a list of files is additionally provided
 *      - unixGroup: set a new unix group for the copied data
 *      - external: if enabled, copied data is additionally granted read and execute permissions at the group level
 *      - seqTypesToCopy: enable the sequence types to copy by removing "//" from the corresponding line
 *      - sampleTypes: additionally filters for sample types (optional)
 */

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

//************ Select whether analyses should be copied (true/false) ************//
boolean copyAnalyses = false

//************ Check if and which files exist (true/false) ************//
boolean checkFileStatus = true

//************ Generate a script for file list (true/false) [checkFileStatus must be false] ************//
boolean getFileList = false

//************ Select new unix group ************//
String unixGroup = ""

//************ Select if data goes external ************//
boolean external = true

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

//************ Select sampleTypes (optional) ************//
String sampleTypes = """
#sampleType1
#sampleType2

"""


// work area
LsdfFilesService lsdfFilesService = ctx.lsdfFilesService

StringBuilder output = new StringBuilder()
StringBuilder outputList = new StringBuilder()

Path targetFolder = Paths.get(targetOutputFolder)
List sampleTypesFilter = splitSampleType(sampleTypes)

String rsyncChmod
String umask
if (external) {
    umask = "027"
    rsyncChmod = "Du=rwx,Dg=rx,Fu=rw,Fg=r"
} else {
    umask = "022"
    rsyncChmod = "Du=rwx,Dgo=rx,Fu=rw,Fog=r"
}

assert targetFolder.absolute : "targetOutputFolder is not an absolute path"
assert seqTypesToCopy : "no seq type selected"
assert unixGroup : "no group given"

def addToOutput = { String s ->
    output.append("${s}\n")
}

def addToOutputList = { String s ->
    outputList.append("${s}\n")
}

if (!checkFileStatus) {
    addToOutput("#!/bin/bash\n\nset -e\numask ${umask}\n")
    addToOutputList("#!/bin/bash\n\nset -e\numask ${umask}\n")
}

patients.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.each { String pid ->
    Path targetFolderWithPid = Paths.get(
            targetFolder.toString(),
            pid,
    )

    if (checkFileStatus) {
        println "${pid}"
    } else {
        addToOutput("mkdir -p ${targetFolderWithPid}")
    }

    if (copyFastqFiles) {
        List<SeqTrack> seqTrackList = SeqTrack.createCriteria().list {
            sample {
                individual {
                    eq("pid", pid)
                }
            }
            'in'("seqType", seqTypesToCopy)
            if (sampleTypesFilter) {
                sample {
                    'in'('sampleType', sampleTypesFilter)
                }
            }
        }

        if (checkFileStatus) {
            println "\n************************************ FASTQ ************************************"
            println "Found ${seqTrackList.size()} lanes:\n"
            seqTypesToCopy.each { seqType ->
                println seqType.toString()
                List l = []
                seqTrackList.each { seqTrack ->

                    if (seqTrack.seqType == seqType) {
                        l.add(seqTrack.sampleType.name)
                    }
                }
                println "\t${l.unique().sort().join("\n\t")}\n"
            }
        }

        seqTrackList.each { seqTrack ->
            String seqTrackPid = seqTrack.individual.pid
            String seqType = seqTrack.seqType.dirName
            String sampleType = seqTrack.sampleType.dirName
            seqTrack.dataFiles.findAll { !it.fileWithdrawn }.each { DataFile dataFile ->
                Path currentFile = Paths.get(lsdfFilesService.getFileFinalPath(dataFile))
                if (currentFile.toFile().exists()) {
                    if (!checkFileStatus) {
                        Path targetFastqFolder = Paths.get(
                                targetFolderWithPid.toString(),
                                lsdfFilesService.getFilePathInViewByPid(dataFile)
                        ).parent
                        addToOutput("echo ${currentFile}")
                        addToOutput("mkdir -p ${targetFastqFolder}")
                        String search = "${currentFile.toString().replaceAll("(_|.)R([1,2])(_|.)", "\$1*\$2\$3")}*"
                        addToOutput("rsync -uvpL --chmod=${rsyncChmod} ${search} ${targetFastqFolder}")
                        if (getFileList) {
                            addToOutputList("ls -l ${search}")
                        }
                    }
                } else {
                    if (checkFileStatus) {
                        println("WARNING: FastQ file ${currentFile} for ${seqTrackPid} ${sampleType} ${seqType} doesn't exist\n")
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
                if (sampleTypesFilter) {
                    sample {
                        'in'('sampleType', sampleTypesFilter)
                    }
                }
            }
        }.findAll { it.isMostRecentBamFile() }

        if (checkFileStatus) {
            println "\n************************************ BAM ************************************"
            println "Found BAM files ${bamFiles.size()}"
            bamFiles.each { bamFile ->
                println "\n" + bamFile.toString()
            }
        }

        bamFiles.each { bamFile ->
            Path basePath = Paths.get(bamFile.getBaseDirectory().getAbsolutePath())
            Path sourceBam
            Path qcFolder = Paths.get(basePath.toString(), "qualitycontrol")
            if (bamFile instanceof ExternallyProcessedMergedBamFile) {
                sourceBam = Paths.get(bamFile.bamFile.toString())
            } else {
                sourceBam = Paths.get(basePath.toString(), bamFile.bamFileName)
            }


            Path targetBamFolder = Paths.get(
                    targetFolderWithPid.toString(),
                    bamFile.seqType.dirName,
                    bamFile.sampleType.getDirName() + (bamFile.workPackage.seqType.hasAntibodyTarget ? "-${bamFile.workPackage.antibodyTarget.name}" : "")
            )
            if (sourceBam.toFile().exists()) {
                if (!checkFileStatus) {
                    if (bamFile.seqType == SeqTypeService.rnaPairedSeqType && copyAnalyses) {
                        addToOutput("echo ${basePath}")
                        addToOutput("mkdir -p ${targetBamFolder}")
                        addToOutput("rsync -uvrpL --exclude=*roddyExec* --exclude=.* --chmod=${rsyncChmod} ${basePath} ${targetBamFolder}")
                        if (getFileList) {
                            addToOutputList("ls -l --ignore=\"*roddyExec*\" ${basePath}")
                        }
                    } else {
                        addToOutput("echo ${sourceBam}")
                        addToOutput("mkdir -p ${targetBamFolder}")
                        addToOutput("rsync -uvpL --chmod=${rsyncChmod} ${sourceBam}* ${targetBamFolder}")
                        if (getFileList) {
                            addToOutputList("ls -l ${sourceBam}*")
                        }
                    }
                    if (qcFolder.toFile().exists()) {
                        addToOutput("echo ${qcFolder}")
                        addToOutput("rsync -uvrpL --chmod=${rsyncChmod} ${qcFolder}* ${targetBamFolder}")
                        if (getFileList) {
                            addToOutputList("ls -l ${qcFolder}*")
                        }
                    }
                }
            } else {
                if (checkFileStatus) {
                    println "WARNING: BAM File ${sourceBam} for ${bamFile.individual.pid} ${bamFile.sampleType.getDirName()} ${bamFile.seqType.dirName} doesn't exist\n"
                }
            }
        }
        addToOutput("")
    }

    if (copyAnalyses) {
        if (checkFileStatus) {
            println "\n************************************ Analyses ************************************ "
        }
        ["Indel", "Sophia", "ACEseq", "SNV", "RunYapsa"].each { instanceName ->
            List<BamFilePairAnalysis> analyses = getBamFilePairAnalysis(seqTypesToCopy, pid, instanceName, sampleTypesFilter)
            if (analyses) {
                if (!checkFileStatus) {
                    analyses.each {
                        Path resultFolder = Paths.get(
                                targetFolderWithPid.toString(),
                                it.seqType.dirName,
                                "${instanceName.toLowerCase()}_results",
                                "${it.samplePair.sampleType1.dirName}_${it.samplePair.sampleType2.dirName}"
                        )
                        addToOutput("echo ${it.instancePath.absoluteDataManagementPath}")
                        addToOutput("mkdir -p ${resultFolder}")
                        addToOutput("rsync -uvrpL --exclude=*roddyExec* --exclude=*bam* --chmod=${rsyncChmod} ${it.instancePath.absoluteDataManagementPath} ${resultFolder}")
                        if (getFileList) {
                            addToOutputList("ls -l --ignore=\"*roddyExec*\" ${it.instancePath.absoluteDataManagementPath}")
                        }
                    }
                } else {
                    println "\nFound following ${instanceName} analyse:"
                    analyses.each {
                        println "\t${it.sampleType1BamFile.sampleType.name}-${it.sampleType2BamFile.sampleType.name}:  " +
                                "${it.instanceName}"
                    }
                }
            }
        }
    }

    if (!checkFileStatus) {
        addToOutput("chgrp -R ${unixGroup} ${targetFolderWithPid}")
    }
}

List<BamFilePairAnalysis> getBamFilePairAnalysis (List seqTypesToCopy, String pid, String instanceName, List sampleTypesHelper) {
    List<BamFilePairAnalysis> analyses = []

    SamplePair.withCriteria {
        mergingWorkPackage1 {
            sample {
                individual {
                    eq('pid', pid)
                }
            }
            'in'('seqType', seqTypesToCopy)
            if (sampleTypesHelper) {
                sample {
                    'in'('sampleType', sampleTypesHelper)
                }
            }
        }
        mergingWorkPackage2 {
            if (sampleTypesHelper) {
                sample {
                    'in'('sampleType', sampleTypesHelper)
                }
            }
        }
    }.each { samplePair1 ->
        List l = BamFilePairAnalysis.withCriteria {
            eq('samplePair', samplePair1)
            eq('processingState', AnalysisProcessingStates.FINISHED)
            like('instanceName', "%${instanceName}%")
            order("id", "desc")
        }
        if (!l.isEmpty()) {
            analyses.add(l.first())
        }
    }



    if (analyses) {
        return analyses
    } else {
        return null
    }
}

List<SampleType> splitSampleType (String input) {
    return input.split('\n')*.trim().findAll {
        it && !it.startsWith('#')
    }.collect {
        SampleType.findAllByName(it)
    }.flatten()
}

println "\n----------- ${checkFileStatus? "to get an output script -> set checkFileStatus = false" : "output script"} -----------"
println output

println "\n\n----------- ${!getFileList? "to get an output list -> set getFileList = true and checkFileStatus = false" : "output list"} -----------"
println outputList
