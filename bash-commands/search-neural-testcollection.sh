/bin/sh
sh ./target/appassembler/bin/SearchCollection -index data/testcollection-neuralindex-7 \
-topicreader TsvInt -topics topics/neuraltopics-test.txt -slr -slr.ip 7 -output data-results/run.collection-neural.sr.neuraltopics-test.txt
