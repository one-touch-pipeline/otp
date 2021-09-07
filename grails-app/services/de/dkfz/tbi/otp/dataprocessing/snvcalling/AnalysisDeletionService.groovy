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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.infrastructure.FileService

import java.nio.file.Path

@Transactional
class AnalysisDeletionService {
    AceseqService aceseqService
    FileService fileService
    IndelCallingService indelCallingService
    SnvCallingService snvCallingService
    SophiaService sophiaService
    BamFileAnalysisServiceFactoryService bamFileAnalysisServiceFactoryService

    /**
     * Delete all subclasses of BamFilePairAnalysis (such as SnvCallingInstance, IndelCallingInstance, etc) from the database.
     */
    File deleteInstance(BamFilePairAnalysis analysisInstance) {
        Path directory = bamFileAnalysisServiceFactoryService.getService(analysisInstance).getWorkDirectory(analysisInstance)
        switch (analysisInstance) {
            case { it instanceof IndelCallingInstance } :
                List<IndelQualityControl> indelQualityControl = IndelQualityControl.findAllByIndelCallingInstance(analysisInstance, [sort: 'id', order: 'desc'])
                indelQualityControl.each {
                    it.delete(flush: true)
                }
                List<IndelSampleSwapDetection> indelSampleSwapDetections = IndelSampleSwapDetection.findAllByIndelCallingInstance(
                        analysisInstance, [sort: 'id', order: 'desc'])
                indelSampleSwapDetections.each {
                    it.delete(flush: true)
                }
                break
            case { it instanceof SophiaInstance } :
                List<SophiaQc> sophiaQc = SophiaQc.findAllBySophiaInstance(analysisInstance, [sort: 'id', order: 'desc'])
                sophiaQc.each {
                    it.delete(flush: true)
                }
                break
            case { it instanceof AceseqInstance } :
                List<AceseqQc> aceseqQc = AceseqQc.findAllByAceseqInstance(analysisInstance, [sort: 'id', order: 'desc'])
                aceseqQc.each {
                    it.delete(flush: true)
                }
                break
        }
        analysisInstance.delete(flush: true)
        return fileService.toFile(directory)
    }

    /**
     * Delete empty SamplePairs (SamplePairs with no further subclasses of BamFilePairAnalysis).
     * The SamplePair directories are parent directories of the subclasses of BamFilePairAnalysis directories,
     * therefore this method has to run after deleteInstance().
     */
    List<File> deleteSamplePairsWithoutAnalysisInstances(List<SamplePair> samplePairs) {
        List<Path> directoriesToDelete = []
        samplePairs.unique().each { SamplePair samplePair ->
            if (!AbstractSnvCallingInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << snvCallingService.getSamplePairPath(samplePair)
            }
            if (!IndelCallingInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << indelCallingService.getSamplePairPath(samplePair)
            }
            if (!AceseqInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << aceseqService.getSamplePairPath(samplePair)
            }
            if (!SophiaInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << sophiaService.getSamplePairPath(samplePair)
            }
            if (!BamFilePairAnalysis.findBySamplePair(samplePair)) {
                samplePair.delete(flush: true)
            }
        }
        return directoriesToDelete.collect { fileService.toFile(it) }
    }

    void assertThatNoWorkflowsAreRunning(List<BamFilePairAnalysis> instances) {
        assert instances.find { it.processingState != AnalysisProcessingStates.IN_PROGRESS || it.withdrawn }:
                "There are some analysis workflows running for ${instances[0].sampleType1BamFile}"
    }
}
