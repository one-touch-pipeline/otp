/*
 * Copyright 2011-2020 The OTP authors
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

package operations.dataCleanup.admin

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * delete a complete project from the OTP database
 * AND/OR
 * removes the complete content of a project from the OTP database
 * but not the files in the file-system. They are needed to include again to OTP.
 * When they are in OTP again, it is easy to remove the project manually from the filesystem.
 */

// input area
//----------------------

String projectName = ""

//script area
//-----------------------------

assert projectName : "No project name given"

Project.withTransaction {
    Project p = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName))
    assert p : "No project with the provided name could be found"

    assert SeqTrack.createCriteria().list {
        sample {
            individual {
                eq("project", p)
            }
        }
    }.isEmpty() : "The project contains already data"

    assert AbstractMergedBamFile.createCriteria().list {
        workPackage {
            sample {
                individual {
                    eq("project", p)
                }
            }
        }
    }.isEmpty() : "The project contains already data"

    String out = "rm -rf ${p.getProjectDirectory()}\nrm -rf ${p.dirAnalysis}"
    ctx.deletionService.deleteProject(p)
    println "Execute the following line on the filesystem:"
    println out
    assert false : "DEBUG: transaction intentionally failed to rollback changes"
}
