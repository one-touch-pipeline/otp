/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileStatic
@Transactional(readOnly = true)
class QaOverviewService {

    @Autowired
    List<AbstractQaOverviewService> baseQaOverviewServices

    QaOverviewFetchDataService qaOverviewFetchDataService

    List<Map<String, ?>> overviewData(Project project, SeqType seqType, Sample sample = null) {
        AbstractQaOverviewService service = findService(seqType)

        String query = service.createQuery(sample != null)
        Map<String, ?> parameters = service.createParameterMap(project, seqType, sample)
        List<Map<String, ?>> qaMapList = Project.executeQuery(query, parameters)
        qaOverviewFetchDataService.addLibraryPreparationKitAndSequencingLengthInformation(qaMapList)
        service.addDerivedData(qaMapList)

        return service.createList(project, seqType, qaMapList)
    }

    Class<? extends AbstractQualityAssessment> qaClass(SeqType seqType) {
        return findService(seqType).qaClass()
    }

    private AbstractQaOverviewService findService(SeqType seqType) {
        return CollectionUtils.exactlyOneElement(baseQaOverviewServices.findAll {
            it.supportedSeqTypes().contains(seqType)
        }, "Could not find a service for '${seqType.displayNameWithLibraryLayout}")
    }

    List<SeqType> allSupportedSeqTypes() {
        return baseQaOverviewServices.collectMany {
            it.supportedSeqTypes()
        }
    }
}