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
package de.dkfz.tbi.otp.filestore

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.Path

/**
 * Service for operations on an UUID based file structure
 */
@Slf4j
class FilestoreService {

    private final static int INDEX_0 = 0
    private final static int INDEX_1 = 1
    private final static int INDEX_2 = 2

    private final static int OFFSET_0 = 0
    private final static int OFFSET_2 = 2
    private final static int OFFSET_4 = 4

    FileSystemService fileSystemService
    FileService fileService

    /**
     * returns a writable {@link BaseFolder}.
     *
     * If multiple exist, one of them are returned
     */
    BaseFolder findAnyWritableBaseFolder() {
        return CollectionUtils.exactlyOneElement(BaseFolder.findAllWhere([
                writable: true,
        ], [
                max: 1,
        ])) as BaseFolder
    }

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

        // UUID is split into 3 parts
        String[] uuidFragments = new String[3]
        uuidFragments[INDEX_0] = uuidString.substring(OFFSET_0, OFFSET_2)
        uuidFragments[INDEX_1] = uuidString.substring(OFFSET_2, OFFSET_4)
        uuidFragments[INDEX_2] = uuidString.substring(OFFSET_4)

        return fileSystemService.remoteFileSystem.getPath(workFolder.baseFolder.path, uuidFragments)
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
        assert !run.workFolder : "work folder already attached and may not be changed"
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

    /**
     * Returns a list of all WorkFolders for a given Project
     */
    @CompileDynamic
    List<WorkFolder> getWorkFolders(Project project) {
        return WorkflowRun.createCriteria().list {
            eq('project', project)
            isNotNull('workFolder')
            projections {
                property('workFolder')
            }
        } as List<WorkFolder>
    }

    /**
     * Deletes the WorkflowFolder on the filesystem
     */
    void deleteWorkFolderOnFileSystem(WorkflowRun run) {
        WorkFolder workFolder = run.workFolder
        if (!workFolder) {
            throw new WorkFolderNotAttachedException("WorkflowRun ${run} has no workFolder attached and no path can be found")
        }
        Path path = getWorkFolderPath(workFolder)
        fileService.deleteDirectoryRecursively(path)
        markWorkFolderAsDeleted(workFolder)
        log.info("Deleted ${path} for WorkflowRun ${run}")
    }

    /**
     * Sets the workFolder to deleted and the size to 0
     */
    void markWorkFoldersAsDeleted(List<WorkFolder> workFolders) {
        workFolders.each { workFolder ->
            markWorkFolderAsDeleted(workFolder)
        }
    }

    private void markWorkFolderAsDeleted(WorkFolder workFolder) {
        workFolder.size = 0
        workFolder.deleted = true
        workFolder.save(flush: true)
    }
}
