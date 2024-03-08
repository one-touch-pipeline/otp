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
package de.dkfz.tbi.otp.dataprocessing.sophia

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.dataprocessing.AbstractAnalysisResultsService

@CompileDynamic
@Transactional
class SophiaResultsService extends AbstractAnalysisResultsService<SophiaInstance> {

    final Class<SophiaInstance> instanceClass = SophiaInstance

    @Override
    void appendQcFetchingCriteria(Criteria criteria) {
    }

    @Override
    void appendQcProjectionCriteria(Criteria criteria) {
        criteria.sophiaQc(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
            property('controlMassiveInvPrefilteringLevel', 'controlMassiveInvPrefilteringLevel')
            property('tumorMassiveInvFilteringLevel', 'tumorMassiveInvFilteringLevel')
            property('rnaContaminatedGenesMoreThanTwoIntron', 'rnaContaminatedGenesMoreThanTwoIntron')
            property('rnaContaminatedGenesCount', 'rnaContaminatedGenesCount')
            property('rnaDecontaminationApplied', 'rnaDecontaminationApplied')
        }
    }

    @Override
    Map mapQcData(Map data) {
        return [
                controlMassiveInvPrefilteringLevel   : data.controlMassiveInvPrefilteringLevel,
                tumorMassiveInvFilteringLevel        : data.tumorMassiveInvFilteringLevel,
                rnaContaminatedGenesMoreThanTwoIntron: data.rnaContaminatedGenesMoreThanTwoIntron,
                rnaContaminatedGenesCount            : data.rnaContaminatedGenesCount,
                rnaDecontaminationApplied            : data.rnaDecontaminationApplied,
        ]
    }
}
