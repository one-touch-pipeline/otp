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
package de.dkfz.tbi.otp.withdraw

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

@CompileDynamic
@Transactional
abstract class AbstractWithdrawBamFileService<E extends AbstractBamFile> implements ProcessingWithdrawService<E, SeqTrack> {

    static final String NON_OTP = 'nonOTP'

    AbstractBamFileService abstractBamFileService
    DeletionService deletionService
    FileSystemService fileSystemService

    @Override
    abstract List<E> collectObjects(List<SeqTrack> entities)

    @Override
    List<String> collectPaths(List<E> entities) {
        return entities.collectMany {
            Path path = abstractBamFileService.getBaseDirectory(it)
            if (Files.exists(path)) {
                Stream<Path> stream
                try {
                    stream = Files.list(path)
                    return stream.findAll { it.fileName.toString() != NON_OTP }
                } catch (IOException e) {
                    throw new WithdrawnException(e)
                } finally {
                    stream?.close()
                }
            }
            return []
        }.unique()*.toString()
    }

    @Override
    void withdrawObjects(List<E> entities) {
        entities.each {
            it.withdrawn = true
            it.save(flush: true)
        }
    }

    @Override
    void unwithdrawObjects(List<E> entities) {
        entities.each {
            it.withdrawn = false
            it.save(flush: true)
        }
    }

    @Override
    void deleteObjects(List<E> entities) {
        entities.collectMany { E bamFile ->
            bamFile.containedSeqTracks
        }.unique().each {
            deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(it)
        }
    }
}
