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

import java.util.List;
import java.util.Objects;
import java.nio.ByteBuffer;
import java.lang.Math;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.Throwable;


public class SparseRepresentationSimilarity extends Similarity {
    private static final Logger LOG = LogManager.getLogger(SparseRepresentationSimilarity.class);
    private double activationValueDivider;
    public SparseRepresentationSimilarity(int decimalPrecision) {
      this.activationValueDivider = Math.pow(10, decimalPrecision);
    }
    
    // Needs to be overridden so is used by the searcher object
    @Override
    public final long computeNorm(FieldInvertState state) {
        return 1;
    }
    
    @Override
    public final SimScorer scorer(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
        Objects.requireNonNull(termStats);
        TermStatistics ts = termStats[0];

        float queryValue = longToFloat(ts.docFreq());
    
        return new SparRepFixed((float) activationValueDivider, queryValue);
    }

    public static float longToFloat(long l) {
        byte[] b = longToBytes(l);
        return byteArrToFloat(b);
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
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
    
    // Does not need to be overridden, but i will leave it in for clarity
    @Override
    public String toString() {
        return "SR using dotproduct";
    }

    private static class SparRepFixed extends SimScorer {
        private final float activationValueDivider;
        private final float queryValue;

        SparRepFixed(float activationValueDivider, float queryValue) {
          this.activationValueDivider = activationValueDivider;
          this.queryValue = queryValue;
        }
    
        @Override
        public float score(float freq, long norm) {
          return this.queryValue * freq / activationValueDivider;
        }
    }

}