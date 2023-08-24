/*
 * Copyright 2011-2019 The OTP authors
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
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils

/**
 * This trait provides the interface to generate testdata for all alignment-like workflows.
 * New pipelines should provide their own implementation. When implementing the trait methods,
 * implementations should return, wherever possible, the most specific subclass that applies to that pipeline,
 * not the Abstract base class.
 */
trait IsPipeline implements DomainFactoryCore {

    abstract AbstractMergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true)

    abstract AbstractBamFile createBamFile(Map properties = [:])

    abstract Map getSeqTypeProperties()

    abstract Map getConfigProperties(Map properties)

    abstract Class getConfigPerProjectAndSeqTypeClass()

    abstract Pipeline findOrCreatePipeline()

    ConfigPerProjectAndSeqType createConfig(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(configPerProjectAndSeqTypeClass, getConfigProperties(properties), properties, saveAndValidate)
    }

    ConfigPerProjectAndSeqType findOrCreateConfig(Map properties = [:], boolean saveAndValidate = true) {
        return findOrCreateDomainObject(configPerProjectAndSeqTypeClass, getConfigProperties(properties), properties, saveAndValidate)
    }

    @Override
    SeqType createSeqType(Map properties = [:], boolean saveAndValidate = true) {
        findOrCreateDomainObject(SeqType, seqTypeProperties, properties ?: seqTypeProperties, saveAndValidate).refresh()
    }

    Pipeline findOrCreatePipeline(Pipeline.Name name, Pipeline.Type type) {
        return findOrCreateDomainObject(Pipeline, [:], [
                name: name,
                type: type,
        ])
    }

    Map<String, ?> baseMergingWorkPackageProperties(Map properties) {
        return DomainFactory.baseMergingWorkPackageProperties(properties)
    }

    Map<String, ?> bamFileDefaultProperties(Map properties, Collection<SeqTrack> seqTracks, AbstractMergingWorkPackage workPackage) {
        return [
                numberOfMergedLanes: seqTracks.size(),
                seqTracks          : seqTracks as Set,
                workPackage        : workPackage,
                md5sum             : {
                    (!properties.containsKey('fileOperationStatus') ||
                            properties.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED) ? HelperUtils.randomMd5sum : null
                },
                fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
                fileSize           : 10000,
                comment            : {
                    [
                            AbstractBamFile.QcTrafficLightStatus.BLOCKED,
                            AbstractBamFile.QcTrafficLightStatus.ACCEPTED,
                            AbstractBamFile.QcTrafficLightStatus.REJECTED,
                    ].contains(properties.qcTrafficLightStatus) ? DomainFactory.createComment() : null
                },
        ]
    }
}
