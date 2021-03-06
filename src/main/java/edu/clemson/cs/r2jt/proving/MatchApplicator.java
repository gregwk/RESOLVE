package edu.clemson.cs.r2jt.proving;

import edu.clemson.cs.r2jt.absyn.Exp;
import edu.clemson.cs.r2jt.collections.List;

/**
 * <p>A <code>MatchApplicator</code> provides a mechanism for cycling over all
 * possible single applications of a <code>MatchReplace</code> over a set of
 * conjuncts.  Also provides useful static methods for replacing all matches
 * at a batch.</p>
 * 
 * @author H. Smith
 *
 */
public class MatchApplicator {

    /**
     * <p>The original list of conjuncts, on which to iterate over possible
     * replacements.</p>
     * 
     * <p>INVARIANT: <code>myConjuncts != null</code></p>
     */
    private final List<Exp> myConjuncts;

    /**
     * <p>The number of elements in <code>myConjuncts</code>.</p>
     * 
     * <p>INVARIANT: <code>numConjuncts = myConjuncts.size()</code>.</p>
     */
    private final int numConjuncts;

    /**
     * <p>The matcher that governs what expressions could be replaced.</p>
     *
     * <p>INVARIANT: <code>myMatcher == null</code>.</p>
     */
    private final MatchReplace myMatcher;

    /**
     * <p>These two counters keep track of where we are in the application 
     * search process.  <code>curConjunct</code> keeps track of which conjunct
     * we should be searching in, and <code>curApplication</code> keeps track
     * of how many applications we've already found in the current conjunct.</p>
     * 
     * <p>INVARIANT: <code>curConjunct &gt;= 0 && curApplication &gt;= 0</code>.
     * </p>
     */
    private int curConjunct = 0, curApplication = 0;

    /**
     * <p>Replaces every expression matched by the provided <code>matcher</code>
     * in the set of expressions rooted in an <code>Exp</code> in 
     * <code>conjuncts</code>.</p>
     * 
     * @param conjuncts The set of expresions to search over, 
     *                  making replacements.
     * @param matcher The matcher to decide what to replace and with what.
     */
    public static void applyAllInPlace(List<Exp> conjuncts, MatchReplace matcher) {

        for (Exp e : conjuncts) {
            performDirectSubstitution(e, matcher);
        }
    }

    /**
     * <p>Returns a deep copy of <code>conjunct</code> in which every expression
     * matched by the provided <code>matcher</code> has been replaced.</p>
     * 
     * @param conjuncts The conjuncts to deep copy.
     * @param matcher The matcher to decide what to replace and with what.
     * 
     * @return A copy with all possible replacements made.
     */
    public static List<Exp> applyAll(List<Exp> conjuncts, MatchReplace matcher) {

        List<Exp> retval = new List<Exp>();

        for (Exp e : conjuncts) {
            retval.add(e.copy());
        }

        applyAllInPlace(retval, matcher);

        return retval;
    }

    /**
     * <p>Traverses a given expression, <code>e</code>, and replaces any
     * matching sub-expression in-place according to the provided 
     * <code>matcher</code. The root expression itself will not be replaced, 
     * even if it matches the <code>matcher</code>.</p>
     * 
     * @param e The expression traverse, performing in-place replacements
     *          according to <code>matcher</code>.
     * @param matcher The matcher to determine which subexpressions to replace
     *                and what to replace them with.
     */
    private static void performDirectSubstitution(Exp e, MatchReplace matcher) {
        List<Exp> subExpressions = e.getSubExpressions();

        int numSubExpressions = subExpressions.size();
        Exp curSubExpression;
        for (int curIndex = 0; curIndex < numSubExpressions; curIndex++) {
            curSubExpression = subExpressions.get(curIndex);
            if (matcher.couldReplace(curSubExpression)) {
                //setSubExpression is supposed to match getSubExpression with
                //respect to which index is which expression.  If a substitution
                //claims to be happening but isn't, make sure the corresponding
                //setSubExpression isn't broken
                e.setSubExpression(curIndex, (Exp) matcher.getReplacement());
            }
            else {
                performDirectSubstitution(curSubExpression, matcher);
            }
        }
    }

