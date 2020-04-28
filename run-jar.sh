#/bin/sh
./target/appassembler/bin/SearchCollection -index lucene-index.msmarco-doc.pos+docvectors+rawdocs -topicreader TsvInt -topics topics.msmarco-doc.dev.txt -sr -output run.msmarco-doc.sr.topics.msmarco-doc.dev.txt
