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
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.MatchesUtils;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
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
import java.util.Objects;
import java.util.Set;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SLRQuery extends TermQuery{
    private final Term term;
    protected final Float activationValue;
    private final TermStates perReaderTermState;

    final class SLRWeight extends Weight {
        private final Similarity similarity;
        private final Similarity.SimScorer simScorer;
        private final TermStates termStates;
        private final ScoreMode scoreMode;

        public SLRWeight(IndexSearcher searcher, ScoreMode scoreMode,
            float boost, TermStates termStates) throws IOException {
            super(SLRQuery.this);
            if (scoreMode.needsScores() && termStates == null) {
                throw new IllegalStateException("termStates are required when scores are needed");
            }
            this.scoreMode = scoreMode;
            this.termStates = termStates;
            this.similarity = searcher.getSimilarity();

            final CollectionStatistics collectionStats;
            final TermStatistics termStats;
            if (scoreMode.needsScores()) {
                collectionStats = searcher.collectionStatistics(term.field());
                termStats = new TermStatistics(term.bytes(), floatToLong((float) activationValue), Long.MAX_VALUE);
            } else {
                // we do not need the actual stats, use fake stats with docFreq=maxDoc=ttf=1
                collectionStats = new CollectionStatistics(term.field(), 1, 1, 1, 1);
                termStats = new TermStatistics(term.bytes(), floatToLong((float) activationValue), Long.MAX_VALUE);
            }
        
            if (termStats == null) {
                this.simScorer = null; // term doesn't exist in any segment, we won't use similarity at all
            } else {
                this.simScorer = similarity.scorer(boost, collectionStats, termStats);
            }
        }

        @Override
        public String toString() {
            return "weight(" + SLRQuery.this + "," + activationValue + ")";
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            assert termStates == null || termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);;
            final TermsEnum termsEnum = getTermsEnum(context);
            if (termsEnum == null) {
                return null;
            }
            LeafSimScorer scorer = new LeafSimScorer(simScorer, context.reader(), term.field(), scoreMode.needsScores());
            if (scoreMode == ScoreMode.TOP_SCORES) {
                return new SLRScorer(this, termsEnum.impacts(PostingsEnum.FREQS), scorer);
            } else {
                return new SLRScorer(this, termsEnum.postings(null, scoreMode.needsScores() ? PostingsEnum.FREQS : PostingsEnum.NONE), scorer);
            }
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            return true;
        }

        /**
         * Returns a {@link TermsEnum} positioned at this weights Term or null if
         * the term does not exist in the given context
         */
        private TermsEnum getTermsEnum(LeafReaderContext context) throws IOException {
            assert termStates != null;
            assert termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) :
                "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);
            final TermState state = termStates.get(context);
            if (state == null) { // term is not present in that reader
                assert termNotInReader(context.reader(), term) : "no termstate found but term exists in reader term=" + term;
                return null;
            }
            final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
            termsEnum.seekExact(term.bytes(), state);
            return termsEnum;
        }

        private boolean termNotInReader(LeafReader reader, Term term) throws IOException {
            // only called from assert
            // System.out.println("TQ.termNotInReader reader=" + reader + " term=" +
            // field + ":" + bytes.utf8ToString());
            return reader.docFreq(term) == 0;
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            SLRScorer scorer = (SLRScorer) scorer(context);
            if (scorer != null) {
                int newDoc = scorer.iterator().advance(doc);
                if (newDoc == doc) {
                float freq = scorer.freq();
                LeafSimScorer docScorer = new LeafSimScorer(simScorer, context.reader(), term.field(), true);
                Explanation freqExplanation = Explanation.match(freq, "freq, occurrences of term within document");
                Explanation scoreExplanation = docScorer.explain(doc, freqExplanation);
                return Explanation.match(
                    scoreExplanation.getValue(),
                    "weight(" + getQuery() + " in " + doc + ") ["
                        + similarity.getClass().getSimpleName() + "], result of:",
                    scoreExplanation);
                }
            }
            return Explanation.noMatch("no matching term");
        }

        @Override
        public void extractTerms(Set<Term> terms) {}

        public Float getValue() {
            return activationValue;
        }
    }

    public SLRQuery(Term t) {
        this(t, 1f);
    }

    /** Constructs a query for the term <code>t</code>, with value <code>v</code>. */
    public SLRQuery(Term t, Float v) {
        super(t);
        term = Objects.requireNonNull(t);
        activationValue = v;
        perReaderTermState = null;
    }

    /**
     * Expert: constructs a TermQuery that will use the provided docFreq instead
     * of looking up the docFreq against the searcher.
     */
    // public TermQuery(Term t, TermStates states) {
    //     assert states != null;
    //     term = Objects.requireNonNull(t);
    //     perReaderTermState = Objects.requireNonNull(states);
    // }

    /** Returns the term of this query. */
    public Term getTerm() {
        return term;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        final IndexReaderContext context = searcher.getTopReaderContext();
        final TermStates termState;
        if (perReaderTermState == null
            || perReaderTermState.wasBuiltFor(context) == false) {
        termState = TermStates.build(context, term, scoreMode.needsScores());
        } else {
        // PRTS was pre-build for this IS
        termState = this.perReaderTermState;
        }

        return new SLRWeight(searcher, scoreMode, boost, termState);
    }

    @Override
    public void visit(QueryVisitor visitor) {
        if (visitor.acceptField(term.field())) {
            visitor.consumeTerms(this, term);
        }
    }

    /** Returns the {@link TermStates} passed to the constructor, or null if it was not passed.
     *
     * @lucene.experimental */
    public TermStates getTermStates() {
        return perReaderTermState;
    }

    /** Returns true iff <code>other</code> is equal to <code>this</code>. */
    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) &&
            term.equals(((SLRQuery) other).term);
    }

    public static long floatToLong(float f) {
        byte[] bytes = floatToByteArr(f);
        return bytesToLong(bytes);
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }
    
    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.put(bytes);
        buffer.flip();//need flip 
        return buffer.getLong();
    }

    // Inspiration for float-to-byte: https://discourse.processing.org/t/float-value-to-byte-array/16698/2
    public static byte[] floatToByteArr(float f) {
        byte[] fb = new byte[4];
        int[] intArr = floatToIntArr(f);
        for (int i = 0; i < 4; i ++) {
            fb[i] = (byte) intArr[i];
        }
        return fb;
    }

    public static int[] floatToIntArr(float f) {
        final int i = Float.floatToIntBits(f);
        return new int[] { i >>> 030, i >> 020 & 0xff, i >> 010 & 0xff, i & 0xff };
    }
}