    /**
     * <p>Creates a new <code>MatchApplicator</code> that will iterate over all
     * single replacements (according to the provided <code>matcher</code>)
     * available in the provided list of conjuncts.</p>
     * 
     * @param conjuncts The expressions in which to make the single replacement.
     * @param matcher The matcher to govern what gets replaced and with what.
     */
    public MatchApplicator(List<Exp> conjuncts, MatchReplace matcher) {
        myConjuncts = conjuncts;
        myMatcher = matcher;
        numConjuncts = conjuncts.size();
    }

    /**
     * <p>Returns a deep copy of the conjuncts provided to the constructor with
     * a single possible replacement made (defined by the matcher provided to
     * the constructor).  Each call to this method will return a new deep copy,
     * each with a different single replacement from any previous call, until
     * no such replacement is possible, at which time it will return 
     * <code>null</code>.</p>
     *  
     * @return Either the next possible single replacement, or 
     *         <code>null</code> if there are no further non-redundant
     *         replacements.
     */
    public List<Exp> getNextApplication() {

        //This is not a very efficient way of doing things on any level and
        //should probably be updated with a better algorithm in a production
        //system

        boolean foundMatch;
        List<Exp> retval = new List<Exp>();
        for (Exp e : myConjuncts) {
            retval.add((Exp) e.clone());
        }

        foundMatch = applyMatcher(retval);

        if (!foundMatch) {
            retval = null;
        }

        return retval;
    }

    /**
     * <p>Applies a single replacement in place that is different from any
     * previous replacement.  Returns <code>true</code> <strong>iff</strong> a
     * replacement was able to be made.</p>
     * 
     * @param conjuncts The conjuncts in which the replacement should be made.
     * @return <code>true</code> <strong>iff</strong> a replacement was able to
     *         be made.
     */
    private boolean applyMatcher(List<Exp> conjuncts) {
        boolean foundMatch = false;

        DummyExp curExp;
        while (curConjunct < numConjuncts && !foundMatch) {
            //The DummyExp is created as a work-around, see the note in the 
            //class comments
            curExp = new DummyExp(conjuncts.get(curConjunct));
            foundMatch = applyMatcher(curExp);
            conjuncts.set(curConjunct, curExp.getWrappedExpression());

            if (foundMatch) {
                curApplication++;
            }
            else {
                curConjunct++;
                curApplication = 0;
            }
        }

        return foundMatch;
    }

    /**
     * <p>Applies the next possible replacement to an expression.  The 
     * replacement is made in place.Returns
     * <code>true</code> <strong>iff</strong> a replacement was made.</p>
     * 
     * @param e The expression in which to make the next possible replacement.
     * @return <code>true</code> <strong>iff</strong> a replacement was made.
     */
    private boolean applyMatcher(Exp e) {
        return performSingleMatch(e, curApplication);
    }

    /**
     * <p>Applies the <code>numSkips</code>th possible replacement to 
     * <code>e</code> in place.  Returns <code>false</code> <strong>iff</strong>
     * there is no <code>numSkips</code>th possible replacement.</p>
     * 
     * @param e The expression in which to make the <code>numSkips</code>th
     *          replacement.
     * @param numSkips The number of the replacement to make.
     * 
     * @return True if a replacement was made, false if no such replacement is
     *         possible.
     */
    private boolean performSingleMatch(Exp e, int numSkips) {
        List<Exp> subExpressions = e.getSubExpressions();

        int numSubExpressions = subExpressions.size();
        Exp curSubExpression;
        int curIndex = 0;
        Exp replacement = null;
        boolean foundMatch = false;
        while (curIndex < numSubExpressions && replacement == null
                && !foundMatch) {

            curSubExpression = subExpressions.get(curIndex);
            if (myMatcher.couldReplace(curSubExpression)) {
                if (numSkips > 0) {
                    numSkips--;
                    foundMatch = performSingleMatch(curSubExpression, numSkips);
                }
                else {
                    replacement = myMatcher.getReplacement();

                    //setSubExpression is supposed to match getSubExpression 
                    //with respect to which index is which expression.  If a 
                    //substitution claims to be happening but isn't, make sure 
                    //the corresponding setSubExpression isn't broken
                    e.setSubExpression(curIndex, replacement);
                }
            }
            else {
                foundMatch = performSingleMatch(curSubExpression, numSkips);
            }

            curIndex++;
        }

        return (foundMatch || replacement != null);
    }
}
