#/bin/sh
sh ./target/appassembler/bin/IndexCollection -collection JsonCollection -input data/collection -generator SLRGenerator -storeSLR -threads 8 -index data/collection-index
