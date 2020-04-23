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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SparseRepresentationSimilarity extends Similarity {
    private static final Logger LOG = LogManager.getLogger(SparseRepresentationSimilarity.class);

    public SparseRepresentationSimilarity() {
        LOG.info("[SparseRepresentationSimilarity] Init!");
    }
    
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

        Vector<Float> queryVec = new Vector<Float>(10);
    
        return new SparRepFixed(boost, queryVec);
    }
    
    // Does not need to be overridden, but i will leave it in for clarity
    @Override
    public String toString() {
        return "SR using dotproduct";
    }

    private static class SparRepFixed extends SimScorer {
    
        private final float boost;
        private final Vector<Float> queryVector;
    
        SparRepFixed(float boost, Vector queryVec) {
          this.boost = boost;
          this.queryVector = queryVec;
        }
    
        @Override
        public float score(float freq, long norm) {
          return 5f;
        }

        public float score(Vector docVec) {
          assert this.queryVector.size() == docVec.size() : "The number of dimensions of the query vector (" + this.queryVector.size() + ") is not the same as the number dimensions (" + docVec.size() + ") of the document vector.";
          float dot = 0f;

          for (int i = 0; i < docVec.size(); i++) {
            dot += (float) queryVector.get(i) * (float) docVec.get(i);
          }
          
          return dot;
        }
    }

}