package edu.clemson.cs.r2jt.sanitycheck;

import java.util.Iterator;
import edu.clemson.cs.r2jt.absyn.CallStmt;
import edu.clemson.cs.r2jt.absyn.ProcedureDec;
import edu.clemson.cs.r2jt.absyn.ProgramParamExp;
import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.treewalk.TreeWalker;
import edu.clemson.cs.r2jt.treewalk.TreeWalkerStackVisitor;

public class VisitorRecursiveCheck extends TreeWalkerStackVisitor {

    /* Private Variables */

    private CompileEnvironment myCompileEnvironment;
    private ProcedureDec myInitialProcedureDec;
    private Boolean isRecursive = false;

    private List<ProcedureDec> myCheckedProcedureDecs;

    /* Constructors */

    public VisitorRecursiveCheck(ProcedureDec dec, CompileEnvironment env) {
        myInitialProcedureDec = dec;
        myCompileEnvironment = env;
        // System.out.println("Proc: " + dec.getName().getName());

        myCheckedProcedureDecs = new List<ProcedureDec>();
    }

    private VisitorRecursiveCheck(ProcedureDec dec,
            List<ProcedureDec> checkedProcDec, CompileEnvironment env) {
        myInitialProcedureDec = dec;
        myCompileEnvironment = env;
        // System.out.println("Proc: " + dec.getName().getName());

        myCheckedProcedureDecs = checkedProcDec;
    }

    @Override
    public void preCallStmt(CallStmt stmt) {
        String name = stmt.getName().getName();
        // System.out.println("CallStmt: " + name);
        checkRecursion(name);
    }

    @Override
    public void preProgramParamExp(ProgramParamExp exp) {
        String name = exp.getName().getName();
        // System.out.println("ProgramParamExp: " + name);
        checkRecursion(name);
    }

    /* Helper Functions */

    public Boolean isRecursive() {
        return isRecursive;
    }

    /**
     * This method recursively uses <code>VisitorRecursiveCheck</code> to
     * determine if any of the <code>myInitialProcedureDec</code>'s function
     * calls in turn call it, causing it to be recursive.
     * 
     * @param name Name of the current stmt/exp being checked
     */
    private void checkRecursion(String name) {
        if (name.equals(myInitialProcedureDec.getName().getName())) {
            isRecursive = true;
        }
        else {
            Iterator<ProcedureDec> i =
                    myCompileEnvironment.encounteredProcedures.iterator();
            while (i.hasNext()) {
                ProcedureDec dec = i.next();
                if (!myCheckedProcedureDecs.contains(dec)) {
                    myCheckedProcedureDecs.add(dec);
                    if (name.equals(dec.getName().getName())) {
                        VisitorRecursiveCheck vrc =
                                new VisitorRecursiveCheck(
                                        myInitialProcedureDec,
                                        myCheckedProcedureDecs,
                                        myCompileEnvironment);
                        TreeWalker tw = new TreeWalker(vrc);
                        tw.visit(dec);
                        // System.out.println(dec.getName().getName() +
                        // " causes isRecursive: " + vrc.isRecursive() + " for "
                        // + myInitialProcedureDec.getName().getName());
                        isRecursive = vrc.isRecursive();
                    }
                }
            }
        }
    }
}
