/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.transform.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException

@CompileDynamic
@Transactional
class CellRangerConfigurationService {

    ProjectService projectService
    SecurityService securityService

    @Immutable
    @ToString
    static class Samples {
        List<Sample> allSamples
        List<Sample> selectedSamples
    }

    @Canonical
    static class PlatformGroupAndKit {
        SeqPlatformGroup seqPlatformGroup
        LibraryPreparationKit libraryPreparationKit
    }

    @Canonical
    static class CellRangerMwpParameter {
        Integer expectedCells
        Integer enforcedCells
        ReferenceGenomeIndex referenceGenomeIndex
        SeqType seqType
    }

    CellRangerWorkflowService cellRangerWorkflowService
    TicketService ticketService

    SeqType getSeqType() {
        return CollectionUtils.exactlyOneElement(seqTypes)
    }

    List<SeqType> getSeqTypes() {
        return pipeline.seqTypes
    }

    Pipeline getPipeline() {
        return CollectionUtils.atMostOneElement(Pipeline.findAllByName(Pipeline.Name.CELL_RANGER))
    }

    MergingCriteria getMergingCriteria(Project project) {
        return CollectionUtils.atMostOneElement(
                MergingCriteria.findAllByProjectAndSeqType(project, seqType)
        )
    }

