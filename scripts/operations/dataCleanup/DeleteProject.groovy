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

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Delete of a project or only parts.
 *
 * The scripts supports the check for empty projects, which is enabled by default. In that case the deletion is stopped, if the project already contains
 * fastq files or bam files. Other data like projectInfos, dta's or samples are allowed in empty projects.
 *
 * There scripts supports two modes:
 *   - DELETE_ALL: Delete the project in OTP and delete the entire project directory
 *   - DELETE_SEQUENCING_ONLY: Delete the project data in OTP. That are patient and all depending data in OTP and the sequencing directory in the file system.
 *     the project itself, projectInfos, dta and workflow configuration are kept.
 *
 * Additional it is possible to include the analyses directory in the delete script.
 *
 * Since many project/analysis directories are linked, the delete script create a rm for the path itself and also for the realpath of that.
 */

// input area
// ----------------------

/**
 * The name of the project for deletion.
 */
String projectName = ""

/**
 * the absolute file name for the created script
 */
String pathName = ''

/**
 * The deletion mode: complete project or only the data inside the project
 */
ProjectDeletionMode mode =
        ProjectDeletionMode.DELETE_ALL // delete the complete project
        // ProjectDeletionMode.DELETE_SEQUENCING_ONLY // delete only the data of the project, but not the project itself

/**
 * checks that the project is empty. That means that it do not contain any fastq or bam files.
 * If the check is true and the project contains fastq or bam files, the scripts stops without doing anything.
 */
boolean assertProjectEmpty = true

/**
 * Should the delete bash script also contain code to delete analysis directory of the project?
 */
boolean deleteAnalysisDirectory = true

// script area
// -----------------------------

enum ProjectDeletionMode {
    DELETE_ALL, DELETE_SEQUENCING_ONLY
}

DeletionService deletionService = ctx.deletionService
ProjectService projectService = ctx.projectService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

FileSystem fileSystem = fileSystemService.remoteFileSystem
Path outputFile = fileSystem.getPath(pathName)

fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(outputFile.parent)

List<String> output = []

try {
    Project.withTransaction {
        Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName), "No project with the provided name could be found")
        String absoluteProjectDirectory = projectService.getProjectDirectory(project)
        String uUIDFolders = deletionService.formatRemoveUUIDFolders(project).join("\n")

        if (assertProjectEmpty) {
            assert !projectHasDataDependencies(project): "The project contains data, disable `assertProjectEmpty` to override this check"
        }

        switch (mode) {
            case ProjectDeletionMode.DELETE_ALL:
                output << """\
                |# Deletion of Project Directory:
                |rm -rf ${absoluteProjectDirectory}/*
                |
                |## In the rare case that the project directory is linked:
                |rm -rf "`readlink -f ${absoluteProjectDirectory}`"
                |
                |## UUID Directories:
                ${uUIDFolders}""".stripMargin()
                output << deleteDirectoryIfFlagSet(deleteAnalysisDirectory, project.dirAnalysis)
                deletionService.deleteProject(project)
                break
            case ProjectDeletionMode.DELETE_SEQUENCING_ONLY:
                output << """\
                |# Sequence Directory:
                |rm -rf ${absoluteProjectDirectory}/sequencing/
                |
                |## UUID Directories:
                ${uUIDFolders}""".stripMargin()
                output << deleteDirectoryIfFlagSet(deleteAnalysisDirectory, project.dirAnalysis)
                deletionService.deleteProjectContent(project)
                break
            default:
                assert false: "no mode selected"
        }

        println "Execute the following line:"
        println output.join("\n\n")

        outputFile.text = output.join('\n\n')

        assert false: "DEBUG: transaction intentionally failed to rollback changes"
    }
} catch (Throwable t) {
    output << 'Execution failed'
    output << t.message
    output << StackTraceUtils.getStackTrace(t)

    println output.join("\n\n")
    outputFile.text = output.join('\n\n')
    throw t
}

String deleteDirectoryIfFlagSet(boolean deleteAnalysisDirectory, String directoryPath) {
    if (deleteAnalysisDirectory && directoryPath) {
        return """\
                    |# Deletion of Analysis Directory:
                    |rm -rf ${directoryPath}/*
                    |
                    |## In the rare case that the project directory is linked:
                    |rm -rf "`readlink -f ${directoryPath}`"
                    """.stripMargin()
    }
    return ""
}

boolean projectHasDataDependencies(Project project) {
    boolean lanesFound = SeqTrack.withCriteria {
        sample {
            individual {
                eq("project", project)
            }
        }
    }

    boolean bamsFound = AbstractBamFile.withCriteria {
        workPackage {
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
    }

    return lanesFound || bamsFound
}

''
