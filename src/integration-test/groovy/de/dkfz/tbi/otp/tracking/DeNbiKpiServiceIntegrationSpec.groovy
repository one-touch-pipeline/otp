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
package de.dkfz.tbi.otp.tracking

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Rollback
@Integration
class DeNbiKpiServiceIntegrationSpec extends Specification implements RoddyRnaFactory, WorkflowSystemDomainFactory, UserDomainFactory {

    DeNbiKpiService deNbiKpiService

    Date startDate
    Date endDate

    void setup() {
        deNbiKpiService = new DeNbiKpiService()

        LocalDate nowLastMonth = LocalDate.now().minusMonths(1)
        LocalDate fromDate = LocalDate.of(nowLastMonth.year, nowLastMonth.month, 1)
        LocalDate toDate = fromDate.plusMonths(1)
        startDate = Date.from(fromDate.atStartOfDay().atZone(ZoneId.of("Europe/Berlin")).toInstant())
        endDate = Date.from(toDate.atStartOfDay().atZone(ZoneId.of("Europe/Berlin")).toInstant())
    }

    void "getPanCanKpi, when panCan executions are started between the given dates, then return the panCan KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage([
                seqType: DomainFactory.createWholeGenomeSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
        ])
        RoddyBamFile roddyBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                workPackage: workPackage,
        ])

        roddyBamFile.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        roddyBamFile.save(flush: true)

        // create more RoddyBamFiles (where the date criteria doesn't fit)
        createBamFile()
        createBamFile()

        when:
        DeNbiKpi kpi = deNbiKpiService.getPanCanKpi(startDate, endDate)

        then:
        kpi.name == "PanCan"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(roddyBamFile.workPackage.sample.individual.project.name)
    }

    void "getRnaAlignmentKpi, when rna executions are started between the given dates, then return the rna alignment KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage([
                seqType: DomainFactory.createRnaPairedSeqType(),
                pipeline: DomainFactory.createRoddyRnaPipeline(),

        ])
        RnaRoddyBamFile rnaRoddyBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                workPackage: workPackage,
        ])

        rnaRoddyBamFile.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        rnaRoddyBamFile.save(flush: true)

        // create more RoddyBamFiles (where the date criteria doesn't fit)
        createBamFile()
        createBamFile()

        when:
        DeNbiKpi kpi = deNbiKpiService.getRnaAlignmentKpi(startDate, endDate)

        then:
        kpi.name == "Rna alignment"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(rnaRoddyBamFile.workPackage.sample.individual.project.name)
    }

    void "getCellRangerKpi, when cellRanger executions are started between the given dates, then return the cellRanger KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage([
                seqType: createSeqType([singleCell: true]),
                pipeline: DomainFactory.createRoddyRnaPipeline(),
        ])
        AbstractMergedBamFile singleCellBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                workPackage: workPackage,
        ])

        singleCellBamFile.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        singleCellBamFile.save(flush: true)

        when:
        DeNbiKpi kpi = deNbiKpiService.getCellRangerKpi(startDate, endDate)

        then:
        kpi.name == "CellRanger"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(singleCellBamFile.workPackage.sample.individual.project.name)
    }

    void "getRunYapsaKpi, when runYapsa executions are started between the given dates, then return the runYapsa KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage()

        SamplePair samplePair = DomainFactory.createSamplePair([
                mergingWorkPackage1: workPackage,
        ])

        BamFilePairAnalysis bamFileAnalysis = DomainFactory.createRunYapsaInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair,
        ])

        bamFileAnalysis.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        bamFileAnalysis.save(flush: true)

        when:
        DeNbiKpi kpi = deNbiKpiService.getRunYapsaKpi(startDate, endDate)

        then:
        kpi.name == "runYapsa"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(bamFileAnalysis.samplePair.mergingWorkPackage1.sample.individual.project.name)
    }

    void "getSnvCallingKpi, when snv executions are started between the given dates, then return the snv KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage()

        SamplePair samplePair = DomainFactory.createSamplePair([
                mergingWorkPackage1: workPackage,
        ])

        BamFilePairAnalysis bamFileAnalysis = DomainFactory.createSnvInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair,
        ])

        bamFileAnalysis.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        bamFileAnalysis.save(flush: true)

        when:
        DeNbiKpi kpi = deNbiKpiService.getSnvCallingKpi(startDate, endDate)

        then:
        kpi.name == "Snv"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(bamFileAnalysis.samplePair.mergingWorkPackage1.sample.individual.project.name)
    }

    void "getIndelKpi, when indel executions are started between the given dates, then return the indel KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage()

        SamplePair samplePair = DomainFactory.createSamplePair([
                mergingWorkPackage1: workPackage,
        ])

        BamFilePairAnalysis bamFileAnalysis = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair,
        ])

        bamFileAnalysis.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        bamFileAnalysis.save(flush: true)

        when:
        DeNbiKpi kpi = deNbiKpiService.getIndelKpi(startDate, endDate)

        then:
        kpi.name == "Indel"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(bamFileAnalysis.samplePair.mergingWorkPackage1.sample.individual.project.name)
    }

    void "getSophiaKpi, when sophia executions are started between the given dates, then return the sophia KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage()

        SamplePair samplePair = DomainFactory.createSamplePair([
                mergingWorkPackage1: workPackage,
        ])

        BamFilePairAnalysis bamFileAnalysis = DomainFactory.createSophiaInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair,
        ])

        bamFileAnalysis.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        bamFileAnalysis.save(flush: true)

        when:
        DeNbiKpi kpi = deNbiKpiService.getSophiaKpi(startDate, endDate)

        then:
        kpi.name == "Sophia"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(bamFileAnalysis.samplePair.mergingWorkPackage1.sample.individual.project.name)
    }

    void "getAceseqKpi, when aceseq executions are started between the given dates, then return the aceseq KPI"() {
        given:
        AbstractMergingWorkPackage workPackage = createMergingWorkPackage()

        SamplePair samplePair = DomainFactory.createSamplePair([
                mergingWorkPackage1: workPackage,
        ])

        BamFilePairAnalysis bamFileAnalysis = DomainFactory.createAceseqInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair,
        ])

        bamFileAnalysis.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        bamFileAnalysis.save(flush: true)

        when:
        DeNbiKpi kpi = deNbiKpiService.getAceseqKpi(startDate, endDate)

        then:
        kpi.name == "Aceseq"
        kpi.count == 1
        kpi.projectCount == 1
        kpi.projects.contains(bamFileAnalysis.samplePair.mergingWorkPackage1.sample.individual.project.name)
    }

    void "getClusterJobKpi, when cluster job executions are started between the given dates, then return the cluster jobs KPI"() {
        given:
        ClusterJob oldClusterJob = createClusterJob([
                workflowStep: null,
                individual: createIndividual(),
                oldSystem: true,
        ])
        oldClusterJob.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        oldClusterJob.save(flush: true)

        ClusterJob newClusterJob = createClusterJob([
                workflowStep: createWorkflowStep(),
                individual: null,
                oldSystem: false,
        ])
        newClusterJob.dateCreated = Date.from(startDate.toInstant().plus(1, ChronoUnit.DAYS))
        newClusterJob.save(flush: true)

        // create more cluster jobs with newer start date
        createClusterJob()
        createClusterJob()

        when:
        DeNbiKpi kpi = deNbiKpiService.getClusterJobKpi(startDate, endDate)

        then:
        kpi.name == "otp cluster jobs"
        kpi.count == 2
        kpi.projectCount == 2
        kpi.projects.contains(oldClusterJob.individual.project.name)
        kpi.projects.contains(newClusterJob.workflowStep.workflowRun.project.name)
    }

    void "generateSumKpi, should generate an aggregated KPI"() {
        given:
        String newKpiName = "newKpiName"
        String project1 = "project1"
        String project2 = "project2"
        String project3 = "project3"
        String project4 = "project4"
        String project5 = "project5"

        DeNbiKpi kpi1 = new DeNbiKpi("abc", 1, [project1])
        DeNbiKpi kpi2 = new DeNbiKpi("def", 2, [project1, project2])
        DeNbiKpi kpi3 = new DeNbiKpi("geh", 3, [project3, project4, project5])

        when:
        DeNbiKpi resultKpi = deNbiKpiService.generateSumKpi(newKpiName, kpi1, kpi2, kpi3)

        then:
        resultKpi.name == newKpiName
        resultKpi.count == 6
        resultKpi.projectCount == 5
        resultKpi.projects.containsAll([project1, project2, project3, project4, project5])
    }

    void "countUsersInProjects, when usersForThisProject executions are started between the given dates, then return the number of users"() {
        given:
        Project project1 = createProject()
        Project project2 = createProject()
        User user1 = createUser()
        User user2 = createUser()

        createUserProjectRole([
                project: project1,
                user: user1,
        ])

        createUserProjectRole([
                project: project2,
                user: user1,
        ])

        createUserProjectRole([
                project: project2,
                user: user2,
        ])

        when:
        int userForProject = deNbiKpiService.countUsersInProjects([project1.name, project2.name])

        then:
        userForProject == 2
    }
}
