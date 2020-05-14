#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input collection -generator SLRGenerator -neuralIndex -threads 1 -index index-collection-neural
