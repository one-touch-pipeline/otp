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
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory

trait AlignmentPipelineFactory {

    static class CellRangerFactoryInstance implements CellRangerFactory {

        static final CellRangerFactoryInstance INSTANCE = new CellRangerFactoryInstance()
    }

    static class RoddyPanCancerFactoryInstance implements RoddyPanCancerFactory {

        static final RoddyPanCancerFactoryInstance INSTANCE = new RoddyPanCancerFactoryInstance()
    }

    static class RoddyRnaFactoryInstance implements RoddyRnaFactory {

        static final RoddyRnaFactoryInstance INSTANCE = new RoddyRnaFactoryInstance()
    }

    private static final Map<Pipeline.Name, Map<String, Closure<?>>> ALIGNMENT_CREATION = [
            (Pipeline.Name.PANCAN_ALIGNMENT)   : [
                    (AbstractBamFile)           : { RoddyPanCancerFactoryInstance.INSTANCE.createBamFile() },
                    (AbstractMergingWorkPackage): { RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage() },
            ],
            (Pipeline.Name.RODDY_RNA_ALIGNMENT): [
                    (AbstractBamFile)           : { RoddyRnaFactoryInstance.INSTANCE.createBamFile() },
                    (AbstractMergingWorkPackage): { RoddyRnaFactoryInstance.INSTANCE.createMergingWorkPackage() },
            ],
            (Pipeline.Name.CELL_RANGER)        : [
                    (AbstractBamFile)           : { CellRangerFactoryInstance.INSTANCE.createBamFile() },
                    (AbstractMergingWorkPackage): { CellRangerFactoryInstance.INSTANCE.createMergingWorkPackage() },
            ],
    ].asImmutable()

    private <C> C createObject(Pipeline.Name pipeline, Class<C> closureType) {
        assert pipeline?.type == Pipeline.Type.ALIGNMENT
        assert ALIGNMENT_CREATION.containsKey(pipeline): "${closureType} creation for Pipeline ${pipeline} is not configured"
        assert ALIGNMENT_CREATION[pipeline].containsKey(closureType): "${closureType} for ${pipeline}  is not configured"
        Closure closure = ALIGNMENT_CREATION[pipeline][closureType]
        return closure()
    }

    AbstractBamFile createBamFileForPipelineName(Pipeline.Name pipeline) {
        return createObject(pipeline, AbstractBamFile)
    }

    MergingWorkPackage createMergingWorkPackageForPipelineName(Pipeline.Name pipeline) {
        return createObject(pipeline, AbstractMergingWorkPackage)
    }
}
