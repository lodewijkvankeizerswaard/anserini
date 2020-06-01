#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input data/testcollection -generator SLRGenerator -appendSLR -SLRIndex -threads 1 -index data/testcollection-index-neural-3
