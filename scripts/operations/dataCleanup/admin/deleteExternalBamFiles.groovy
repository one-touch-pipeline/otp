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
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair

/**
 * Script to delete ExternallyProcessedMergedBamFile inclusive dependencies for given pids.
 *
 * The script change only the database. For the filesystem a bash script is displayed.
 *
 * The script has a tryRun mode, were all changes are roll backed.
 *
 * input:
 * - a list of pids
 *
 * output:
 * - list of deleted bam files and analysis
 * - bash script to delete it from the file system
 *
 * The script
 */

//--------------------------------
//input

/**
 * list of Pids.
 */
List<String> pidsToDelete = [
        'pid1',
        'pid2',
]

/**
 * flag to allow a try and rollback the changes at the end (true) or do the changes(false)
 */
boolean tryRun = true


//--------------------------------
//work

def bamFiles = ExternallyProcessedMergedBamFile.createCriteria().listDistinct {
    workPackage {
        sample {
            individual {
                'in'('pid', pidsToDelete)
            }
        }
    }
}

List<String> dirsToDelete = []

AnalysisDeletionService analysisDeletionService = ctx.analysisDeletionService


ExternallyProcessedMergedBamFile.withTransaction {
    bamFiles.each { ExternallyProcessedMergedBamFile epmbf ->
        println "to delete: ${epmbf}"

        ExternalMergingWorkPackage workPackage = epmbf.workPackage
        workPackage.bamFileInProjectFolder = null
        workPackage.save()

        dirsToDelete << epmbf.importFolder
        BamFilePairAnalysis.findAllBySampleType1BamFileOrSampleType2BamFile(epmbf, epmbf).each {
            println "  --> delete analysis: ${it}"
            dirsToDelete << analysisDeletionService.deleteInstance(it)
        }
        SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(workPackage, workPackage).each { SamplePair samplePair ->
            println "  --> delete pair: ${samplePair}"
            List<SamplePair> otherSamplePairs = SamplePair.withCriteria {
                mergingWorkPackage1 {
                    eq('sample', samplePair.mergingWorkPackage1.sample)
                    eq('seqType', samplePair.seqType)
                }
                mergingWorkPackage2 {
                    eq('sample', samplePair.mergingWorkPackage2.sample)
                }
                ne('id', samplePair.id)
            }
            List<File> files = analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances([samplePair])
            if (otherSamplePairs) {
                println "    --> there exist other sample pairs with same seqtype and sampletypes --> keep the result paths itself"
            } else {
                dirsToDelete.addAll(files)
            }
        }

        ExternalProcessedMergedBamFileQualityAssessment.createCriteria().list {
            qualityAssessmentMergedPass {
                eq('abstractMergedBamFile', epmbf)
            }
        }.each { ExternalProcessedMergedBamFileQualityAssessment qualityAssessment ->
            println "  --> delete qa: ${qualityAssessment}"
            QualityAssessmentMergedPass qualityAssessmentMergedPass = qualityAssessment.qualityAssessmentMergedPass
            qualityAssessment.delete()
            qualityAssessmentMergedPass.delete()

        }

        ImportProcess.withCriteria {
            externallyProcessedMergedBamFiles {
                eq('id', epmbf.id)
            }
        }.each { ImportProcess importProcess->
            println "  --> remove from: ${importProcess}"
            importProcess.externallyProcessedMergedBamFiles.remove(epmbf)
            importProcess.save()
            if (importProcess.externallyProcessedMergedBamFiles.empty) {
                println "    --> is now empty --> delete it"
                importProcess.delete()
            }
        }

        epmbf.delete()

        workPackage.delete()
        println "  ==> deleted"
    }
    it.flush()

    println "\n\nDirectories to delete"
    println dirsToDelete.collect {
        "rm -rf ${it}"
    }.join('\n')
    println "\n"

    assert !tryRun: "Rollback, since only tryRun."
}
