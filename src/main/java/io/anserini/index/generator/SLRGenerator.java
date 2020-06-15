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

 package io.anserini.index.generator;

 import io.anserini.collection.InvalidContentsException;
 import io.anserini.collection.MultifieldSourceDocument;
 import io.anserini.collection.SourceDocument;
 import io.anserini.index.IndexArgs;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldType;
 import org.apache.lucene.document.SortedDocValuesField;
 import org.apache.lucene.document.StoredField;
 import org.apache.lucene.document.StringField;
 import org.apache.lucene.index.IndexOptions;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.document.DoubleDocValuesField;
 import org.apache.logging.log4j.Level;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 import org.apache.logging.log4j.core.config.Configurator;

import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Math;
import java.util.Arrays;

// import org.pytorch.Tensor;
// import org.pytorch.IValue;
// import org.pytorch.Module;

import java.io.File;  
import java.util.Scanner; 

/**
 * Converts a {@link SourceDocument} into a Lucene {@link Document}, ready to be indexed.
 *
 * @param <T> type of the source document
 */
public class SLRGenerator<T extends SourceDocument> implements LuceneDocumentGenerator<T> {
  protected IndexArgs args;
  private static final Logger LOG = LogManager.getLogger(SLRGenerator.class);
  private Map<String, String> slrMap;
  private File slrFile;
  private Scanner slrFileScanner;
  private String[] currentFileLine;
  private int currentLineNr;
  private boolean usingModel = false, usingFile = false, usingContents = false;

  protected SLRGenerator() {
  }

  /**
   * Constructor with config and counters
   *
   * @param args configuration arguments
   */
  public SLRGenerator(IndexArgs args) throws Exception{
    // if(!(args.slrModel == "" ^ args.slrFilePath == ""))
    //   throw new Exception("The SLR generator needs a python model or a representation file!");
    
    this.args = args;
    slrMap = new HashMap<String, String>(100);

    if(args.slrFilePath != "") { // Reading slr's from file
      LOG.info("Reading representations from: " + args.slrFilePath);
      usingFile = true;

      slrFile = new File(args.slrFilePath);
      currentLineNr = 0;
      try {
        slrFileScanner = new Scanner(slrFile);
      } catch(Exception e) {
        LOG.error("Could not read the slr file!");
      }
    } else if(args.slrModel != "") { // Using python model to compute slr
      LOG.info("Using python model: " + args.slrModel);
      usingModel = true;
      // slrModel = Module.load(args.slrModel);
    } else {
      LOG.info("Reading representations from contents.");
      usingContents = true;
    }
    
  }

  private String normalizeFloatFormat(String input) {
    Integer indexOfE = -1;
    try {
      indexOfE = input.indexOf("e");
    } catch(Exception e) { LOG.info(input + " has no e"); }

    if(indexOfE == -1) {
      return input;
    }
    String valString = input.substring(0, indexOfE).replaceAll("\\.", "");
    Integer exponent = Integer.parseInt(input.substring(indexOfE + 1, indexOfE + 4)) * -1;
    String zeros = "";
    for(int i = 0; i < exponent - 1; i++) { zeros += "0"; }
    return "0." + zeros + valString;
  } 

  private void getSLRFromContent(String content){
    slrMap.clear();
    String[] splitValues = content.split("\\s");
    for(int i = 0; i < splitValues.length; i++) {
      if(splitValues[i] != null && !splitValues[i].isEmpty() && splitValues[i] != "\n") {
        try {
          if(Float.parseFloat(splitValues[i]) != 0) {
            String mapValue = normalizeFloatFormat(splitValues[i]);
            slrMap.put(Integer.toString(i), mapValue);
          }
        } catch(Exception e) { }
      }  
    }
}

  private void getSLRFromFile(String docID){
    slrMap.clear();
    currentLineNr = 0;
    while(slrFileScanner.hasNextLine()) {
      currentFileLine = slrFileScanner.nextLine().split("\\t");

      if(currentFileLine[0].equals(docID)) {

        for(int i = 1; i < currentFileLine.length; i++) {
          LOG.info(currentFileLine[i]);
          if(Float.parseFloat(currentFileLine[i]) != 0)
            slrMap.put(Integer.toString(i-1), currentFileLine[i]);
        }
        if(currentLineNr > 0)
          LOG.info("Missed " + currentLineNr + "times");
        break;
      } else {
        LOG.info("Miss: " + currentFileLine[0] + " != " + docID);
      }
      currentLineNr++;
    }
  }
  private void getSLRFromModel(String content) {
    String output = null;
    slrMap.clear();
    try {

      Process pythonModel = Runtime.getRuntime().exec("python3 " + args.slrModel + " -content " + content);
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(pythonModel.getInputStream()));
      output = stdInput.readLine();
      if(output == null)
        throw new IOException("Model execution not succesfull");
      String[] slr = output.replaceAll("[\\[(),\\]]", "").split(" ");

      for(int i = 0; i < slr.length; i+=2) {
        slrMap.put(slr[i], slr[i + 1]);
      }

    } catch (IOException e) {
      LOG.error("Error while executing python module!");
    }
  }

  private String slrToContent() {
    String rep = "";
    for(Map.Entry<String, String> cursor : slrMap.entrySet()) {
      rep += " " + cursor.getKey() + cursor.getValue();
    }
    return rep;
  }



  @Override
  public Document createDocument(T src) throws GeneratorException {
    String id = src.id();
    String contents;

    try {
      contents = src.contents();
    } catch (InvalidContentsException e) {
      // Catch and rethrow; indexer will eat the exception at top level and increment counters accordingly.
      throw new InvalidDocumentException();
    }

    if (contents.trim().length() == 0) {
      throw new EmptyDocumentException();
    }

    // Make a new, empty document.
    final Document document = new Document();

    // Store the collection docid.
    document.add(new StringField(IndexArgs.ID, id, Field.Store.YES));
    // This is needed to break score ties by docid.
    document.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef(id)));

    FieldType fieldType = new FieldType();
    fieldType.setStored(args.storeContents); // TODO what does this exactly!!

    // Are we storing document vectors?
    if (args.storeDocvectors) {
      fieldType.setStoreTermVectors(true);
      fieldType.setStoreTermVectorPositions(true);
    }

    // Are we building a "positional" or "count" index?
    if (args.storePositions) {
      fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    } else {
      fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    }

    // double SLR[] = SparseLatentRepresentation(contents, 100, 0.9);
    if(usingFile)
      getSLRFromFile(id);
    if(usingModel)
      getSLRFromModel(contents);
    if(usingContents)
      getSLRFromContent(contents);
    

    if (args.storeRaw || args.slrAppend) {
      Map<String, String> dictionary = new HashMap<String, String>();

      // Are we storing the sparse latent representation seperately?
	    if (args.slrAppend) {
      	dictionary.put("slr", slrMap.toString());
      }

      if (args.storeRaw) {
        dictionary.put("raw", src.raw());
      }

      document.add(new StoredField(IndexArgs.RAW, dictionary.toString()));
    }

    // Are we making a neural or traditional index?
    if(args.slrIndex) {
      String sparseRep = slrToContent();
      document.add(new Field(IndexArgs.CONTENTS, sparseRep, fieldType));
    } else {
      document.add(new Field(IndexArgs.CONTENTS, contents, fieldType));
    }

    return document;
  }
}