    List<CellRangerMergingWorkPackage> findCellRangerMergingWorkPackageByProject(Project project) {
        return CellRangerMergingWorkPackage.createCriteria().list {
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
    }

    CellRangerConfig getWorkflowConfig(Project project) {
        return CollectionUtils.atMostOneElement(
                CellRangerConfig.findAllWhere(
                        project: project,
                        seqType: seqType,
                        pipeline: pipeline,
                        obsoleteDate: null,
                )
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    Errors configureAutoRun(Project project,
                            boolean enableAutoExec,
                            Integer expectedCells,
                            Integer enforcedCells,
                            ReferenceGenomeIndex referenceGenomeIndex) {
        CellRangerConfig config = getWorkflowConfig(project)

        try {
            config.autoExec = enableAutoExec
            if (enableAutoExec) {
                config.referenceGenomeIndex = referenceGenomeIndex
                config.expectedCells = expectedCells
                config.enforcedCells = enforcedCells
            } else {
                config.referenceGenomeIndex = null
                config.expectedCells = null
                config.enforcedCells = null
            }
            config.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<Sample> getAllSamples(Project project, List<Individual> individuals, List<SampleType> sampleTypes) {
        return Sample.createCriteria().listDistinct {
            seqTracks {
                eq("seqType", seqType)
            }
            individual {
                eq("project", project)
            }
            if (individuals) {
                'in'("individual", individuals)
            }

            if (sampleTypes) {
                'in'("sampleType", sampleTypes)
            }
        } as List<Sample>
    }

    Map<PlatformGroupAndKit, List<SeqTrack>> getSeqTracksGroupedByPlatformGroupAndKit(Collection<SeqTrack> seqTracks) {
        return seqTracks.groupBy { SeqTrack seqTrack ->
            [
                    seqPlatformGroup     : seqTrack.seqPlatformGroup,
                    libraryPreparationKit: seqTrack.libraryPreparationKit,
            ]
        }.collectEntries { Map<String, Entity> key, List<SeqTrack> seqTracksPerPlatformGroupAndKit ->
            PlatformGroupAndKit platformGroupAndKit = new PlatformGroupAndKit(
                    seqPlatformGroup: key['seqPlatformGroup'] as SeqPlatformGroup,
                    libraryPreparationKit: key['libraryPreparationKit'] as LibraryPreparationKit,
            )
            return [(platformGroupAndKit): seqTracksPerPlatformGroupAndKit]
        }
    }

    /**
     * Currently it is not supported to have SeqTracks of differing SeqPlatformGroups and LibPrepKits within one
     * Sample. For SingleCell data the import validator already throws an Error, see {@link MergingPreventionValidator}.
     */
    static void constrainSeqTracksGroupedByPlatformGroupAndKit(Map<PlatformGroupAndKit, List<SeqTrack>> map) {
        assert map.size() <= 1: "Can not handle SeqTracks processed over multiple platforms or with different library preparation kits"
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#samples[0].project, 'OTP_READ_ACCESS')")
    void prepareCellRangerExecution(List<Sample> samples, Integer expectedCells, Integer enforcedCells, ReferenceGenomeIndex referenceGenomeIndex) {
        CellRangerMwpParameter parameter = new CellRangerMwpParameter(expectedCells, enforcedCells, referenceGenomeIndex, seqType)

        User requester = securityService.currentUser
        List<CellRangerMergingWorkPackage> mwps = createMergingWorkPackagesForSamples(samples, parameter, requester)
        resetAllTicketsOfSeqTracksForCellRangerExecution(mwps.collectMany { return it.seqTracks } as Set<SeqTrack>)
    }

    void resetAllTicketsOfSeqTracksForCellRangerExecution(Set<SeqTrack> seqTracks) {
        ticketService.findAllTickets(seqTracks).each { Ticket ticket ->
            resetTicketForCellRangerExecution(ticket)
        }
    }

    void resetTicketForCellRangerExecution(Ticket ticket) {
        ticket.finalNotificationSent = false
        ticket.alignmentFinished = null
        ticket.save(flush: true)
    }

    List<CellRangerMergingWorkPackage> findMergingWorkPackage(List<Sample> samples, Pipeline pipeline) {
        return samples ? CellRangerMergingWorkPackage.findAllBySampleInListAndPipeline(samples, pipeline) : []
    }

    List<CellRangerMergingWorkPackage> createMergingWorkPackagesForSamples(List<Sample> samples, CellRangerMwpParameter parameter, User requester) {
        return samples.collectMany { Sample sample ->
            return findAllMergingWorkPackagesBySamplesAndPipeline(sample, parameter, requester)
        }
    }

    void runOnImport(Project project, Sample sample) {
        CellRangerConfig config = getWorkflowConfig(project)

        if (config && config.autoExec) {
            CellRangerMwpParameter parameter = new CellRangerMwpParameter(config.expectedCells, config.enforcedCells, config.referenceGenomeIndex, seqType)
            findAllMergingWorkPackagesBySamplesAndPipeline(sample, parameter, null)
        }
    }

    List<CellRangerMergingWorkPackage> findAllMergingWorkPackagesBySamplesAndPipeline(Sample sample, CellRangerMwpParameter parameter, User requester) {
        Map<PlatformGroupAndKit, List<SeqTrack>> map = getSeqTracksGroupedByPlatformGroupAndKit(sample.seqTracks.findAll { it.seqType == parameter.seqType })
        constrainSeqTracksGroupedByPlatformGroupAndKit(map)
        return map.collect { PlatformGroupAndKit platformGroupAndKit, List<SeqTrack> seqTracks ->
            return new CellRangerMergingWorkPackage([
                    seqType              : parameter.seqType,
                    pipeline             : pipeline,
                    sample               : sample,
                    seqTracks            : seqTracks,
                    expectedCells        : parameter.expectedCells,
                    enforcedCells        : parameter.enforcedCells,
                    config               : getWorkflowConfig(sample.project),
                    statSizeFileName     : null,
                    referenceGenomeIndex : parameter.referenceGenomeIndex,
                    referenceGenome      : parameter.referenceGenomeIndex.referenceGenome,
                    antibodyTarget       : null,
                    seqPlatformGroup     : platformGroupAndKit.seqPlatformGroup,
                    libraryPreparationKit: platformGroupAndKit.libraryPreparationKit,
                    needsProcessing      : true,
                    requester            : requester,
            ]).save(flush: true)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#mwpToKeep.project, 'OTP_READ_ACCESS')")
    void selectMwpAsFinal(CellRangerMergingWorkPackage mwpToKeep) {
        if (mwpToKeep.project.state == Project.State.ARCHIVED || mwpToKeep.project.state == Project.State.DELETED) {
            throw new FileAccessForProjectNotAllowedException(
                    "Cannot set Mwp of ${mwpToKeep.project.state.name().toLowerCase()} project ${mwpToKeep.project} to final"
            )
        }
        List<CellRangerMergingWorkPackage> allMwps = getAllMwps(
                mwpToKeep.sample, mwpToKeep.seqType, mwpToKeep.config.programVersion, mwpToKeep.referenceGenomeIndex,
                CellRangerMergingWorkPackage.Status.UNSET
        )
        deleteMwps(allMwps - mwpToKeep)
        mwpToKeep.status = CellRangerMergingWorkPackage.Status.FINAL
        mwpToKeep.save(flush: true)
        cellRangerWorkflowService.correctFilePermissions(mwpToKeep.bamFileInProjectFolder as SingleCellBamFile)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#sample.project, 'OTP_READ_ACCESS')")
    void selectNoneAsFinal(Sample sample, SeqType seqType, String programVersion, ReferenceGenomeIndex reference) {
        if (sample.project.state == Project.State.ARCHIVED || sample.project.state == Project.State.DELETED) {
            throw new FileAccessForProjectNotAllowedException("Cannot delete Mwp of ${sample.project.state.name().toLowerCase()} project ${sample.project}")
        }
        deleteMwps(getAllMwps(sample, seqType, programVersion, reference, CellRangerMergingWorkPackage.Status.UNSET))
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#sample.project, 'OTP_READ_ACCESS')")
    void deleteFinalMwp(Sample sample, SeqType seqType, String programVersion, ReferenceGenomeIndex reference) {
        if (sample.project.state == Project.State.ARCHIVED  || sample.project.state == Project.State.DELETED) {
            throw new FileAccessForProjectNotAllowedException("Cannot delete Mwp of ${sample.project.state.name().toLowerCase()} project ${sample.project}")
        }
        deleteMwps(getAllMwps(sample, seqType, programVersion, reference, CellRangerMergingWorkPackage.Status.FINAL), false)
    }

    private List<CellRangerMergingWorkPackage> getAllMwps(Sample sample, SeqType seqType, String programVersion, ReferenceGenomeIndex reference,
                                                          CellRangerMergingWorkPackage.Status status) {
        return (CellRangerMergingWorkPackage.createCriteria().list {
            eq("sample", sample)
            eq("seqType", seqType)
            config {
                eq("programVersion", programVersion)
            }
            eq("referenceGenomeIndex", reference)
            eq("status", status)
        } as List<CellRangerMergingWorkPackage>)
    }

    void deleteMwps(List<CellRangerMergingWorkPackage> mwpToDelete, boolean checkFinal = true) {
        mwpToDelete.each {
            if (checkFinal) {
                assert it.status != CellRangerMergingWorkPackage.Status.FINAL
            }
            if (it.project.state == Project.State.ARCHIVED || it.project.state == Project.State.DELETED) {
                String projectState = it.project.state.name().toLowerCase()
                throw new FileAccessForProjectNotAllowedException("Cannot delete Mwp ${it} since project ${it.project} is ${projectState}.")
            }
        }
        mwpToDelete.each {
            it.status = CellRangerMergingWorkPackage.Status.DELETED
            it.save(flush: true)
            SingleCellBamFile.findAllByWorkPackage(it).each {
                cellRangerWorkflowService.deleteOutputDirectory(it)
            }
        }
    }

    void setInformedFlag(CellRangerMergingWorkPackage crmwp, Date date) {
        crmwp.informed = date
        crmwp.save(flush: true)
    }
}
