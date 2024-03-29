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
package de.dkfz.tbi.otp.withdraw

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFileAnalysisServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService

@CompileDynamic
@Transactional
class WithdrawAnalysisService implements ProcessingWithdrawService<BamFilePairAnalysis, AbstractBamFile> {
    AnalysisDeletionService analysisDeletionService
    BamFileAnalysisServiceFactoryService bamFileAnalysisServiceFactoryService

    @Override
    List<BamFilePairAnalysis> collectObjects(List<AbstractBamFile> entities) {
        return entities ? BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(
                entities,
                entities,
        ) : []
    }

    @Override
    List<String> collectPaths(List<BamFilePairAnalysis> entities) {
        return entities.collect {
            bamFileAnalysisServiceFactoryService.getService(it).getWorkDirectory(it).toString()
        }
    }

    @Override
    void withdrawObjects(List<BamFilePairAnalysis> entities) {
        entities.each {
            it.withdrawn = true
            it.save(flush: true)
        }
    }

    @Override
    void unwithdrawObjects(List<BamFilePairAnalysis> entities) {
        entities.each {
            it.withdrawn = false
            it.save(flush: true)
        }
    }

    @Override
    void deleteObjects(List<BamFilePairAnalysis> entities) {
        entities.each {
            analysisDeletionService.deleteInstance(it)
        }
    }
}
