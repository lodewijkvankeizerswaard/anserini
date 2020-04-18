
package io.anserini.search.latent;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;
import java.util.Vector;
import java.util.Random;

public class SparseLatentQuery extends Query{
    private int dimensions;
    private float sparsityRatio;
    private Vector<Float> representation;
    private Random random;

    final class LatentWeight extends Weight {
        private final Similarity similarity;
        private final Similarity.SimScorer simScorer;
        private final TermStates termStates;
        private final ScoreMode scoreMode;

        public LatentWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost, TermStates termStates) {
            this.similarity = searcher.getSimilarity();
            this.scoreMode = scoreMode;
            this.termStates = termStates;
            final CollectionStatistics collectionStats;
            final TermStatistics termStats;
            this.simScorer = this.similarity.scorer(boost, collectionStats, termStats);
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            return Explanation.match(1f, "this explains the LatentWeight");
        }

        // @Override
        // public 
    }

    public SparseLatentQuery(String queryString) {
        this(queryString, 10, 0.9f);
    }

    public SparseLatentQuery(String queryString, int dimensions, float sparsityRatio) {
        this.dimensions = dimensions;
        this.sparsityRatio = sparsityRatio;
        this.representation = new Vector<Float>(this.dimensions);
        this.random = new Random();

        for (int i = 0; i < this.dimensions; i++) {
          representation.add(i, (float) this.random.nextFloat());
        }
    }

    public Vector getVector() {
        return this.representation;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
       return new LatentWeight(searcher, scoreMode, boost);
    }

    @Override
    public String toString(String field) {
        return representation.toString();
    }

    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) && representation.equals(other);
    }

    @Override
    public int hashCode() {
        return representation.hashCode();
    }
}