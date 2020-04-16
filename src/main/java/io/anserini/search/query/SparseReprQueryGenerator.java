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
 * 
 * Altered by: Lodewijk van Keizerswaard - University of Amsterdam
 */

package io.anserini.search.query;

import io.anserini.analysis.AnalyzerUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.Vector;
import java.util.Random;

// PYTHON IMPLEMENTATION
// def get_sparse_representation(data, dim=1000, sparsity_ratio=0.9):
//     rand = np.random.uniform(0, 1, size=(len(data), dim))
//     rand[rand <= sparsity_ratio] = 0
//     return rand

import java.util.List;
public class SparseReprQueryGenerator  extends QueryGenerator {
    private final int dimensions;
    private final float sparsity_ratio;
    private Random random;

    public SparseReprQueryGenerator() {
      this.dimensions = 10000;
      this.sparsity_ratio = (float) 0.9;
      this.random = new Random();
    }

    public SparseReprQueryGenerator(int dim, float sparsity) {
      this.dimensions = dim;
      this.sparsity_ratio = sparsity;
      this.random = new Random();
    }

    @Override
    public Query buildQuery(String field, Analyzer analyzer, String queryText) {
      List<String> tokens = AnalyzerUtils.analyze(analyzer, queryText);
    
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      for (String t : tokens) {
        builder.add(new TermQuery(new Term(field, t)), BooleanClause.Occur.SHOULD);
      }
    
      return builder.build();
    }

    public Vector buildVector(Query query) {
      Vector vec = new Vector(this.dimensions);

      for (int i = 0; i < this.dimensions; i++) {
        vec.add(i, this.random.nextFloat());
      }

      return vec;
    }
  }