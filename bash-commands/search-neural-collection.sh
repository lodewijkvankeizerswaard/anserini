#/bin/sh
sh ./target/appassembler/bin/SearchCollection -index data/collection-index-neural-1 -topicreader TsvInt -topics topics/neuraltopics-test.txt -sr -sr.ip 1 -output data-results/run.collection-neural.sr.neuraltopics-test.txt
