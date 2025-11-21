/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INSTRUMENT_MODEL
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INSTRUMENT_PLATFORM
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_READ_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.BASE_MATERIAL
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_KIT

@Integration
@Rollback
class GroupSeqPlatformValidatorIntegrationSpec extends Specification implements DomainFactoryCore {

    GroupSeqPlatformValidator groupSeqPlatformValidator

    private static String createMetadata(Project project, SeqType seqType, SeqPlatform seqPlatform) {
        return [
                [SAMPLE_NAME, SEQUENCING_TYPE, SEQUENCING_READ_TYPE, PROJECT, INSTRUMENT_PLATFORM, INSTRUMENT_MODEL, BASE_MATERIAL, SEQUENCING_KIT]*.name()
                        .join('\t'),
                ["sample1", seqType.name, "PAIRED", project.name, seqPlatform.name, seqPlatform.seqPlatformModelLabel, "", ""].join('\t'),
        ].join('\n')
    }

    void "should add problem when seqPlatform not in group and useSeqPlatformGroup USE_OTP_DEFAULT"() {
        given:
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqPlatform seqPlatform = createSeqPlatform()
        createMergingCriteria(
                seqType: seqType,
                project: project,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        )

        String metadata = createMetadata(project, seqType, seqPlatform)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.any { it.message.contains("does not belong to any sequencing platform group") }
    }

    void "should not fail when seqPlatform is in a default group and useSeqPlatformGroup USE_OTP_DEFAULT"() {
        given:
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqPlatform seqPlatform = createSeqPlatform()
        createMergingCriteria(
                seqType: seqType,
                project: project,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        )

        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup()
        seqPlatformGroup.addToSeqPlatforms(seqPlatform)
        seqPlatformGroup.save(flush: true)
        seqPlatform.refresh()

        String metadata = createMetadata(project, seqType, seqPlatform)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.empty
    }

    void "should add problem when seqPlatform not in group and useSeqPlatformGroup USE_PROJECT_SEQ_TYPE_SPECIFIC"() {
        given:
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqPlatform seqPlatform = createSeqPlatform()
        createMergingCriteria(
                seqType: seqType,
                project: project,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        )

        String metadata = createMetadata(project, seqType, seqPlatform)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.any { it.message.contains("does not belong to any sequencing platform group") }
    }

    void "should not add problem when seqPlatform not in group and useSeqPlatformGroup IGNORE_FOR_MERGING"() {
        given:
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqPlatform seqPlatform = createSeqPlatform()
        createMergingCriteria(
                seqType: seqType,
                project: project,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING,
        )

        String metadata = createMetadata(project, seqType, seqPlatform)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.empty
    }

    void "should not add problem when seqPlatform in group and useSeqPlatformGroup USE_PROJECT_SEQ_TYPE_SPECIFIC"() {
        given:
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        MergingCriteria mergingCriteria = createMergingCriteria(
                seqType: seqType,
                project: project,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        )

        SeqPlatform seqPlatform = createSeqPlatform()
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup(
                mergingCriteria: mergingCriteria,
        )
        seqPlatformGroup.addToSeqPlatforms(seqPlatform)
        seqPlatformGroup.save(flush: true)
        seqPlatform.refresh()

        String metadata = createMetadata(project, seqType, seqPlatform)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.empty
    }

    void "should not add problem when seqType not found in metadata"() {
        given:
        Project project = createProject()
        SeqPlatform seqPlatform = createSeqPlatform()
        String header = [SAMPLE_NAME, SEQUENCING_TYPE, SEQUENCING_READ_TYPE, PROJECT, INSTRUMENT_PLATFORM, INSTRUMENT_MODEL, BASE_MATERIAL, SEQUENCING_KIT]
                *.name().join('\t')
        String row = ["sample1", "UNKNOWN_SEQ_TYPE", "PAIRED", project.name, seqPlatform.name, seqPlatform.seqPlatformModelLabel, "", ""].join('\t')
        String metadata = [header, row].join('\n')
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.empty
    }

    void "should not add problem when seqPlatform not found in metadata"() {
        given:
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        createMergingCriteria(
                seqType: seqType,
                project: project,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING,
        )

        String header = [SAMPLE_NAME, SEQUENCING_TYPE, SEQUENCING_READ_TYPE, PROJECT, INSTRUMENT_PLATFORM, INSTRUMENT_MODEL, BASE_MATERIAL, SEQUENCING_KIT]*.name().join('\t')
        String row = ["sample1", seqType.name, "PAIRED", project.name, "UNKNOWN_PLATFORM", "UNKNOWN_MODEL", "", ""].join('\t')
        String metadata = [header, row].join('\n')
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.empty
    }

    void "should not add problem when merging criteria not found"() {
        given:
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqPlatform seqPlatform = createSeqPlatform()

        String metadata = createMetadata(project, seqType, seqPlatform)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(metadata)

        when:
        groupSeqPlatformValidator.validate(context)

        then:
        context.problems.empty
    }
}
