package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import grails.converters.*
import org.codehaus.groovy.grails.web.json.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

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
            qcTrafficLightService.setQcTrafficLightStatusBasedOnThreshold(aceseqInstance, qcOne)

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
