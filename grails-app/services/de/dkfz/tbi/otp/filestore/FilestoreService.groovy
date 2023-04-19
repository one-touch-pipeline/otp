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

import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Service for operations on an UUID based file structure
 */
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
        // check if the work folder has been attached to a workflowRun
        List<WorkflowRun> attachedRuns = WorkflowRun.findAllByWorkFolder(workFolder)
        if (!attachedRuns.isEmpty()) {
            throw new WorkFolderAttachedException("WorkFolder ${workFolder} has been attached to WorkflowRun ${attachedRuns[0]}")
        }

        run.workFolder = workFolder
        run.save(flush: true)
    }

    /**
     * Returns a valid path based on the base folder and the uuid
     * Since the uuid is separated with dash into 5 parts, the path after base folder
     * is also segmented into 5 subfolders
     * For this UUID a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11 a folder structure like below is created:
     * base_folder/a0eebc99/9c0b/4ef8/bb6d/6bb9bd380a11
     *
     * @param a workflowRun
     * @return a valid path
     */
    Path getWorkFolderPath(WorkflowRun run) {
        // construct the folder
        List<String> uuidFragments = run.workFolder.uuid.toString().split(WorkFolder.UUID_SEPARATOR)
        return Paths.get(run.workFolder.baseFolder.path, uuidFragments.toArray(new String[0]))
    }
}
