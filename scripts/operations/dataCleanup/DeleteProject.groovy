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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * delete a complete project from the OTP database
 * run DeleteProjectContent.groovy first!
 */

// input area
//----------------------

String projectName = ""

//script area
//-----------------------------

assert projectName


try {
    // `flush: true` is intentionally left out at certain places to improve performance
    Project.withTransaction {

        Project project = exactlyOneElement(Project.findAllByName(projectName))

        /*
         * Deletes the connection of the project to the reference genome
         */
        ReferenceGenomeProjectSeqType.findAllByProject(project)*.delete()

        MergingCriteria.findAllByProject(project)*.delete()

        ProcessingThresholds.findAllByProject(project)*.delete()

        SampleTypePerProject.findAllByProject(project)*.delete()

        /*
         * Deletes the ProcessingOptions of the project
         */
        ProcessingOption.findAllByProject(project)*.delete(flush: true)

        ConfigPerProjectAndSeqType.findAllByProjectAndPreviousConfigIsNotNull(project)*.delete(flush: true)
        ConfigPerProjectAndSeqType.findAllByProject(project)*.delete(flush: true)

        UserProjectRole.findAllByProject(project)*.delete(flush: true)
        QcThreshold.findAllByProject(project)*.delete(flush: true)
        ProjectInfo.findAllByProject(project)*.delete(flush: true)

        /*
         * Finally delete the project
         */
        project.delete(flush: true)

        //just used while testing to make sure that the DB changes are rolled back -> otherwise each time a new DB dump has to be included
        assert false
    }
} catch (Throwable e) {
    e.printStackTrace(System.out)
}
