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
package de.dkfz.tbi.otp.dataprocessing.indelcalling

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.FormatHelper

@CompileDynamic
@Transactional
class IndelResultsService extends AbstractAnalysisResultsService<IndelCallingInstance> {

    final Class<IndelCallingInstance> instanceClass = IndelCallingInstance

    @Override
    void appendQcFetchingCriteria(Criteria criteria) {
    }

    @Override
    void appendQcProjectionCriteria(Criteria criteria) {
        criteria.indelQualityControl(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
            property('numIndels', 'numIndels')
            property('numIns', 'numIns')
            property('numDels', 'numDels')
            property('numSize1_3', 'numSize1_3')
            property('numDelsSize4_10', 'numSize4_10')
        }
        criteria.indelSampleSwapDetection(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
            property('germlineSmallVarsInBothRare', 'germlineSmallVarsInBothRare')
            property('somaticSmallVarsInTumor', 'somaticSmallVarsInTumor')
            property('somaticSmallVarsInControl', 'somaticSmallVarsInControl')
            property('somaticSmallVarsInTumorCommonInGnomad', 'somaticSmallVarsInTumorCommonInGnomad')
            property('somaticSmallVarsInControlCommonInGnomad', 'somaticSmallVarsInControlCommonInGnomad')
            property('somaticSmallVarsInTumorPass', 'somaticSmallVarsInTumorPass')
            property('somaticSmallVarsInControlPass', 'somaticSmallVarsInControlPass')
            property('tindaSomaticAfterRescue', 'tindaSomaticAfterRescue')
            property('tindaSomaticAfterRescueMedianAlleleFreqInControl', 'tindaSomaticAfterRescueMedianAlleleFreqInControl')
        }
    }

    @Override
    Map mapQcData(Map data) {
        return [
                numIndels                                       : data.numIndels ?: "",
                numIns                                          : data.numIns ?: "",
                numDels                                         : data.numDels ?: "",
                numSize1_3                                      : data.numSize1_3 ?: "",
                numSize4_10                                     : data.numSize4_10 ?: "",
                germlineSmallVarsInBothRare                     : data.germlineSmallVarsInBothRare != null ? data?.germlineSmallVarsInBothRare : "N/A",
                somaticSmallVarsInTumor                         : data.somaticSmallVarsInTumor ?: "",
                somaticSmallVarsInControl                       : data.somaticSmallVarsInControl ?: "",
                somaticSmallVarsInTumorCommonInGnomad           : data.somaticSmallVarsInTumorCommonInGnomad ?: "",
                somaticSmallVarsInControlCommonInGnomad         : data.somaticSmallVarsInControlCommonInGnomad ?: "",
                somaticSmallVarsInTumorPass                     : data.somaticSmallVarsInTumorPass ?: "",
                somaticSmallVarsInControlPass                   : data.somaticSmallVarsInControlPass ?: "",
                tindaSomaticAfterRescue                         : data.tindaSomaticAfterRescue ?: "",
                tindaSomaticAfterRescueMedianAlleleFreqInControl: data.tindaSomaticAfterRescueMedianAlleleFreqInControl ?
                        FormatHelper.formatNumber(data.tindaSomaticAfterRescueMedianAlleleFreqInControl) : "",
        ]
    }
}
