package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService

import java.nio.file.*
import java.util.stream.Collectors

class CellRangerWorkflowService {

    FileSystemService fileSystemService

    FileService fileService


    void linkResultFiles(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path workDirectory = fileSystem.getPath(singleCellBamFile.workDirectory.absolutePath)
        Path resultDirectory = fileSystem.getPath(singleCellBamFile.resultDirectory.absolutePath)

        singleCellBamFile.getFileMappingForLinks().each { String linkName, String resultPathName ->
            Path link = workDirectory.resolve(linkName)
            Path target = resultDirectory.resolve(resultPathName)
            if (!Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                fileService.createLink(link, target, singleCellBamFile.realm)
            }
        }
    }

    void cleanupOutputDirectory(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path outputDirectory = fileSystem.getPath(singleCellBamFile.outputDirectory.absolutePath)
        Path resultDirectory = fileSystem.getPath(singleCellBamFile.resultDirectory.absolutePath)

        List<Path> pathToDelete = Files.list(outputDirectory).collect(Collectors.toList())
        assert pathToDelete.remove(resultDirectory)

        pathToDelete.each {
            fileService.deleteDirectoryRecursively(it)
        }
        assert Files.exists(resultDirectory)
    }

    void correctFilePermissions(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path workDirectory = fileSystem.getPath(singleCellBamFile.workDirectory.absolutePath)
        fileService.correctPathPermissionRecursive(workDirectory)
    }
}
