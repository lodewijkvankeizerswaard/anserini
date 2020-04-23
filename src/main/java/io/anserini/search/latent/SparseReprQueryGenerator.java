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

package io.anserini.search.latent;

import io.anserini.analysis.AnalyzerUtils;
import io.anserini.search.latent.SparseLatentQuery;
import io.anserini.search.query.QueryGenerator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SparseReprQueryGenerator extends QueryGenerator {
    private static final Logger LOG = LogManager.getLogger(SparseReprQueryGenerator.class);
    @Override
    public Query buildQuery(String field, Analyzer analyzer, String queryText) {
      LOG.info("Generating query for: " + field + " containing " + queryText);
      return new SparseLatentQuery(queryText, field);
    }
}