/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*

trait IsRoddy implements IsPipeline {

    @Override
    MergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true) {
        Pipeline pipeline = properties.pipeline ?: findOrCreatePipeline()
        return createDomainObject(MergingWorkPackage, baseMergingWorkPackageProperties(properties) + [
                seqType         : { createSeqType() },
                pipeline        : pipeline,
                statSizeFileName: {
                    pipeline?.name == Pipeline.Name.PANCAN_ALIGNMENT ? "statSizeFileName_${nextId}.tab" : null
                },
        ], properties, saveAndValidate)
    }

    @Override
    RoddyBamFile createBamFile(Map properties = [:]) {
        return createRoddyBamFile(properties, RoddyBamFile)
    }

    public <T> T createRoddyBamFile(Map properties = [:], Class<T> clazz) {
        MergingWorkPackage workPackage = properties.workPackage
        if (!workPackage) {
            Map workPackageProperties = properties.seqTracks ?
                    [
                            seqType: properties.seqTracks.first().seqType,
                            sample : properties.seqTracks.first().sample,
                    ] : [:]
            workPackage = createMergingWorkPackage(workPackageProperties)
            DomainFactory.createReferenceGenomeProjectSeqType(
                    referenceGenome : workPackage.referenceGenome,
                    project         : workPackage.project,
                    seqType         : workPackage.seqType,
                    statSizeFileName: workPackage.statSizeFileName,
            )
        }
        return createRoddyBamFile(properties, workPackage, clazz)
    }

    public <T> T createRoddyBamFile(Map properties = [:], MergingWorkPackage workPackage, Class<T> clazz) {
        createMergingCriteriaLazy(
                project: workPackage.project,
                seqType: workPackage.seqType,
        )

        Collection<SeqTrack> seqTracks = properties.seqTracks ?: [DomainFactory.createSeqTrackWithFastqFiles(workPackage)]
        workPackage.seqTracks = seqTracks
        workPackage.save(flush: true)

        T bamFile = createDomainObject(clazz, bamFileDefaultProperties(properties, seqTracks, workPackage) +
                [
                workDirectoryName           : "${RoddyBamFileService.WORK_DIR_PREFIX}_${nextId}",
                identifier                  : RoddyBamFile.nextIdentifier(workPackage),
                config                      : {
                    findOrCreateConfig(
                            pipeline: workPackage.pipeline,
                            project: workPackage.project,
                            seqType: workPackage.seqType,
                            adapterTrimmingNeeded: workPackage.seqType.isRna() || workPackage.seqType.isWgbs() || workPackage.seqType.isChipSeq(),
                    )
                },
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ], properties)
        return bamFile
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : SeqTypeNames.WHOLE_GENOME.seqTypeName,
                displayName  : 'WGS',
                dirName      : 'whole_genome_sequencing',
                roddyName    : 'WGS',
                libraryLayout: SequencingReadType.PAIRED,
                singleCell   : false,
        ]
    }

    @Override
    Map getConfigProperties(Map properties) {
        return DomainFactory.createRoddyWorkflowConfigMapHelper(properties)
    }

    @SuppressWarnings('GetterMethodCouldBeProperty')
    @Override
    Class getConfigPerProjectAndSeqTypeClass() {
        return RoddyWorkflowConfig
    }

    @Override
    Pipeline findOrCreatePipeline() {
        return findOrCreatePipeline(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }
}
