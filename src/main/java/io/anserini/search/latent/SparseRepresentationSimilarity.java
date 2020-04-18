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

public class SparseRepresentationSimilarity extends Similarity {

    public SparseRepresentationSimilarity() {
    }

    protected float idf(long docFreq, long docCount) {
        return (float) Math.log(1 + (docCount - docFreq + 0.5D) / (docFreq + 0.5D));
    }
    
    private float avgFieldLength(CollectionStatistics collectionStats) {
        return (float) (collectionStats.sumTotalTermFreq() / (double) collectionStats.docCount());
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
        // Explanation idf = termStats.length == 1 ? idfExplain(collectionStats, termStats[0]) : idfExplain(collectionStats, termStats);
        float avgdl = avgFieldLength(collectionStats);
    
        return new SparRepFixed(boost, avgdl);
    }
    
    // Does not need to be overridden, but i will leave it in for clarity
    @Override
    public String toString() {
        return "SR using dotproduct";
    }
    private static class SparRepFixed extends SimScorer {
    
        private final float boost;
        private final float avgdl;
    
        /**
         * weight (idf * boost)
         */
        private final float weight;
    
        SparRepFixed(float boost, float avgdl) {
          this.boost = boost;
          this.avgdl = avgdl;
          this.weight = boost;
          // Normally avgdl should be >= 1, but let's use Math.max to avoid division by zero just in case
        }
    
        @Override
        public float score(float freq, long norm) {
          float docLen = norm;
          float wf = this.weight * freq;
          return wf;
        }
    }

}