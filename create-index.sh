#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input collection -generator SLRGenerator -storeSLR -threads 1 -index index-collection
