#/bin/sh
sh target/appassembler/bin/IndexCollection \
-collection JsonCollection \
-input data/robust04_json_test \
-generator SLRGenerator \
-slr \
-slr.index \
-slr.decimalPrecision 7 \
-threads 2 \
-index data/testrobust-single