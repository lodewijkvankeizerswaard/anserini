package io.anserini.search.latent;

import java.io.IOException;

import org.apache.lucene.search.Scorer;

import org.apache.lucene.index.ImpactsEnum;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SlowImpactsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.ImpactsDISI;
import org.apache.lucene.search.Weight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LatentScorer extends Scorer{
    private final PostingsEnum postingsEnum;
    private final ImpactsEnum impactsEnum;
    private final DocIdSetIterator iterator;
    private final LeafSimScorer docScorer;
    private final ImpactsDISI impactsDisi;

    private static final Logger LOG = LogManager.getLogger(LatentScorer.class);

    public LatentScorer(Weight weight, PostingsEnum postingsEnum, LeafSimScorer docScorer) {
        super(weight);
        iterator = this.postingsEnum = postingsEnum;
        impactsEnum = new SlowImpactsEnum(postingsEnum);
        LOG.info(docID());
        impactsDisi = new ImpactsDISI(impactsEnum, impactsEnum, docScorer.getSimScorer());
        this.docScorer = docScorer;
        LOG.info("------------------Latent Scorer Init --------------------");
        // LOG.info("[LatentScorer] Init! Weight: " + weight.toString() + "\npostingsEnum: " + postingsEnum.toString() + "\nLeafSimScorer: " + docScorer.toString());
    }

    @Override
    public int docID(){
        return postingsEnum.docID();
    }

    @Override
    public DocIdSetIterator iterator() {
        return iterator;
    }

    @Override
    public float score() throws IOException {
        assert docID() != DocIdSetIterator.NO_MORE_DOCS;
        LOG.info("[LatentScorer] Score: " + postingsEnum.toString());
        return docScorer.score(postingsEnum.docID(), postingsEnum.freq());
    }
    @Override
    public int advanceShallow(int target) throws IOException {
        return impactsDisi.advanceShallow(target);
    }

    @Override
    public float getMaxScore(int upTo) throws IOException {
        return impactsDisi.getMaxScore(upTo);
    }

    @Override
    public void setMinCompetitiveScore(float minScore) {
        impactsDisi.setMinCompetitiveScore(minScore);
    }

    /** Returns a string representation of this <code>LatentScorer</code>. */
    @Override
    public String toString() { return "scorer(" + weight + ")[" + super.toString() + "]"; }

}