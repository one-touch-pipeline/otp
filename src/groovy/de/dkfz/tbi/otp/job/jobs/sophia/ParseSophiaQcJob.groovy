package de.dkfz.tbi.otp.job.jobs.sophia

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService

@Component
@Scope("prototype")
@UseJobLog
class ParseSophiaQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute() throws Exception {
        final SophiaInstance sophiaInstance = getProcessParameterObject()
        File qcFile = sophiaInstance.getQcJsonFile()
        JSONObject qcJson = JSON.parse(qcFile.text)
        SophiaInstance.withTransaction {
            SophiaQc sophiaQc = qcJson.values()
            sophiaQc.sophiaInstance = sophiaInstance
            assert sophiaQc.save(flush: true)
            qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(sophiaInstance, sophiaQc)

            sophiaInstance.processingState = AnalysisProcessingStates.FINISHED
            assert sophiaInstance.save(flush: true)

            succeed()
        }
    }
}
