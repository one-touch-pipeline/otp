package de.dkfz.tbi.otp.dataprocessing.indelcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class IndelResultsService extends AbstractAnalysisResultsService<IndelCallingInstance> {

    @Override
    String getVersionAttributeName() {
        "pluginVersion"
    }

    @Override
    Class<IndelCallingInstance> getInstanceClass() {
        IndelCallingInstance
    }

    @Override
    Map getQcData(IndelCallingInstance analysis) {
        IndelQualityControl qc = IndelQualityControl.findByIndelCallingInstance(analysis)
        IndelSampleSwapDetection sampleSwap = IndelSampleSwapDetection.findByIndelCallingInstance(analysis)
        [
                numIndels: qc?.numIndels ?: "",
                numIns: qc?.numIns ?: "",
                numDels: qc?.numDels ?: "",
                numSize1_3: qc?.numSize1_3 ?: "",
                numSize4_10: qc?.numDelsSize4_10 ?: "",
                somaticSmallVarsInTumor: sampleSwap?.somaticSmallVarsInTumor ?: "",
                somaticSmallVarsInControl: sampleSwap?.somaticSmallVarsInControl ?: "",
                somaticSmallVarsInTumorCommonInGnomad: sampleSwap?.somaticSmallVarsInTumorCommonInGnomad ?: "",
                somaticSmallVarsInControlCommonInGnomad: sampleSwap?.somaticSmallVarsInControlCommonInGnomad ?: "",
                somaticSmallVarsInTumorPass: sampleSwap?.somaticSmallVarsInTumorPass ?: "",
                somaticSmallVarsInControlPass: sampleSwap?.somaticSmallVarsInControlPass ?: "",
                tindaSomaticAfterRescue: sampleSwap?.tindaSomaticAfterRescue ?: "",
                tindaSomaticAfterRescueMedianAlleleFreqInControl: sampleSwap ? FormatHelper.formatNumber(sampleSwap.tindaSomaticAfterRescueMedianAlleleFreqInControl) : "",
        ]
    }
}
