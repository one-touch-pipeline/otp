#!/usr/bin/env Rscript

suppressPackageStartupMessages(library("optparse"))
suppressPackageStartupMessages(library("tools"))

# specify our desired options in a list
# by default OptionParser will add an help option equivalent to
# make_option(c("-h", "--help"), action="store_true", default=FALSE,
#                help="Show this help message and exit")
option_list = list(
make_option(c("-v", "--verbose"), action="store_true", default = FALSE,
        help="Print extra output (disabled by default)"),
make_option(c("-m", "--mainTitle"), default = "Insert size distribution",
        help="Main title [default '%default']"),
make_option(c("-x", "--xLabel"), default = "Insert size",
        help="x axis label [default '%default']"),
make_option(c("-y", "--yLabel"), default = "Frequency",
        help="y axis label [default '%default']"),
make_option(c("-w", "--width"), type="integer", default = 800,
        help="plot width [default %default]", metavar = "width"),
make_option(c("-t", "--height"), type="integer", default = 600,
        help="plot height [default %default]", metavar = "height")
)
parser = OptionParser(usage = "%prog [options] inputFile.tsv chromosomeIdentifier outputFile.png", option_list = option_list)
arguments = parse_args(parser, positional_arguments = TRUE)
opt = arguments$options
if (length(arguments$args) != 3) {
    write("Incorrect number of required positional arguments\n\n", stderr())
    print_help(parser)
    stop()
} else {
    inputFile = arguments$args[1]
    chr = arguments$args[2]
    outputFile = arguments$args[3]
}
if (file.access(inputFile) == -1) {
    stop(write(paste("Specified file (", inputFile ,") does not exist"), stderr()))
}
if (file_ext(outputFile) != "png") {
    stop(write(paste("Specified file (", outputFile ,") has to end with '.png'"), stderr()))
}

mainTitle = opt$mainTitle
xLabel = opt$xLabel
yLabel = opt$yLabel
pngWidth = as.integer(opt$width)
pngHeight = as.integer(opt$height)
# print some progress messages to stderr if "quietly" wasn't requested
if (opt$verbose) {
    write("Optional parameters:", stdout())
    write(paste("mainTitle  : ", mainTitle), stdout())
    write(paste("xLabel     : ", xLabel), stdout())
    write(paste("yLabel     : ", yLabel), stdout())
    write(paste("pngWidth   : ", pngWidth), stdout())
    write(paste("pngHeight  : ", pngHeight), stdout())
}
# load data
insertSizeData <- read.table(inputFile, header = FALSE, sep = "\t")
# convert x labels from integer to character
xlabs <- as.character(insertSizeData[insertSizeData$V1 == chr,]$V2)
# delete every second label
xlabs[seq(length(xlabs)) %% 2 == 0] <- ''
# create png file
png(outputFile, width = pngWidth, height = pngHeight, units = "px")
# do ploting
mp <- barplot(height = insertSizeData[insertSizeData$V1 == chr,]$V3, space = 0, main = mainTitle, xlab = xLabel, ylab = yLabel)
# do labeling of bars:
# adj = c(x,y) controls label positioning
# cex = scaling factor for fontsize
text(mp, par("usr")[3], srt = 90, adj = c(1, 0.5), labels = xlabs, xpd = TRUE, cex = 0.7)
dev.off()
