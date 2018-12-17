package de.dkfz.tbi.otp.job.jobs.indelCalling

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue

@Component
@Scope("prototype")
@UseJobLog
class ParseIndelQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute() throws Exception {
        final IndelCallingInstance instance = getProcessParameterObject()

        File indelQcFile = instance.getIndelQcJsonFile()
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(indelQcFile)
        File sampleSwapFile = instance.getSampleSwapJsonFile()
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(sampleSwapFile)
        JSONObject qcJson = JSON.parse(indelQcFile.text)
        JSONObject sampleSwapJson = JSON.parse(sampleSwapFile.text)
        IndelCallingInstance.withTransaction {

            IndelQualityControl indelQc = qcJson.values()
            indelQc.file = new File(indelQc.file.replace('./', '')).path
            indelQc.indelCallingInstance = instance
            assert indelQc.save(flush: true)

            IndelSampleSwapDetection sampleSwap = sampleSwapJson
            sampleSwap.indelCallingInstance = instance
            assert sampleSwap.save(flush: true)

            [indelQc, sampleSwap].each { QcTrafficLightValue qc ->
                qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(instance, qc)
            }

            instance.updateProcessingState(AnalysisProcessingStates.FINISHED)
            succeed()
        }
    }
}
