#/bin/sh
sh ./target/appassembler/bin/IndexCollection -collection JsonCollection -input data/msmarco-pas -slr -slr.append -threads 8 -index data/msmarco-pas-index
