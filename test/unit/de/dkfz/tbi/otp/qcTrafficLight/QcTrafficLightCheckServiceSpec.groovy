package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.domainFactory.pipelines.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AbstractMergedBamFile,
        Comment,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        ProcessingOption,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
])
class QcTrafficLightCheckServiceSpec extends Specification implements IsRoddy {

    @Unroll
    void "handleQcCheck, if status is #status, then call callback and do not call informResultsAreBlocked"() {
        given:
        RoddyBamFile bamFile = createBamFile([
                qcTrafficLightStatus: status,
        ])

        QcTrafficLightCheckService service = new QcTrafficLightCheckService([
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    0 * informResultsAreBlocked(_)
                },
        ])

        boolean called = false
        Closure closure = {
            called = true
        }

        when:
        service.handleQcCheck(bamFile, closure)

        then:
        called

        where:
        status << AbstractMergedBamFile.QcTrafficLightStatus.values().findAll {
            it.jobLinkCase == AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_LINKS
        } + [null]
    }

    @Unroll
    void "handleQcCheck, if status is #status and qcThresholdHandling is #qcThresholdHandling, then do not call the callback, but call informResultsAreBlocked"() {
        given:
        int count = qcThresholdHandling.notifiesUser?1:0
        RoddyBamFile bamFile = createBamFile([
                qcTrafficLightStatus: status,
        ])
        bamFile.project.qcThresholdHandling = qcThresholdHandling

        QcTrafficLightCheckService service = new QcTrafficLightCheckService([
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    count * informResultsAreBlocked(_)
                },
        ])

        Closure closure = {
            assert false: 'should not be called'
        }


        when:
        service.handleQcCheck(bamFile, closure)

        then:
        noExceptionThrown()

        where:
        values <<
                [
                        AbstractMergedBamFile.QcTrafficLightStatus.values().findAll {
                            it.jobLinkCase == AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_NO_LINK
                        },
                        QcThresholdHandling.values(),
                ].combinations()
        status = (AbstractMergedBamFile.QcTrafficLightStatus)values[0]
        qcThresholdHandling = (QcThresholdHandling )values[1]
    }

    @Unroll
    void "handleQcCheck, if status is #status, then do not call the callback but throw exception"() {
        given:
        RoddyBamFile bamFile = createBamFile([
                qcTrafficLightStatus: status,
        ])

        QcTrafficLightCheckService service = new QcTrafficLightCheckService([
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    0 * informResultsAreBlocked(_)
                },
        ])

        Closure closure = {
            assert false: 'should not be called'
        }

        when:
        service.handleQcCheck(bamFile, closure)

        then:
        RuntimeException e = thrown()
        e.message.contains(status.toString())

        where:
        status << AbstractMergedBamFile.QcTrafficLightStatus.values().findAll {
            it.jobLinkCase == AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR
        }
    }
}
