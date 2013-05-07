# example call
# R -f plotInsertSize.r --no-save --no-restore --args "$BQ_ROOTPATH/$USER/insertSizePlot/output.hst" "ALL" "Insert size distribution ALL chromosomes" "Insert size" "frequency" "$BQ_ROOTPATH/$USER/insertSizePlot/output.png"

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
# 6. absolute filename, has to end with .png
outputfile = cmdArgs[6]

insertSizeData <- read.table(insertSizeInput, header = FALSE, sep = "\t")
png(outputfile)
plot(insertSizeData[insertSizeData$V1 == chr,][2:3], type="l", main=maintitle, xlab=xlabel, ylab=ylabel)
dev.off()

