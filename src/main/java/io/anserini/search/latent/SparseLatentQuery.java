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
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.lucene.index.SegmentReader;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SparseLatentQuery extends Query{
    private final ArrayList<Term> latentIndices;
    private final Map indicesDictionary;
    private final TermStates perReaderTermState;
    private final String field;
    private final int nrIndices;

    // lijst van terms Term(3:0.9), Term(50:0.89)

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
            this.termStates = termStates;
            final CollectionStatistics collectionStats;
            final TermStatistics[] termStats = new TermStatistics[nrIndices];
            if (scoreMode.needsScores()) {              
                Iterator it = indicesDictionary.entrySet().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    String indexKey = pair.getKey().toString();
                    Float indexVal = Float.parseFloat(pair.getValue().toString());
                    final TermStatistics termStat = new TermStatistics(new BytesRef(indexKey.getBytes()), floatToLong(indexVal), 1);
                    termStats[i] = termStat;
                    i++;
                }
                collectionStats = searcher.collectionStatistics(latentIndices.get(0).field());
            } else {
                LOG.warn("Not using the correct termStatistics!!!");
                // we do not need the actual stats, use fake stats with docFreq=maxDoc=ttf=1
                collectionStats = new CollectionStatistics(field, 1, 1, 1, 1);
                termStats[0] = new TermStatistics(new Term(field, "bla").bytes(), 1, 1);
            }
            this.simScorer = this.similarity.scorer(boost, collectionStats, termStats);
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            assert termStates == null || termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);;
            
            final TermsEnum termsEnum = getTermsEnum(context);

            if (termsEnum == null) {
              return null;
            }
            LeafSimScorer scorer = new LeafSimScorer(simScorer, context.reader(), field, scoreMode.needsScores());
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
            
            LOG.info("context.ord: " + context.ord + " \t docbase: " + context.docBase);

            final TermState state = termStates.get(context);

            LOG.info("state: " + state);

            LeafReader sr = context.reader();

            // TODO hier zoeken op alle indices als term zodat alle documenten met een van de indices in de lijst komen te staan
            LOG.info("context: " + context.getClass());
            LOG.info("reader has " + context.reader().numDocs() + " documents.");
            LOG.info("reader has these terms in the field:" + sr.terms(field) + " " + sr.terms(field).getClass());
            final TermsEnum termsEnum = context.reader().terms(field).iterator();

            LOG.info("termsEnum: " + termsEnum);
            // No idea what this does internally, but it just returns true or false, and it errored so it was disabled.
            termsEnum.seekExact(latentIndices.get(0).bytes(), state);
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

    public SparseLatentQuery(Map dictionary, String fieldString) {
        this.perReaderTermState = null;
        this.indicesDictionary = dictionary;
        this.field = fieldString;

        this.latentIndices = new ArrayList<Term>();

        Iterator it = dictionary.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String indexKey = pair.getKey().toString();
            Term indexTerm = new Term(fieldString, new BytesRef(indexKey.getBytes()));
            this.latentIndices.add(indexTerm);
            LOG.info(pair.getKey() + "=" + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
            i++;
        }
        this.nrIndices = i;
    }

    public ArrayList<Term> getTerms() {
        return this.latentIndices;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        final IndexReaderContext context = searcher.getTopReaderContext();
        final TermStates termState;
        if (perReaderTermState == null
            || perReaderTermState.wasBuiltFor(context) == false) {
          termState = TermStates.build(context, this.latentIndices.get(0), scoreMode.needsScores());
        } else {
          // PRTS was pre-build for this IS
          termState = this.perReaderTermState;
        }
        return new LatentWeight(searcher, scoreMode, boost, termState);
    }

    @Override
    public String toString(String field) {
        return indicesDictionary.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other.getClass().equals( this.getClass()) && other.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        return this.indicesDictionary.hashCode();
    }

    public static ArrayList<Float> stringToVec(String vec) {
        vec = vec.replaceAll("([\\[\\] ])", "");
        String[] nmbs = vec.split(",");
        ArrayList<Float> flvec = new ArrayList<Float>(nmbs.length);
        for (String fl : nmbs) {
            flvec.add(Float.parseFloat(fl));
        }
        return flvec;
    }

    public static byte[] vecToBytes(ArrayList<Float> vec) {
        byte[] f = new byte[4];
        byte[] v = new byte[4 * vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            f = floatToByteArr(vec.get(i));

            for (int j = 0; j < 4; j++) {
                v[i*4 + j] = f[j];
            }
        }

        return v;
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
      
    public static final float byteArrToFloat(byte... b) {
        assert b != null : "bytes to Float conversion failed (invalid bytes)";
        int[] fb = new int[4];
        for (int i = 0; i < 4; i++) {
            fb[i] = (int) b[i];
        }
        return intArrToFloat(fb);
    }
      
    public static float intArrToFloat(int... i) {
        assert i == null && i.length < 4 : "Float to Int conversion failed (invalid integers)";
        return Float.intBitsToFloat( i[0] << 030 | (i[1] << 020 & 0x00ff0000) | (i[2] << 010 & 0x0000ff00) | (i[3] & 0x000000ff));
    }
}