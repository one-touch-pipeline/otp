package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import org.codehaus.groovy.control.io.*
import spock.lang.*

class RoddyWorkflowConfigServiceIntegrationSpec extends Specification {

    void "test method loadPanCanConfigAndTriggerAlignment, valid"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        GroovyMock(RoddyWorkflowConfig, global: true)
        1 * RoddyWorkflowConfig.importProjectConfigFile(*_)

        when:
        LogThreadLocal.withThreadLog(NullWriter.DEFAULT, {
            RoddyWorkflowConfigService.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, roddyBamFile.seqType, HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, roddyBamFile.individual)
        })

        then:
        roddyBamFile.mergingWorkPackage.needsProcessing == true
        roddyBamFile.withdrawn == true
    }


    void "test method loadPanCanConfigAndTriggerAlignment no individual should throw exception"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()

        when:
        RoddyWorkflowConfigService.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, roddyBamFile.seqType, HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, null)

        then:
        AssertionError e = thrown()
        e.message.contains("The individual is not allowed to be null")
    }


    void "test method loadPanCanConfigAndTriggerAlignment importProjectConfigFile throws exception"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        GroovyMock(RoddyWorkflowConfig, global: true)
        1 * RoddyWorkflowConfig.importProjectConfigFile(*_) >> { throw new Exception("importProjectConfigFile failed") }

        when:
        RoddyWorkflowConfigService.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, roddyBamFile.seqType, HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, roddyBamFile.individual)

        then:
        Exception e = thrown()
        e.message.contains("importProjectConfigFile failed")
        roddyBamFile.mergingWorkPackage.needsProcessing == false
        roddyBamFile.withdrawn == false
    }


    void "test method loadPanCanConfigAndTriggerAlignment no merging work package found"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        GroovyMock(RoddyWorkflowConfig, global: true)
        1 * RoddyWorkflowConfig.importProjectConfigFile(*_)

        when:
        RoddyWorkflowConfigService.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, DomainFactory.createSeqType(), HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, roddyBamFile.individual)

        then:
        AssertionError e = thrown()
        e.message.contains("no MWP found")
        roddyBamFile.mergingWorkPackage.needsProcessing == false
        roddyBamFile.withdrawn == false
    }
}
