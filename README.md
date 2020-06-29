Anserini (Neural Fork)
========
This is a fork of the main Anserini master that aims to implement a fully sparse neural ranker. The goal of this extension is to be able to score documents on query vectors. Two steps need to be taken to be able to do this. First, the activation values of sparse latent representations (SLRs) need to be stored in a Lucene index, and then the scoring process has to be changed to score using a sparse dot product between the document and query vectors. The implementation of these two objectives are discussed below.

<!-- be able to search this index (both using precomputed vectors). of the query down to a newly implemented document scoring function, that implements the sparse dot product between two vectors. To be able to use as much of the efficient functionality of the Lucene engine, the implementation has to stay as close as possible to the intended use. However, this is not possible at every stage of the ranking pipeline, which will  become cleart in the discussion of each class below. -->

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


## Added Indexing Functionality

The activation value gets stored in the term frequency of the corresponding latent term. This requires a conversion from a floating point to an integer, which is done by multiplying the activation value by `10^p`, where p is equal to the value set by `-slr.decimalPrecision p`. Three new classes have been added to implement this approach: `SLRGenerator`, `SLRAnalyser` and `SLRTokenizer`. These can be activated on the commandline by specifying `-slr` and `-slr.index`, and will be discussed below.

### SLRGenerator

The `SLRGenerator` takes in the precomputed text based (sparse) document representation, reads the active latent terms and stores them in a string, which will later be read by the `SLRTokenizer`. The reading is done by the following function:
```
slrMap.clear();
String[] splitValues = content.split("\\s");
for(int i = 0; i < splitValues.length; i++) {
    if(splitValues[i] != null && !splitValues[i].isEmpty() && splitValues[i] != "\n") {
    try {
        if(Float.parseFloat(splitValues[i]) != 0) {
        String mapValue = normalizeFloatFormat(splitValues[i]);
        slrMap.put(Integer.toString(i), mapValue);
        }
    } catch(Exception e) { }
    }  
}
```
The white space separated values are read in, and when the value is non-zero, it is added to a `HashMap` (slrMap). The `normalizeFloat` function changes the scientific float representations to normal zero padded representations. Later this map is converted to a specific string format, which is then passed on to the `SLRAnalyser`:
```
String rep = "";
for(Map.Entry<String, String> cursor : slrMap.entrySet()) {
    rep += " " + cursor.getKey() + cursor.getValue();
}
return rep;
```
Because every float starts with `'0.'`, the exact values can later be extracted from this format.

### SLRAnalyser

The `SLRAnalyser` can be seen as a wrapper for the `SLRTokenizer` since this is the only `TokenStreamComponent` that is needed to completely analyse the content. Normally, multiple components would be stacked such as case filters, stop word filters and stemmers.

### SLRTokenizer

The `SLRTokenizer` in the `incrementToken` function, extracts the term-value pairs, formatted by the `SLRGenerator`, using the `getSLRToken` and `getSLRValue` functions. These values are stored in the corresponding object buffers. The buffers are then used to store the correct term frequency value (activation value multiplied by 10^decimal precision) for each latent term:
```
getSLRValue(termAtt.buffer());
int val = Integer.parseInt(CharBuffer.wrap(valueBuffer), 0, valueBuffer.length, 10);
if(val >= 1) {
    posIncrAtt.setPositionIncrement(skippedPositions+1);
    freqAtt.setTermFrequency(val);

    getSLRToken(termAtt.buffer());
    termAtt.copyBuffer(tokenBuffer, 0, tokenBuffer.length);
    final int start = scanner.yychar();
    offsetAtt.setOffset(correctOffset(start), correctOffset(start+termAtt.length()));
    typeAtt.setType(SLRTokenizer.TOKEN_TYPES[tokenType]);
    return true;
} 
```


The resulting index has the activation value per non-zero latent term stored in the (latent) term frequency, multiplied by a known power of ten. 

## Added Searching Functionality

To be able to search this index and score documents using (precomputed) sparse vector representations the entire query time functionality had to be controlled. The objective is to pass the activation values of the latent query terms down to the scoring function. This is done by five new classes that each handle one part of the seach command: `SLRQueryGenerator`, `SLRQuery`, `SLRWeight`, `SLRScorer`, `SLRSimilarity`, all part of the added `io.anserini.search.latent` package. 



### SLRQueryGenerator

The task of the `SLRQueryGenerator` is the conversion of the word query into a SLR, and to then create a `BooleanQuery` object. 
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

The `SLRSimilarity` has a function that returns a so-called fixed scoring object. This is called fixed because it scores documents for one term. The term activation value is stored in this object. The scoring function that returns an actual term-document score is:

```
return this.queryValue * freq / activationValueDivider;
```
, where `freq` represents the term frequency of the latent term in the index (which in this case stores the activation value, see Added Indexing Functionality), and `activationValueDivider` is equal to `10^p` where p is set by the command line option `-slr.ip p`.
