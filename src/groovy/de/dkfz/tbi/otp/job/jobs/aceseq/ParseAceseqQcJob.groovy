package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import grails.converters.*
import org.codehaus.groovy.grails.web.json.*


class ParseAceseqQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Override
    void execute() throws Exception {
        final AceseqInstance aceseqInstance = getProcessParameterObject()

        File qcFile = aceseqInstance.getQcJsonFile()
        JSONObject qcJson = JSON.parse(qcFile.text)
        AceseqQc.withTransaction {
            qcJson.each { String number, Map values ->
                AceseqQc qc = new AceseqQc(values)
                qc.number = Integer.parseInt(number)
                qc.aceseqInstance = aceseqInstance
                assert qc.save(flush: true)
            }
        }
    }
}
