package io.anserini.index.generator;

import io.anserini.collection.InvalidContentsException;
import io.anserini.collection.MultifieldSourceDocument;
import io.anserini.collection.SourceDocument;
import io.anserini.document.SparseVectorField;
import io.anserini.index.IndexArgs;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SparseVectorGenerator<T extends SourceDocument> implements LuceneDocumentGenerator<T> {
    protected IndexArgs args;
    private static final Logger LOG = LogManager.getLogger(SparseVectorGenerator.class);

    public SparseVectorGenerator(IndexArgs args) throws Exception {
        this.args = args;
    }

    @Override
    public Document createDocument(T src) throws GeneratorException {
        String id = src.id();
        String contents;

        try {
            contents = src.contents();
        } catch (InvalidContentsException e) {
            throw new InvalidDocumentException();
        }

        if (contents.trim().length() == 0) {
            throw new EmptyDocumentException();
        }

        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);

        final Document document = new Document();
        
        // Dump contents in a default content field
        document.add(new Field(IndexArgs.CONTENTS, contents, fieldType));

        // for now we are getting the slr via a JSON field
        if (src instanceof MultifieldSourceDocument) {
            String sv = ((MultifieldSourceDocument) src).fields().get(IndexArgs.SV);
            document.add(new SparseVectorField(IndexArgs.SV, sv));
        }
        

        return document;
    }
}