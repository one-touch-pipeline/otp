import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(COMMAND_LOAD_MODULE_LOADER, "")

processingOptionService.createOrUpdate(COMMAND_ENABLE_MODULE, "module load")

// when updating fastqc, make sure that FastqcDataFilesService.fastqcFileNameWithoutZipSuffix is still correct
processingOptionService.createOrUpdate(COMMAND_FASTQC, "fastqc")
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_FASTQC, "module load fastqc/0.11.5")

processingOptionService.createOrUpdate(COMMAND_GROOVY, 'groovy')
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_GROOVY, 'module load groovy/2.4.15')

processingOptionService.createOrUpdate(COMMAND_ACTIVATION_JAVA, 'module load java/1.8.0_131')

processingOptionService.createOrUpdate(COMMAND_SAMTOOLS, 'samtools')
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_SAMTOOLS, 'module load samtools/1.2')

processingOptionService.createOrUpdate(COMMAND_ACTIVATION_R, "module load R/3.4.0")
processingOptionService.createOrUpdate(COMMAND_R, "Rscript")

processingOptionService.createOrUpdate(COMMAND_RUN_YAPSA, "runYAPSA.R")
