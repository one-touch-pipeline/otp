# Creates a coverage plot for a single sample
# Getting all args because it is necessary to get the path to the executing file to be able to source an additional library, all arguments are considered
cmdArgs = commandArgs(trailingOnly = F)
fileArgument = "--file="
scriptPath = dirname(sub(fileArgument, "", cmdArgs[grep(fileArgument, cmdArgs)]))
# Includes required functions
source(paste(scriptPath, "coveragePlotGenomeLib.R", sep = "/"))
numberOfParamsExpected = 2
# Index needed to only consider the arguments after "--args"
beginArgsIndexes = which(cmdArgs == "--args")
numberOfParams = length(cmdArgs) - beginArgsIndexes
if (numberOfParams != numberOfParamsExpected) {
    errorMsg = paste("Incorrect number of arguments (", numberOfParamsExpected," expected): ", numberOfParams, sep="")
    exampleMsg = paste("Required arguments: pathToData pathToOutputPlot.png",
    "",
    "Example: ",
    "Rscript coveragePlot.R \"./control_PID_merged.bam.rmdup_readCoverage_1kb_windows.txt\" \"./control_PID_merged.bam.rmdup_readCoverage_1kb_windows.png\"",
    sep = "\n")
    fullMsg = cat(errorMsg, exampleMsg, sep = "\n")
    print(fullMsg)
    q()
}
filename = cmdArgs[beginArgsIndexes + 1]
# TODO use optparse to allow optional parameters
countType = "read"
outputPath = cmdArgs[beginArgsIndexes + 2]
sampleName = basename(filename)
#input coverage data
sampleRaw = read.table(filename, sep = "\t", as.is = TRUE)
# validating the window size
windowSizeValues = sampleRaw[,2]
windowSize = sampleRaw[2,2] - sampleRaw[1,2]
sampleSplitByChromosome = splitsByChromosomeAndNameColumns(sampleRaw)
# Start PNG device driver to save output to figure.png
png(outputPath, width = 2100, height = 1000)
# create a grid for the images and setting number of images ( the total number of images in this case = 3 x 1 = 3, and margins)
par(mfrow = c(3, 1), mar = c(2, 4, 2, 2))
# plot coverage for whole genome
yValue = 17
plotChromosomes(sampleName, sampleRaw, yValue)
drawPlotGridAndLabels(sampleSplitByChromosome, yValue)
graphics.off()
