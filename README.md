Anserini (Neural Fork)
========
This is a fork of the main Anserini master that aims to implement a fully sparse neural ranker.

## Getting Started

Run the following commands to see it functioning:

```
./compile-jar.sh
```
This compiles the java executable to be able to create an index and to search it. The index is created with

```
./create-index.sh
```
This creates a test index (in the testindex directory) from the files in the testfiles directory. This index can then be searched with
```
./run-jar.sh
```
which uses the queries in the topics.msmarco.doc.dev.txt to search the index. The results can be found in run.msmarco-doc.sr.topics.msmarco-doc.dev.txt
