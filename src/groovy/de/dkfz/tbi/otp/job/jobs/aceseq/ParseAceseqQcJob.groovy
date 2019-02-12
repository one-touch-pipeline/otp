package de.dkfz.tbi.otp.job.jobs.aceseq

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

@Component
@Scope("prototype")
@UseJobLog
class ParseAceseqQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute() throws Exception {
        final AceseqInstance aceseqInstance = getProcessParameterObject()

        File qcFile = aceseqInstance.getQcJsonFile()
        JSONObject qcJson = JSON.parse(qcFile.text)
        AceseqQc.withTransaction {
            AceseqQc qcOne = qcJson.collect { String number, Map values ->
                AceseqQc qc = new AceseqQc(values)
                qc.number = Integer.parseInt(number)
                qc.aceseqInstance = aceseqInstance
                assert qc.save(flush: true)
                return qc
            }.find {
                it.number == 1
            }

            qcOne //the instance to used for qc
            // TODO OTP-3097: triger qc handling here

            List<File> files = aceseqInstance.getAllFiles()
            files.each {
                LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
            }

            aceseqInstance.processingState = AnalysisProcessingStates.FINISHED
            assert aceseqInstance.save(flush: true)
            succeed()
        }
    }
}
