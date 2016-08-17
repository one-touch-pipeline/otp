package de.dkfz.tbi.otp.job.jobs.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import grails.converters.*
import org.codehaus.groovy.grails.web.json.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*


@Component
@Scope("prototype")
@UseJobLog
class ParseSophiaQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Override
    void execute() throws Exception {
        final SophiaInstance sophiaInstance = getProcessParameterObject()

        File qcFile = sophiaInstance.getQcJsonFile()
        JSONObject qcJson = JSON.parse(qcFile.text)
        SophiaInstance.withTransaction {
            SophiaQc sophiaQc = qcJson.values()
            sophiaQc.sophiaInstance = sophiaInstance
            assert sophiaQc.save(flush: true)
            sophiaInstance.processingState = AnalysisProcessingStates.FINISHED
            assert sophiaInstance.save(flush: true)
            succeed()
        }
    }
}
