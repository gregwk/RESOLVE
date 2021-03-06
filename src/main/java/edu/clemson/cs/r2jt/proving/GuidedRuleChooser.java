package edu.clemson.cs.r2jt.proving;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;

import edu.clemson.cs.r2jt.absyn.EqualsExp;
import edu.clemson.cs.r2jt.absyn.Exp;
import edu.clemson.cs.r2jt.analysis.MathExpTypeResolver;

public class GuidedRuleChooser extends RuleProvider {

    private List<MatchReplace> myGlobalRules = new LinkedList<MatchReplace>();
    private List<Exp> myExpCorrespondance = new LinkedList<Exp>();
    private MathExpTypeResolver myTyper;

    private boolean myLockedFlag;

    private DirectReplaceWrapper myAntecedentWrapper =
            new DirectReplaceWrapper();

    public GuidedRuleChooser(MathExpTypeResolver typer) {
        myTyper = typer;
        myLockedFlag = false;
    }

    public void addRules(List<String> names, List<Exp> rules) {
        Iterator<String> namesIterator = names.iterator();
        Iterator<Exp> rulesIterator = rules.iterator();

        while (namesIterator.hasNext()) {
            addRule(namesIterator.next(), rulesIterator.next());
        }
    }

    public void addRule(String friendlyName, Exp rule) {
        if (myLockedFlag) {
            throw new IllegalStateException();
        }

        if (rule instanceof EqualsExp) {
            EqualsExp equivalency = (EqualsExp) rule;

            if (equivalency.getOperator() == EqualsExp.EQUAL) {
                //Substitute right expression for left
                MatchReplace matcher =
                        new BindReplace(equivalency.getLeft(), equivalency
                                .getRight(), myTyper);
                myGlobalRules.add(matcher);
                myExpCorrespondance.add(rule);

                //Substitute left expression for left
                matcher =
                        new BindReplace(equivalency.getRight(), equivalency
                                .getLeft(), myTyper);
                myGlobalRules.add(matcher);
                myExpCorrespondance.add(rule);
            }
        }
        else {
            System.out.println("BlindIterativeRule.addRule --- "
                    + "Non equals Theorem.");
            System.out.println(rule.toString(0));
        }
    }

    public int getRuleCount() {
        return myGlobalRules.size();
    }

    public KnownSizeIterator<MatchReplace> consider(VerificationCondition vC,
            int curLength, Metrics metrics,
            Deque<VerificationCondition> pastStates) {

        //We only want those antecedents that are in the form of an equality,
        //and for each of those we need it going both left-to-right and 
        //right-to-left
        List<Exp> antecedentTransforms =
                buildFinalAntecedentList(vC.getAntecedents());

        Iterator<MatchReplace> antecedentIterator =
                new LazyActionIterator<Exp, MatchReplace>(antecedentTransforms
                        .iterator(), myAntecedentWrapper);

        ChainingIterator<MatchReplace> totalIterator =
                new ChainingIterator<MatchReplace>(antecedentIterator,
                        myGlobalRules.iterator());

        GuidedListSelectIterator<MatchReplace> finalIterator =
                new GuidedListSelectIterator<MatchReplace>("VC " + vC.getName()
                        + " - Select a proof rule...", vC.getAntecedents()
                        .toString()
                        + " =====> " + vC.getConsequents().toString(),
                        totalIterator);

        return new SizedIterator<MatchReplace>(finalIterator,
                antecedentTransforms.size() + myGlobalRules.size());
    }

    private List<Exp> buildFinalAntecedentList(List<Exp> originalAntecedents) {
        List<Exp> antecedentTransforms = new LinkedList<Exp>();
        for (Exp antecedent : originalAntecedents) {
            if (antecedent instanceof EqualsExp) {
                EqualsExp antecedentAsEqualsExp = (EqualsExp) antecedent;

                if (antecedentAsEqualsExp.getOperator() == EqualsExp.EQUAL) {

                    antecedentTransforms.add(antecedent);

                    EqualsExp flippedAntecedent =
                            new EqualsExp(antecedentAsEqualsExp.getLocation(),
                                    antecedentAsEqualsExp.getRight(),
                                    antecedentAsEqualsExp.getOperator(),
                                    antecedentAsEqualsExp.getLeft());
                    antecedentTransforms.add(flippedAntecedent);
                }
            }
        }

        return antecedentTransforms;
    }

    public boolean isLocked() {
        return myLockedFlag;
    }

    public void removeRule(Exp exp) {
        if (myLockedFlag) {
            throw new IllegalStateException();
        }

        Iterator correspondance = myExpCorrespondance.iterator();
        Iterator rules = myGlobalRules.iterator();

        while (correspondance.hasNext()) {
            if (correspondance.next() == exp) {
                correspondance.remove();
                rules.remove();
            }
        }
    }

    public void setLocked(Boolean locked) {
        myLockedFlag = locked;
    }

    private class DirectReplaceWrapper
            implements
                Transformer<Exp, MatchReplace> {

        public DirectReplace transform(Exp source) {
            if (!(source instanceof EqualsExp)) {
                equalsOnlyException(source);
            }

            EqualsExp sourceAsEqualsExp = (EqualsExp) source;
            return new DirectReplace(sourceAsEqualsExp.getLeft(),
                    sourceAsEqualsExp.getRight());
        }
    }

    private void equalsOnlyException(Exp e) {
        throw new RuntimeException("The prover does not yet work for "
                + "theorems not in the form of an equality, such as:\n"
                + e.toString(0));
    }

    public int getApproximateRuleSetSize() {
        return -1;
    }

    /*
     * The GuidedRuleChooser was created to help debug the 
     * BlindIterativeRuleChooser (and it's descendants, like 
     * UpfrontFitnessSortRuleChooser.)  However, there are cases where
     * GuidedRuleChooser is able to prove things that BlindIterativeRuleChooser
     * cannot.  What follows are backups of the old methods that "do more".  In
     * fact, the GuidedRuleChooser must do EXACTLY what 
     * BlindIterativeRuleChooser does so it can be useful as a debuging tool.
     * The new versions "do the same". 
     */
    /*
    public void addRule(String friendlyName, Exp rule) {
    	if (myLockedFlag) {
    		throw new IllegalStateException();
    	}
    	
    	if (rule instanceof EqualsExp) {
    		EqualsExp equivalency = (EqualsExp) rule;
    		
    		if (equivalency.getOperator() == EqualsExp.EQUAL) {
    			//Substitute right expression for left
    			MatchReplace matcher = new BindReplace(equivalency.getLeft(),
    					equivalency.getRight(), myTyper);
    			myGlobalRules.add(matcher);
    			myExpCorrespondance.add(rule);
    			
    			//Substitute left expression for left
    			matcher = new BindReplace(equivalency.getRight(),
    					equivalency.getLeft(), myTyper);
    			myGlobalRules.add(matcher);
    			myExpCorrespondance.add(rule);
    		}
    	}
    	else {
    		System.out.println("BlindIterativeRule.addRule --- " +
    				"Non equals Theorem.");
    		System.out.println(rule.toString(0));
    	}
    }
     */
}
