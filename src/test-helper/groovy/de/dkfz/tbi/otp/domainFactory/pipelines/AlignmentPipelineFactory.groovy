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

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory

trait AlignmentPipelineFactory {

    static class CellRangerFactoryInstance implements CellRangerFactory {

        static final INSTANCE = new CellRangerFactoryInstance()
    }

    static class RoddyRnaFactoryInstance implements RoddyRnaFactory {

        static final INSTANCE = new RoddyRnaFactoryInstance()
    }


    private static final Map<Pipeline.Name, Closure<AbstractBamFile>> alignmentCreation = [
            (Pipeline.Name.DEFAULT_OTP)        : { DomainFactory.createProcessedMergedBamFile() },
            (Pipeline.Name.PANCAN_ALIGNMENT)   : { DomainFactory.createRoddyBamFile() },
            (Pipeline.Name.RODDY_RNA_ALIGNMENT): { RoddyRnaFactoryInstance.INSTANCE.createBamFile() },
            (Pipeline.Name.CELL_RANGER)        : { CellRangerFactoryInstance.INSTANCE.createBamFile() },
    ].asImmutable()

    AbstractBamFile createBamFileForPipelineName(Pipeline.Name pipeline) {
        assert pipeline?.type == Pipeline.Type.ALIGNMENT
        assert alignmentCreation.containsKey(pipeline): "Bam file creation for Pipeline ${pipeline} is not configured"
        Closure removeMeOnceCodeNarcIsUpdated = alignmentCreation[pipeline]
        return removeMeOnceCodeNarcIsUpdated()
    }
}
