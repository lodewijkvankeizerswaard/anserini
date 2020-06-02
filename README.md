Anserini (Neural Fork)
========
This is a fork of the main Anserini master that aims to implement a fully sparse neural ranker.

## Getting Started

TODO

<!-- Run the following commands to see it functioning:

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
which uses the queries in the topics.msmarco.doc.dev.txt to search the index. The results can be found in run.msmarco-doc.sr.topics.msmarco-doc.dev.txt -->

## Added Searching Functionality

To be able to search using (precomputed) sparse vector representations the entire query time functionality had to be controlled. This is done by five new classes that each handle one part of the seach command: `SLRQueryGenerator`, `SLRQuery`, `SLRWeight`, `SLRScorer`, `SLRSimilarity`, all part of the added `io.anserini.search.latent` package. 

The goal of this extension is to get the SLR of the query down to a newly implemented document scoring function, that implements the sparse dot product between two vectors. To be able to use as much of the efficient functionality of the Lucene engine, the implementation has to stay as close as possible to the intended use. However, this is not possible at every stage of the ranking pipeline, which will  become cleart in the discussion of each class below.

### SLRQueryGenerator

The task of the SLRQueryGenerator is the conversion of the word query into a SLR, and to then create a `BooleanQuery` object. 
```
 BooleanQuery.Builder builder = new BooleanQuery.Builder();
```

The `BooleanQuery` object will store each latent term in a `Query` object. 
```
for (String ind : indices) {
    builder.add(new SLRQuery(new Term(field, key), value), BooleanClause.Occur.SHOULD);
}
```

Given multiple `Query` objects, the default behavior for word based queries is to take the sum of each token score, that is computed by a `Similarity` object. This is exactly the same as the calculation of a sparse dot product, where each latent token can be seen as a word. The `BooleanQuery` object can thus be seen as a container for all the latent terms, which means the only thing that is left to do is to score documents for each latent term. This is done by creating a `SLRQuery` object for each term.

### SLRQuery

The `SLRQuery` can be seen as a normal `TermQuery` from the Lucene engine, with a string reprensentation of the latent index number as the term. The standard `TermQuery` has no place to store the activation value of that index, which is why the new class has a `value` property. The rest of the implementation is quite similar to the `TermQuery` implementation. The only exeption that is worth mentioning is the `createWeight` function, that return a `SLRWeight` object instead of a `TermWeight` object. 

### SLRWeight

The `SLRWeight`, very similair to the `TermWeight`, is responsible for the creation of a `TermStatistics` and a `CollectionStatistics` object, containing statistics about the term and collection. These object are used for the creation of a `SLRSimilarity` object, that will score documents for the contained latent index. Futhermore, a computed `PostingsEnum` will contain a list of documents (with a document id, termfrequencies, and a normalization factor for each document) that matched to the term in the inverted index. These objects are all that is needed to rank the documents. However, it is worth mentioning how the creation of these objects is different from a normal word based query. 

- The `TermStatistics` object is the only way to pass the term (or index) value to the `Scorer` object. Since the `TermStatistics` object contains a term frequency argument of the type `long`, it is not directly possible to pass the `float` activition value to the constructor. Using a `float` to `long` conversion function, that copies the binairy representation into a long, the activation value is passed along.
- The statistics about the collection are not relevant for computing the sparse dot product, which means that an empty `CollectionStatstics` object is passed to the `Scorer` object.
- The two objects above are passed to the `SLRSimilarity` object constructor. This object will score documents using the activation value of the latent index, converted back to a float, and document frequencies, passed along by the `SLRScorer` object.
- The list of documents in the `PostingsEnum` object is computed by looking up documents that match the index number. **The computation of the `PostingsEnum` is where the functionality of the Lucene engine can be exploited for the efficient neural scoring of documents.** Since the latent representations are sparse, the list of matched documents be significantly shorter for each term, then when using a dense neural representation. 

All these objects are passed to the `SLRScorer` object that is responsible for putting everything together.

### SLRScorer

The behaviour of the `SLRScorer` object is currently exactly the same as the `TermScorer` object. The most important function is the `score` function, that scores the next document in the matched document list with the `score` function of the `SLRSimilarity` object.

### SLRSimilarity

