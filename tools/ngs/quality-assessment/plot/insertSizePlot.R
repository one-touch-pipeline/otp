# example call
# R -f insertSizePlot.R --no-save --no-restore --args "$BQ_ROOTPATH/$USER/insertSizePlot/output.hst" "ALL" "Insert size distribution ALL chromosomes" "Insert size" "frequency" "$BQ_ROOTPATH/$USER/insertSizePlot/output.png" "800" "600"

cmdArgs = commandArgs(TRUE)
print(cmdArgs)
### list of parameters
# 1. absolute filename for textfile to use as input
insertSizeInput = cmdArgs[1]
# 2. which chromosome to plot insertSize for
chr = cmdArgs[2]
# 3. main title in plot to be used
maintitle = cmdArgs[3]
# 4. xlabel
xlabel = cmdArgs[4]
# 5. ylabel
ylabel = cmdArgs[5]
# 6. absolute filename, should end with .png
outputfile = cmdArgs[6]
# 7. png width
pngWidth = as.integer(cmdArgs[7])
# 8. png height
pngHeight = as.integer(cmdArgs[8])

insertSizeData <- read.table(insertSizeInput, header = FALSE, sep = "\t")
png(outputfile, width = pngWidth, height = pngHeight, units = "px")
# do ploting
mp <- barplot(height = insertSizeData[insertSizeData$V1 == "ALL",]$V3, space = 0, main = maintitle, xlab = xlabel, ylab = ylabel)
# do labeling of bars:
# adj = c(x,y) controls label positioning
# cex = scaling factor for fontsize
text(mp, par("usr")[3], srt = 0, adj = c(0.5, 1), labels = insertSizeData[insertSizeData$V1 == chr,]$V2, xpd = TRUE, cex = 0.8)
dev.off()

