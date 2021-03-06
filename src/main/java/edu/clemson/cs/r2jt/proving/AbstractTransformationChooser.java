package edu.clemson.cs.r2jt.proving;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.clemson.cs.r2jt.analysis.MathExpTypeResolver;
import edu.clemson.cs.r2jt.proving.absyn.PExp;

/**
 * <p>Provides some skeleton code for implementing a 
 * <code>TransformationChooser</code> to simplify development.</p>
 */
public abstract class AbstractTransformationChooser
        implements
            TransformationChooser {

    private final Iterable<VCTransformer> myTransformerLibrary;

    protected final MathExpTypeResolver myTyper;

    private ChainingIterable<VCTransformer> myPerVCSubstitutions;

    public AbstractTransformationChooser(Iterable<VCTransformer> library,
            MathExpTypeResolver r) {

        myTyper = r;
        myTransformerLibrary = library;
    }

    /**
     * <p>A dummy implementation of 
     * {@link TransformationChooser#preoptimizeForVC} that does nothing.
     * Subclasses that need to implement per-VC optimizations should override
     * this method.</p>
     * 
     * @param vc The VC to be proved.
     */
    @Override
    public void preoptimizeForVC(VC vc) {

    }

    /**
     * <p>Returns the set of <code>VCTransformers</code> available for the 
     * current <code>VC</code>, which include the global library and those
     * theorems drawn from the VC's antecedent.</p>
     * 
     * @return An <code>Iterable</code> set of <code>VCTransformers</code> that
     *         may be applied to the current VC (i.e., the VC provided in the
     *         last call to <code>preoptimizeForVC()</code>.
     */
    protected final Iterable<VCTransformer> getTransformerLibrary() {
        return myTransformerLibrary;
    }

    @Override
    public final Iterator<ProofPathSuggestion> suggestTransformations(VC vc,
            int curLength, Metrics metrics, ProofData d) {

        myPerVCSubstitutions = new ChainingIterable<VCTransformer>();
        myPerVCSubstitutions.add(myTransformerLibrary);

        List<VCTransformer> localTheorems = new LinkedList<VCTransformer>();

        RuleNormalizer n = new SubstitutionRuleNormalizer(myTyper, false);

        for (PExp e : vc.getAntecedent()) {
            for (VCTransformer t : n.normalize(e)) {
                localTheorems.add(t);
            }
        }

        return new ChainingIterator<ProofPathSuggestion>(
                new LazyMappingIterator<VCTransformer, ProofPathSuggestion>(
                        localTheorems.iterator(),
                        new StaticProofDataSuggestionMapper(d)),
                doSuggestTransformations(vc, curLength, metrics, d,
                        localTheorems));
    }

    protected abstract Iterator<ProofPathSuggestion> doSuggestTransformations(
            VC vc, int curLength, Metrics metrics, ProofData d,
            Iterable<VCTransformer> localTheorems);
}
