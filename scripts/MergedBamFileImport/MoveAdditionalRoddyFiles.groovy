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

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException
import java.nio.file.*
import java.util.regex.*
import groovy.transform.Field
import groovy.util.*

/**
 * Works with: groovy-2.4.5 + Oracle JDK 1.8.0_45
 *
 * Usage: groovy MoveAdditionalRoddyFiles.groovy /path/to/example/project/PID_PREFIX*
 *
 * Assumptions:
 * * .bam and .bai files were already moved to their respective import directories
 * * There is a directory containing source PID subdirectories with files to move.
 * * The source PID directory also contains the link to the previously moved .bam file
 * * From the PID directories the subdirectories in roddyPathsToMove (see .below) should be moved.
 * * All merged BAMs contain the sample types as prefixes (as produced by Roddy).
 *
 * The directories in roddyPathsToMove may be shared for all sample types. Therefore they are moved
 * to the import directory of the "primary" sample type only (with the .bam and .bai files). The other
 * sample type directories only get links to the primary sample type directory (i.e. to the respective paths
 * in roddyPathsToMove). Additionally the source PID directories get links to the moved directories,
 * just like the .bam and .bai files. The primary sample type is determined as the first found sample type
 * in the list sampleTypes below.
 *
 * Missing features!!!!
 * * check all required permissions (creating, deleting files, etc.) before
 * * first copy, then, only if moving was successful, remove from source
 * * skip over directories that apparently are already moved to the target directory (with notification)
 * * set permissions of moved files in target directory
 * * maybe use "mv -i" (interactive move?)
 * * maybe remove still existing lane BAM files to obtain a minimal (only links) PID directory.
 */

def directories = this.args

// Add sample types in the order or priority. Order determines what primary sample types are (for the
// respective move.
@Field
List<String> sampleTypes = ["control",
                            "CONTROL",
                            "control_01",
                            "control_02",
                            "blood",
                            "tumor",
                            "tumor01",
                            "tumor02",
                            "tumor_lk01",
                            "tumor_lk02",
                            "tumor_01",
                            "tumor_02",
                            "TUMOR_01",
                            "TUMOR_02",
                            "metastasis",
                            "relapse"]

@Field
List<String> roddyPathsToMove = ["qualitycontrol",
                                 "roddyExecutionStore",
                                 ".roddyExecutionDirectory",
                                 ".roddyExecCache.txt"]

List<Path> listSubdirectories(Path inPath) {
    return Files.list(inPath).toArray().toList()
}

List<Path> getMergedBams(Path inPath) {
    return listSubdirectories(inPath).findAll { it =~ /_merged\.mdup\.bam$/ }
}

Path targetPathForLinkedBam(Path bamPath) {
    if (!Files.isSymbolicLink(bamPath)) {
        throw new NotLinkException("${bamPath} should be link! Didn't you import all merged BAMs?")
    }
    return bamPath.toRealPath().getParent()
}

String matchSampleTypeInMergedBam(Path bamFile) {
    Matcher sampleType = (bamFile =~ /.+\/((?:${sampleTypes.join("|")})_?\d*)_.+/)
    if (!sampleType) {
        throw new ParseException("Could not match sample type in ${bamFile}: Checked against ${sampleTypes}")
    }
    return sampleType[0][1]
}

/**
 * Return the first sampleType, that is found in the list of foundSampleTypes.
 * In the import directories sample types are in separate directories. The
 * addititional files will be moved to the first directory/sampleType only.
 */
String selectTargetSampleType(Collection<String> foundSampleTypes) {
    sampleTypes.find {
        foundSampleTypes.contains(it)
    }
}

String moveCommand(Path bamFile, Path targetDir, delim = " \\\n\t") {
    ['mv', bamFile.toString(), targetDir.toString()].join(delim)
}

String linkCommand(Path from, Path to, delim = " \\\n\t") {
    ['ln -s', from.toString(), to.toString()].join(delim)
}

void assertThatFileExists(Path path) {
    if (!path.toFile().exists()) {
        throw new NoSuchFileException(sourcePath)
    }
}

void assertThatFileDoesNotExist(Path path) { }

/**
 * Could be more transactional by linking commands via && and by first copying and only then (after successful copying
 * removing and relinking the original files.
 */
void printShellCommands(Path moveSourceParentPath,
                        Path moveTargetParentPath,
                        String targetSampleType,
                        Map<String, Path> linkTargetPathsBySampleType,
                        ArrayList<String> subDirNames) {
    assertThatFileExists(moveTargetParentPath)

    subDirNames.each { subDir ->

        Path sourcePath = Paths.get(moveSourceParentPath.toString(), subDir)
        assertThatFileExists(sourcePath)
        Path targetPath = Paths.get(moveTargetParentPath.toString(), subDir)
        assertThatFileDoesNotExist(targetPath)

        // Move subdirectories to the primary sample type directory (moveTargetPath).
        println(moveCommand(sourcePath, targetPath))

        // Link subdirectories in the source directory to the primary sample type directory.
        println(linkCommand(targetPath, Paths.get(moveSourceParentPath.toString(), subDir)))

        // Link subdirectories in the secondary sample type directories to the primary sample type directory.
        linkTargetPathsBySampleType.each { sampleType, linkTargetParentPath ->
            if (sampleType != targetSampleType) {
                Path sampleTypePath = Paths.get(linkTargetParentPath.toString(), subDir)
                assertThatFileDoesNotExist(sampleTypePath)
                println(linkCommand(targetPath, sampleTypePath))
            }
        }
    }
}

void assertThatSampleTypesAreUnique (List<ArrayList> sampleTypesAndBamFiles) {
    assert sampleTypesAndBamFiles.countBy { it[0] }.every { type, count -> count == 1 }
}

void moveAdditionalRoddyFilesInDirectory(Path sourceDir) {
    println("\n\n# sourcedir=" + sourceDir)
    List<Path> bamFiles = getMergedBams(sourceDir)

    List<ArrayList> sampleTypesAndBamFiles = bamFiles.collect {
        [matchSampleTypeInMergedBam(it), it]
    }

    // This indicates a parse error.
    assertThatSampleTypesAreUnique sampleTypesAndBamFiles

    Map<String,Path> targetPathsBySampleTypes = sampleTypesAndBamFiles.
            collectEntries { type, path ->
                [type, targetPathForLinkedBam(path)]
            }

    String sampleTypeToMove = selectTargetSampleType(targetPathsBySampleTypes.keySet())

    printShellCommands(
            sourceDir,
            targetPathsBySampleTypes[sampleTypeToMove],
            sampleTypeToMove,
            targetPathsBySampleTypes,
            roddyPathsToMove)
}

/*
Path directoryWithPidSubdirectories = Paths.get("/path/to/example/project")
Pattern pidSelectorPattern = ~/.*PID_PREFIX\d+.*//*

List<Path> matchPidDirectoriesToImport(Path from, Pattern pidPattern) {
    Files.list(from).toArray().findAll { it =~ pidPattern }.asList().sort()
}

// I am not sure how well this is adaptable to other projects.
for (directory in matchPidDirectoriesToImport(directoryWithPidSubdirectories, pidSelectorPattern)) {
    moveAdditionalRoddyFilesInDirectory(directory)
}
*/

println("#!/bin/bash -x")
this.args.each {
    moveAdditionalRoddyFilesInDirectory(Paths.get(it).toAbsolutePath())
}

