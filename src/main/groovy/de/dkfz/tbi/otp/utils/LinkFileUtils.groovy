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
package de.dkfz.tbi.otp.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.Path


@Component
class LinkFileUtils {

    @Autowired
    FileService fileService

    @Autowired
    FileSystemService fileSystemService

    /**
     * Creates relative symbolic links.
     * Links which already exist are overwritten, parent directories are created automatically if necessary.
     * @param sourceLinkMap The values of the map are used as link names, the keys as the link targets.
     */
    void createAndValidateLinks(Map<File, File> sourceLinkMap, Realm realm, String unixGroup = '')  {
        assert sourceLinkMap
        assert realm

        if (!sourceLinkMap.isEmpty()) {
            FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

            sourceLinkMap.values().each {
                Path dir = fileService.toPath(it, fileSystem)
                fileService.deleteDirectoryRecursively(dir)
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(dir.parent, realm, unixGroup)
            }

            sourceLinkMap.each { File source, File link ->
                Path sourcePath = fileService.toPath(source, fileSystem)
                Path linkPath = fileService.toPath(link, fileSystem)
                fileService.createLink(linkPath, sourcePath, realm, CreateLinkOption.DELETE_EXISTING_FILE)
            }
        }
    }
}
