/*
 * Copyright 2011-2021 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.cron

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService

import java.time.LocalDate
import java.time.ZoneId

@Rollback
@Integration
class GenerateAndSendKPIsForNBIIntegrationSpec extends Specification implements RoddyRnaFactory {

    Date startDate
    Date endDate
    GenerateAndSendKPIsForNBI job = new GenerateAndSendKPIsForNBI()

    void setup() {
        LocalDate nowLastMonth = LocalDate.now().minusMonths(1)
        LocalDate fromDate = LocalDate.of(nowLastMonth.year, nowLastMonth.month, 1)
        LocalDate toDate = fromDate.plusMonths(1)
        startDate = Date.from(fromDate.atStartOfDay().atZone(ZoneId.of("Europe/Berlin")).toInstant())
        endDate = Date.from(toDate.atStartOfDay().atZone(ZoneId.of("Europe/Berlin")).toInstant())

        job.configService = Mock(ConfigService) {
            getTimeZoneId() >> {
                return ZoneId.of("Europe/Berlin")
            }
        }

        job.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(_) >> {
                return "test@otp.de"
            }
        }
    }

    void "wrappedExecute, send email with list of all required workflows when job runs"() {
        when:
        job.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _)  >> { String subject, String body, String recipient ->
                assert subject.startsWith("KPIs for de.NBI - ")
                assert body.contains("Workflow: PanCan")
                assert body.contains("Workflow: Rna alignment")
                assert body.contains("Workflow: CellRanger")
                assert body.contains("Workflow: Snv")
                assert body.contains("Workflow: Indel")
                assert body.contains("Workflow: Sophia")
                assert body.contains("Workflow: Aceseq")
                assert body.contains("Workflow: runYapsa")
                assert body.contains("Workflow: otp workflow")
                assert body.contains("Workflow: roddy")
                assert body.contains("Workflow: otp cluster jobs")
            }
        }

        job.wrappedExecute()

        then:
        noExceptionThrown()
    }

    void "panCanData, when panCan executions are started between the given dates, then return the workflow executions"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage([
                seqType: DomainFactory.createWholeGenomeSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
        ])
        RoddyBamFile roddyBamFile1 = DomainFactory.createRoddyBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                workPackage: workPackage,
        ])

        roddyBamFile1.dateCreated = startDate + 1
        roddyBamFile1.save(flush: true)

        // create more RoddyBamFiles (where the date criteria doesn't fit)
        DomainFactory.createRoddyBamFile()
        DomainFactory.createRoddyBamFile()

        when:
        List<Object[]> panCanData = job.panCanData(startDate, endDate)

        then:
        panCanData.size() == 1
        panCanData[0].contains(roddyBamFile1.workPackage.sample.individual.project.name)
    }

    void "rna, when rna executions are started between the given dates, then return the rna workflow executions"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage([
                seqType: DomainFactory.createRnaPairedSeqType(),
                pipeline: DomainFactory.createRoddyRnaPipeline(),

        ])
        RnaRoddyBamFile rnaRoddyBamFile1 = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                workPackage: workPackage,
        ])

        rnaRoddyBamFile1.dateCreated = startDate + 1
        rnaRoddyBamFile1.save(flush: true)

        // create more RoddyBamFiles (where the date criteria doesn't fit)
        createBamFile()
        createBamFile()

        when:
        List<Object[]> rnaData = job.rna(startDate, endDate)

        then:
        rnaData.size() == 1
        rnaData[0].contains(rnaRoddyBamFile1.workPackage.sample.individual.project.name)
    }

    void "single cell, when single cell executions are started between the given dates, then return the single cell workflow executions"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage([
                seqType: createSeqType([singleCell: true]),
                pipeline: DomainFactory.createRoddyRnaPipeline(),
        ])
        AbstractMergedBamFile singleCellBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                workPackage: workPackage,
        ])

        singleCellBamFile.dateCreated = startDate + 1
        singleCellBamFile.save(flush: true)

        when:
        List<Object[]> singleCellData = job.singleCell(startDate, endDate)

        then:
        singleCellData.size() == 1
        singleCellData[0].contains(singleCellBamFile.workPackage.sample.individual.project.name)
    }

    void "analysis, when analysis executions are started between the given dates, then return the analysis workflow executions"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage()

        SamplePair samplePair = DomainFactory.createSamplePair([
                mergingWorkPackage1: workPackage,
        ])

        BamFilePairAnalysis bamFileAnalysis = DomainFactory.createRunYapsaInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair,
        ])

        bamFileAnalysis.dateCreated = startDate + 1
        bamFileAnalysis.save(flush: true)

        when:
        List<Object[]> analysisData = job.analysis(startDate, endDate, RunYapsaInstance)

        then:
        analysisData.size() == 1
        analysisData[0].contains(bamFileAnalysis.samplePair.mergingWorkPackage1.sample.individual.project.name)
    }

    void "clusterjob, when clusterjob executions are started between the given dates, then return the clusterjob workflow executions"() {
        given:
        ClusterJob clusterJob = DomainFactory.createClusterJob([
                individual: createIndividual(),
        ])
        clusterJob.dateCreated = startDate + 1
        clusterJob.save(flush: true)

        // create more cluster jobs with newer start date
        DomainFactory.createClusterJob()
        DomainFactory.createClusterJob()

        when:
        List<Object[]> clusterJobs = job.clusterJobs(startDate, endDate)

        then:
        clusterJobs.size() == 1
        clusterJobs[0].contains(clusterJob.individual.project.name)
    }

    void "usersForThisProject, when usersForThisProject executions are started between the given dates, then return the usersForThisProject executions"() {
        given:
        Project project1 = createProject()
        Project project2 = createProject()
        User user1 = DomainFactory.createUser()
        User user2 = DomainFactory.createUser()

        DomainFactory.createUserProjectRole([
                project: project1,
                user: user1,
        ])

        DomainFactory.createUserProjectRole([
                project: project2,
                user: user1,
        ])

        DomainFactory.createUserProjectRole([
                project: project2,
                user: user2,
        ])

        when:
        int userForProject = job.usersForProjects([project1.name, project2.name])

        then:
        userForProject == 2
    }
}
