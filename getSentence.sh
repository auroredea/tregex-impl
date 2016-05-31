#!/bin/sh

# return sentences from list of tree on a file
# input : tree input file
# output : output file

java -mx900m -cp /home/sfl/Public/TregexGUI/tregex.jar edu.stanford.nlp.trees.tregex.TregexPattern -w -t "ROOT" $1 > $2