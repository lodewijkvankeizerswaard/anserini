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
import io.anserini.search.latent.SLRQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.index.Term;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

// IN -> 3:0.9 50:0.89 72:0.99
// multi term query

public class SLRQueryGenerator extends QueryGenerator {
    private String pythonCommand = "";
    private static final Logger LOG = LogManager.getLogger(SLRQueryGenerator.class);

    public SLRQueryGenerator(String pythonModel) {
        if (pythonModel == "")
            LOG.info("Using python model: false");
        else
            LOG.info("Using python model: " + pythonModel);
        pythonCommand = pythonModel;
    }

    @Override
    public Query buildQuery(String field, Analyzer analyzer, String queryText) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Map<String, Float> querySLR = getQuerySLR(queryText);

        for(Map.Entry<String, Float> cursor : querySLR.entrySet()) {
            Query q = new SLRQuery(new Term(field, cursor.getKey()), cursor.getValue());
            builder.add(q, BooleanClause.Occur.SHOULD);
            LOG.info("key=" + cursor.getKey() + " value= " + cursor.getValue());
        }

        return builder.build();
    }

    private Map<String, Float> getQuerySLR(String query) {
        Map<String, Float> querySLR = new HashMap<String, Float>(query.split(" ").length);
        if(pythonCommand == ""){ // preprocessed input 
            String[] indices = query.split(" ");

            for (String ind : indices) {
                String[] keyValue = ind.split(":");
                querySLR.put(keyValue[0].toString(), Float.parseFloat(keyValue[1].toString()));
            }

        } else { // running python model
            try {

                Process pythonModel = Runtime.getRuntime().exec("python3 " + pythonCommand);
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(pythonModel.getInputStream()));
                String[] slr = stdInput.readLine().replaceAll("[\\[(),\\]]", "").split(" ");

                for(int i = 0; i < Math.round(slr.length / 2); i+=2) {
                    querySLR.put(slr[i], Float.parseFloat(slr[i + 1]));
                }

            } catch (IOException e) {
                LOG.error("Python module could not be executed!");
            }
        }

        return querySLR;
    }
}


// Old implementation
// public class SparseReprQueryGenerator extends QueryGenerator {
//     private static final Logger LOG = LogManager.getLogger(SparseReprQueryGenerator.class);
//     @Override
//     public Query buildQuery(String field, Analyzer analyzer, String queryText) {
// //       Map<String, Float> dictionary = new HashMap<String, Float>();
//       String[] indices = queryText.split(" ");
      
//       for (String ind : indices) {
//         String[] keyValue = ind.split(":");
//         dictionary.put(keyValue[0], Float.parseFloat(keyValue[1]));
//       }
      
//       LOG.info("Generating query for: " + field + " containing " + queryText);
//       LOG.info(dictionary.toString());
//       return new SparseLatentQuery(dictionary, field);
//     }
// }