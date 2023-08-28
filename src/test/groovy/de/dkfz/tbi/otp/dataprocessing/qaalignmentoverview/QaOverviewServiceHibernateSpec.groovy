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

import grails.test.hibernate.HibernateSpec
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project

class QaOverviewServiceHibernateSpec extends HibernateSpec implements DomainFactoryCore {

    private QaOverviewService service

    @Override
    List<Class> getDomainClasses() {
        return [
                Sample,
                SeqType,
        ]
    }

    private void setupData() {
        service = new QaOverviewService()
    }

    @Unroll
    void "overviewData, when execute (case: #name), then call the depending services correctly"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        Sample sample = useSample ? createSample() : null

        setupData()

        AbstractQaOverviewService abstractQaOverviewService = Mock(AbstractQaOverviewService) {
            0 * _
            1 * supportedSeqTypes() >> [seqType]
            1 * createParameterMap(project, seqType, sample) >> [:]
            1 * addDerivedData(_ as List<Map<String, ?>>)
            1 * createList(project, seqType, _ as List<Map<String, ?>>) >> [[:]]

            // groovy magic replace call 'createQuery' by call of '$tt__createQuery'
            0 * createQuery(_)
            // method added and used by groovy magic, is used instead of 'createQuery'
            1 * $tt__createQuery(_, _) >> 'select new map(p.id, p.version) from Project p'
            // method added and used by groovy magic
            _ * getTransactionManager() >> super.transactionManager
        }

        service.qaOverviewFetchDataService = Mock(QaOverviewFetchDataService) {
            0 * _
            1 * addLibraryPreparationKitAndSequencingLengthInformation(_)
        }
        service.baseQaOverviewServices = [
                Mock(AbstractQaOverviewService) {
                    0 * _
                    1 * supportedSeqTypes() >> [createSeqType()]
                },
                abstractQaOverviewService,
        ]

        when:
        service.overviewData(project, seqType, sample)

        then:
        noExceptionThrown()

        where:
        name          | useSample
        'no sample'   | false
        'with sample' | true
    }
}
