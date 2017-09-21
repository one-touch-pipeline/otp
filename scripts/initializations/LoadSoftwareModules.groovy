import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(COMMAND_LOAD_MODULE_LOADER, ". /path/to/programs/sourcme")

processingOptionService.createOrUpdate(COMMAND_SAMTOOLS, "samtools")
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_SAMTOOLS, "module load samtools/1.2")

processingOptionService.createOrUpdate(COMMAND_FASTQC, "fastqc")
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_FASTQC, "module load fastqc/0.11.5")
