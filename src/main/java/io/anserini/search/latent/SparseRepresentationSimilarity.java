/*
 * Anserini: A Lucene toolkit for replicable information retrieval research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.anserini.search.similarity;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.Throwable;


public class SparseRepresentationSimilarity extends Similarity {
    private static final Logger LOG = LogManager.getLogger(SparseRepresentationSimilarity.class);

    public SparseRepresentationSimilarity() { }
    
    // Needs to be overridden so is used by the searcher object
    @Override
    public final long computeNorm(FieldInvertState state) {
        final int numTerms;
        if (state.getIndexOptions() == IndexOptions.DOCS && state.getIndexCreatedVersionMajor() >= 8) {
          numTerms = state.getUniqueTermCount();
        } else {
          numTerms = state.getLength();
        }
        return numTerms;
    }
    
    // Needs to be overridden so is used by the searcher object
    @Override
    public final SimScorer scorer(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
        Objects.requireNonNull(termStats);
        if (termStats.length > 1) {
            LOG.warn("[SparseSim] More than one termstatistic was passed (only one handledl)!");
        }
        TermStatistics ts = termStats[0];
        LOG.info("[SparseSim] Binary version of term: " + ts.term().toString());

        ArrayList<Float> queryVec = new ArrayList<Float>(3);

        queryVec = bytesToVec(ts.term().bytes);
    
        return new SparRepFixed(boost, queryVec);
    }
    
    // Does not need to be overridden, but i will leave it in for clarity
    @Override
    public String toString() {
        return "SR using dotproduct";
    }

    public static ArrayList<Float> bytesToVec(byte[] b) {
      int flArrLen = (int) b.length / 4;
      ArrayList<Float> l = new ArrayList<Float>(flArrLen);

      for (int i = 0; i < b.length; i+=4) {
        float f = byteArrToFloat(b[i], b[i+1], b[i+2], b[i+3]);
        l.add(f);
      }
      return l;
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

    private static class SparRepFixed extends SimScorer {
    
        private final float boost;
        private final ArrayList<Float> queryVector;
    
        SparRepFixed(float boost, ArrayList<Float> queryVec) {
          LOG.info("[Scorer object] init: " +  queryVec.toString());
          this.boost = boost;
          this.queryVector = queryVec;
        }
    
        @Override
        public float score(float freq, long norm) {
          LOG.info("[Score] 1f");

          // try {
          //   throw new Exception("Getting the stack");
          // } catch (Throwable e) {
          //   e.printStackTrace();
          // }
          


          return 1f;
        }

        public float score(ArrayList<Float> docVec) {
          assert this.queryVector.size() == docVec.size() : "The nr. of dim. of the query vector (" + this.queryVector.size() + ") is not the same as the nr. dim. (" + docVec.size() + ") of the document vector.";
          LOG.info("[Score] vec: " + docVec.toString());
          float dot = 0f;

          for (int i = 0; i < docVec.size(); i++) {
            dot += (float) queryVector.get(i) * (float) docVec.get(i);
          }
          
          return dot;
        }
    }

}