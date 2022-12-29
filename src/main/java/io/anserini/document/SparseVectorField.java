package io.anserini.document;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.BytesTermAttribute;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

public class SparseVectorField extends Field {
    public static final FieldType TYPE = new FieldType();
    static {
        TYPE.setStored(false);
        TYPE.setTokenized(false);
        TYPE.setIndexOptions(IndexOptions.DOCS);
        TYPE.freeze();
    }

    public static BytesRef pack(String sv) {
        if (sv == null) { 
            throw new IllegalArgumentException("string must not be null");
        }

        String[] vectorElements = sv.split("\\s");
        byte[] packed = new byte[vectorElements.length * (Integer.BYTES + Double.BYTES)];

        for (int term = 0; term < vectorElements.length; term++) {
            String[] pair = vectorElements[term].split("=");
            encodeElement(Integer.parseInt(pair[0]), Double.parseDouble(pair[1]), packed, term * (Integer.BYTES + Double.BYTES));
        }

        return new BytesRef(packed);
    }

    public static void encodeElement(Integer term, Double av, byte dest[], int offset) {
        NumericUtils.intToSortableBytes(term, dest, offset);
        long step = NumericUtils.doubleToSortableLong(av);
        NumericUtils.longToSortableBytes(step, dest, offset + Integer.BYTES);
    }

    public static SimpleEntry<Integer, Double> decodeElement(byte value[], int offset) {
        Integer key = NumericUtils.sortableBytesToInt(value, offset);
        long step = NumericUtils.sortableBytesToLong(value, offset + Integer.BYTES);
        Double val = NumericUtils.sortableLongToDouble(step);
        return new SimpleEntry<Integer,Double>(key, val);
    }

    public SparseVectorField(String name, String sv) {
        super(name, pack(sv), TYPE);
    }

    public byte[] getKeys() {
        byte[] bytes = ((BytesRef) fieldsData).bytes;
        int termCount = Math.round(bytes.length / (Integer.BYTES + Double.BYTES));
        byte[] keys = new byte[termCount * Integer.BYTES];

        for (int term = 0; term * (Integer.BYTES + Double.BYTES) < bytes.length; term++) {
            keys[term * Integer.BYTES] = bytes[term * (Integer.BYTES + Double.BYTES)];
            keys[term * Integer.BYTES + 1] = bytes[term * (Integer.BYTES + Double.BYTES) + 1];
            keys[term * Integer.BYTES + 2] = bytes[term * (Integer.BYTES + Double.BYTES) + 2];
            keys[term * Integer.BYTES + 3] = bytes[term * (Integer.BYTES + Double.BYTES) + 3];
        }

        return keys;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append(" <");
        result.append(name);
        result.append(':');

        BytesRef bytes = (BytesRef) fieldsData;
        for (int term = 0; term * (Integer.BYTES + Double.BYTES) < bytes.length; term++) {
            if (term > 0) {
                result.append(",");
            }
            result.append(decodeElement(bytes.bytes, term * (Integer.BYTES + Double.BYTES)));
        }

        result.append('>');
        return result.toString();
    }

    // @Override
    // public TokenStream tokenStream(Analyzer analyser, TokenStream reuse) {
    //     return ((BinaryTokenStream) reuse).setValue(new BytesRef(getKeys()));;
    // }
}

// private static final class VectorTermTokenStream extends TokenStream {
//     private final BytesTermAttribute bytesatt = addAttribute(BytesTermAttribute.class);

// }

