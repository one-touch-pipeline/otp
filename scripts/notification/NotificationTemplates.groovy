import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.tracking.*

ProcessingOptionService processingOptionService = ctx.processingOptionService


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_BASE,
        null,
        null,
        '''
Dear user,

${stepInformation}${seqCenterComment}

If you want to get access to OTP, please contact the ODCF service team.
${addition}

Best regards,
OTP
${phabricatorAlias}
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_INSTALLATION,
        null,
        null,
'''The FASTQ files of run(s) ${runs} have been installed.
They are available here:
${paths}

Samples:
${samples}

An overview of all processed samples is shown on
${links}
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_INSTALLATION_FURTHER_PROCESSING,
        null,
        null,
'''The samples marked with [A] will be aligned by OTP. As soon as the alignment is finished, OTP will send you another notification e-mail.
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT,
        null,
        null,
'''The FASTQ files of the following samples have been aligned and merged:
${samples}

You can check their quality here:
${links}

The BAM files were produced with the following configuration(s):
${processingValues}

You can find the merged BAM files in the "view-by-pid" folder:
${paths}
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT_FURTHER_PROCESSING,
        null,
        null,
        '''For the following sample pairs variants will be called by OTP:
${samplePairsWillProcess}
OTP executes the following variant calling pipeline(s): ${variantCallingPipelines}
As soon as a variant calling is finished, OTP will send you another notification e-mail.
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT_NO_FURTHER_PROCESSING,
        null,
        null,
'''The following sample pairs will not been variant-called, most likely because they have not reached a threshold yet:
${samplePairsWontProcess}
If you want these sample pairs to be processed nevertheless, please contact the ODCF service team.
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT_PROCESSING,
        null,
        null,
        '''${seqType} ${individuals ? "("+individuals+")" : ""}
    Reference genome: ${referenceGenome}
    Alignment program: ${alignmentProgram} ${alignmentParameter}
    Merging/duplication marking program: ${mergingProgram} ${mergingParameter}
    SAMtools program: ${samtoolsProgram}
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_SNV_PROCESSED,
        null,
        null,
'''The SNV calling for following sample pairs is finished:
${samplePairsFinished}

For the SNV calling you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_INDEL_PROCESSED,
        null,
        null,
'''The Indel calling for following sample pairs is finished:
${samplePairsFinished}

For the Indel calling you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ACESEQ_PROCESSED,
        null,
        null,
        '''The CNV calling from ACEseq for following sample pairs is finished:
${samplePairsFinished}

For the CNV calling from ACEseq you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_SOPHIA_PROCESSED,
        null,
        null,
        '''The SV calling from SOPHIA for following sample pairs is finished:
${samplePairsFinished}

For the SV calling from SOPHIA you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_SNV_NOT_PROCESSED,
        null,
        null,
'''The following sample pairs have not been SNV-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the ODCF service team.
'''
)


println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_INDEL_NOT_PROCESSED,
        null,
        null,
'''The following sample pairs have not been Indel-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the ODCF service team.
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ACESEQ_NOT_PROCESSED,
        null,
        null,
        '''The following sample pairs have not been CNV-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the ODCF service team.
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_SOPHIA_NOT_PROCESSED,
        null,
        null,
        '''The following sample pairs have not been SV-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the ODCF service team.
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ADDITION,
        OtrsTicket.ProcessingStep.SOPHIA.notificationSubject,
        null,
        '''
SV calling is done by SOPHIA, an unpublished algorithm developed by Umut Toprak, Division of Theoretical Bioinformatics. For questions regarding the output and its interpretation please contact u.toprak@dkfz.de
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_ADDITION,
        OtrsTicket.ProcessingStep.ACESEQ.notificationSubject,
        null,
        '''
CNV calling is done by ACEseq developed by Kortine Kleinheinz, Division of Theoretical Bioinformatics. For questions regarding the output and its interpretation a documentation can be found here: http://aceseq.readthedocs.io/
'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_QC_TRAFFIC_BLOCKED_SUBJECT,
        Pipeline.Type.ALIGNMENT.name(),
        null,
        '''QC issues for bam file of ${roddyBamFile.sample} ${roddyBamFile.seqType}'''
)

println processingOptionService.createOrUpdate(
        OptionName.NOTIFICATION_TEMPLATE_QC_TRAFFIC_BLOCKED_MESSAGE,
        Pipeline.Type.ALIGNMENT.name(),
        null,
        '''\
Dear user,

There are QC issues for the bam file of ${roddyBamFile.sample} ${roddyBamFile.seqType} in project ${roddyBamFile.project}.
Further processing of this sample was stopped and the result files were not linked in the usual structure.

Please check out the result files on the filesystem: ${roddyBamFile.workDirectory}
or directly on our GUI where the problematic fields are highlighted:
${link}

In case the QC issues are not problematic please mark the bam file in the GUI as valid and provide a reason in the comment field.
As soon as the bam file is accepted the different variant callings are triggered automatically.
In the other case please mark them as rejected and also provide a reason in the comment field.

In case you have any questions please do not hesitate to contact us.


Best regards,
OTP
'''
)
