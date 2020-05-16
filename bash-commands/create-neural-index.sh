#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input data/collection -generator SLRGenerator -storeSLR -neuralIndex -niDecimalPrecision 1 -threads 8 -memorybuffer 40960 -index data/collection-index-neural-1
