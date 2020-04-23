package io.anserini.search.latent;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import java.io.IOException;
import java.util.Vector;
import java.util.Random;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SparseLatentQuery extends Query{
    private final Vector<Float> representation;
    private final Term term;
    private final TermStates perReaderTermState;
    private final Random random;

    private static final Logger LOG = LogManager.getLogger(SparseLatentQuery.class);
    

    final class LatentWeight extends Weight {
        private final Similarity similarity;
        private final Similarity.SimScorer simScorer;
        private final TermStates termStates;
        private final ScoreMode scoreMode;

        public LatentWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost, TermStates termStates) throws IOException {
            super(SparseLatentQuery.this);
            this.similarity = searcher.getSimilarity();
            this.scoreMode = scoreMode;
            LOG.info("[LatentWeight] scoreMode = " + scoreMode.toString());
            this.termStates = termStates;
            final CollectionStatistics collectionStats;
            final TermStatistics termStats;
            if (scoreMode.needsScores()) {
                collectionStats = searcher.collectionStatistics(term.field());
                termStats = termStates.docFreq() > 0 ? searcher.termStatistics(term, termStates.docFreq(), termStates.totalTermFreq()) : null;
            } else {
                // we do not need the actual stats, use fake stats with docFreq=maxDoc=ttf=1
                collectionStats = new CollectionStatistics(term.field(), 1, 1, 1, 1);
                termStats = new TermStatistics(term.bytes(), 1, 1);
            }
            this.simScorer = this.similarity.scorer(boost, collectionStats, termStats);
            LOG.info("[LatentWeight] simScorer = " + this.simScorer.toString());
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            assert termStates == null || termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);;
            
            final TermsEnum termsEnum = getTermsEnum(context);
            LOG.info("[LatentWeight] terms from context:" + termsEnum.toString());

            if (termsEnum == null) {
              return null;
            }
            LeafSimScorer scorer = new LeafSimScorer(simScorer, context.reader(), term.field(), scoreMode.needsScores());
            if (scoreMode == ScoreMode.TOP_SCORES) {
              return new LatentScorer(this, termsEnum.impacts(PostingsEnum.NONE), scorer);
            } else {
              return new LatentScorer(this, termsEnum.postings(null, scoreMode.needsScores() ? PostingsEnum.FREQS : PostingsEnum.NONE), scorer);
            }
        }

        private TermsEnum getTermsEnum(LeafReaderContext context) throws IOException {
            assert termStates != null;
            assert termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) :
                "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);
            
            final TermState state = termStates.get(context);
            final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
            // No idea what this does internally, but it just returns true or false, and it errored so it was disabled.
            // termsEnum.seekExact(term.bytes(), state);
            return termsEnum;
          }

        @Override
        public void extractTerms(Set<Term> terms) {}

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
          return true;
        }

        @Override
        public String toString() {
            return termStates.toString();
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            return Explanation.match(1f, "this explains the LatentWeight");
        }
    }

    public SparseLatentQuery(String queryString, String fieldString) {
        this(new Term(fieldString, new BytesRef(queryString.getBytes())));
    }

    public SparseLatentQuery(Term t) {
        LOG.info("[Query] string: " + t.text() + "field:" + t.field());
        this.perReaderTermState = null;
        this.representation = new Vector<Float>(10);
        this.term = new Term(t.field(), new BytesRef("*".getBytes()));

        // TODO - convert string vector to Vector
        this.random = new Random();
        for (int i = 0; i < 10; i++) {
          representation.add(i, (float) this.random.nextFloat());
        }
        
    }

    public Vector getVector() {
        return this.representation;
    }

    public Term getTerm() {
        return this.term;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        final IndexReaderContext context = searcher.getTopReaderContext();
        final TermStates termState;
        if (perReaderTermState == null
            || perReaderTermState.wasBuiltFor(context) == false) {
          termState = TermStates.build(context, this.term, scoreMode.needsScores());
        } else {
          // PRTS was pre-build for this IS
          termState = this.perReaderTermState;
        }
        return new LatentWeight(searcher, scoreMode, boost, termState);
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
        return this.representation.hashCode();
    }
}