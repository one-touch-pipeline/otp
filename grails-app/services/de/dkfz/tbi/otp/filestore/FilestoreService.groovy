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
package de.dkfz.tbi.otp.filestore

import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Service for operations on an UUID based file structure
 */
@Slf4j
class FilestoreService {

    /**
     * Create a WorkFolder with assigned BaseFolder, which can be assigned to WorkFlowRun
     *
     * @param baseFolder is a BaseFolder
     * @return the newly created WorkFolder
     */
    WorkFolder createWorkFolder(BaseFolder baseFolder) {
        UUID uuid = UUID.randomUUID()
        WorkFolder workFolder = new WorkFolder([
                baseFolder: baseFolder,
                uuid      : uuid,
                size      : 0,
        ])
        workFolder.save(flush: true)

        return workFolder
    }

    /**
     * Returns a valid path based on the base folder and the uuid
     * uuid is segmented into 3 subfolders starting with a subfolder with 2 hex digits, then followed with another 2 digits
     * then the rest of the uuid text. The hyphen is included into the folder names
     * For this UUID a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11 a folder structure like below is created:
     * base_folder/a0/ee/bc99-9c0b-4ef8-bb6d-6bb9bd380a11
     *
     * @param workFolder is a workFolder
     * @return a valid path
     */
    Path getWorkFolderPath(WorkFolder workFolder) {
        String uuidString = workFolder.uuid

        // UUID is splitted into 3 parts
        String[] uuidFragments = new String[3]
        uuidFragments[0] = uuidString.substring(0, 2)
        uuidFragments[1] = uuidString.substring(2, 4)
        uuidFragments[2] = uuidString.substring(4)

        return Paths.get(workFolder.baseFolder.path, uuidFragments)
    }
    /**
     * Create an UUID and assign to the given {@link de.dkfz.tbi.otp.workflowExecution.WorkflowRun}
     *
     * @param run is a WorkflowRun
     * @return the newly created UUID
     */
    UUID createUuid(WorkflowRun run) {
        run.workFolder.uuid = UUID.randomUUID()
        run.save(flush: true)

        return run.workFolder.uuid
    }

    /**
     * Attach the given work folder to the {@link de.dkfz.tbi.otp.workflowExecution.WorkflowRun}
     *
     * @param run is a WorkflowRun
     * @param workFolder is a WorkFolder
     */
    void attachWorkFolder(WorkflowRun run, WorkFolder workFolder) {
        if (!run.workFolder) {
            log.warn("WorkFolder ${run.workFolder} has been attached to this WorkflowRun ${run}, which will be overwritten by ${workFolder} ")
            // data in the old folder should be moved to the new location and delete the old folder?
        }
        run.workFolder = workFolder
        run.save(flush: true)
    }

    /**
     * Return a valid path back if its workFolder is attached, otherwise
     * an WorkFolderNotAttachedException is thrown
     * @param run is a WorkflowRun object
     * @return a valid Path
     */
    Path getWorkFolderPath(WorkflowRun run) {
        if (!run.workFolder) {
            throw new WorkFolderNotAttachedException("WorkflowRun ${run} has no workFolder attached and no path can be found")
        }
        return getWorkFolderPath(run.workFolder)
    }
}
