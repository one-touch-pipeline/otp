package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import grails.converters.*
import org.codehaus.groovy.grails.web.json.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class ParseIndelQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Override
    void execute() throws Exception {
        final IndelCallingInstance indelCallingInstance = getProcessParameterObject()

        File indelQcFile = indelCallingInstance.getIndelQcJsonFile()
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(indelQcFile)
        File sampleSwapFile = indelCallingInstance.getSampleSwapJsonFile()
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(sampleSwapFile)
        JSONObject qcJson = JSON.parse(indelQcFile.text)
        JSONObject sampleSwapJson = JSON.parse(sampleSwapFile.text)
        IndelCallingInstance.withTransaction {

            IndelQualityControl indelQc = qcJson.values()
            indelQc.file = new File(indelQc.file.replace('./', '')).path
            indelQc.indelCallingInstance = indelCallingInstance
            assert indelQc.save(flush: true)

            IndelSampleSwapDetection sampleSwap = sampleSwapJson
            sampleSwap.indelCallingInstance = indelCallingInstance
            assert sampleSwap.save(flush: true)

            indelCallingInstance.updateProcessingState(AnalysisProcessingStates.FINISHED)
            succeed()
        }
    }
}
