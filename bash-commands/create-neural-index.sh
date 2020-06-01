#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input data/collection -generator SLRGenerator -storeSLR -neuralIndex -niDecimalPrecision 2 -threads 10 -memorybuffer 61440 -index data/collection-index-neural-2
