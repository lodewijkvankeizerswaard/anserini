package io.anserini.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerImpl;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.tokenattributes.TermFrequencyAttribute;
import org.apache.lucene.util.AttributeFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SLRTokenizer extends Tokenizer{
    private static final Logger LOG = LogManager.getLogger(SLRTokenizer.class);

    /** A private instance of the JFlex-constructed scanner */
    private StandardTokenizerImpl scanner;

    /** Alpha/numeric token type */
    public static final int ALPHANUM = 0;
    /** Numeric token type */
    public static final int NUM = 1;
    /** Southeast Asian token type */
    public static final int SOUTHEAST_ASIAN = 2;
    /** Ideographic token type */
    public static final int IDEOGRAPHIC = 3;
    /** Hiragana token type */
    public static final int HIRAGANA = 4;
    /** Katakana token type */
    public static final int KATAKANA = 5;
    /** Hangul token type */
    public static final int HANGUL = 6;
    /** Emoji token type. */
    public static final int EMOJI = 7;
    
    /** String token types that correspond to token type int constants */
    public static final String [] TOKEN_TYPES = new String [] {
        "<ALPHANUM>",
        "<NUM>",
        "<SOUTHEAST_ASIAN>",
        "<IDEOGRAPHIC>",
        "<HIRAGANA>",
        "<KATAKANA>",
        "<HANGUL>",
        "<EMOJI>"
    };
    
    /** Absolute maximum sized token */
    public static final int MAX_TOKEN_LENGTH_LIMIT = 1024 * 1024;
    
    private int skippedPositions;

    private int maxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

    public static int SLRmultiplier = 1000000000;

    public SLRTokenizer(){
        init();
    }

    public SLRTokenizer(AttributeFactory factory){
        super(factory);
        init();
    }

    /**
     * Set the max allowed token length.  Tokens larger than this will be chopped
     * up at this token length and emitted as multiple tokens.  If you need to
     * skip such large tokens, you could increase this max length, and then
     * use {@code LengthFilter} to remove long tokens.  The default is
     * {@link StandardAnalyzer#DEFAULT_MAX_TOKEN_LENGTH}.
     * 
     * @throws IllegalArgumentException if the given length is outside of the
     *  range [1, {@value #MAX_TOKEN_LENGTH_LIMIT}].
     */ 
    public void setMaxTokenLength(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("maxTokenLength must be greater than zero");
        } else if (length > MAX_TOKEN_LENGTH_LIMIT) {
            throw new IllegalArgumentException("maxTokenLength may not exceed " + MAX_TOKEN_LENGTH_LIMIT);
        }
        if (length != maxTokenLength) {
            maxTokenLength = length;
            scanner.setBufferSize(length);
        }
    }

    /** Returns the current maximum token length
     * 
     *  @see #setMaxTokenLength */
    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    private void init() {
        this.scanner = new StandardTokenizerImpl(input);
    }

    // this tokenizer generates three attributes:
    // term offset, positionIncrement and type
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private final TermFrequencyAttribute freqAtt = addAttribute(TermFrequencyAttribute.class);

    /*
    * (non-Javadoc)
    *
    * @see org.apache.lucene.analysis.TokenStream#next()
    */
    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        skippedPositions = 0;

        while(true) {
            int tokenType = scanner.getNextToken();

            if (tokenType == StandardTokenizerImpl.YYEOF) {
                return false;
            }

            if (scanner.yylength() <= maxTokenLength) {
                posIncrAtt.setPositionIncrement(skippedPositions+1);
                scanner.getText(termAtt);
                LOG.info("input:" + termAtt.toString());

                char [] slrValue = getSLRValue(termAtt.buffer());
                double termValue = Double.parseDouble(String.valueOf(slrValue)) * SLRmultiplier;
                LOG.info("Parsed double: " + Double.parseDouble(String.valueOf(slrValue)));
                LOG.info("SLRmultiplier: " + SLRmultiplier);
                LOG.info("value: " + String.valueOf(termValue));
                LOG.info("intval: " + (int) Math.round(termValue));
                freqAtt.setTermFrequency((int) Math.round(termValue));

                char [] slrToken = getSLRToken(termAtt.buffer());
                termAtt.copyBuffer(slrToken, 0, slrToken.length);
                
                final int start = scanner.yychar();
                offsetAtt.setOffset(correctOffset(start), correctOffset(start+termAtt.length()));
                typeAtt.setType(SLRTokenizer.TOKEN_TYPES[tokenType]);
                return true;
            } else
                // When we skip a too-long term, we still increment the
                // position increment
                skippedPositions++;
        }
    }

    private int getSLRDotPos(char[] buffer) {
        for(int i = 0; i < buffer.length; i++) {
            if(buffer[i] == '.')
                return i;
        }
        return -1;
    }

    private char[] getSLRToken(char[] buffer) {
        int valStart = getSLRDotPos(buffer) - 1;
        char[] result = new char[valStart];
        for(int i = 0; i < result.length; i++)
            result[i] = buffer[i];
        return result;
    }

    private char[] getSLRValue(char[] buffer) {
        int valStart = getSLRDotPos(buffer) - 1;
        char[] result = new char[18]; // Strings of doubles are 18 chars long
        for(int i = 0; i < result.length; i++) {
            result[i] = buffer[i + valStart];
        }
        return result;
    }
    
    @Override
    public final void end() throws IOException {
        super.end();
        // set final offset
        int finalOffset = correctOffset(scanner.yychar() + scanner.yylength());
        offsetAtt.setOffset(finalOffset, finalOffset);
        // adjust any skipped tokens
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement()+skippedPositions);
    }

    @Override
    public void close() throws IOException {
        super.close();
        scanner.yyreset(input);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        scanner.yyreset(input);
        skippedPositions = 0;
    }
}