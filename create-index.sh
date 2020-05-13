#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input testfiles -generator SLRGenerator -storeSLR -threads 1 -index testindex
