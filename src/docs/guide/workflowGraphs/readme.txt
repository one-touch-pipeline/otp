How to create the plots with dot:
1) install 'graphviz' via 'yast' (graphviz contains dot)
2) command to create the plots:
   dot -Tpdf -o bwa_aln.pdf src/docs/guide/workflowGraphs/bwa_aln.dot && dot -Tpdf -o PanCan.pdf src/docs/guide/workflowGraphs/PanCan.dot
