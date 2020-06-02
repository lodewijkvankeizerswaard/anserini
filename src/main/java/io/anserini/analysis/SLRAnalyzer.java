package io.anserini.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import io.anserini.analysis.SLRTokenizer;

// import io.anserini.analysis.DefaultEnglishAnalyzer;

public class SLRAnalyzer extends Analyzer{
    public int decPrecision;
    public SLRAnalyzer(int argsDecPrecision) {
        this.decPrecision = argsDecPrecision;
    }

    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new SLRTokenizer(decPrecision);
        TokenStream result;
        result = source;

        return new TokenStreamComponents(source, result);
    }
    
}