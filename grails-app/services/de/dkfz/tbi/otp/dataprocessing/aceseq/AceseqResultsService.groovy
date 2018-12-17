package de.dkfz.tbi.otp.dataprocessing.aceseq

import de.dkfz.tbi.otp.dataprocessing.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.AceseqQc
import de.dkfz.tbi.otp.ngsdata.AbstractAnalysisResultsService
import de.dkfz.tbi.otp.utils.FormatHelper

class AceseqResultsService extends AbstractAnalysisResultsService<AceseqInstance> {

    @Override
    String getVersionAttributeName() {
        "pluginVersion"
    }

    @Override
    Class<AceseqInstance> getInstanceClass() {
        AceseqInstance
    }

    @Override
    Map getQcData(AceseqInstance analysis) {
        AceseqQc qc = AceseqQc.findByAceseqInstanceAndNumber(analysis, 1)
        [
                tcc             : FormatHelper.formatNumber(qc?.tcc),
                ploidy          : FormatHelper.formatNumber(qc?.ploidy),
                ploidyFactor    : qc?.ploidyFactor,
                goodnessOfFit   : FormatHelper.formatNumber(qc?.goodnessOfFit),
                gender          : qc?.gender,
                solutionPossible: qc?.solutionPossible,
        ]
    }
}
