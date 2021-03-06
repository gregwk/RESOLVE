/*
 * This software is released under the new BSD 2006 license.
 * 
 * Note the new BSD license is equivalent to the MIT License, except for the
 * no-endorsement final clause.
 * 
 * Copyright (c) 2007, Clemson University
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the Clemson University nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * This sofware has been developed by past and present members of the
 * Reusable Sofware Research Group (RSRG) in the School of Computing at
 * Clemson University. Contributors to the initial version are:
 * 
 * Steven Atkinson
 * Greg Kulczycki
 * Kunal Chopra
 * John Hunt
 * Heather Keown
 * Ben Markle
 * Kim Roche
 * Murali Sitaraman
 */
/*
 * Verifier.java
 * 
 * The Resolve Software Composition Workbench Project
 * 
 * Copyright (c) 2005-2010
 * Resolve Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.verification;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList; // import java.util.ListIterator;
import java.util.StringTokenizer;

import edu.clemson.cs.r2jt.ResolveCompiler;
import edu.clemson.cs.r2jt.absyn.*;
import edu.clemson.cs.r2jt.scope.*;
import edu.clemson.cs.r2jt.entry.*;
import edu.clemson.cs.r2jt.type.*;
import edu.clemson.cs.r2jt.utilities.Flag;
import edu.clemson.cs.r2jt.utilities.FlagDependencies;
import edu.clemson.cs.r2jt.location.*;
import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.collections.Iterator;
import edu.clemson.cs.r2jt.compilereport.CompileReport;
import edu.clemson.cs.r2jt.data.*; // import edu.clemson.cs.r2jt.errors.*;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.analysis.TypeResolutionException;
import edu.clemson.cs.r2jt.analysis.ProgramExpTypeResolver;
import edu.clemson.cs.r2jt.errors.ErrorHandler;

public class Verifier extends ResolveConceptualVisitor {

    private CompileEnvironment myInstanceEnvironment;
    private ErrorHandler err;
    private SymbolTable table;

    private StringBuffer usesItemBuf = new StringBuffer();
    private List<String> importList;
    private List<String> parmOpList;

    private List<String> typeParms;
    private List<String> concParms;

    // YS - Part of the cheap fix to getCurrentModuleDec not returning what
    //      we need. NEEDS TO GO!
    private FacilityModuleDec myFacilityModuleDec;

    private String name;

    // flag to turn on debug - I'm not sure how much this is used
    private boolean debug = false;

    // flag indicating we're applying the intialization rule. This probably can be removed by reworking the code
    private boolean initializationRule = false;

    private boolean isInInterface = false;

    // This buffer holds the verbose data
    private StringBuffer VCBuffer;

    // This buffer holds just the VCs
    private StringBuffer assrtBuf = new StringBuffer();

    private static final String FLAG_SECTION_NAME = "GenerateVCs";

    private static final String FLAG_DESC_VERBOSE_VC =
            "Generate VCs showing each step taken by the VC generator.";

    private static final String FLAG_DESC_ISABELLE_VC =
            "Generate VCs in an Isabelle format.";

    private static final String FLAG_DESC_REPARG_VC =
            "Generate VCs and generate warnings for repeated arguments.";

    private static final String FLAG_DESC_SIMPLIFY_VC =
            "Attempt to simplify VCs before sending them to the prover.";

    private static final String FLAG_DESC_REASONS_FOR_GIVEN =
            "Include Reasons for each given.";

    private static final String FLAG_DESC_VERIFY_VC = "Generate VCs.";

    private static final String FLAG_DESC_FINALVERB_VC =
            "Show Final VCs in Old 'Verbose' Format.";

    private static final String FLAG_DESC_LISTVCS_VC = "";

    /**
     * <p></p>
     */
    public static final Flag FLAG_VERBOSE_VC =
            new Flag(FLAG_SECTION_NAME, "verbose", FLAG_DESC_VERBOSE_VC);

    /**
     * <p></p>
     */
    public static final Flag FLAG_VERIFY_VC =
            new Flag(FLAG_SECTION_NAME, "VCs", FLAG_DESC_VERIFY_VC);

    /**
     * <p></p>
     */
    public static final Flag FLAG_ISABELLE_VC =
            new Flag(FLAG_SECTION_NAME, "isabelle", FLAG_DESC_ISABELLE_VC);

    /**
     * <p></p>
     */
    public static final Flag FLAG_REPARG_VC =
            new Flag(FLAG_SECTION_NAME, "repeated", FLAG_DESC_REPARG_VC);

    /**
     * <p></p>
     */
    public static final Flag FLAG_FINALVERB_VC =
            new Flag(FLAG_SECTION_NAME, "finalVerbose", FLAG_DESC_FINALVERB_VC,
                    Flag.Type.HIDDEN);
    /**
     * <p></p>
     */
    public static final Flag FLAG_SIMPLIFY_VC =
            new Flag(FLAG_SECTION_NAME, "simplify", FLAG_DESC_SIMPLIFY_VC);

    /**
     * <p></p>
     */
    public static final Flag FLAG_REASON_FOR_GIVEN =
            new Flag(FLAG_SECTION_NAME, "reasons", FLAG_DESC_REASONS_FOR_GIVEN);

    /**
     * <p></p>
     */
    public static final Flag FLAG_LISTVCS_VC =
            new Flag(FLAG_SECTION_NAME, "listVCs", FLAG_DESC_LISTVCS_VC,
                    Flag.Type.HIDDEN);

    final static List<DotExp> myRememberedExp = new List<DotExp>();

    public static OldExp buildOldExp(Exp original) {
        OldExp result = new OldExp(original.getLocation(), original);
        result.setType(original.getType());

        return result;
    }

    /*
     * Initialize flags for help
     */
    public static void setUpFlags() {

        FlagDependencies.addRequires(FLAG_VERBOSE_VC, FLAG_VERIFY_VC);
        FlagDependencies.addRequires(FLAG_REPARG_VC, FLAG_VERIFY_VC);
        FlagDependencies.addRequires(FLAG_ISABELLE_VC, FLAG_VERIFY_VC);
        FlagDependencies.addRequires(FLAG_SIMPLIFY_VC, FLAG_VERIFY_VC);
        FlagDependencies.addRequires(FLAG_LISTVCS_VC, FLAG_VERIFY_VC);

    }

    /**
     * A list that will be built up with <code>AssertiveCode</code> objects,
     * each representing a VC or group of VCs that must be satisfied to verify
     * a parsed program.
     */
    private Collection<AssertiveCode> myFinalVCs =
            new LinkedList<AssertiveCode>();

    /**
     * Construct a Verifier.
     */
    public Verifier(SymbolTable table,
            final CompileEnvironment instanceEnvironment) {
        this.table = table;

        myInstanceEnvironment = instanceEnvironment;
        this.err = myInstanceEnvironment.getErrorHandler();
        name = myInstanceEnvironment.getTargetFile().getName();
        name = name.substring(0, name.indexOf("."));

        /*try{
        	outFile = new FileWriter(getMainFileName());
        	outFile.write("");
        } catch (Exception ex){
        	System.err.println("Something went wrong when creating output file");
        }*/

        VCBuffer = new StringBuffer(buildHeaderComment());

        //Reset some static variables in AssertiveCode, in case the compiler
        //has not been re-instantiated since the last Verification attempt.
        AssertiveCode.resetVCNumbering();

    }

    private void addFreeVar(Dec var, AssertiveCode assertion) {
        ConcType freeVar = getFreeVar(var);
        if (freeVar != null)
            assertion.addFreeVar(freeVar);

    }

    private Exp addGlobalsAsFreeVariables(OperationDec curOperation,
            AssertiveCode assertion) {

        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();
        ModuleID cid = mid.getConceptID();
        Exp constraints = null;
        if (cid != null && cid.getName() != null) {
            /* Find Corresponding EnhancementModuleDec*/
            ConceptModuleDec cDec =
                    (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);
            addGlobalVariableForConcept(cDec, constraints, assertion, null);
        }

        ModuleDec modDec = getCurrentModuleBodyDec();

        if (modDec instanceof ConceptBodyModuleDec) {
            List<Dec> decs = ((ConceptBodyModuleDec) modDec).getDecs();
            Iterator<Dec> decIt = decs.iterator();
            while (decIt.hasNext()) {
                Dec myDec = decIt.next();
                if (myDec instanceof FacilityDec) {
                    FacilityDec facDec = (FacilityDec) myDec;
                    ModuleID id =
                            ModuleID.createConceptID(facDec.getConceptName());
                    if (myInstanceEnvironment.contains(id)) {

                        ModuleDec dec = myInstanceEnvironment.getModuleDec(id);
                        if (dec instanceof ConceptModuleDec) {
                            addGlobalVariableForConcept((ConceptModuleDec) dec,
                                    constraints, assertion, facDec.getName()
                                            .getName());
                        }
                    }
                }
            }
        }
        //List<UsesItem> list =modDec.getUsesItems();
        //Iterator<UsesItem> myUsesIt = list.iterator();
        //while(myUsesIt.hasNext()){
        //	UsesItem item = (UsesItem)myUsesIt.next();
        //	
        //    ModuleID id = ModuleID.createConceptID(item.getName());

        //    if (env.contains(id)) {

        //        ModuleDec dec = env.getModuleDec(id);
        //        if(dec instanceof ConceptModuleDec){
        //        	addGlobalVariableForConcept((ConceptModuleDec)dec, constraints, assertion);
        //        }
        //    }
        // }

        return constraints;
    }

    void addGlobalVariableForConcept(ConceptModuleDec cDec, Exp constraints,
            AssertiveCode assertion, String facName) {
        if (cDec != null) {

            List<Dec> decs = cDec.getDecs();
            Iterator<Dec> i = decs.iterator();

            while (i.hasNext()) {

                Dec tmpDec = i.next();
                if (tmpDec instanceof MathVarDec) {
                    VarDec tmpVarDec = toVarDec(tmpDec);
                    if (facName != null) {
                        //String name = tmpVarDec.getName().getName();
                        //name = facName + "." +name;
                        //PosSymbol newName = createPosSymbol(facName);
                        //newName.setSymbol(Symbol.symbol(name));
                        //tmpVarDec.setName(newName);
                    }

                    if (tmpVarDec != null) {
                        Exp constr = getConstraints(tmpVarDec);
                        if (isTrueExp(constr)) {

                        }
                        else if (constraints != null && constr != null) {
                            InfixExp tmp = new InfixExp();
                            tmp.setOpName(createPosSymbol("and"));
                            tmp.setLeft(constr);
                            tmp.setRight(constraints);
                            tmp.setType(BooleanType.INSTANCE);
                            constraints = tmp;
                        }
                        else
                            constraints = constr;

                        addFreeVar(tmpVarDec, assertion);
                    }
                }
            }
        }
    }

    private void addToContext(List<Dec> context, Dec declaration) {

        Iterator<Dec> it = context.iterator();
        Dec myDec;
        boolean found = false;

        while (it.hasNext()) {
            myDec = it.next();
            if (myDec.getName().equals(declaration.getName())) {
                found = true;
            }

        }
        if (!found) {
            context.add(declaration);
        }

    }

    // ===========================================================
    // Public Methods - Abstract Visit Methods
    // ===========================================================

    private void appendToLocation(Exp exp, String text) {
        if (exp != null && exp.getLocation() != null
                && exp.getLocation().getDetails() != null) {
            exp.getLocation().setDetails(
                    exp.getLocation().getDetails().concat(text));
        }

        if (exp instanceof InfixExp) {
            appendToLocation(((InfixExp) exp).getLeft(), text);
            appendToLocation(((InfixExp) exp).getRight(), text);
        }

    }

    private void applyAssumeRule(VerificationStatement assume,
            AssertiveCode assertion) {

        if ((Exp) assume.getAssertion() instanceof VarExp
                && ((VarExp) assume.getAssertion()).getName().toString()
                        .equals(getTrueVarExp().getName().toString())) {
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nAssume Rule Applied and Simplified: \n");
            VCBuffer.append(assertion.assertionToString());
        }
        else {
            Exp conf = assertion.getFinalConfirm();
            InfixExp newConf = new InfixExp();
            PosSymbol opName = new PosSymbol();
            opName.setSymbol(Symbol.symbol("implies"));
            newConf.setType(BooleanType.INSTANCE);

            newConf.setLeft(((Exp) assume.getAssertion()));
            newConf.setOpName(opName);
            newConf.setRight(conf);
            assertion.setFinalConfirm(newConf);
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nAssume Rule Applied: \n");
            VCBuffer.append(assertion.assertionToString());
        }
    }

    private void applyAuxCodeRule(AuxCodeStmt stmt, AssertiveCode assertion) {

        Iterator<Statement> i = stmt.getStatements().iterator();
        while (i.hasNext()) {
            assertion.addCode(i.next());
        }

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nAux_Code Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());

    }

    private void applyChangeRule(VerificationStatement change,
            AssertiveCode assertion) {

        Exp finalConf = assertion.getFinalConfirm();
        Object tmplist = change.getAssertion();
        List<Exp> list = new List<Exp>();
        if (tmplist instanceof List<?>) {
            list = (List<Exp>) tmplist;
        }

        Iterator<Exp> it = list.iterator();
        while (it.hasNext()) {
            Object tmpObj = it.next();
            if (tmpObj instanceof ConcType) {
                ConcType tmp = (ConcType) tmpObj;
                VarExp tmpVarExp = new VarExp();
                tmpVarExp.setName(tmp.getName());
                VarExp repVarExp = new VarExp();
                tmp = NQV(finalConf, tmp, assertion);
                repVarExp.setName(tmp.getName());

                finalConf = replace(finalConf, tmpVarExp, repVarExp);

                if (finalConf.containsVar(tmp.getName().toString(), false))
                    assertion.addFreeVar(tmp);
            }
        }
        assertion.setFinalConfirm(finalConf);
        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nChange Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());
    }

    private void applyConfirmRule(VerificationStatement confirm,
            AssertiveCode assertion) {
        if ((Exp) confirm.getAssertion() instanceof VarExp
                && ((VarExp) confirm.getAssertion()).getName().toString()
                        .equals(getTrueVarExp().getName().toString())) {
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nConfirm Rule Applied and Simplified: \n");
            VCBuffer.append(assertion.assertionToString());
        }
        else {

            Exp conf = assertion.getFinalConfirm();
            InfixExp newConf = new InfixExp();
            PosSymbol opName = new PosSymbol();
            opName.setSymbol(Symbol.symbol("and"));
            newConf.setType(BooleanType.INSTANCE);

            newConf.setLeft((Exp) confirm.getAssertion());
            newConf.setOpName(opName);
            newConf.setRight(conf);

            assertion.setFinalConfirm(newConf);
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nConfirm Rule Applied: \n");
            VCBuffer.append(assertion.assertionToString());
        }

    }

    private void applyEBAssumeStmt(AssumeStmt stmt, AssertiveCode assertion) {
        if ((Exp) stmt.getAssertion() instanceof VarExp
                && ((VarExp) stmt.getAssertion()).getName().toString().equals(
                        getTrueVarExp().getName().toString())) {
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nAssume Rule Applied and Simplified: \n");
            VCBuffer.append(assertion.assertionToString());
        }
        else {
            Exp conf = assertion.getFinalConfirm();
            conf = replaceAssumeRule(stmt, conf, assertion);
            if (stmt.getAssertion() != null) {
                InfixExp newConf = new InfixExp();
                PosSymbol opName = new PosSymbol();
                opName.setSymbol(Symbol.symbol("implies"));
                newConf.setLeft(((Exp) stmt.getAssertion()));
                newConf.setOpName(opName);
                newConf.setRight(conf);
                newConf.setType(BooleanType.INSTANCE);
                assertion.setFinalConfirm(newConf);
            }
            else
                assertion.setFinalConfirm(conf);
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nAssume Rule Applied: \n");
            VCBuffer.append(assertion.assertionToString());
        }
    }

    // ===========================================================
    // Public Methods - Declarations
    // ===========================================================

    // -----------------------------------------------------------
    // Module Declarations
    // -----------------------------------------------------------

    private void applyEBCallStmtRule(CallStmt stmt, AssertiveCode assertion) {
        Exp ensures = null;
        Exp requires = null;

        ModuleID mid = getCurrentModuleID();
        ModuleDec ebDec = null;
        if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ebDec =
                    (EnhancementBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
        }
        else if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ebDec =
                    (ConceptBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
        }
        else if (mid.getModuleKind() == ModuleKind.FACILITY) {
            ebDec = (FacilityModuleDec) myInstanceEnvironment.getModuleDec(mid);
        }

        /* Find Corresponding OperationDec and Specification*/
        OperationDec opDec = null;
        if (ebDec instanceof EnhancementBodyModuleDec) {
            opDec =
                    getEnhancementOperationDec(stmt.getName(), stmt
                            .getArguments());
        }
        else if (ebDec instanceof ConceptBodyModuleDec) {
            opDec = getConceptOperationDec(stmt.getName(), stmt.getArguments());
        }
        else if (ebDec instanceof FacilityModuleDec) {
            opDec =
                    getFacilityOperationDec(stmt.getName(), stmt.getArguments());
        }

        //OpDec may have append specification move repeated argument call here and add the opDec, assertion is what i need to modify
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_REPARG_VC)) {
            RepeatedArguments ra = new RepeatedArguments();
            ra.checkRepeatedArguments(stmt, assertion, opDec);
        }
        /* Get Ensures Clause - Set to "true" if nonexistent*/
        if (opDec != null) {
            Exp ens = (Exp) (((OperationDec) opDec).getEnsures());
            if (ens != null)
                ensures = (Exp) ens.clone();
            else
                ensures = getTrueVarExp();

            Exp req = ((OperationDec) opDec).getRequires();
            if (req != null)
                requires = (Exp) req.clone();

            ensures =
                    modifyEnsuresIfCallingQuantified(ensures, opDec, assertion,
                            ensures);

            if (((OperationDec) opDec).getEnsures() != null)
                ensures.setLocation(((OperationDec) opDec).getEnsures()
                        .getLocation());
            if (ensures.getLocation() != null)
                ensures.getLocation().setDetails(
                        "Ensures Clause For " + opDec.getName());

        }
        else {
            ensures = getTrueVarExp();
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nSpec not found \n");
            VCBuffer.append(assertion.assertionToString());

            System.err.println("Error passed operation not found: "
                    + stmt.getName().asString(1, 1));
            throw new RuntimeException("Error passed operation not found: "
                    + stmt.getName().asString(1, 1));
        }
        Dec tmp = getCurrentProcedure();

        /* Is this a recursive Call of Itself */
        if (tmp instanceof ProcedureDec
                && opDec.getName().toString().equals(
                        ((ProcedureDec) tmp).getName().toString())
                && compareArguments(stmt.getArguments(), ((ProcedureDec) tmp)
                        .getParameters())
                && ((ProcedureDec) tmp).getDecreasing() != null) {

            VarExp var = new VarExp();
            ConcType pval = getPVAL();

            var.setName(pval.getName());

            InfixExp PExp = new InfixExp();
            PExp.setLeft((Exp) ((ProcedureDec) tmp).getDecreasing().clone());
            PExp.setRight((Exp) var.clone());
            PExp.setOpName(createPosSymbol("<"));
            PExp.setType(BooleanType.INSTANCE);
            ConfirmStmt conf = new ConfirmStmt();
            if (((ProcedureDec) tmp).getDecreasing().getLocation() != null) {
                Location loc =
                        ((ProcedureDec) tmp).getDecreasing().getLocation();
                loc.setDetails("Show Termination of Recursive Call");
                setLocation(PExp, loc);
            }
            conf.setAssertion(PExp);
            assertion.addCode(conf);
        }

        ensures = modifyEnsuresForParameterModes(ensures, opDec, stmt);

        /* Is there a reassigns, Can we use the Simple Rule */
        /*List<ParameterVarDec> paramsRea = containsReassigns(opDec);
        if(!paramsRea.isEmpty()){   
        	if(!inSimpleForm(ensures, paramsRea)){
            //	VCBuffer.append("\n_____________________ \n");
            //	VCBuffer.append("\nInappropriate Use of Reassigns: \n");
        	}
        	else {
        		
        		
        	Exp simpleConf = applySimpleCallRule(requires, ensures, opDec, 
        			(CallStmt)stmt.clone(), paramsRea, assertion);
        
        	assertion.setFinalConfirm(simpleConf);   	
        	VCBuffer.append("\n_____________________ \n");
        	VCBuffer.append("\nSimple Operation Call Rule Applied: \n");
        	VCBuffer.append(assertion.assertionToString());
        	return;
        	}
        }*/

        VCBuffer.append("\n");

        /* General Call Rule */

        /* Replace PreCondition Variables */
        requires =
                replacePreConditionVariables(requires, stmt.getArguments(),
                        opDec, assertion);

        /* Replace PostCondition Variables */
        ensures =
                replacePostConditionVariables(stmt.getArguments(), ensures,
                        opDec, assertion);
        Exp conf = assertion.getFinalConfirm();

        /*PosSymbol opName = new PosSymbol();
        opName.setSymbol(Symbol.symbol("implies"));*/

        if (requires != null) {
            if (stmt.getName().getLocation() != null) {
                Location loc = (Location) stmt.getName().getLocation().clone();
                Dec myDec = getCurrentProcedure();
                String details = "";
                if (myDec != null) {
                    details = " in Procedure " + myDec.getName();
                }
                loc.setDetails("Requires Clause of " + opDec.getName()
                        + details);
                setLocation(requires, loc);
            }
            else {
                Location loc = new Location(null, null);
                Dec myDec = getCurrentProcedure();
                String details = "";
                if (myDec != null) {
                    details = " in Procedure " + myDec.getName();
                }
                loc.setDetails("Requires Clause of " + opDec.getName()
                        + details);
                setLocation(requires, loc);
            }
            assertion.addConfirm(requires);
        }
        if (ensures != null) {
            if (stmt.getName().getLocation() != null) {
                Location loc = (Location) stmt.getName().getLocation().clone();
                loc.setDetails("Ensures Clause of " + opDec.getName());
                setLocation(ensures, loc);
            }
            assertion.addAssume(ensures);
        }

        assertion.setFinalConfirm(conf);
        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nOperation Call Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());

    }

    private void applyEBConfirmStmtRule(ConfirmStmt stmt,
            AssertiveCode assertion) {
        if ((Exp) stmt.getAssertion() instanceof VarExp
                && ((VarExp) stmt.getAssertion()).getName().toString().equals(
                        getTrueVarExp().getName().toString())) {
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nConfirm Rule Applied and Simplified: \n");
            VCBuffer.append(assertion.assertionToString());
        }
        else if (assertion.getFinalConfirm() instanceof VarExp
                && ((VarExp) assertion.getFinalConfirm()).getName().toString()
                        .equals(getTrueVarExp().getName().toString())) {
            assertion.setFinalConfirm(stmt.getAssertion());
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nConfirm Rule Applied and Simplified: \n");
            VCBuffer.append(assertion.assertionToString());
        }
        else {

            Exp conf = assertion.getFinalConfirm();
            InfixExp newConf = new InfixExp();
            PosSymbol opName = new PosSymbol();
            opName.setSymbol(Symbol.symbol("and"));
            newConf.setType(BooleanType.INSTANCE);
            Exp confirm = (Exp) stmt.getAssertion();
            //	confirm.setLocation(stmt.getLocation());
            newConf.setLeft(confirm);
            newConf.setOpName(opName);
            newConf.setRight(conf);

            assertion.setFinalConfirm(newConf);
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nConfirm Rule Applied: \n");
            VCBuffer.append(assertion.assertionToString());
        }

    }

    private void applyEBFuncAssignStmtRule(FuncAssignStmt stmt,
            AssertiveCode assertion) {

        Exp cond = invk_cond(stmt.getAssign(), assertion);

        if (cond != null) {
            Location loc = (Location) stmt.getAssign().getLocation().clone();
            Dec myDec = getCurrentProcedure();
            String details = "";
            if (myDec != null) {
                details = " in Procedure " + myDec.getName();
            }
            loc.setDetails("Requires Clause of " + stmt.getAssign() + details);
            setLocation(cond, loc);
            assertion.addConfirm(cond);
        }

        Exp replacement = getCorAssignPartExp(stmt.getAssign(), assertion);
        Exp var = null;
        if (stmt.getVar() instanceof VariableDotExp) {
            var = new DotExp();
            ((DotExp) var).setType(stmt.getVar().getType());
            List<VariableExp> segements =
                    ((VariableDotExp) stmt.getVar()).getSegments();
            List<Exp> newSegements = new List<Exp>();
            Iterator<VariableExp> it = segements.iterator();
            while (it.hasNext()) {
                VarExp varExp = new VarExp();
                VariableExp varName = (VariableExp) it.next();
                if (varName instanceof VariableNameExp) {
                    varExp.setName(((VariableNameExp) varName).getName());
                    varExp.setType(((VariableNameExp) varName).getType());
                    newSegements.add(varExp);
                }
                else {
                    System.err.println("Problem");
                }
            }
            ((DotExp) var).setSegments(newSegements);
        }
        else if (stmt.getVar() instanceof VariableExp) {
            var = new VarExp();
            ((VarExp) var)
                    .setName(createPosSymbol(getVarNameStr(stmt.getVar())));
            ((VarExp) var).setType(stmt.getVar().getType());
        }
        /*		if(stmt.getAssign() instanceof ProgramParamExp){
         ModuleDec ebDec = null;
         ModuleID mid = getCurrentModuleID();
         if(mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY){
         ebDec = 
         (EnhancementBodyModuleDec)env.getModuleDec(mid);
         } else if(mid.getModuleKind() == ModuleKind.CONCEPT_BODY){
         ebDec = 
         (ConceptBodyModuleDec)env.getModuleDec(mid);
         } else if(mid.getModuleKind() == ModuleKind.FACILITY){
         ebDec = 
         (FacilityModuleDec)env.getModuleDec(mid);
         }
        
         OperationDec opDec = null;
         if(ebDec instanceof EnhancementBodyModuleDec){
         opDec = getEnhancementOperationDec(((ProgramParamExp)stmt.getAssign()).getName(), ((ProgramParamExp)stmt.getAssign()).getArguments());
         } else if(ebDec instanceof ConceptBodyModuleDec){
         opDec = getConceptOperationDec(((ProgramParamExp)stmt.getAssign()).getName(),((ProgramParamExp)stmt.getAssign()).getArguments());
         } else if(ebDec instanceof FacilityModuleDec){
         opDec = getFacilityOperationDec(((ProgramParamExp)stmt.getAssign()).getName(),((ProgramParamExp)stmt.getAssign()).getArguments());
         }
         VarDec tmpVarDec = new VarDec();
         tmpVarDec.setTy(opDec.getReturnTy());
         tmpVarDec.setName(var.getName());
        
         Exp constr = getConstraints(tmpVarDec);
         constr = constr.replace(var, replacement);		
         //	System.out.println("Constr: "+  constr)	;
         assertion.addAssume(constr);	
         }*/

        Exp conf = assertion.getFinalConfirm();
        //String str = conf.toString(0);
        conf = conf.replace(var, replacement);

        assertion.setFinalConfirm(conf);
        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nFunction Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());
    }

    /**
     * 
     * @param assertion This <code>AssertiveCode</code> will be stored for later
     *                  use and therefore should be considered immutable after
     *                  a call to this method.
     */
    private void applyEBRules(AssertiveCode assertion) {

        while (assertion.hasAnotherAssertion()) {
            VerificationStatement curAssertion = assertion.getLastAssertion();
            if (curAssertion.getType() == VerificationStatement.ASSUME)
                applyAssumeRule(curAssertion, assertion);
            else if (curAssertion.getType() == VerificationStatement.CONFIRM)
                applyConfirmRule(curAssertion, assertion);
            else if (curAssertion.getType() == VerificationStatement.CODE) {
                visitEBCodeRule(curAssertion, assertion);
                if ((Statement) curAssertion.getAssertion() instanceof WhileStmt
                        || (Statement) curAssertion.getAssertion() instanceof IfStmt)
                    return;
            }
            else if (curAssertion.getType() == VerificationStatement.REMEMBER)
                applyRememberRule(curAssertion, assertion);
            else if (curAssertion.getType() == VerificationStatement.VARIABLE)
                applyVariableDeclRule(curAssertion, assertion);
            else if (curAssertion.getType() == VerificationStatement.CHANGE)
                applyChangeRule(curAssertion, assertion);
            else {}
            if (myInstanceEnvironment.flags.isFlagSet(FLAG_SIMPLIFY_VC))
                applySimplificationRules(assertion);
        }
        assertion.setName(name);
        myFinalVCs.add(assertion);
        assrtBuf.append(assertion.assertionToString(true) + "\n\n");
        return;
    }

    // -----------------------------------------------------------
    // Type Declarations
    // -----------------------------------------------------------

    private void applyEBSwapStmtRule(SwapStmt stmt, AssertiveCode assertion) {

        Exp conf = assertion.getFinalConfirm();

        VariableExp left = (VariableExp) stmt.getLeft().clone();
        VariableExp right = (VariableExp) stmt.getRight().clone();

        String lftStr = getVarName(left).toString();

        String lftTmp = "_";
        lftTmp = lftTmp.concat(lftStr);

        Exp leftV;
        Exp rightV;
        if (left instanceof VariableDotExp) {
            leftV = new DotExp();
            ((DotExp) leftV).setSemanticExp(((VariableDotExp) left)
                    .getSemanticExp());
            List<Exp> myList = new List<Exp>();
            for (int i = 0; i < ((VariableDotExp) left).getSegments().size(); i++) {
                VariableExp varExp =
                        ((VariableDotExp) left).getSegments().get(i);
                varExp.setType(left.getType());
                myList.add(i, varExp);
            }
            ((DotExp) leftV).setSegments(myList);
        }
        else {
            leftV = new VarExp();
            ((VarExp) leftV).setName(getVarName(left));
        }
        leftV.setType(left.getType());

        if (right instanceof VariableDotExp) {
            rightV = new DotExp();
            ((DotExp) rightV).setSemanticExp(((VariableDotExp) right)
                    .getSemanticExp());
            List<Exp> myList = new List<Exp>();
            for (int i = 0; i < ((VariableDotExp) right).getSegments().size(); i++) {
                VariableExp varExp =
                        ((VariableDotExp) right).getSegments().get(i);
                varExp.setType(right.getType());
                myList.add(i, varExp);
            }
            ((DotExp) rightV).setSegments(myList);
        }
        else {
            rightV = new VarExp();
            ((VarExp) rightV).setName(getVarName(right));

        }

        // Need to Set Exp for rightV and leftV

        List lst = conf.getSubExpressions();
        for (int i = 0; i < lst.size(); i++) {
            if (lst.get(i) instanceof VarExp) {
                VarExp thisExp = (VarExp) lst.get(i);
                if (rightV instanceof VarExp) {
                    if (thisExp.getName().toString().equals(
                            ((VarExp) rightV).getName().toString())) {
                        rightV.setType(thisExp.getType());
                    }
                }
                if (leftV instanceof VarExp) {
                    if (thisExp.getName().toString().equals(
                            ((VarExp) leftV).getName().toString())) {
                        leftV.setType(thisExp.getType());
                    }
                }
            }
        }

        VarExp tmp = new VarExp();
        tmp.setName(createPosSymbol(lftTmp));
        tmp.setType(left.getType());

        conf = replace(conf, rightV, tmp);
        conf = replace(conf, leftV, rightV);
        conf = replace(conf, tmp, leftV);

        //	  	conf = replace(conf, right, tmp);
        //	  	conf = replace(conf, left, right);
        //	  	conf = replace(conf, tmp, left);

        assertion.setFinalConfirm(conf);

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nSwap Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());
    }

    private void applyEBWhileRuleStmt(WhileStmt stmt, AssertiveCode assertion) {

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nWhile Statement Rule: \n");

        /* I want to re-arrange this to convert the while rule into an if statement as does the latest version of the rule */
        /* Part One */
        applyPartOneWhileRule(stmt, assertion);

        /* Part Two */
        applyPartTwoWhileRule(stmt, assertion);

        VCBuffer.append("\n\tEnd While Rule \n");

    }

    private void applyIfStmtRule(IfStmt stmt, AssertiveCode assertion) {

        /* If Part */
        AssertiveCode ifAssertion = (AssertiveCode) assertion.clone();
        appendToLocation(ifAssertion.confirm, " , If \"if\" condition at "
                + stmt.getTest().getLocation().toString() + " is true");

        Exp conf =
                (Exp) invk_cond((ProgramExp) stmt.getTest().clone(), assertion)
                        .clone();
        if (conf != null) {
            Location loc;
            if (conf.getLocation() != null) {
                loc = (Location) (conf.getLocation()).clone();
            }
            else {
                loc = (Location) stmt.getTest().getLocation().clone();
            }
            if (loc != null) {
                loc.setDetails("Requirements from Condition from If Statement");
                setLocation(conf, loc);
            }
            ifAssertion.addConfirm(conf);
        }
        Exp assume =
                getCorAssignPartExp((ProgramExp) stmt.getTest().clone(),
                        ifAssertion);
        if (stmt.getTest().getLocation() != null) {
            Location loc = (Location) (stmt.getTest().getLocation()).clone();
            loc.setDetails("If Statement Condition");
            setLocation(assume, loc);
        }
        ifAssertion.addAssume(assume);

        Iterator<Statement> i = stmt.getThenclause().iterator();
        while (i.hasNext()) {
            ifAssertion.addCode(i.next());
        }

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nIf Part Rule Applied: \n");
        VCBuffer.append(ifAssertion.assertionToString());

        applyEBRules(ifAssertion);
        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\n\nIf Part Rule Completed \n");

        /* Negation of If Part */
        AssertiveCode negifAssertion = (AssertiveCode) assertion.clone();
        appendToLocation(negifAssertion.confirm, " , If \"if\" condition at "
                + stmt.getTest().getLocation().toString() + " is false");

        Exp cond = invk_cond((ProgramExp) stmt.getTest().clone(), assertion);
        if (cond != null) {
            Location loc;
            if (cond.getLocation() != null) {
                loc = (Location) (cond.getLocation()).clone();
            }
            else {
                loc = (Location) stmt.getTest().getLocation().clone();
            }
            if (loc != null) {

                loc.setDetails("Requirements If Statement Condition");
                setLocation(cond, loc);
            }
        }
        ifAssertion.addConfirm(cond);
        Exp tmp =
                getCorAssignPartExp((ProgramExp) stmt.getTest().clone(),
                        ifAssertion);
        Exp neg = negateExp(tmp);
        if (stmt.getTest().getLocation() != null) {
            Location loc = (Location) (stmt.getTest().getLocation()).clone();
            loc.setDetails("If Statement Condition Negated");
            setLocation(neg, loc);
        }
        negifAssertion.addAssume(neg);

        if (stmt.getElseclause() != null) {
            i = stmt.getElseclause().iterator();
            while (i.hasNext()) {
                negifAssertion.addCode(i.next());
            }
        }

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nNegation of If Part Rule Applied: \n");
        VCBuffer.append(negifAssertion.assertionToString());

        applyEBRules(negifAssertion);

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\n\n\nNegation of If Part Rule Completed\n");

    }

    private void applyIterateRule(IterateStmt stmt, AssertiveCode assertion) {

        assertion.addConfirm(stmt.getMaintaining());
        if (!stmt.getChanging().isEmpty()) {
            List<ConcType> change = getChangeList(stmt, assertion);
            assertion.addChange(change);
        }

        assertion.addAssume(getIterateRuleAssume(stmt, assertion));
        Iterator<Statement> i;
        i = stmt.getStatements().iterator();

        while (i.hasNext()) {
            Statement tmp = i.next();
            if (tmp instanceof IterateExitStmt) {
                assertion.addCode(processIterateExitStmt(stmt, assertion, i,
                        tmp));
            }
            else
                assertion.addCode(tmp);
        }

        assertion.setFinalConfirm(getTrueVarExp());
        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nIterate Statement Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());

    }

    private void applyPartOneWhileRule(WhileStmt stmt, AssertiveCode assertion) {

        ModuleDec moduleDec = getCurrentBodyModuleDec();
        boolean concept = false;
        VarExp exemplar = null, cExem = null;
        if (moduleDec != null && moduleDec instanceof ConceptBodyModuleDec) {
            concept = true;

            ConceptModuleDec cmDec = (ConceptModuleDec) getCurrentModuleDec();

            Iterator<Dec> decs = cmDec.getDecs().iterator();
            while (decs.hasNext()) {
                Dec tmpDec = decs.next();
                if (tmpDec instanceof TypeDec) {

                    exemplar = new VarExp();
                    Type exemType =
                            getTypeFromTy(((TypeDec) tmpDec).getModel());

                    cExem = new VarExp();
                    exemplar.setName(((TypeDec) tmpDec).getExemplar());
                    exemplar.setType(exemType);

                    cExem.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    cExem.setType(exemType);

                    VarDec concVar = new VarDec();
                    concVar.setName(createPosSymbol("Conc"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    concVar.setTy(((TypeDec) tmpDec).getModel());
                    addFreeVar(concVar, assertion);
                }
            }

        }

        AssertiveCode part_one = (AssertiveCode) assertion.clone();
        Exp maintaining = getTrueVarExp();
        if (stmt.getMaintaining() != null) {
            maintaining = (Exp) stmt.getMaintaining().clone();

            if (maintaining.getLocation() != null) {
                Location loc = (Location) (maintaining.getLocation()).clone();
                Dec myDec = getCurrentProcedure();
                String details = "";
                if (myDec != null) {
                    details = " in Procedure " + myDec.getName();
                }
                loc.setDetails("Base Case of the Invariant of While Statement"
                        + details);
                setLocation(maintaining, loc);
            }
        }

        part_one.addConfirm(maintaining);

        Exp assume = getWhileRuleAssume(stmt, assertion);

        // add Change
        List<ConcType> change = getChangeList(stmt, assertion);
        if (!change.isEmpty())
            part_one.addChange(change);

        if (concept) {
            //		assume = replace(assume, exemplar, cExem);       		
            //		assume = replace(assume , new OldExp(null, exemplar), new OldExp(null, cExem));
            part_one.addAssume(assume);
        }
        else {
            part_one.addAssume(assume);
        }

        Exp cond = invk_cond(stmt.getTest(), assertion);
        if (cond != null) {
            part_one.addConfirm(cond);
        }

        // add Assume
        Exp test = getMathTest(stmt.getTest(), stmt, assertion);
        test.setLocation((Location) stmt.getTest().getLocation().clone());
        test.getLocation().setDetails("While Loop Condition");
        part_one.addAssume(test);

        Iterator<Statement> i = stmt.getStatements().iterator();
        while (i.hasNext()) {
            part_one.addCode((Statement) (i.next()).clone());
        }
        Exp conf = getTrueVarExp();
        if (stmt.getDecreasing() != null)
            conf = getWhileRuleConfirm(stmt, assertion);
        else
            conf = stmt.getMaintaining();

        Location loc = (Location) conf.getLocation();
        if (loc != null) {

            Dec myDec = getCurrentProcedure();
            String details = "";
            if (myDec != null) {
                details = " in Procedure " + myDec.getName();
            }
            loc.setDetails("Inductive Case of Invariant of While Statement"
                    + details);
            setLocation(conf, loc);
        }
        setLocation(conf, loc);

        if (concept) {
            //	conf = replace(conf, exemplar, cExem);       		
            //	conf = replace(conf , new OldExp(null, exemplar), new OldExp(null, cExem));
            part_one.setFinalConfirm(conf);
        }
        else {
            part_one.setFinalConfirm(conf);
        }

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\n\tPart One: \n");
        VCBuffer.append(part_one.assertionToString());

        applyEBRules(part_one);

        VCBuffer.append("\n\n\n\tEnd Part One \n");

    }

    private void applyPartTwoWhileRule(WhileStmt stmt, AssertiveCode assertion) {

        ModuleDec moduleDec = getCurrentBodyModuleDec();
        boolean concept = false;
        VarExp exemplar = null, cExem = null;
        if (moduleDec != null && moduleDec instanceof ConceptBodyModuleDec) {
            concept = true;

            ConceptModuleDec cmDec = (ConceptModuleDec) getCurrentModuleDec();

            Iterator<Dec> decs = cmDec.getDecs().iterator();
            while (decs.hasNext()) {
                Dec tmpDec = decs.next();
                if (tmpDec instanceof TypeDec) {

                    exemplar = new VarExp();

                    cExem = new VarExp();
                    exemplar.setName(((TypeDec) tmpDec).getExemplar());

                    Type exemType =
                            getTypeFromTy(((TypeDec) tmpDec).getModel());
                    exemplar.setType(exemType);
                    cExem.setType(exemType);

                    cExem.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    VarDec concVar = new VarDec();
                    concVar.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    concVar.setTy(((TypeDec) tmpDec).getModel());
                    addFreeVar(concVar, assertion);
                }
            }

        }

        AssertiveCode part_two = (AssertiveCode) assertion.clone();

        List<ConcType> change = getChangeList(stmt, assertion);
        if (!change.isEmpty())
            part_two.addChange(change);

        // add Change
        Exp assume = getTrueVarExp();
        if (stmt.getMaintaining() != null) {
            assume = (Exp) stmt.getMaintaining().clone();
        }

        if (assume.getLocation() != null) {
            Location loc = (Location) assume.getLocation().clone();
            loc.setDetails("Invariant");
            setLocation(assume, loc);
        }
        if (concept) {
            //	assume = replace(assume, exemplar, cExem);       		
            //	assume = replace(assume , new OldExp(null, exemplar), new OldExp(null, cExem));
            part_two.addAssume(assume);
        }
        else {
            part_two.addAssume(assume);
        }

        Exp cond = invk_cond(stmt.getTest(), assertion);
        if (cond != null) {
            part_two.addConfirm(cond);
        }

        Exp negateExp = negateExp(getMathTest(stmt.getTest(), stmt, assertion));
        if ((Location) stmt.getTest().getLocation() != null) {
            negateExp.setLocation((Location) stmt.getTest().getLocation()
                    .clone());
            negateExp.getLocation().setDetails("While Loop Condition Negated");
        }
        part_two.addAssume(negateExp);

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\n\tPart Two: \n");
        VCBuffer.append(part_two.assertionToString());

        applyEBRules(part_two);

        VCBuffer.append("\n\n\n\tEnd Part Two \n");

    }

    private void applyProofRulesToAssertiveCode(Statement stmt,
            AssertiveCode assertion) {
        VCBuffer.append("\t\t");

        if (stmt instanceof FuncAssignStmt) {
            applyEBFuncAssignStmtRule((FuncAssignStmt) stmt, assertion);
        }
        else if (stmt instanceof SwapStmt) {
            applyEBSwapStmtRule((SwapStmt) stmt, assertion);
        }
        else if (stmt instanceof CallStmt) {
            applyEBCallStmtRule((CallStmt) stmt, assertion);
        }
        else if (stmt instanceof IfStmt) {
            applyIfStmtRule((IfStmt) stmt, assertion);
        }
        else if (stmt instanceof IterateExitStmt) {}
        else if (stmt instanceof IterateStmt) {
            applyIterateRule((IterateStmt) stmt, assertion);
        }
        else if (stmt instanceof MemoryStmt) {}
        else if (stmt instanceof SelectionStmt) {}
        else if (stmt instanceof AssumeStmt) {
            applyEBAssumeStmt((AssumeStmt) stmt, assertion);
        }
        else if (stmt instanceof ConfirmStmt) {
            applyEBConfirmStmtRule((ConfirmStmt) stmt, assertion);
        }
        else if (stmt instanceof WhileStmt) {
            applyEBWhileRuleStmt((WhileStmt) stmt, assertion);
        }
        else if (stmt instanceof AuxCodeStmt) {
            applyAuxCodeRule((AuxCodeStmt) stmt, assertion);
        }
        else {
            assert false;
        }

        //hack
        ModuleDec moduleDec = getCurrentBodyModuleDec();
        if (moduleDec != null) {
            ModuleID mid = getCurrentModuleID();
            if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {

                Exp conf = fixupTypes(assertion.getFinalConfirm());
                assertion.setFinalConfirm(conf);
            }
        }

        VCBuffer.append("\n");
    }

    /*
     * Applies the Proof rule for Remember
     */
    private void applyRememberRule(VerificationStatement remember,
            AssertiveCode assertion) {
        Exp conf = assertion.getFinalConfirm();
        conf = conf.remember();

        assertion.setFinalConfirm(conf);

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nRemember Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());
    }

    private Exp applySimpleCallRule(Exp requires, Exp ensures,
            OperationDec opDec, CallStmt stmt, List<ParameterVarDec> paramsRea,
            AssertiveCode assertion) {

        requires =
                replacePreConditionVariables(requires, stmt.getArguments(),
                        opDec, assertion);
        if (ensures == null)
            ensures =
                    replaceSimplePostConditionVariables(stmt.getArguments(),
                            opDec, assertion, true);
        else {
            ensures =
                    replaceSimplePostConditionVariables(stmt.getArguments(),
                            opDec, assertion, true);

        }

        Exp conf = assertion.getFinalConfirm();

        if (ensures instanceof EqualsExp) {
            if (((EqualsExp) ensures).getLeft() instanceof VarExp) {
                //replace every instance of left side exp with right side	
                Exp tmp =
                        conf.replace((Exp) ((EqualsExp) ensures).getLeft()
                                .clone(), (Exp) ((EqualsExp) ensures)
                                .getRight().clone());
                ;
                if (tmp != null)
                    conf = tmp;
            }
            else {
                //replace every instance of right side exp with left side	
                Exp tmp =
                        conf.replace(((EqualsExp) ensures).getRight(),
                                ((EqualsExp) ensures).getLeft());
                if (tmp != null)
                    conf = tmp;
            }
        }
        else if (ensures instanceof InfixExp) {
            if (((InfixExp) ensures).getOpName().equals("and")) {
                // apply again to each part of and statement 
                //(if all parts are in simple form)
                if (inSimpleForm(((InfixExp) ensures).getLeft(), paramsRea)
                        && inSimpleForm(((InfixExp) ensures).getRight(),
                                paramsRea)) {
                    Exp tmp =
                            applySimpleCallRule(requires, ((InfixExp) ensures)
                                    .getLeft(), opDec, stmt, paramsRea,
                                    assertion);
                    if (tmp != null)
                        conf = tmp;
                    AssertiveCode tmpAC =
                            new AssertiveCode(myInstanceEnvironment);
                    tmpAC.setFinalConfirm(conf);
                    tmp =
                            applySimpleCallRule(requires, ((InfixExp) ensures)
                                    .getRight(), opDec, stmt, paramsRea, tmpAC);
                    if (tmp != null)
                        conf = tmp;
                }
            }
        }

        InfixExp newConf = new InfixExp();
        if (requires != null) {
            if (requires instanceof VarExp
                    && ((VarExp) requires).getName().toString().equals("true"))
                return conf;
            else {
                newConf = InfixExp.formAndStmt(conf, requires);
            }
        }
        else
            return conf;
        return newConf;
    }

    private void applySimplificationRules(AssertiveCode assertion) {
        Exp simplified = ((Exp) assertion.getFinalConfirm().clone()).simplify();

        if (!simplified.toString(1).equals(
                assertion.getFinalConfirm().toString(1))) {
            assertion.setFinalConfirm(simplified);
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("Assertion Simplified:" + "\n\n");
            VCBuffer.append(assertion.assertionToString());
        }
    }

    private void applyVariableDeclRule(VerificationStatement var,
            AssertiveCode assertion) {
        if (var != null) {
            VarDec varDec = (VarDec) var.getAssertion();

            addFreeVar(varDec, assertion);

            Exp conf = assertion.getFinalConfirm();
            InfixExp newConf = new InfixExp();
            newConf.setRight(conf);

            Exp init = (Exp) getInitialExp((VarDec) var.getAssertion()).clone();
            if (init.getLocation() != null) {
                Location loc = init.getLocation();
                init.getLocation().setDetails(
                        "Initial Value for " + varDec.getName().getName());
                setLocation(init, loc);

            }

            Exp constraints = getConstraints((VarDec) var.getAssertion());
            if (constraints == null) {
                constraints = getTrueVarExp();
            }
            if (constraints.getLocation() != null) {

                constraints.getLocation().setDetails(
                        "Constraints on " + varDec.getName().getName());
            }

            if (inSimpleForm(init, varDec)) {

                if (init instanceof EqualsExp) {
                    /* If initialization of variable sets the variable equal to a value, then 
                     * that variable should be set to that value in the expression
                     */
                    if (((EqualsExp) init).getLeft() instanceof VarExp) {

                        PosSymbol exemplar = getCurrentExemplar();
                        Exp conceptVarExp = null;
                        if (!initializationRule
                                && ((VarExp) ((EqualsExp) init).getLeft())
                                        .getName().equals(exemplar.toString())) {
                            conceptVarExp = determineConceptualVariableName();
                        }
                        Exp varToReplace = conceptVarExp;
                        if (varToReplace == null) {
                            varToReplace = ((EqualsExp) init).getLeft();

                            /* Check if we have an assertive code that involves this variable expression */
                            Iterator<VerificationStatement> it =
                                    assertion.assertive_code.iterator();
                            while (it.hasNext()) {
                                VerificationStatement st = it.next();

                                if (st.getAssertion() instanceof FuncAssignStmt) {
                                    FuncAssignStmt assignStmt =
                                            (FuncAssignStmt) st.getAssertion();
                                    VariableExp leftExp = assignStmt.getVar();
                                    if (leftExp instanceof VariableDotExp) {
                                        VariableDotExp leftDotExp =
                                                (VariableDotExp) leftExp;
                                        if (leftDotExp.toString().equals(
                                                varToReplace.toString())) {
                                            ((EqualsExp) init)
                                                    .setRight(assignStmt
                                                            .getAssign());
                                        }
                                    }
                                }
                            }

                            if (varToReplace instanceof VarExp
                                    && ((VarExp) varToReplace).getName()
                                            .toString().contains(".")) {
                                VarExp oldExp = (VarExp) varToReplace;
                                varToReplace = new DotExp();
                                List<Exp> myList = new List<Exp>();
                                VarExp first = (VarExp) oldExp.clone();
                                VarExp second = (VarExp) oldExp.clone();
                                int indexOfDot =
                                        oldExp.getName().toString()
                                                .indexOf(".");
                                first.setName(createPosSymbol(oldExp.getName()
                                        .toString().substring(0, indexOfDot)));
                                second.setName(createPosSymbol(oldExp.getName()
                                        .toString().substring(indexOfDot + 1)));
                                myList.add(first);
                                myList.add(1, second);
                                ((DotExp) varToReplace).setSegments(myList);
                            }
                        }
                        if (varToReplace instanceof VarExp
                                && conf.containsVar(((VarExp) varToReplace)
                                        .getName().toString(), false)) {

                            appendToLocation(conf,
                                    " modified by Variable Declaration rule");
                        }
                        Exp tmp =
                                ((Exp) conf.clone()).replace(varToReplace,
                                        ((EqualsExp) init).getRight());
                        if (tmp == null) {
                            tmp = (Exp) conf.clone();
                        }

                        if (tmp.containsVar(varDec.getName().toString(), false)) {
                            /*  If the variable is in the new assertion and the constraints
                             *  are not simply true, then add the constraints to the assertion
                             */
                            if (isTrueExp(constraints)) {
                                assertion.setFinalConfirm(tmp);
                            }
                            else {
                                newConf.setLeft(constraints);
                                newConf.setOpName(createPosSymbol("implies"));
                                newConf.setRight(tmp);
                                newConf.setType(BooleanType.INSTANCE);
                                assertion.setFinalConfirm(newConf);
                            }
                        }
                        else {
                            assertion.setFinalConfirm(tmp);
                        }
                        VCBuffer.append("\n_____________________ \n");
                        VCBuffer
                                .append("\nVariable Declaration Rule Applied: \n");
                        VCBuffer.append(assertion.assertionToString());
                        return;
                    }
                }
            }

            /* The initialization will not simply allow a replacement of the variable
             * with the appropriate value
             */
            String myVar = varDec.getName().toString();
            int index = myVar.indexOf('.');
            if (index > 0)
                myVar = myVar.substring(0, index);
            if (conf.containsVar(myVar, false)) {
                if (isTrueExp(constraints) || constraints == null) {
                    /* When the variable is in the assertion, and the constraints are true,
                     * add the initialization
                     */
                    newConf.setLeft(init);
                    newConf.setOpName(createPosSymbol("implies"));
                    newConf.setType(BooleanType.INSTANCE);
                    assertion.setFinalConfirm(newConf);
                }
                else {
                    /* When the variable is in the assertion, and the constraints are not true,
                     * add the initialization and constraints
                     */
                    InfixExp constrAndInit = new InfixExp();
                    constrAndInit.setOpName(createPosSymbol("and"));
                    constrAndInit.setType(BooleanType.INSTANCE);
                    constrAndInit.setLeft(constraints);
                    constrAndInit.setRight(init);
                    newConf.setLeft(constrAndInit);
                    newConf.setOpName(createPosSymbol("implies"));
                    newConf.setType(BooleanType.INSTANCE);
                    assertion.setFinalConfirm(newConf);
                }
            }

            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nVariable Declaration Rule Applied: \n");
            VCBuffer.append(assertion.assertionToString());
        }

    }

    /**
     * Builds a comment footer to identify VC files generated
     * by the compiler and from which Resolve source file the generated
     * file is derived.
     */
    private String buildFooterComment() {
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_ISABELLE_VC)) {
            return "\n" + "end\n";
        }
        else
            return "\n";
    }

    /**
     * Builds a comment header to identify VC files generated
     * by the compiler and from which Resolve source file the generated
     * file is derived.
     */
    private String buildHeaderComment() {
        if (myInstanceEnvironment.flags.isFlagSet(Verifier.FLAG_ISABELLE_VC)) {
            return "theory "
                    + name
                    + "\n"
                    +
                    //"(*" +		        
                    " (* Generated by the RESOLVE Verifier, March 2009 version"
                    + " *)\n" + " (* from file: "
                    + myInstanceEnvironment.getTargetFile().getName() + " *)\n"
                    + " (* on: " + new Date() + " *)\n" + "\n\n";
        }
        else
            return "//\n"
                    + "// Generated by the RESOLVE Verifier, March 2009 version"
                    + "\n" + "// from file:  "
                    + myInstanceEnvironment.getTargetFile().getName() + "\n"
                    + "// on:         " + new Date() + "\n" + "//\n";
    }

    void buildListAvailableSpecs(Dec dec) {

    }

    private boolean checkImportDup(String importStr) {
        return importList.contains(importStr);
    }

    private Dec checkModuleScope(ModuleScope scope) {

        Dec dec;
        ModuleID id = scope.getModuleID();

        if (myInstanceEnvironment.contains(id)) {
            dec = myInstanceEnvironment.getModuleDec(id);

            if (dec instanceof ShortFacilityModuleDec) {
                FacilityDec fDec = ((ShortFacilityModuleDec) dec).getDec();

                ModuleID cid = ModuleID.createConceptID(fDec.getConceptName());
                ConceptModuleDec cDec =
                        (ConceptModuleDec) myInstanceEnvironment
                                .getModuleDec(cid);
                return cDec;
            }
            else
                return dec;
        }
        else
            return null;
    }

    /*
     * Clear buffers
     */
    public void clearMainBuffer() {
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_VERBOSE_VC)) {
            //env.flags.isFlagSet(FLAG_FINALVERB_VC) || env.flags.isFlagSet(FLAG_VERIFY_VC)){
            VCBuffer = new StringBuffer();
        }
        else {
            assrtBuf = new StringBuffer();
        }
    }

    private boolean compareArguments(List<ProgramExp> arguments,
            List<ParameterVarDec> params) {
        if (arguments == null) {
            return true;
        }
        Iterator<ProgramExp> argIt = arguments.iterator();
        Iterator<ParameterVarDec> parIt = params.iterator();

        if (parIt.hasNext()) {
            if (argIt.hasNext()) {
                ModuleID t = getCurrentModuleID();
                TypeConverter TC =
                        new TypeConverter(myInstanceEnvironment
                                .getSymbolTable(t));
                ProgramExpTypeResolver PETR =
                        new ProgramExpTypeResolver(myInstanceEnvironment
                                .getSymbolTable(t), myInstanceEnvironment);

                Type argType;

                try {

                    argType = PETR.getProgramExpType(argIt.next());
                }
                catch (TypeResolutionException e) {
                    return false;
                }
                ParameterVarDec tmp = parIt.next();
                Type paramType = TC.getMathType((tmp.getTy()));
                TypeMatcher TM = new TypeMatcher();

                Type mathParamType = paramType.toMath();
                Type mathArgType = argType.toMath();
                Type convParamType = TC.getProgramType(tmp.getTy());

                TypeName programParamType = convParamType.getProgramName();
                TypeName programArgType = argType.getProgramName();

                if (TM.programMatches(argType, convParamType)) {}
                else if (paramType instanceof IndirectType
                        && ((IndirectType) paramType).getName().toString()
                                .equals("Entry")) {}
                else if (argType != null && paramType != null
                        && argType instanceof IndirectType
                        && paramType instanceof IndirectType) {
                    if (!((IndirectType) argType).getName().toString().equals(
                            ((IndirectType) paramType).getName().toString())) {
                        // Check for the case where we have the following:
                        // Ex: Type Aliased_Stack is represented by Stack_Fac.Stack;
                        // - YS    						
                        Binding binding = table.getModuleScope().getBinding();
                        Type retType =
                                binding.getType(((IndirectType) argType)
                                        .getQualifier(),
                                        ((IndirectType) argType).getName());

                        // Check to see if the type referred by the indirect type is what we are looking for or not.
                        // Using the example above, retType.getName() should be Stack
                        if (retType instanceof IndirectType
                                && !((IndirectType) retType).getName()
                                        .getName().equals(
                                                ((IndirectType) paramType)
                                                        .getName().getName())) {
                            return false;
                        }
                        else if (retType instanceof NameType
                                && !((NameType) retType).getName().getName()
                                        .equals(
                                                ((IndirectType) paramType)
                                                        .getName().getName())) {
                            return false;
                        }
                    }

                }
                /*else if(!matchesStandard(argType, convParamType, PETR)){
                    return false;
                }*/
                else if (argType != null && paramType != null
                        && !argType.toString().equals(paramType.toString())) {
                    return false;
                }
            }
            else
                return false;
        }
        else if (argIt.hasNext()) {
            return false;
        }
        return true;
    }

    private boolean matchesStandard(Type t1, Type t2,
            ProgramExpTypeResolver PETR) {
        List<ModuleID> stdUses = new List<ModuleID>();
        for (String std : myInstanceEnvironment.getStdUses()) {
            String stdName = "Std_" + std + "_Fac";
            stdUses.add(ModuleID.createFacilityID(Symbol.symbol(stdName)));
        }
        TypeName tn1 = t1.getProgramName();
        TypeName tn2 = t2.getProgramName();
        if (tn1 == null) {
            for (ModuleID module : stdUses) {
                Scope ms = myInstanceEnvironment.getModuleScope(module);
                if (ms.containsVariable(Symbol.symbol(t2.asString()))) {
                    t1 = ms.getVariable(Symbol.symbol(t2.asString())).getType();
                }
            }
        }
        if (tn2 == null) {
            for (ModuleID module : stdUses) {
                Scope ms = myInstanceEnvironment.getModuleScope(module);

                if (ms.containsVariable(Symbol.symbol(t2.asString()))) {
                    t2 = ms.getVariable(Symbol.symbol(t2.asString())).getType();
                }
            }
        }
        tn1 = t1.getProgramName();
        tn2 = t2.getProgramName();

        if (tn1 == null || tn2 == null) {
            return false;
        }
        return (tn1.equals(tn2));
    }

    private boolean compareParameters(List<ParameterVarDec> paramsA,
            List<ParameterVarDec> paramsB) {

        Iterator<ParameterVarDec> parItA = paramsA.iterator();
        Iterator<ParameterVarDec> parItB = paramsB.iterator();
        if (parItA.hasNext()) {
            if (parItB.hasNext()) {
                if (!parItA.next().getTy().asString(0, 0).equals(
                        parItB.next().getTy().asString(0, 0))) {
                    //need to compare a different way
                    return false;
                }
            }
            else
                return false;
        }
        else if (parItB.hasNext()) {
            return false;
        }
        return true;
    }

    private List<ParameterVarDec> containsReassigns(OperationDec opDec) {
        List<ParameterVarDec> lst = opDec.getParameters();
        Iterator<ParameterVarDec> i = lst.iterator();
        List<ParameterVarDec> reassigns = new List<ParameterVarDec>();
        while (i.hasNext()) {
            ParameterVarDec tmp = i.next();
            if (tmp.getMode() == Mode.REASSIGNS) {
                reassigns.add(tmp);
            }
        }
        return reassigns;
    }

    private ConcType convertToConcType(PosSymbol name, Type type) {
        ModuleScope curr = table.getModuleScope();
        /* The type forms a ConcType */
        if (type instanceof ArrayType) {
            ConcType tmp =
                    new ConcType(((ArrayType) type).getModuleID(), name,
                            (ArrayType) type);
            return tmp;
        }
        else if (type instanceof ConcType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, ((ConcType) type)
                            .getType());
            return tmp;
        }
        else if (type instanceof ConstructedType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name,
                            (ConstructedType) type);
            return tmp;
        }
        else if (type instanceof FieldItem) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (FieldItem) type);
            return tmp;
        }
        else if (type instanceof FormalType) {
            ConcType tmp =
                    new ConcType(((FormalType) type).getModuleID(), name,
                            (FormalType) type);
            return tmp;
        }
        else if (type instanceof FunctionType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (FunctionType) type);
            return tmp;
        }
        else if (type instanceof IndirectType) {

            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (IndirectType) type);
            return tmp;
        }
        else if (type instanceof NameType) {
            ConcType tmp =
                    new ConcType(((NameType) type).getModuleID(), name, type);
            return tmp;
        }
        else if (type instanceof PrimitiveType) {
            ConcType tmp =
                    new ConcType(((PrimitiveType) type).getModuleID(), name,
                            (PrimitiveType) type);
            return tmp;
        }
        else if (type instanceof RecordType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (RecordType) type);
            return tmp;
        }
        else if (type instanceof VoidType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (VoidType) type);
            return tmp;
        }
        else if (type instanceof TupleType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (TupleType) type);
            return tmp;
        }

        return null;
    }

    Type convertToMathTypes(Type type, Binding binding) {
        if (type instanceof RecordType) {
            Iterator<FieldItem> it = ((RecordType) type).getFields().iterator();
            List<FieldItem> lst = new List<FieldItem>();
            while (it.hasNext()) {
                FieldItem item = it.next();
                TypeName tyName = item.getType().getProgramName();
                if (binding.contains(new PosSymbol(null, tyName
                        .getFacilityQualifier()), new PosSymbol(null, tyName
                        .getName()))) {
                    Type tmp =
                            binding.getType(new PosSymbol(null, tyName
                                    .getFacilityQualifier()), new PosSymbol(
                                    null, tyName.getName()));
                    lst.add(new FieldItem(item.getName(), tmp));
                }
            }
            ((RecordType) type).setFields(lst);
        }
        else if (type instanceof IndirectType) {
            if (binding.contains(((IndirectType) type).getQualifier(),
                    ((IndirectType) type).getName())) {
                type =
                        binding.getType(((IndirectType) type).getQualifier(),
                                ((IndirectType) type).getName());
            }
        }
        else if (type instanceof ConstructedType) {
            if (binding.contains(((ConstructedType) type).getQualifier(),
                    ((ConstructedType) type).getName())) {
                type =
                        binding.getType(((IndirectType) type).getQualifier(),
                                ((IndirectType) type).getName());
            }
        }
        return type;
    }

    private ProgramFunctionExp corProgramFuncExp(ProgramOpExp exp) {

        PosSymbol name = getProgramOpName(exp);
        ProgramFunctionExp pFE = new ProgramFunctionExp();
        pFE.setName(name);
        List<ProgramExp> lst = new List<ProgramExp>();
        lst.add(exp.getFirst());
        lst.add(exp.getSecond());
        pFE.setArguments(lst);
        setLocation(pFE, exp.getLocation());

        return pFE;

    }

    private Exp corProgramFunctionExp(ProgramFunctionExp exp,
            AssertiveCode assertion) {

        Exp ensures = getTrueVarExp();

        ModuleID mid = getCurrentModuleID();
        ModuleDec ebDec = null;
        if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ebDec =
                    (EnhancementBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
        }
        else if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ebDec =
                    (ConceptBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
        }
        else if (mid.getModuleKind() == ModuleKind.FACILITY) {
            ebDec = (FacilityModuleDec) myInstanceEnvironment.getModuleDec(mid);
        }

        /* Find Corresponding OperationDec and Specification*/
        OperationDec opDec = null;
        if (ebDec instanceof EnhancementBodyModuleDec) {
            opDec =
                    getEnhancementOperationDec(exp.getName(), exp
                            .getArguments());
        }
        else if (ebDec instanceof ConceptBodyModuleDec) {
            opDec = getConceptOperationDec(exp.getName(), exp.getArguments());
        }
        else if (ebDec instanceof FacilityModuleDec) {
            opDec = getFacilityOperationDec(exp.getName(), exp.getArguments());
        }

        if (opDec != null) {
            if (opDec.getEnsures() != null) {
                ensures = (Exp) ((OperationDec) opDec).getEnsures().clone();
            }
        }
        else {
            ensures = getTrueVarExp();
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nSpec not found \n");
            VCBuffer.append(assertion.assertionToString());

            System.err.println("Error passed Function operation not found: "
                    + exp.getName().asString(1, 1));
            throw new RuntimeException();
        }

        if (opDec != null)
            ensures =
                    replacePostConditionVariables(exp.getArguments(), null,
                            opDec, assertion);

        if (ensures instanceof EqualsExp) {
            if (((EqualsExp) ensures).getLeft() instanceof VarExp) {
                if (((VarExp) ((EqualsExp) ensures).getLeft()).getName()
                        .toString().equals(exp.getName().toString())) {

                    return (((EqualsExp) ensures).getRight());
                }
            }
        }
        return ensures;
    }

    private PosSymbol createPosSymbol(String name) {
        PosSymbol posSym = new PosSymbol();
        posSym.setSymbol(Symbol.symbol(name));
        return posSym;
    }

    public boolean decInCurConcept(Dec dec) {
        ModuleDec moduleDec = getCurrentBodyModuleDec();
        if (moduleDec instanceof ConceptBodyModuleDec) {
            Iterator it =
                    ((ConceptBodyModuleDec) moduleDec).getDecs().iterator();
            while (it.hasNext()) {
                Dec myDec = (Dec) it.next();
                if (myDec.getName().equals(dec.getName())) {
                    return true;
                }

            }
        }
        return false;
    }

    public VarExp determineConceptualVariableName() {
        ModuleDec moduleDec = getCurrentBodyModuleDec();
        if (moduleDec == null) {
            ModuleID mid = getCurrentModuleID();
            if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
                moduleDec =
                        (EnhancementBodyModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);

                ModuleID tid = mid.getEnhancementID();
                moduleDec = myInstanceEnvironment.getModuleDec(tid);

            }
            else if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
                moduleDec =
                        (ConceptBodyModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);
            }
            else if (mid.getModuleKind() == ModuleKind.FACILITY) {
                moduleDec =
                        (FacilityModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);
            }
        }

        Exp requires = new VarExp();
        Exp ensures = new VarExp();
        boolean concept = false;

        Exp correspondence = null;
        Exp convention = null;
        Exp moduleLevelRequires = null;
        VarExp exemplar = null, cExem = null;
        if (moduleDec != null && moduleDec instanceof ConceptBodyModuleDec) {
            concept = true;
            Iterator<Dec> decsIt =
                    ((ConceptBodyModuleDec) moduleDec).getDecs().iterator();
            while (decsIt.hasNext()) {
                Dec tmp = decsIt.next();
                if (tmp instanceof RepresentationDec) {
                    correspondence =
                            (Exp) ((RepresentationDec) tmp).getCorrespondence()
                                    .clone();
                    if (((RepresentationDec) tmp).getConvention() != null) {
                        convention =
                                (Exp) ((RepresentationDec) tmp).getConvention()
                                        .clone();
                    }
                    else {
                        convention = getTrueVarExp();
                    }

                }
            }
            ConceptModuleDec cmDec = (ConceptModuleDec) getCurrentModuleDec();
            moduleLevelRequires = cmDec.getRequirement();

            Iterator<Dec> decs = cmDec.getDecs().iterator();
            while (decs.hasNext()) {
                Dec tmpDec = decs.next();
                if (tmpDec instanceof TypeDec) {

                    exemplar = new VarExp();

                    cExem = new VarExp();
                    exemplar.setName(((TypeDec) tmpDec).getExemplar());

                    Type exemplarType =
                            getTypeFromTy(((TypeDec) tmpDec).getModel());

                    cExem.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    cExem.setType(exemplarType);
                    VarDec concVar = new VarDec();
                    concVar.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    concVar.setTy(((TypeDec) tmpDec).getModel());

                }
            }

        }

        return cExem;

    }

    private Exp fixupTypes(Exp myExp) {
        if (myExp instanceof InfixExp) {
            ((InfixExp) myExp)
                    .setLeft(fixupTypes(((InfixExp) myExp).getLeft()));
            ((InfixExp) myExp).setRight(fixupTypes(((InfixExp) myExp)
                    .getRight()));
        }
        else if (myExp instanceof EqualsExp) {
            ((EqualsExp) myExp).setLeft(fixupTypes(((EqualsExp) myExp)
                    .getLeft()));
            ((EqualsExp) myExp).setRight(fixupTypes(((EqualsExp) myExp)
                    .getRight()));
        }
        else if (myExp instanceof FunctionExp) {
            ((FunctionExp) myExp).getSubExpressions().set(
                    0,
                    fixupTypes(((FunctionExp) myExp).getSubExpressions()
                            .remove(0)));

        }
        else if (myExp instanceof DotExp) {
            if (myExp.getType() == null) {
                getCurrentExemplar();
                ConceptBodyModuleDec modDec =
                        (ConceptBodyModuleDec) getCurrentBodyModuleDec();

                ModuleID modID =
                        ModuleID
                                .createConceptID(((ConceptBodyModuleDec) modDec)
                                        .getConceptName());
                ConceptModuleDec moduleDec =
                        (ConceptModuleDec) myInstanceEnvironment
                                .getModuleDec(modID);

                //	if(((DotExp)myExp).getSegments().get(0).toString().equals(getCurrentExemplar().toString())){
                for (int i = 0; i < myRememberedExp.size(); i++) {
                    if (myExp.getSubExpressions().get(1).equals(
                            myRememberedExp.get(i).getSubExpressions().get(1))) {
                        myExp.setType(myRememberedExp.get(i).getType());
                    }
                }
                //	}
                //	myExp.setType(moduleDec.getDecs())

            }
            else {
                myRememberedExp.add((DotExp) myExp);
            }
            if (myExp.getType() == null) {
                myExp.setType(null);
            }
        }
        return myExp;
    }

    private String formJavaImport(File file) {
        StringBuffer pkgPath = new StringBuffer();
        boolean pkgStart = false;

        StringTokenizer stTok =
                new StringTokenizer(file.getPath(), File.separator);
        while (stTok.hasMoreTokens()) {
            String curTok = stTok.nextToken();
            if (pkgStart) {
                if (stTok.hasMoreTokens()) { // stop at parent
                    pkgPath.append(".");
                    pkgPath.append(curTok);
                }
            }
            if ("RESOLVE".equals(curTok)) {
                pkgPath.append("import RESOLVE");
                pkgStart = true;
            }
        }
        if (pkgPath.length() > 0) {
            pkgPath.append(".*;\n");
        }
        return pkgPath.toString();
    }

    public String formNewVarDecl(VarDec curVar) {
        StringBuffer thisBuf = new StringBuffer();

        return thisBuf.toString();
    }

    public OperationDec formOpDec(FacilityOperationDec dec) {

        OperationDec curOperation = new OperationDec();

        if (((FacilityOperationDec) dec).getName() != null) {
            curOperation.setName(((FacilityOperationDec) dec).getName());
        }
        if (!((FacilityOperationDec) dec).getParameters().isEmpty()) {
            curOperation.setParameters(((FacilityOperationDec) dec)
                    .getParameters());
        }
        if (((FacilityOperationDec) dec).getReturnTy() != null) {
            curOperation.setParameters(((FacilityOperationDec) dec)
                    .getParameters());

        }
        if (((FacilityOperationDec) dec).getStateVars() != null) {
            curOperation.setParameters(((FacilityOperationDec) dec)
                    .getParameters());

        }
        if ((Exp) ((FacilityOperationDec) dec).getRequires() != null) {
            curOperation.setRequires((Exp) ((FacilityOperationDec) dec)
                    .getRequires().clone());

        }
        if ((Exp) ((FacilityOperationDec) dec).getEnsures() != null) {
            curOperation.setEnsures((Exp) ((FacilityOperationDec) dec)
                    .getEnsures().clone());

        }
        return curOperation;

    }

    private void generateVCsForOperationParameter(FacilityDec dec,
            EnhancementBodyItem eBI, EnhancementBodyModuleDec curEnhDec,
            EnhancementModuleDec curESpecDec) {

        Iterator it = curEnhDec.getParameters().iterator();
        Iterator it2 = eBI.getBodyParams().iterator();

        while (it.hasNext()) {
            AssertiveCode assertion = new AssertiveCode(myInstanceEnvironment);
            AssertiveCode assertion2 = new AssertiveCode(myInstanceEnvironment);

            ModuleParameter param = (ModuleParameter) it.next();
            ModuleArgumentItem arg = (ModuleArgumentItem) it2.next();

            OperationDec dec2 = getFacilityOperationDec(arg.getName(), null);
            if (dec2 == null) {
                return;
            }
            if (param instanceof OperationDec) {
                //if(((OperationDec)param).getRequires() != null)
                {
                    Exp ens2 = ((OperationDec) param).getRequires();
                    if (ens2 == null) {
                        ens2 = getTrueVarExp();
                    }
                    Exp ens1 = ((OperationDec) dec2).getRequires();
                    if (ens1 == null) {
                        ens1 = getTrueVarExp();
                    }

                    ens2 =
                            replaceFacilityDeclarationVariables(ens2, curEnhDec
                                    .getParameters(), eBI.getBodyParams());
                    ens2 =
                            replaceFacilityDeclarationVariables(ens2,
                                    ((OperationDec) param).getParameters(),
                                    dec2.getParameters());
                    ens2 =
                            replaceFacilityDeclarationVariables(ens2,
                                    curESpecDec.getParameters(), eBI
                                            .getParams());

                    Location myLoc1 = null;

                    try {
                        myLoc1 = ((Location) ens1.getLocation().clone());
                    }
                    catch (Exception ex) {
                        myLoc1 =
                                ((Location) ((OperationDec) dec2).getName()
                                        .getLocation().clone());
                    }
                    myLoc1.setDetails("Requires from " + dec.getName());
                    setLocation(ens1, myLoc1);

                    Location myLoc2 = null;
                    try {
                        myLoc2 = ((Location) ens2.getLocation().clone());
                    }
                    catch (Exception ex) {
                        myLoc2 =
                                ((Location) ((OperationDec) param).getName()
                                        .getLocation().clone());
                    }
                    myLoc2.setDetails("Requires from "
                            + ((OperationDec) param).getName());
                    setLocation(ens2, myLoc2);

                    assertion.setFinalConfirm(ens2);
                    assertion.addAssume(ens1);

                    VCBuffer.append("\n Facility Dec Name:\t");
                    VCBuffer.append(dec.getName().getSymbol().toString());
                    VCBuffer.append("\n");

                    VCBuffer.append("\n_____________________ \n");
                    VCBuffer
                            .append("\nFacility Declaration Rule Applied for Operation parameter (requires): "
                                    + dec2.getName() + "\n\n");
                    VCBuffer.append(assertion.assertionToString());

                    /* Applies Proof Rules */
                    applyEBRules(assertion);
                    VCBuffer.append("\n_____________________ \n\n");

                }
                //else if(((OperationDec)dec2).getEnsures() != null)
                {
                    Exp ens1 = ((OperationDec) dec2).getEnsures();
                    Exp ens2 = ((OperationDec) param).getEnsures();

                    if (ens1 == null) {
                        ens1 = getTrueVarExp();
                    }

                    ens1 =
                            modifyEnsuresForParameterModes(ens1,
                                    (OperationDec) dec2);

                    ens2 =
                            modifyEnsuresForParameterModes(ens2,
                                    ((OperationDec) param));
                    ens2 =
                            replaceFacilityDeclarationVariables(ens2, curEnhDec
                                    .getParameters(), eBI.getBodyParams());
                    ens2 =
                            replaceFacilityDeclarationVariables(ens2,
                                    ((OperationDec) param).getParameters(),
                                    dec2.getParameters());
                    ens2 =
                            replaceFacilityDeclarationVariables(ens2,
                                    curESpecDec.getParameters(), eBI
                                            .getParams());

                    Location myLoc1 = null;

                    try {
                        myLoc1 = ((Location) ens1.getLocation().clone());
                    }
                    catch (Exception ex) {
                        myLoc1 =
                                ((Location) ((OperationDec) dec2).getName()
                                        .getLocation().clone());
                    }
                    myLoc1.setDetails("Ensures from " + dec.getName());
                    setLocation(ens1, myLoc1);

                    Location myLoc2 = null;
                    try {
                        myLoc2 = ((Location) ens2.getLocation().clone());
                    }
                    catch (Exception ex) {
                        myLoc2 =
                                ((Location) ((OperationDec) param).getName()
                                        .getLocation().clone());
                    }
                    myLoc2.setDetails("Ensures from "
                            + ((OperationDec) param).getName());
                    setLocation(ens2, myLoc2);

                    assertion2.setFinalConfirm(ens1);
                    assertion2.addAssume(ens2);

                    VCBuffer.append("\n Facility Dec Name:\t");
                    VCBuffer.append(dec.getName().getSymbol().toString());
                    VCBuffer.append("\n");

                    VCBuffer.append("\n_____________________ \n");
                    VCBuffer
                            .append("\nFacility Declaration Rule Applied for Operation parameter (ensures): "
                                    + dec2.getName() + ": \n\n");
                    VCBuffer.append(assertion2.assertionToString());

                    /* Applies Proof Rules */
                    applyEBRules(assertion2);
                    VCBuffer.append("\n_____________________ \n\n");
                }
            }

        }

    }

    private Exp getAssumptionsFromConceptDec(AssertiveCode assertion,
            ConceptModuleDec cmDec, Dec parentDec, boolean instanciated,
            boolean procedureInConcept) {
        Exp constraints = null;
        List<Exp> constrList = cmDec.getConstraints();
        String exemplar = getExemplar(cmDec).toString();
        String details = "";
        if (parentDec != null) {
            details = " for " + parentDec.getName();
        }

        Exp require = null;
        if (cmDec.getRequirement() != null) {
            require = (Exp) cmDec.getRequirement().clone();
            if (require.getLocation() != null) {
                Location myLoc = ((Location) require.getLocation().clone());
                myLoc.setDetails("Requires from " + cmDec.getName() + details);
                setLocation(require, myLoc);

            }
            if (require != null && require.containsVar(exemplar, false)
                    && !instanciated) {
                require = null;
            }
        }
        Iterator<Dec> decIt = cmDec.getDecs().iterator();
        Iterator<ModuleParameter> paramit = cmDec.getParameters().iterator();

        while (paramit.hasNext()) {
            ModuleParameter modParam = paramit.next();
            Exp tmpConstraints = null;
            if (modParam instanceof ConstantParamDec) {
                //			ModuleID mid = ModuleID.createID(((ConstantParamDec)modParam));
                assertion.addFreeVar(getFreeVar((ConstantParamDec) modParam));
                tmpConstraints =
                        getConstraints(toVarDec((ConstantParamDec) modParam));
                if (tmpConstraints.getLocation() != null) {

                    Location myLoc =
                            ((Location) tmpConstraints.getLocation().clone());
                    myLoc
                            .setDetails("Constraints from "
                                    + ((ConstantParamDec) modParam).getName()
                                    + details);
                    setLocation(tmpConstraints, myLoc);

                }
                if (tmpConstraints != null
                        && tmpConstraints.containsVar(exemplar, false)
                        && !instanciated) {
                    tmpConstraints = null;
                }
            }
            else if (modParam instanceof DefinitionDec) {

                //	ModuleID mid = ModuleID.createID((ConceptModuleDec)modParam);
                assertion.addFreeVar(getFreeVar((DefinitionDec) modParam));

                tmpConstraints =
                        getConstraints(toVarDec((DefinitionDec) modParam));
                if (tmpConstraints.getLocation() != null) {
                    Location myLoc =
                            ((Location) tmpConstraints.getLocation().clone());
                    myLoc.setDetails("Constraints from "
                            + ((DefinitionDec) modParam).getName() + details);
                    setLocation(tmpConstraints, myLoc);
                }
                if (tmpConstraints != null
                        && tmpConstraints.containsVar(exemplar, false)
                        && !instanciated) {
                    tmpConstraints = null;
                }
            }
            if (tmpConstraints != null
                    && !tmpConstraints.equals(getTrueVarExp())) {
                if (constraints != null) {
                    constraints =
                            InfixExp.formAndStmt(tmpConstraints, constraints);
                }
                else {
                    constraints = (Exp) tmpConstraints.clone();
                }

            }
        }
        while (decIt.hasNext()) {
            Dec dec = decIt.next();
            Exp tmpConstraints = null;
            if (dec instanceof DefinitionDec) {
                ModuleID mid = ModuleID.createID(((ConceptModuleDec) cmDec));
                assertion.addFreeVar(getFreeVar(dec, new ModuleScope(mid,
                        myInstanceEnvironment)));
                tmpConstraints = getConstraints(toVarDec((DefinitionDec) dec));
                if (tmpConstraints.getLocation() != null) {
                    Location myLoc =
                            ((Location) tmpConstraints.getLocation().clone());
                    myLoc.setDetails("Constraints from "
                            + ((DefinitionDec) dec).getName() + details);

                    setLocation(tmpConstraints, myLoc);
                }
                if (tmpConstraints != null
                        && tmpConstraints.containsVar(exemplar, false)
                        && !instanciated) {
                    tmpConstraints = null;
                }
            }
            else if (dec instanceof TypeDec) {
                if (!isCurrentConceptModuleDec((ConceptModuleDec) cmDec)
                        || !procedureInConcept) {
                    if (((TypeDec) dec).getConstraint() != null) {
                        tmpConstraints = ((TypeDec) dec).getConstraint();
                        if (tmpConstraints.getLocation() != null) {
                            Location myLoc =
                                    ((Location) tmpConstraints.getLocation()
                                            .clone());
                            myLoc.setDetails("Constraints from "
                                    + dec.getName() + details);
                            setLocation(tmpConstraints, myLoc);
                        }
                    }
                }
                if (tmpConstraints != null
                        && tmpConstraints.containsVar(exemplar, false)
                        && !instanciated) {
                    tmpConstraints = null;
                }
            }
            if (tmpConstraints != null
                    && !tmpConstraints.equals(getTrueVarExp())) {
                if (constraints != null) {
                    constraints =
                            InfixExp.formAndStmt(tmpConstraints, constraints);
                }
                else {
                    constraints = (Exp) tmpConstraints.clone();
                }
            }
        }

        if (require != null) {

            if (require.getLocation() != null) {
                Location myLoc = ((Location) require.getLocation().clone());
                myLoc.setDetails("Requires from " + cmDec.getName() + details);
                setLocation(require, myLoc);

            }

            if (constraints == null) {
                constraints = require;
            }
            else {
                constraints = InfixExp.formAndStmt(require, constraints);
            }
        }

        Iterator<Exp> constrIt = constrList.iterator();
        while (constrIt.hasNext()) {
            Exp constraint = (Exp) constrIt.next().clone();
            if (constraint.getLocation() != null) {
                Location myLoc = ((Location) constraint.getLocation().clone());
                myLoc.setDetails("Constraints from " + cmDec.getName()
                        + details);
                setLocation(constraint, myLoc);

            }

            if (constraints == null) {
                constraints = (Exp) constraint.clone();
            }
            else {
                constraints =
                        InfixExp.formAndStmt((Exp) constraint, constraints);
            }
        }

        return constraints;
    }

    private List<ConcType> getChangeList(Statement stmt, AssertiveCode assertion) {
        List<VariableExp> change;
        if (stmt instanceof WhileStmt)
            change = ((WhileStmt) stmt).getChanging();
        else
            change = ((IterateStmt) stmt).getChanging();

        List<ConcType> list = new List<ConcType>();

        if (change == null) {
            return list;
        }
        Iterator<VariableExp> j = change.iterator();

        Dec tmpDec = getCurrentProcedure();
        ProcedureDec pDec = null;
        FacilityOperationDec fODec = null;
        if (tmpDec instanceof ProcedureDec) {
            pDec = (ProcedureDec) tmpDec;
        }
        else if (tmpDec instanceof FacilityOperationDec) {
            fODec = (FacilityOperationDec) tmpDec;
        }

        Iterator<ConcType> it = assertion.getFreeVars2().iterator();
        j = change.iterator();
        while (it.hasNext()) {
            ConcType tmp = it.next();

            while (j.hasNext()) {
                ProgramExp t = j.next();
                if (t instanceof VariableNameExp) {
                    if (((VariableNameExp) t).getName().toString().equals(
                            tmp.getName().toString())) {
                        list.add(tmp);
                    }
                }
            }
            j = change.iterator();

        }
        Iterator<VarDec> varIt = null;
        if (tmpDec instanceof ProcedureDec) {
            varIt = pDec.getVariables().iterator();
        }
        else {
            varIt = fODec.getVariables().iterator();
        }
        while (varIt.hasNext()) {
            ConcType tmp = getFreeVar(varIt.next());

            if (tmp != null) {

                while (j.hasNext()) {
                    ProgramExp t = j.next();
                    if (t instanceof VariableNameExp) {
                        if (((VariableNameExp) t).getName().toString().equals(
                                tmp.getName().toString())) {
                            list.add(tmp);
                        }
                    }
                }
                j = change.iterator();
            }
        }

        return list;
    }

    private OperationDec getConceptOperationDec(PosSymbol name,
            List<ProgramExp> arguments) {
        Scope current = table.getCurrentScope();
        ModuleID mid = getCurrentModuleID();

        List<Dec> decs;
        Iterator<Dec> i;

        if ((myInstanceEnvironment.getModuleDec(mid) instanceof ConceptBodyModuleDec)) {

            ConceptBodyModuleDec cbDec =
                    (ConceptBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);

            /* Find Corresponding EnhancementModuleDec*/
            ModuleID cid = mid.getConceptID();
            ConceptModuleDec cDec =
                    (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);

            /* Find Corresponding OperationDec and Specification*/

            // Check parameters for Operations
            List<ModuleParameter> params = cbDec.getParameters();
            Iterator<ModuleParameter> paramIt = params.iterator();
            while (paramIt.hasNext()) {
                ModuleParameter tmp = paramIt.next();
                if (tmp instanceof OperationDec) {
                    OperationDec opDec = (OperationDec) tmp;
                    if (name.toString().equals(opDec.getName().toString())) {
                        //Iterator<ParameterVarDec> paramsIt = opDec.getParameters().iterator();
                        if (compareArguments(arguments, opDec.getParameters()))
                            return opDec;
                    }
                }
            }

            // Check the concept 
            decs = cDec.getDecs();
            i = decs.iterator();
            while (i.hasNext()) {
                Dec tmpDec = i.next();
                if ((tmpDec instanceof OperationDec)
                        && (name.getSymbol() == ((OperationDec) tmpDec)
                                .getName().getSymbol())) {
                    if (compareArguments(arguments, ((OperationDec) tmpDec)
                            .getParameters()))
                        return (OperationDec) tmpDec;
                }
            }

            // Check the Declared Facilities 
            Iterator<Dec> decIt = cbDec.getDecs().iterator();
            while (decIt.hasNext()) {
                Dec tmp = decIt.next();

                if ((tmp instanceof FacilityOperationDec)
                        && (name.getSymbol() == ((FacilityOperationDec) tmp)
                                .getName().getSymbol())) {
                    if (compareArguments(arguments,
                            ((FacilityOperationDec) tmp).getParameters()))
                        return new OperationDec(((FacilityOperationDec) tmp)
                                .getName(), ((FacilityOperationDec) tmp)
                                .getParameters(), ((FacilityOperationDec) tmp)
                                .getReturnTy(), ((FacilityOperationDec) tmp)
                                .getStateVars(), ((FacilityOperationDec) tmp)
                                .getRequires(), ((FacilityOperationDec) tmp)
                                .getEnsures());
                }

                if (tmp instanceof FacilityDec) {
                    FacilityDec tmpFacDec = (FacilityDec) tmp;
                    ModuleID cmid =
                            ModuleID
                                    .createConceptID(tmpFacDec.getConceptName());
                    ModuleDec concDec =
                            myInstanceEnvironment.getModuleDec(cmid);
                    if (concDec instanceof ConceptModuleDec) {
                        decs = ((ConceptModuleDec) concDec).getDecs();
                        i = decs.iterator();
                        while (i.hasNext()) {
                            Dec tmpDec = i.next();
                            if ((tmpDec instanceof OperationDec)
                                    && (name.getSymbol() == ((OperationDec) tmpDec)
                                            .getName().getSymbol())
                                    && compareArguments(arguments,
                                            ((OperationDec) tmpDec)
                                                    .getParameters())) {

                                Exp req = ((OperationDec) tmpDec).getRequires();
                                if (req != null) {
                                    req =
                                            replaceFacilityDeclarationVariables(
                                                    req,
                                                    ((ConceptModuleDec) concDec)
                                                            .getParameters(),
                                                    tmpFacDec
                                                            .getConceptParams());
                                    ((OperationDec) tmpDec).setRequires(req);
                                }
                                Exp ens = ((OperationDec) tmpDec).getEnsures();
                                if (ens != null) {
                                    ens =
                                            replaceFacilityDeclarationVariables(
                                                    ens,
                                                    ((ConceptModuleDec) concDec)
                                                            .getParameters(),
                                                    tmpFacDec
                                                            .getConceptParams());
                                    ((OperationDec) tmpDec).setEnsures(ens);
                                }

                                return (OperationDec) tmpDec;

                            }
                        }
                    }

                    if (((FacilityDec) tmp).getEnhancementBodies() != null) {
                        List<EnhancementBodyItem> enhancements =
                                ((FacilityDec) tmp).getEnhancementBodies();
                        Iterator<EnhancementBodyItem> enhanceIt =
                                enhancements.iterator();
                        while (enhanceIt.hasNext()) {
                            EnhancementBodyItem currentEnhance =
                                    enhanceIt.next();

                            ModuleID ehmid =
                                    ModuleID.createEnhancementID(currentEnhance
                                            .getName(), ((FacilityDec) tmp)
                                            .getConceptName());
                            ModuleDec myDec =
                                    myInstanceEnvironment.getModuleDec(ehmid);
                            if (myDec instanceof EnhancementModuleDec) {
                                EnhancementModuleDec ebDec =
                                        (EnhancementModuleDec) myDec;
                                // Check current Enhancement Body
                                decs = ebDec.getDecs();
                                i = decs.iterator();
                                while (i.hasNext()) {
                                    Dec tmpDec = i.next();
                                    if ((tmpDec instanceof OperationDec)
                                            && (name.getSymbol() == ((OperationDec) tmpDec)
                                                    .getName().getSymbol())) {
                                        if (compareArguments(arguments,
                                                ((OperationDec) tmpDec)
                                                        .getParameters()))
                                            return (OperationDec) tmpDec;
                                    }

                                    if ((tmpDec instanceof FacilityOperationDec)
                                            && (name.getSymbol() == ((FacilityOperationDec) tmpDec)
                                                    .getName().getSymbol())) {
                                        if (compareArguments(arguments,
                                                ((FacilityOperationDec) tmpDec)
                                                        .getParameters()))
                                            return new OperationDec(
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getName(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getParameters(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getReturnTy(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getStateVars(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getRequires(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getEnsures());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Check Program Visible Modules 
            if (current instanceof ProcedureScope) {
                ModuleScope mScope =
                        ((ProcedureScope) current).getModuleScope();
                Iterator<ModuleScope> it = mScope.getProgramVisibleModules();
                while (it.hasNext()) {

                    Dec tmp = checkModuleScope(it.next());
                    if (tmp instanceof ConceptModuleDec) {
                        decs = ((ConceptModuleDec) tmp).getDecs();
                        i = decs.iterator();
                        while (i.hasNext()) {
                            Dec tmpDec = i.next();
                            if ((tmpDec instanceof OperationDec)
                                    && (name.getSymbol() == ((OperationDec) tmpDec)
                                            .getName().getSymbol())) {
                                if (compareArguments(arguments,
                                        ((OperationDec) tmpDec).getParameters()))
                                    return (OperationDec) tmpDec;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /*   
       private void visitStatement(Statement stmt) {
           stmtBuf.append("\t\t");
           if (stmt instanceof FuncAssignStmt) {
             visitFuncAssignStmt((FuncAssignStmt)stmt);
           } else if (stmt instanceof SwapStmt) {
             visitSwapStmt((SwapStmt)stmt);
           } else if (stmt instanceof CallStmt) {
             visitCallStmt((CallStmt)stmt);
           } else if (stmt instanceof IfStmt) {
             visitIfStmt((IfStmt)stmt);
           } else if (stmt instanceof IterateExitStmt) {
           } else if (stmt instanceof IterateStmt) {
           } else if (stmt instanceof MemoryStmt) {
           } else if (stmt instanceof SelectionStmt) {
           } else if (stmt instanceof WhileStmt) {
             visitWhileStmt((WhileStmt)stmt);
           } else {
               assert false;
           }
           stmtBuf.append("\n");
       }*/

    private Ty getConceptualVariableTy(String name) {
        ConceptModuleDec module = (ConceptModuleDec) getCurrentModuleDec();

        Ty result = null;
        if (module != null) {
            for (Dec d : module.getDecs()) {

                if (d instanceof MathVarDec) {
                    MathVarDec dAsMathVarDec = (MathVarDec) d;

                    if (dAsMathVarDec.getName().getName().equals(name)) {
                        result = dAsMathVarDec.getTy();
                    }
                }
            }
        }

        if (result == null) {
            throw new IllegalStateException("No Ty for: " + name);
        }

        return result;
    }

    private List<String> getConcParms(ModuleID cid) {
        List<String> concParms = new List<String>();
        ConceptModuleDec cDec =
                (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);
        List<ModuleParameter> mpList = cDec.getParameters();
        Iterator<ModuleParameter> mpIt = mpList.iterator();
        ModuleParameter mp = null;
        while (mpIt.hasNext()) {
            mp = mpIt.next();
            if (mp instanceof ConstantParamDec) {
                concParms.addUnique(((ConstantParamDec) mp).getName()
                        .toString());
            }
        }
        return concParms;
    }

    private Exp getConstraints(Type vnType, String name) {

        if (vnType instanceof ConcType) {
            ModuleID mid = ((ConcType) vnType).getModuleID();
            ConceptModuleDec eDec =
                    (ConceptModuleDec) myInstanceEnvironment.getModuleDec(mid);

            //	Iterator<Exp> listIt = eDec.getConstraints().iterator();

            Exp allConstraint = null;

            Iterator<Dec> i = eDec.getDecs().iterator();
            while (i.hasNext()) {
                Dec tmp = i.next();
                if (tmp instanceof TypeDec) {
                    if (((TypeDec) tmp).getConstraint() == null)
                        return null;
                    Exp constraint =
                            (Exp) ((TypeDec) tmp).getConstraint().clone();

                    VarExp par = new VarExp();
                    par.setName(createPosSymbol(name));
                    VarExp old = new VarExp();

                    old.setName(((TypeDec) tmp).getExemplar());
                    constraint = constraint.replace(old, par);

                    if (allConstraint != null) {
                        InfixExp exp = new InfixExp();
                        exp.setLeft(constraint);
                        exp.setRight((Exp) allConstraint.clone());
                        exp.setOpName(createPosSymbol("and"));
                        exp.setType(BooleanType.INSTANCE);
                        return exp;
                    }
                    else
                        return constraint;
                }
            }
        }
        else if (vnType instanceof NameType) {
            ModuleID mid = ((NameType) vnType).getModuleID();
            ModuleDec dec = myInstanceEnvironment.getModuleDec(mid);
            if (dec instanceof ShortFacilityModuleDec) {
                FacilityDec facDec = ((ShortFacilityModuleDec) dec).getDec();

                ModuleID modId =
                        ModuleID.createConceptID(facDec.getConceptName());
                ConceptModuleDec modDec =
                        (ConceptModuleDec) myInstanceEnvironment
                                .getModuleDec(modId);

                Exp allConstraint = null;

                Iterator<Dec> i = modDec.getDecs().iterator();
                while (i.hasNext()) {
                    Dec tmp = i.next();
                    if (tmp instanceof TypeDec) {
                        Exp constraint = getTrueVarExp();
                        if (((TypeDec) tmp).getConstraint() != null)
                            constraint =
                                    (Exp) ((TypeDec) tmp).getConstraint()
                                            .clone();
                        VarExp par = new VarExp();
                        par.setName(createPosSymbol(name));
                        VarExp old = new VarExp();

                        old.setName(((TypeDec) tmp).getExemplar());
                        constraint = constraint.replace(old, par);
                        if (allConstraint != null) {
                            InfixExp exp = new InfixExp();
                            exp.setLeft(constraint);
                            exp.setRight((Exp) allConstraint.clone());
                            exp.setOpName(createPosSymbol("and"));
                            exp.setType(BooleanType.INSTANCE);
                            return exp;
                        }
                        else
                            return constraint;
                    }
                }
            }
        }

        return getTrueVarExp();

    }

    private Exp getConstraints(VarDec var) {

        VarExp left = new VarExp();

        Ty varTy = var.getTy();

        if (varTy instanceof NameTy) {

            left.setName(createPosSymbol(((NameTy) var.getTy()).getName()
                    .toString()));

            NameTy varNameTy = (NameTy) varTy;
            ModuleScope curr = table.getModuleScope();
            TypeHolder typeHold = curr.getTypeHolder();
            TypeID typeID = new TypeID(varNameTy.getName().getSymbol());
            Type vnType = typeHold.searchForType(typeID);

            if (vnType instanceof ConcType) {
                ModuleID mid = ((ConcType) vnType).getModuleID();
                ConceptModuleDec eDec =
                        (ConceptModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);

                //	Iterator<Exp> listIt = eDec.getConstraints().iterator();

                Exp allConstraint = null;

                Iterator<Dec> i = eDec.getDecs().iterator();
                while (i.hasNext()) {
                    Dec tmp = i.next();
                    if (tmp instanceof TypeDec) {
                        if (((TypeDec) tmp).getConstraint() == null)
                            return null;
                        Exp constraint =
                                (Exp) ((TypeDec) tmp).getConstraint().clone();

                        VarExp par = new VarExp();
                        par.setName(createPosSymbol(var.getName().toString()));
                        par.setType(getTypeFromTy(var.getTy()));

                        VarExp old = new VarExp();
                        old.setName(((TypeDec) tmp).getExemplar());
                        old.setType(getTypeFromTy(((TypeDec) tmp).getModel()));

                        constraint = constraint.replace(old, par);

                        if (allConstraint != null) {
                            InfixExp exp = new InfixExp();
                            exp.setLeft(constraint);
                            exp.setRight((Exp) allConstraint.clone());
                            exp.setOpName(createPosSymbol("and"));
                            exp.setType(BooleanType.INSTANCE);

                            return exp;
                        }
                        else {
                            return constraint;
                        }
                    }
                }
            }
            else if (vnType instanceof NameType) {
                ModuleID mid = ((NameType) vnType).getModuleID();
                ModuleDec dec = myInstanceEnvironment.getModuleDec(mid);
                if (dec instanceof ShortFacilityModuleDec) {
                    FacilityDec facDec =
                            ((ShortFacilityModuleDec) dec).getDec();

                    ModuleID modId =
                            ModuleID.createConceptID(facDec.getConceptName());
                    ConceptModuleDec modDec =
                            (ConceptModuleDec) myInstanceEnvironment
                                    .getModuleDec(modId);

                    Exp allConstraint = null;

                    Iterator<Dec> i = modDec.getDecs().iterator();
                    while (i.hasNext()) {
                        Dec tmp = i.next();
                        if (tmp instanceof TypeDec) {
                            Exp constraint = getTrueVarExp();
                            if (((TypeDec) tmp).getConstraint() != null) {
                                constraint =
                                        (Exp) ((TypeDec) tmp).getConstraint()
                                                .clone();
                            }
                            VarExp par = new VarExp();
                            par.setName(createPosSymbol(var.getName()
                                    .toString()));
                            par.setType(getTypeFromTy(var.getTy()));

                            VarExp old = new VarExp();
                            old.setName(((TypeDec) tmp).getExemplar());
                            old.setType(getTypeFromTy(((TypeDec) tmp)
                                    .getModel()));

                            constraint = constraint.replace(old, par);
                            if (allConstraint != null) {
                                InfixExp exp = new InfixExp();
                                exp.setLeft(constraint);
                                exp.setRight((Exp) allConstraint.clone());
                                exp.setOpName(createPosSymbol("and"));
                                exp.setType(BooleanType.INSTANCE);

                                return exp;
                            }
                            else {
                                return constraint;
                            }
                        }
                    }
                }
            }
        }
        else {
            return getTrueVarExp();
        }
        return getTrueVarExp();

    }

    private Exp getConstraintsFromCurrentContext(AssertiveCode assertion,
            boolean skipThisConcept, boolean procedureInConcept) {
        List<Dec> context = getContext(skipThisConcept);
        Iterator<Dec> it = context.iterator();

        Exp constraints = null;

        /* Move to a get_Constraints_for_Proc_Dec function */
        while (it.hasNext()) {
            Dec tmp = it.next();
            if (tmp instanceof ConceptModuleDec) {
                boolean currentModule = false;
                if (isCurrentConceptModuleDec((ConceptModuleDec) tmp)) {
                    currentModule = true;
                }

                Exp myConstraints =
                        getAssumptionsFromConceptDec(assertion,
                                (ConceptModuleDec) tmp, null, currentModule,
                                procedureInConcept);

                if (currentModule && myConstraints != null) {

                    VarExp cName = new VarExp();

                    String exemplar =
                            getExemplar((ConceptModuleDec) tmp).toString();
                    cName.setName(createPosSymbol("Conc_" + exemplar));

                    Ty model = getModel((ConceptModuleDec) tmp);
                    cName.setType(getTypeFromTy(model));

                    VarExp exempVarExp = new VarExp();
                    exempVarExp.setName(createPosSymbol(exemplar));
                    myConstraints = replace(myConstraints, exempVarExp, cName);
                }

                if (myConstraints != null) {
                    if (constraints == null) {
                        constraints = (Exp) myConstraints.clone();
                    }
                    else {
                        constraints =
                                InfixExp
                                        .formAndStmt(myConstraints, constraints);
                    }
                }

            }
            else if (tmp instanceof FacilityDec) {
                FacilityDec tmpFacDec = (FacilityDec) tmp;
                ModuleID cmid =
                        ModuleID.createConceptID(tmpFacDec.getConceptName());
                ConceptModuleDec concDec =
                        (ConceptModuleDec) myInstanceEnvironment
                                .getModuleDec(cmid);

                ModuleID modBodyID =
                        ModuleID.createConceptBodyID(tmpFacDec.getBodyName(),
                                tmpFacDec.getConceptName());
                ConceptBodyModuleDec concBodyDec =
                        (ConceptBodyModuleDec) myInstanceEnvironment
                                .getModuleDec(modBodyID);

                Exp myConstraints =
                        getAssumptionsFromConceptDec(assertion, concDec,
                                (FacilityDec) tmp, true, false);

                ModuleDec currentModuleDec = getCurrentBodyModuleDec();
                if (currentModuleDec == null) {
                    currentModuleDec = concBodyDec;
                }
                if (currentModuleDec instanceof ConceptBodyModuleDec) {
                    Iterator<Dec> myIt =
                            ((ConceptBodyModuleDec) currentModuleDec).getDecs()
                                    .iterator();
                    while (myIt.hasNext()) {
                        Dec myDec = myIt.next();
                        if (myDec instanceof RepresentationDec) {
                            Ty myType =
                                    ((RepresentationDec) myDec)
                                            .getRepresentation();
                            if (myType instanceof RecordTy) {
                                RecordTy typ = (RecordTy) myType;
                                Iterator myit = typ.getFields().iterator();
                                while (myit.hasNext()) {
                                    try {
                                        VarDec dec = (VarDec) myit.next();
                                        if (dec.getTy() instanceof NameTy
                                                && ((NameTy) dec.getTy())
                                                        .getQualifier()
                                                        .equals(
                                                                tmpFacDec
                                                                        .getName()
                                                                        .toString())) {
                                            DotExp myReplExp = new DotExp();
                                            List<Exp> mySegments =
                                                    new List<Exp>();
                                            VarExp varExp = new VarExp();

                                            ModuleID modID =
                                                    ModuleID
                                                            .createConceptID(((ConceptBodyModuleDec) currentModuleDec)
                                                                    .getConceptName());
                                            ConceptModuleDec moduleDec =
                                                    (ConceptModuleDec) myInstanceEnvironment
                                                            .getModuleDec(modID);
                                            PosSymbol exemplar =
                                                    getExemplar(moduleDec);
                                            if (exemplar.toString().length() > 0) {
                                                varExp.setName(exemplar);
                                                mySegments.add((Exp) varExp
                                                        .clone());
                                            }
                                            varExp.setName(dec.getName());

                                            mySegments
                                                    .add((Exp) varExp.clone());
                                            myReplExp.setSegments(mySegments);

                                            modID =
                                                    ModuleID
                                                            .createConceptID(tmpFacDec
                                                                    .getConceptName());
                                            moduleDec =
                                                    (ConceptModuleDec) myInstanceEnvironment
                                                            .getModuleDec(modID);
                                            exemplar = getExemplar(moduleDec);
                                            if (exemplar.toString().length() > 0) {
                                                varExp.setName(exemplar);

                                            }
                                            myConstraints =
                                                    replace(myConstraints,
                                                            varExp, myReplExp);
                                        }
                                    }
                                    catch (Exception ex) {}
                                }
                            }
                        }
                    }
                    if (myConstraints != null)
                        myConstraints =
                                replaceFacilityDeclarationVariables(
                                        myConstraints, concDec.getParameters(),
                                        tmpFacDec.getConceptParams());
                }
                else {
                    myConstraints = null;
                }
                if (myConstraints != null) {
                    if (constraints == null) {
                        constraints = (Exp) myConstraints.clone();
                    }
                    else {
                        constraints =
                                InfixExp
                                        .formAndStmt(myConstraints, constraints);
                    }
                }
            }
        }

        return constraints;
    }

    private List<Dec> getContext(boolean skipThisConcept) {
        Scope current = table.getCurrentScope();
        ModuleID mid = getCurrentModuleID();
        ModuleDec modDec = null;
        ModuleID tid = null;

        if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            modDec = myInstanceEnvironment.getModuleDec(mid);
        }
        else if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            tid = mid.getEnhancementID();
            modDec = myInstanceEnvironment.getModuleDec(tid);
        }

        List<Dec> context = new List<Dec>();
        if (modDec instanceof EnhancementModuleDec) {

            EnhancementBodyModuleDec ebDec =
                    (EnhancementBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);

            /* Find Corresponding EnhancementModuleDec*/
            EnhancementModuleDec eDec =
                    (EnhancementModuleDec) myInstanceEnvironment
                            .getModuleDec(tid);
            ModuleID cid = mid.getConceptID();
            ConceptModuleDec cDec =
                    (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);

            addToContext(context, eDec);

            addToContext(context, cDec);

            /* Check the Declared Facilities */
            Iterator<Dec> decIt = ebDec.getDecs().iterator();
            while (decIt.hasNext()) {
                Dec tmp = decIt.next();

                if (tmp instanceof FacilityDec) {

                    addToContext(context, tmp);

                }
            }
        }
        else if (modDec instanceof ConceptModuleDec) {
            EnhancementBodyModuleDec ebDec =
                    (EnhancementBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);

            /* Find Corresponding EnhancementModuleDec*/
            ModuleID cid = mid.getConceptID();
            ConceptModuleDec cDec =
                    (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);

            addToContext(context, cDec);

            /* Check the Declared Facilities */
            Iterator<Dec> decIt = ebDec.getDecs().iterator();
            while (decIt.hasNext()) {
                Dec tmp = decIt.next();

                if (tmp instanceof FacilityDec) {
                    addToContext(context, tmp);

                }
            }
        }
        else if (modDec instanceof ConceptBodyModuleDec) {

            /* Find Corresponding ConcepttModuleDec*/
            ModuleID cid = mid.getConceptID();
            ConceptModuleDec cDec =
                    (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);

            if (!skipThisConcept) {
                addToContext(context, cDec);
            }

            /* Check the Declared Facilities */
            Iterator<Dec> decIt =
                    ((ConceptBodyModuleDec) modDec).getDecs().iterator();
            while (decIt.hasNext()) {
                Dec tmp = decIt.next();

                if (tmp instanceof FacilityDec) {

                    addToContext(context, tmp);

                }
            }
        }

        /* Check Program Visible Modules */
        ModuleScope mScope = null;
        if (current instanceof ProcedureScope) {
            mScope = ((ProcedureScope) current).getModuleScope();
        }
        else if (current instanceof ModuleScope) {
            mScope = (ModuleScope) current;
        }

        if (mScope instanceof ModuleScope) {
            Iterator<ModuleScope> it =
                    ((ModuleScope) mScope).getProgramVisibleModules();
            while (it.hasNext()) {
                Dec tmp = checkModuleScope(it.next());
                if (tmp instanceof ConceptModuleDec) {
                    addToContext(context, tmp);
                }
                else if (tmp instanceof FacilityModuleDec) {
                    addToContext(context, tmp);
                    List<Dec> myDecs = ((FacilityModuleDec) tmp).getDecs();
                    for (Dec element : myDecs) // or sArray
                    {
                        addToContext(context, element);
                    }
                }
                else
                    addToContext(context, tmp);
            }
        }

        return context;
    }

    private Exp getCorAssignPartExp(ProgramExp exp, AssertiveCode assertion) {

        if (exp instanceof ProgramCharExp) {
            return exp;
        }
        else if (exp instanceof ProgramDotExp) {

            DotExp tmp = new DotExp();
            setLocation(tmp, ((ProgramDotExp) exp).getLocation());

            tmp.setSemanticExp(((ProgramDotExp) exp).getSemanticExp());
            Iterator<ProgramExp> it =
                    ((ProgramDotExp) exp).getSegments().iterator();
            List<Exp> lst = new List<Exp>();
            while (it.hasNext()) {
                ProgramExp tmpExp = it.next();
                if (tmpExp instanceof ProgramParamExp) {
                    return (getCorAssignPartExp(tmpExp, assertion));
                }
                lst.add(getCorAssignPartExp(tmpExp, assertion));

            }
            tmp.setSegments(lst);

            return tmp;
        }
        else if (exp instanceof ProgramDoubleExp) {
            return exp;
        }
        else if (exp instanceof ProgramFunctionExp) {
            Exp replacement =
                    corProgramFunctionExp((ProgramFunctionExp) exp.clone(),
                            assertion);
            return replacement;
        }
        else if (exp instanceof ProgramIntegerExp) {
            return exp;
        }
        else if (exp instanceof ProgramOpExp) {

            ProgramOpExp tmp = (ProgramOpExp) exp;
            PosSymbol name = getProgramOpName(tmp);
            if (name.toString().toLowerCase().equals("and")) {
                Exp exp1 =
                        getCorAssignPartExp(
                                (ProgramExp) tmp.getFirst().clone(), assertion);
                Exp exp2 =
                        getCorAssignPartExp((ProgramExp) tmp.getSecond()
                                .clone(), assertion);
                Exp result = InfixExp.formAndStmt(exp1, exp2);
                return result;
            }
            else {
                ProgramFunctionExp tmp2 = corProgramFuncExp((ProgramOpExp) exp);
                Exp result =
                        corProgramFunctionExp(((ProgramFunctionExp) tmp2
                                .clone()), assertion);
                return result;
            }

        }
        else if (exp instanceof ProgramParamExp) {
            if (((ProgramParamExp) exp).getSemanticExp() == null) {
                ProgramFunctionExp tmp = new ProgramFunctionExp();
                tmp.setArguments(((ProgramParamExp) exp).getArguments());
                tmp.setName(((ProgramParamExp) exp).getName());
                Exp replacement = corProgramFunctionExp(tmp, assertion);
                return replacement;
            }
            Exp replacement =
                    getCorAssignPartExp((ProgramExp) ((ProgramParamExp) exp)
                            .getSemanticExp().clone(), assertion);

            return replacement;
        }
        else if (exp instanceof ProgramStringExp) {
            return exp;
        }
        else {
            return exp;
        }

    }

    private OperationDec getCorOpDec(ProcedureDec dec) {

        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();
        List<Dec> decs = null;

        if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ModuleID tid = mid.getEnhancementID();

            /* Find Corresponding EnhancementModuleDec*/
            EnhancementModuleDec eDec =
                    (EnhancementModuleDec) myInstanceEnvironment
                            .getModuleDec(tid);
            decs = eDec.getDecs();
        }
        else if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ModuleID tid = mid.getConceptID();
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(tid);

            ConceptModuleDec eDec = (ConceptModuleDec) tmp;
            decs = eDec.getDecs();
        }
        else
            return null;

        OperationDec curOperation = null;

        /* Find Corresponding OperationDec and Specification*/
        Iterator<Dec> i = decs.iterator();

        while (i.hasNext()) {
            Dec tmpDec = i.next();
            if ((tmpDec instanceof OperationDec)
                    && (dec.getName().getSymbol() == ((OperationDec) tmpDec)
                            .getName().getSymbol())
                    && compareParameters(((OperationDec) tmpDec)
                            .getParameters(), dec.getParameters())) {
                curOperation = (OperationDec) tmpDec;
            }
        }
        return curOperation;
    }

    private ModuleDec getCurrentBodyModuleDec() {
        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();

        if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(mid);

            return tmp;
        }
        else if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(mid);

            return tmp;
        }
        else
            return null;
    }

    private PosSymbol getCurrentExemplar() {

        ModuleDec myMDec = getCurrentModuleDec();
        if (myMDec instanceof ConceptModuleDec) {
            return getExemplar((ConceptModuleDec) myMDec);
        }
        ModuleDec myMBdec = getCurrentBodyModuleDec();
        if (myMBdec instanceof ConceptBodyModuleDec) {
            ModuleID modID =
                    ModuleID.createConceptID(((ConceptBodyModuleDec) myMBdec)
                            .getConceptName());
            ConceptModuleDec moduleDec =
                    (ConceptModuleDec) myInstanceEnvironment
                            .getModuleDec(modID);
            return getExemplar(moduleDec);
        }
        return createPosSymbol("");
    }

    private ModuleDec getCurrentModuleBodyDec() {
        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();

        if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {

            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(mid);

            return tmp;
        }
        else if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ModuleID tid = mid.getConceptID();
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(tid);

            return tmp;
        }
        else
            return null;
    }

    private ModuleDec getCurrentModuleDec() {
        // This may be WRONG, use getTheCurrentModuleDec
        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();

        if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ModuleID tid = mid.getConceptID();
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(tid);

            return tmp;
        }
        else if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ModuleID tid = mid.getConceptID();
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(tid);

            return tmp;
        }
        else
            return null;
    }

    private ModuleID getCurrentModuleID() {
        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();

        return mid;
    }

    private ModuleKind getCurrentModuleKind() {
        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();
        return mid.getModuleKind();
    }

    private Dec getCurrentProcedure() {
        ScopeID sid = table.getCurrentScope().getScopeID();

        ModuleID mid = sid.getModuleID();
        List<Dec> list = null;
        Iterator<Dec> it = null;
        String name = null;
        if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            EnhancementBodyModuleDec mdec =
                    (EnhancementBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
            list = mdec.getDecs();
            it = list.iterator();
            name = sid.toString();
            int index = name.lastIndexOf(".");
            name = name.substring(index + 1, name.length());
        }
        if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ConceptBodyModuleDec mdec =
                    (ConceptBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
            list = mdec.getDecs();
            it = list.iterator();
            name = sid.toString();
            int index = name.lastIndexOf(".");
            name = name.substring(index + 1, name.length());
        }
        if (mid.getModuleKind() == ModuleKind.FACILITY) {
            FacilityModuleDec fdec =
                    (FacilityModuleDec) myInstanceEnvironment.getModuleDec(mid);
            list = fdec.getDecs();
            it = list.iterator();
            name = sid.toString();
            int index = name.lastIndexOf(".");
            name = name.substring(index + 1, name.length());
        }
        while (it != null && it.hasNext()) {
            Dec tmp = it.next();
            if (tmp instanceof ProcedureDec) {
                if (((ProcedureDec) tmp).getName().toString().equals(name)) {
                    return ((ProcedureDec) tmp);
                }
            }
            else if (tmp instanceof FacilityOperationDec) {
                if (((FacilityOperationDec) tmp).getName().toString().equals(
                        name)) {
                    return ((FacilityOperationDec) tmp);
                }
            }
        }
        return null;
    }

    private OperationDec getEnhancementOperationDec(PosSymbol name,
            List<ProgramExp> arguments) {
        Scope current = table.getCurrentScope();
        ModuleID mid = getCurrentModuleID();
        ModuleID tid = mid.getEnhancementID();

        EnhancementBodyModuleDec ebDec =
                (EnhancementBodyModuleDec) myInstanceEnvironment
                        .getModuleDec(mid);

        /* Find Corresponding EnhancementModuleDec*/
        EnhancementModuleDec eDec =
                (EnhancementModuleDec) myInstanceEnvironment.getModuleDec(tid);
        ModuleID cid = mid.getConceptID();
        ConceptModuleDec cDec =
                (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);

        /* Find Corresponding OperationDec and Specification*/

        // Check parameters for Operations
        List<ModuleParameter> params = ebDec.getParameters();
        Iterator<ModuleParameter> paramIt = params.iterator();
        while (paramIt.hasNext()) {
            ModuleParameter tmp = paramIt.next();
            if (tmp instanceof OperationDec) {
                OperationDec opDec = (OperationDec) tmp;
                if (name.getSymbol().equals(opDec.getName().getSymbol())) {
                    //Iterator<ParameterVarDec> paramsIt = opDec.getParameters().iterator();
                    if (compareArguments(arguments, opDec.getParameters()))
                        return opDec;
                }
            }
        }

        // Check current Enhancement
        List<Dec> decs = eDec.getDecs();
        Iterator<Dec> i = decs.iterator();
        while (i.hasNext()) {
            Dec tmpDec = i.next();
            if ((tmpDec instanceof OperationDec)
                    && (name.getSymbol() == ((OperationDec) tmpDec).getName()
                            .getSymbol())) {
                if (compareArguments(arguments, ((OperationDec) tmpDec)
                        .getParameters()))
                    return (OperationDec) tmpDec;
            }
        }

        // Check current Enhancement Body
        decs = ebDec.getDecs();
        i = decs.iterator();
        while (i.hasNext()) {
            Dec tmpDec = i.next();
            if ((tmpDec instanceof OperationDec)
                    && (name.getSymbol() == ((OperationDec) tmpDec).getName()
                            .getSymbol())) {
                if (compareArguments(arguments, ((OperationDec) tmpDec)
                        .getParameters()))
                    return (OperationDec) tmpDec;
            }

            if ((tmpDec instanceof FacilityOperationDec)
                    && (name.getSymbol() == ((FacilityOperationDec) tmpDec)
                            .getName().getSymbol())) {
                if (compareArguments(arguments, ((FacilityOperationDec) tmpDec)
                        .getParameters()))
                    return new OperationDec(((FacilityOperationDec) tmpDec)
                            .getName(), ((FacilityOperationDec) tmpDec)
                            .getParameters(), ((FacilityOperationDec) tmpDec)
                            .getReturnTy(), ((FacilityOperationDec) tmpDec)
                            .getStateVars(), ((FacilityOperationDec) tmpDec)
                            .getRequires(), ((FacilityOperationDec) tmpDec)
                            .getEnsures());
            }
        }

        // Check the Enhanced concept 
        decs = cDec.getDecs();
        i = decs.iterator();
        while (i.hasNext()) {
            Dec tmpDec = i.next();
            if ((tmpDec instanceof OperationDec)
                    && (name.getSymbol() == ((OperationDec) tmpDec).getName()
                            .getSymbol())) {
                if (compareArguments(arguments, ((OperationDec) tmpDec)
                        .getParameters()))
                    return (OperationDec) tmpDec;
            }
        }

        // Check Program Visible Modules 
        if (current instanceof ProcedureScope) {
            ModuleScope mScope = ((ProcedureScope) current).getModuleScope();
            Iterator<ModuleScope> it = mScope.getProgramVisibleModules();
            while (it.hasNext()) {

                Dec tmp = checkModuleScope(it.next());
                if (tmp instanceof ConceptModuleDec) {
                    decs = ((ConceptModuleDec) tmp).getDecs();
                    i = decs.iterator();
                    while (i.hasNext()) {
                        Dec tmpDec = i.next();
                        if ((tmpDec instanceof OperationDec)
                                && (name.getSymbol() == ((OperationDec) tmpDec)
                                        .getName().getSymbol())) {
                            if (compareArguments(arguments,
                                    ((OperationDec) tmpDec).getParameters()))
                                return (OperationDec) tmpDec;
                        }
                    }
                }
            }
        }

        // Check the Declared Facilities 
        Iterator<Dec> decIt = ebDec.getDecs().iterator();
        while (decIt.hasNext()) {
            Dec tmp = decIt.next();

            if (tmp instanceof FacilityDec) {
                FacilityDec tmpFacDec = (FacilityDec) tmp;
                ModuleID cmid =
                        ModuleID.createConceptID(tmpFacDec.getConceptName());
                ModuleDec concDec = myInstanceEnvironment.getModuleDec(cmid);
                if (concDec instanceof ConceptModuleDec) {
                    decs = ((ConceptModuleDec) concDec).getDecs();
                    i = decs.iterator();
                    while (i.hasNext()) {
                        Dec tmpDec = i.next();
                        if ((tmpDec instanceof OperationDec)
                                && (name.getSymbol() == ((OperationDec) tmpDec)
                                        .getName().getSymbol())
                                && compareArguments(arguments,
                                        ((OperationDec) tmpDec).getParameters())) {
                            Exp req = ((OperationDec) tmpDec).getRequires();
                            return (OperationDec) tmpDec;
                        }
                    }
                }
            }
        }

        // Check incoming operations
        /*   Iterator<ModuleParameter> itParam = ebDec.getParameters().iterator();
           while(itParam.hasNext()){
        	   ModuleParameter tmp = itParam.next();
        	   if(tmp instanceof OperationDec){
        		   return (OperationDec)tmp;
        	   }
           }*/

        return null;
    }

    private PosSymbol getExemplar(ConceptModuleDec dec) {
        Iterator it = dec.getDecs().iterator();
        while (it.hasNext()) {
            Dec tmpDec = (Dec) it.next();
            if (tmpDec instanceof TypeDec) {
                return ((TypeDec) tmpDec).getExemplar();
            }
        }
        return createPosSymbol("");
    }

    private OperationDec getFacilityOperationDec(PosSymbol name,
            List<ProgramExp> arguments) {
        Scope current = table.getCurrentScope();
        ModuleID mid = getCurrentModuleID();

        List<Dec> decs;
        Iterator<Dec> i;

        if ((myInstanceEnvironment.getModuleDec(mid) instanceof FacilityModuleDec)) {

            FacilityModuleDec fDec =
                    (FacilityModuleDec) myInstanceEnvironment.getModuleDec(mid);

            /* Find Corresponding OperationDec and Specification*/

            // Check the concept 
            decs = fDec.getDecs();
            i = decs.iterator();
            while (i.hasNext()) {
                Dec tmpDec = i.next();
                if ((tmpDec instanceof OperationDec)
                        && (name.getSymbol() == ((OperationDec) tmpDec)
                                .getName().getSymbol())) {
                    if (compareArguments(arguments, ((OperationDec) tmpDec)
                            .getParameters()))
                        return (OperationDec) tmpDec;
                }
            }

            // Check the Declared Facilities 
            Iterator<Dec> decIt = fDec.getDecs().iterator();
            while (decIt.hasNext()) {
                Dec tmp = decIt.next();

                if (tmp instanceof FacilityDec) {
                    FacilityDec tmpFacDec = (FacilityDec) tmp;
                    ModuleID cmid =
                            ModuleID
                                    .createConceptID(tmpFacDec.getConceptName());
                    ModuleDec concDec =
                            myInstanceEnvironment.getModuleDec(cmid);
                    if (concDec instanceof ConceptModuleDec) {
                        decs = ((ConceptModuleDec) concDec).getDecs();
                        i = decs.iterator();
                        while (i.hasNext()) {
                            Dec tmpDec = i.next();
                            if ((tmpDec instanceof OperationDec)
                                    && (name.getSymbol() == ((OperationDec) tmpDec)
                                            .getName().getSymbol())
                                    && compareArguments(arguments,
                                            ((OperationDec) tmpDec)
                                                    .getParameters())) {

                                Exp req = ((OperationDec) tmpDec).getRequires();
                                if (req != null) {
                                    req =
                                            replaceFacilityDeclarationVariables(
                                                    req,
                                                    ((ConceptModuleDec) concDec)
                                                            .getParameters(),
                                                    tmpFacDec
                                                            .getConceptParams());
                                    ((OperationDec) tmpDec).setRequires(req);
                                }
                                Exp ens = ((OperationDec) tmpDec).getEnsures();
                                if (ens != null) {
                                    ens =
                                            replaceFacilityDeclarationVariables(
                                                    ens,
                                                    ((ConceptModuleDec) concDec)
                                                            .getParameters(),
                                                    tmpFacDec
                                                            .getConceptParams());
                                    ((OperationDec) tmpDec).setEnsures(ens);
                                }

                                return (OperationDec) tmpDec;

                            }
                        }
                    }
                    Iterator<EnhancementItem> enhIt =
                            tmpFacDec.getEnhancements().iterator();
                    while (enhIt.hasNext()) {
                        EnhancementItem enh = enhIt.next();
                        ModuleID enhID =
                                ModuleID.createEnhancementID(tmpFacDec
                                        .getConceptName(), enh.getName());
                        ModuleDec enhDec =
                                myInstanceEnvironment.getModuleDec(enhID);
                        if (enhDec instanceof EnhancementModuleDec) {
                            decs = ((EnhancementModuleDec) enhDec).getDecs();
                            i = decs.iterator();
                            while (i.hasNext()) {
                                Dec tmpDec = i.next();
                                if ((tmpDec instanceof OperationDec)
                                        && (name.getSymbol() == ((OperationDec) tmpDec)
                                                .getName().getSymbol())
                                        && compareArguments(arguments,
                                                ((OperationDec) tmpDec)
                                                        .getParameters())) {
                                    Exp req =
                                            ((OperationDec) tmpDec)
                                                    .getRequires();
                                    if (req != null) {
                                        req =
                                                replaceFacilityDeclarationVariables(
                                                        req,
                                                        ((ConceptModuleDec) concDec)
                                                                .getParameters(),
                                                        tmpFacDec
                                                                .getConceptParams());
                                        ((OperationDec) tmpDec)
                                                .setRequires(req);
                                    }
                                    Exp ens =
                                            ((OperationDec) tmpDec)
                                                    .getEnsures();
                                    if (ens != null) {
                                        ens =
                                                replaceFacilityDeclarationVariables(
                                                        ens,
                                                        ((ConceptModuleDec) concDec)
                                                                .getParameters(),
                                                        tmpFacDec
                                                                .getConceptParams());
                                        ((OperationDec) tmpDec).setEnsures(ens);
                                    }
                                    return (OperationDec) tmpDec;
                                }
                            }
                        }
                    }

                    if (((FacilityDec) tmpFacDec).getEnhancementBodies() != null) {
                        List<EnhancementBodyItem> enhancements =
                                ((FacilityDec) tmpFacDec)
                                        .getEnhancementBodies();
                        Iterator<EnhancementBodyItem> enhanceIt =
                                enhancements.iterator();
                        while (enhanceIt.hasNext()) {
                            EnhancementBodyItem currentEnhance =
                                    enhanceIt.next();

                            ModuleID ehmid =
                                    ModuleID.createEnhancementID(currentEnhance
                                            .getName(),
                                            ((FacilityDec) tmpFacDec)
                                                    .getConceptName());
                            ModuleDec myDec =
                                    myInstanceEnvironment.getModuleDec(ehmid);
                            if (myDec instanceof EnhancementModuleDec) {
                                EnhancementModuleDec ebDec =
                                        (EnhancementModuleDec) myDec;
                                // Check current Enhancement Body
                                decs = ebDec.getDecs();
                                i = decs.iterator();
                                while (i.hasNext()) {
                                    Dec tmpDec = i.next();
                                    if ((tmpDec instanceof OperationDec)
                                            && (name.getSymbol() == ((OperationDec) tmpDec)
                                                    .getName().getSymbol())) {
                                        if (compareArguments(arguments,
                                                ((OperationDec) tmpDec)
                                                        .getParameters()))
                                            return (OperationDec) tmpDec;
                                    }

                                    if ((tmpDec instanceof FacilityOperationDec)
                                            && (name.getSymbol() == ((FacilityOperationDec) tmpDec)
                                                    .getName().getSymbol())) {
                                        if (compareArguments(arguments,
                                                ((FacilityOperationDec) tmpDec)
                                                        .getParameters()))
                                            return new OperationDec(
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getName(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getParameters(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getReturnTy(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getStateVars(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getRequires(),
                                                    ((FacilityOperationDec) tmpDec)
                                                            .getEnsures());
                                    }
                                }
                            }
                        }
                    }

                }
                else if (tmp instanceof FacilityOperationDec) {
                    FacilityOperationDec tmpFacDec = (FacilityOperationDec) tmp;
                    if (tmpFacDec.getName().getSymbol()
                            .equals(name.getSymbol())) {
                        OperationDec newOpDec = new OperationDec();
                        newOpDec.setName(tmpFacDec.getName());
                        newOpDec.setEnsures(tmpFacDec.getEnsures());
                        newOpDec.setParameters(tmpFacDec.getParameters());
                        newOpDec.setRequires(tmpFacDec.getRequires());
                        newOpDec.setReturnTy(tmpFacDec.getReturnTy());
                        newOpDec.setStateVars(tmpFacDec.getStateVars());
                        return newOpDec;
                    }
                }
            }
        }

        // Check Program Visible Modules 
        if (current instanceof ProcedureScope) {
            ModuleScope mScope = ((ProcedureScope) current).getModuleScope();
            Iterator<ModuleScope> it = mScope.getProgramVisibleModules();
            while (it.hasNext()) {

                Dec tmp = checkModuleScope(it.next());
                if (tmp instanceof ConceptModuleDec) {
                    decs = ((ConceptModuleDec) tmp).getDecs();
                    i = decs.iterator();
                    while (i.hasNext()) {
                        Dec tmpDec = i.next();
                        if ((tmpDec instanceof OperationDec)
                                && (name.getSymbol() == ((OperationDec) tmpDec)
                                        .getName().getSymbol())) {
                            if (compareArguments(arguments,
                                    ((OperationDec) tmpDec).getParameters()))
                                return (OperationDec) tmpDec;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * <p>This will return the VCs that were built by the last parse through an
     * AST, as an <code>AssertiveCode</code>.</p>
     * 
     * @return The VCs from the last parse.
     */
    public Collection<AssertiveCode> getFinalVCs() {
        return myFinalVCs;
    }

    private ConcType getFreeVar(Dec var) {
        return getFreeVar(var, table.getModuleScope());
    }

    private ConcType getFreeVar(Dec var, ModuleScope scope) {
        Type type = null;
        ModuleScope curr = table.getModuleScope();

        /* Assuming the Declaration is of these types, 
         * the type is found
         */
        Ty varTy = null;
        if (var instanceof VarDec) {
            varTy = ((VarDec) var).getTy();
        }
        else if (var instanceof ParameterVarDec) {
            varTy = ((ParameterVarDec) var).getTy();
        }
        else if (var instanceof ConstantParamDec) {
            varTy = ((ConstantParamDec) var).getTy();

        }
        else if (var instanceof ConceptTypeParamDec) {
            varTy = null;
        }
        else if (var instanceof MathVarDec) {
            varTy = ((MathVarDec) var).getTy();
        }
        else if (var instanceof DefinitionDec) {
            varTy = ((DefinitionDec) var).getReturnTy();
        }
        else {
            return null;
        }
        Binding binding = curr.getBinding();

        if (varTy instanceof NameTy) {
            Symbol name = null, qual = null;
            if (((NameTy) varTy).getName() != null)
                name = ((NameTy) varTy).getName().getSymbol();
            if (((NameTy) varTy).getQualifier() != null)
                qual = ((NameTy) varTy).getQualifier().getSymbol();

            ModuleScope modSc = getTypeModuleScope(qual, name);
            if (modSc != null)
                type = modSc.getType(name).getType();

        }

        if (type == null) {
            TypeConverter TC =
                    new TypeConverter(myInstanceEnvironment
                            .getSymbolTable(getCurrentModuleID()));

            if (type == null)
                type = TC.getMathType(varTy);

            if (type == null && varTy instanceof NameTy) {
                Boolean cont = binding.contains(((NameTy) varTy).getName());
                if (cont) {
                    type = binding.getType(null, ((NameTy) varTy).getName());
                    //	return convertToConcType(var.getName(), type);
                }
            }

            if (type == null) {
                type = TC.getProgramType(varTy);
            }
        }

        return convertToConcType(var.getName(), type);
    }

    private ConcType getIfInFreeVarList(String name, AssertiveCode assertion) {

        return getIfInFreeVarList(name, assertion, false);
    }

    private ConcType getIfInFreeVarList(String name, AssertiveCode assertion,
            boolean global) {
        Iterator<ConcType> freeVars = assertion.getFreeVars2().iterator();
        while (freeVars.hasNext()) {
            ConcType tmp = freeVars.next();
            if (global && tmp.getName().toString().endsWith("." + name)) {
                return tmp;
            }
            else if (tmp.getName().toString().equals(name)) {

                return tmp;
            }
        }
        return null;
    }

    private Exp getInitialExp(VarDec var) {
        DotExp exp = new DotExp();
        List<Exp> list = new List<Exp>();
        VarExp left = new VarExp();
        FunctionExp right = new FunctionExp();
        FunctionArgList fAL = new FunctionArgList();
        List<Exp> params = new List<Exp>();
        List<FunctionArgList> faList = new List<FunctionArgList>();

        Ty varTy = var.getTy();

        if (varTy instanceof NameTy) {

            left.setName(createPosSymbol(((NameTy) var.getTy()).getName()
                    .toString()));
            left.setType(TypeType.INSTANCE);

            NameTy varNameTy = (NameTy) varTy;

            ModuleScope curr = table.getModuleScope();
            TypeHolder typeHold = curr.getTypeHolder();
            TypeID typeID = new TypeID(varNameTy.getName().getSymbol());
            //	TypeConverter tC = new TypeConverter();
            Type vnType = typeHold.searchForType(typeID);

            if (vnType instanceof IndirectType) {
                /*
                FacilityDec facDec = null;
                ConceptModuleDec modDec = null;

                if(((IndirectType)vnType).getProgramName() != null){
                	ModuleID mid = ((IndirectType)vnType).getProgramName().getModuleID();
                
                	ModuleDec dec = env.getModuleDec(mid);  
                	
                	if(dec instanceof ShortFacilityModuleDec){
                		facDec = ((ShortFacilityModuleDec)dec).getDec();
                	}
                	else {
                
                    	
                	}

                	if(facDec != null){
                		ModuleID modId = ModuleID.createConceptID(facDec.getConceptName());
                	
                		modDec = (ConceptModuleDec)env.getModuleDec(modId); 
                	}
                	if(dec instanceof ConceptBodyModuleDec){
                		ModuleID cid = mid.getConceptID();
                		modDec = (ConceptModuleDec)env.getModuleDec(cid);            		
                	}
                }
                if(modDec != null){
                	Iterator<Dec> i = modDec.getDecs().iterator();
                	while(i.hasNext()){          	
                		Dec tmp = i.next();
                		if(tmp instanceof TypeDec){
                  		
                			InitItem item = ((TypeDec)tmp).getInitialization();

                			VarExp par = new VarExp();
                			par.setName(createPosSymbol(var.getName().toString()));
                			par.setType(getTypeFromTy(varTy));
                			
                			VarExp old = new VarExp();
                  			old.setName(((TypeDec)tmp).getExemplar());
                  			old.setType(getTypeFromTy(((TypeDec)tmp).getModel()));
                  			
                      		Exp initEns = ((Exp)item.getEnsures().clone());
                      		
                      		Location myLoc = initEns.getLocation();
                      		myLoc.setDetails("Initialization Ensures for " + tmp.getName());
                      		setLocation(initEns, myLoc);
                  			
                      		Exp result = initEns.replace(old, par);
                      		return result;
                		}
                	}
                }else{           	
                 */
                vnType = ((IndirectType) vnType).getType();
                //}
            }
            if (vnType instanceof ConcType) {
                ModuleID mid = ((ConcType) vnType).getModuleID();
                ConceptModuleDec eDec =
                        (ConceptModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);
                Iterator<Dec> i = eDec.getDecs().iterator();
                while (i.hasNext()) {
                    Dec tmp = i.next();
                    if (tmp instanceof TypeDec) {
                        InitItem item = ((TypeDec) tmp).getInitialization();

                        VarExp par = new VarExp();
                        par.setName(createPosSymbol(var.getName().toString()));
                        par.setType(getTypeFromTy(varTy));

                        VarExp old = new VarExp();
                        old.setName(((TypeDec) tmp).getExemplar());
                        old.setType(getTypeFromTy(((TypeDec) tmp).getModel()));

                        Exp initEns = ((Exp) item.getEnsures().clone());
                        Location myLoc = initEns.getLocation();
                        myLoc.setDetails("Initialization Ensures for"
                                + tmp.getName());
                        setLocation(initEns, myLoc);

                        Exp result = initEns.replace(old, par);
                        return result;
                    }
                }
            }
            if (vnType instanceof RecordType) {
                ModuleID mid = ((RecordType) vnType).getModuleID();
                ModuleDec eDec = myInstanceEnvironment.getModuleDec(mid);
                if (eDec != null & eDec instanceof ConceptBodyModuleDec) {
                    ModuleID cid = mid.getConceptID();
                    ModuleDec cDec = myInstanceEnvironment.getModuleDec(cid);
                    if (cDec instanceof ConceptModuleDec && cDec != null) {
                        Iterator<Dec> i =
                                ((ConceptModuleDec) cDec).getDecs().iterator();
                        while (i.hasNext()) {
                            Dec tmp = i.next();
                            if (tmp instanceof TypeDec) {
                                InitItem item =
                                        ((TypeDec) tmp).getInitialization();

                                VarExp par = new VarExp();
                                par.setName(createPosSymbol(var.getName()
                                        .toString()));
                                par.setType(getTypeFromTy(varTy));

                                VarExp old = new VarExp();
                                old.setName(((TypeDec) tmp).getExemplar());
                                old.setType(getTypeFromTy(((TypeDec) tmp)
                                        .getModel()));

                                Exp initEns = ((Exp) item.getEnsures().clone());
                                Location myLoc = initEns.getLocation();
                                myLoc.setDetails("Initialization Ensures for"
                                        + tmp.getName());
                                setLocation(initEns, myLoc);

                                Exp result = initEns.replace(old, par);
                                return result;
                            }
                        }
                    }
                }
            }
            else if (vnType instanceof NameType) {

                FacilityDec facDec = null;

                ModuleID mid = ((NameType) vnType).getModuleID();
                ModuleDec dec = myInstanceEnvironment.getModuleDec(mid);

                if (dec instanceof ShortFacilityModuleDec) {
                    facDec = ((ShortFacilityModuleDec) dec).getDec();
                }
                else {
                    QualifierLocator QL = new QualifierLocator(this.table, err);
                    if (((NameType) vnType).getFacility() != null
                            && QL.isProgramQualifier(((NameType) vnType)
                                    .getFacility())) {
                        try {
                            ModuleScope a =
                                    QL.locateProgramModule(((NameType) vnType)
                                            .getFacility());
                            facDec = a.getFacilityDec();
                        }
                        catch (SymbolSearchException err) {}
                    }

                }
                if (facDec != null) {
                    ModuleID modId =
                            ModuleID.createConceptID(facDec.getConceptName());

                    ConceptModuleDec modDec =
                            (ConceptModuleDec) myInstanceEnvironment
                                    .getModuleDec(modId);
                    Iterator<Dec> i = modDec.getDecs().iterator();
                    while (i.hasNext()) {
                        Dec tmp = i.next();
                        if (tmp instanceof TypeDec) {

                            InitItem item = ((TypeDec) tmp).getInitialization();

                            VarExp par = new VarExp();
                            par.setName(createPosSymbol(var.getName()
                                    .toString()));
                            par.setType(getTypeFromTy(varTy));

                            VarExp old = new VarExp();
                            old.setName(((TypeDec) tmp).getExemplar());
                            old.setType(getTypeFromTy(((TypeDec) tmp)
                                    .getModel()));

                            Exp initEns = ((Exp) item.getEnsures().clone());

                            Location myLoc = initEns.getLocation();
                            myLoc.setDetails("Initialization Ensures for "
                                    + tmp.getName());
                            setLocation(initEns, myLoc);

                            Exp result = initEns.replace(old, par);
                            return result;
                        }
                    }
                }
            }
        }
        else if (varTy instanceof ArrayTy) {
            //left.setName(createPosSymbol(((ArrayTy)var.getTy()).get().toString()));
        }

        VarExp param = new VarExp();
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_ISABELLE_VC)) {
            param.setName(createPosSymbol(var.getName().toString()));
            param.setType(getTypeFromTy(var.getTy()));

            right.setName(createPosSymbol("IsInitial"));
            right.setType(BooleanType.INSTANCE);

            params.add(param);
            fAL.setArguments(params);

            faList.add(fAL);
            right.setParamList(faList);

            list.add(left);
            list.add(right);

            exp.setSegments(list);
            exp.setType(BooleanType.INSTANCE);
        }
        else {
            param.setName(createPosSymbol(var.getName().toString()));
            param.setType(getTypeFromTy(var.getTy()));

            right.setName(createPosSymbol("is_initial"));
            right.setType(BooleanType.INSTANCE);

            params.add(param);
            fAL.setArguments(params);

            faList.add(fAL);
            right.setParamList(faList);

            list.add(left);
            list.add(right);

            exp.setSegments(list);
            exp.setType(BooleanType.INSTANCE);
        }

        return exp;
    }

    private Exp getIterateRuleAssume(IterateStmt stmt, AssertiveCode assertion) {
        InfixExp assume = new InfixExp();
        Exp inv = (Exp) stmt.getMaintaining().clone();

        assume.setLeft(inv);

        EqualsExp PExp = new EqualsExp();
        PExp.setType(BooleanType.INSTANCE);
        VarExp var = new VarExp();
        if (stmt.getDecreasing() == null)
            return inv;
        ConcType pval = getPVAL();
        pval = NQV(assertion.getFinalConfirm(), pval, assertion);
        assertion.addFreeVar(pval);
        assertion.addFreeVar(pval);
        var.setName(pval.getName());
        PExp.setOperator(EqualsExp.EQUAL);
        PExp.setLeft((Exp) var.clone());
        PExp.setRight((Exp) (stmt.getDecreasing()).clone());
        assume.setOpName(createPosSymbol("and"));
        assume.setRight(PExp);
        assume.setType(BooleanType.INSTANCE);

        return assume;
    }

    private Exp getIterateRuleConfirm(IterateStmt stmt, AssertiveCode assertion) {
        InfixExp assume = new InfixExp();
        Exp inv = (Exp) stmt.getMaintaining().clone();

        assume.setLeft(inv);
        VarExp var = new VarExp();
        if (stmt.getDecreasing() == null)
            return inv;
        ConcType pval = getPVAL();
        pval = NQV(assertion.getFinalConfirm(), pval, assertion);
        assertion.addFreeVar(pval);
        var.setName(pval.getName());
        InfixExp PExp = new InfixExp();
        PExp.setLeft((Exp) stmt.getDecreasing().clone());
        PExp.setRight((Exp) var.clone());
        PExp.setOpName(createPosSymbol("<"));
        assume.setOpName(createPosSymbol("and"));
        assume.setType(BooleanType.INSTANCE);
        assume.setRight(PExp);

        return assume;
    }

    private Location getLocationOfLastLine(List<Statement> stmts) {
        Location loc = null;
        if (stmts != null && stmts.size() > 0) {
            Statement stmt = stmts.get(stmts.size() - 1);
            if (stmt instanceof WhileStmt) {
                return getLocationOfLastLine(((WhileStmt) stmt).getStatements());
            }
            else if (stmt instanceof IfStmt) {
                if (((IfStmt) stmt).getElseclause() != null) {
                    return getLocationOfLastLine(((IfStmt) stmt)
                            .getElseclause());
                }
                else if (((IfStmt) stmt).getElseifpairs() != null
                        && ((IfStmt) stmt).getElseifpairs().size() > 0) {
                    ConditionItem item =
                            ((IfStmt) stmt).getElseifpairs()
                                    .get(
                                            ((IfStmt) stmt).getElseifpairs()
                                                    .size() - 1);
                    return getLocationOfLastLine(item.getThenclause());
                }
                else {
                    return getLocationOfLastLine(((IfStmt) stmt)
                            .getThenclause());
                }
            }

            if (stmt instanceof CallStmt) {
                loc = ((CallStmt) stmt).getName().getLocation();
            }
            else if (stmt instanceof SwapStmt) {
                loc = ((SwapStmt) stmt).getLocation();
            }
            else if (stmt instanceof FuncAssignStmt) {
                loc = ((FuncAssignStmt) stmt).getAssign().getLocation();
            }

        }
        return loc;
    }

    // Return output String based on flags
    public String getMainBuffer() {
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_VERBOSE_VC)) {
            //	env.flags.isFlagSet(FLAG_FINALVERB_VC) || env.flags.isFlagSet(FLAG_VERIFY_VC)){
            return VCBuffer.toString();
        }
        else {
            if (!myInstanceEnvironment.flags
                    .isFlagSet(ResolveCompiler.FLAG_XML_OUT)) {
                return buildHeaderComment() + assrtBuf.toString()
                        + buildFooterComment();
            }
            else {
                return assrtBuf.toString();
                //return ResolveCompiler.webEncode(buildHeaderComment()) +
                //assrtBuf.toString() +
                //ResolveCompiler.webEncode(buildFooterComment());
            }

        }
    }

    private String getMainFile() {
        File file = myInstanceEnvironment.getTargetFile();
        ModuleID cid = myInstanceEnvironment.getModuleID(file);
        file = myInstanceEnvironment.getFile(cid);
        String filename = file.toString();
        int temp = filename.indexOf(".");
        String tempfile = filename.substring(0, temp);
        String mainFileName = myInstanceEnvironment.getOutputFilename();
        if (mainFileName != null)
            return mainFileName;

        if (myInstanceEnvironment.flags.isFlagSet(FLAG_ISABELLE_VC)) {
            mainFileName = tempfile + ".thy";
        }
        else if (myInstanceEnvironment.flags.isFlagSet(FLAG_FINALVERB_VC)
                || myInstanceEnvironment.flags
                        .isFlagSet(Verifier.FLAG_VERIFY_VC)) {
            mainFileName = tempfile + ".asrt";
        }
        else {
            mainFileName = tempfile + ".asrt";
        }
        return mainFileName;
    }

    public String getMainFileName() {
        return getMainFile();
    }

    private Exp getMathTest(ProgramExp test, WhileStmt stmt,
            AssertiveCode assertion) {
        return getCorAssignPartExp(stmt.getTest(), assertion);
    }

    //getExemplar assumes that the first type declaration is the one we want.
    //I'm not convinced this is true--there could be more than one and we might
    //want a later one.  This should probably be fixed eventually.  Regardless,
    //I based getModel off of it for consistency.  -HwS
    private Ty getModel(ConceptModuleDec dec) {
        Iterator it = dec.getDecs().iterator();
        while (it.hasNext()) {
            Dec tmpDec = (Dec) it.next();
            if (tmpDec instanceof TypeDec) {
                return ((TypeDec) tmpDec).getModel();
            }
        }

        throw new UnsupportedOperationException("No model!");
    }

    private PosSymbol getName(Ty varTy) {

        if (varTy instanceof NameTy) {
            return ((NameTy) varTy).getName();
        }
        else
            return new PosSymbol();
    }

    private PosSymbol getProgramOpName(ProgramOpExp exp) {
        if (exp.getOperator() == ProgramOpExp.GT) {
            return createPosSymbol("Greater");
        }
        else if (exp.getOperator() == ProgramOpExp.GT_EQL) {
            return createPosSymbol("Greater_Or_Equal");
        }
        else if (exp.getOperator() == ProgramOpExp.AND) {
            return createPosSymbol("And");
        }
        else if (exp.getOperator() == ProgramOpExp.DIV) {
            return createPosSymbol("Div");
        }
        else if (exp.getOperator() == ProgramOpExp.DIVIDE) {
            return createPosSymbol("Divide");
        }
        else if (exp.getOperator() == ProgramOpExp.EQUAL) {
            return createPosSymbol("Are_Equal");
        }
        else if (exp.getOperator() == ProgramOpExp.EXP) {
            return createPosSymbol("Power");
        }
        else if (exp.getOperator() == ProgramOpExp.LT) {
            return createPosSymbol("Less");
        }
        else if (exp.getOperator() == ProgramOpExp.LT_EQL) {
            return createPosSymbol("Less_Or_Equal");
        }
        else if (exp.getOperator() == ProgramOpExp.MINUS) {
            return createPosSymbol("Difference");
        }
        else if (exp.getOperator() == ProgramOpExp.MOD) {
            return createPosSymbol("Mod");
        }
        else if (exp.getOperator() == ProgramOpExp.MULTIPLY) {
            return createPosSymbol("Product");
        }
        else if (exp.getOperator() == ProgramOpExp.NOT) {
            return createPosSymbol("Not");
        }
        /*		else if(exp.getOperator() == ProgramOpExp.INC){
         return createPosSymbol("Increment");
         }
         else if(exp.getOperator() == ProgramOpExp.DEC){
         return createPosSymbol("Decrement");
         }*/
        else if (exp.getOperator() == ProgramOpExp.NOT_EQUAL) {
            return createPosSymbol("Are_Not_Equal");
        }
        else if (exp.getOperator() == ProgramOpExp.OR) {
            return createPosSymbol("Or");
        }
        else if (exp.getOperator() == ProgramOpExp.PLUS) {
            return createPosSymbol("Sum");
        }
        else if (exp.getOperator() == ProgramOpExp.REM) {
            return createPosSymbol("Rem");
        }
        else if (exp.getOperator() == ProgramOpExp.UNARY_MINUS) {
            return createPosSymbol("Negate");
        }
        else
            return new PosSymbol();
    }

    private ConcType getPVAL() {

        ModuleScope curr = table.getModuleScope();
        TypeHolder typeHold = curr.getTypeHolder();
        typeHold.searchForBuiltInTypes();
        Type type = typeHold.getTypeN();

        if (type == null) {
            PosSymbol modName = createPosSymbol("Natural_Number_Theory");
            ModuleID mid = ModuleID.createTheoryID(modName);
            PosSymbol name = createPosSymbol("N");
            IndirectType ty = new IndirectType(null, name, null);
            type = new NameType(mid, name, ty);
        }

        return convertToConcType(createPosSymbol("P_val"), type);
    }

    private Exp getReplacement(Exp realRep, AssertiveCode assertion) {
        if (realRep instanceof ProgramIntegerExp) {
            IntegerExp exp = new IntegerExp();
            exp.setValue(((ProgramIntegerExp) realRep).getValue());
            exp.setType(table.getTypeHolder().getTypeZ());
            return exp;
        }
        else if (realRep instanceof VariableNameExp) {
            VarExp exp = new VarExp();
            exp.setName(((VariableNameExp) realRep).getName());
            return exp;
        }
        else if (realRep instanceof VariableDotExp) {
            DotExp exp = new DotExp();
            List<VariableExp> segements =
                    ((VariableDotExp) realRep).getSegments();
            List<Exp> newSegements = new List<Exp>();
            Iterator<VariableExp> it = segements.iterator();
            while (it.hasNext()) {
                VarExp varExp = new VarExp();
                VariableExp varName = (VariableExp) it.next();
                if (varName instanceof VariableNameExp) {
                    varExp.setName(((VariableNameExp) varName).getName());
                    varExp.setType(((VariableNameExp) varName).getType());
                    newSegements.add(varExp);
                }
                else {
                    System.err.println("Problem");
                }
            }
            exp.setSegments(newSegements);
            return exp;
        }
        else if (realRep instanceof ProgramExp) {
            return getCorAssignPartExp((ProgramExp) realRep, assertion);
        }
        else
            return realRep;
    }

    private Exp getRequiresForProgFuncExp(ProgramFunctionExp exp,
            AssertiveCode assertion) {

        Exp requires = new VarExp();
        ModuleDec ebDec = null;
        ModuleID mid = getCurrentModuleID();
        if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ebDec =
                    (EnhancementBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
        }
        else if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ebDec =
                    (ConceptBodyModuleDec) myInstanceEnvironment
                            .getModuleDec(mid);
        }
        else if (mid.getModuleKind() == ModuleKind.FACILITY) {
            ebDec = (FacilityModuleDec) myInstanceEnvironment.getModuleDec(mid);
        }

        /* Find Corresponding OperationDec and Specification*/
        OperationDec opDec = null;
        if (ebDec instanceof EnhancementBodyModuleDec) {
            opDec =
                    getEnhancementOperationDec(exp.getName(), exp
                            .getArguments());
        }
        else if (ebDec instanceof ConceptBodyModuleDec) {
            opDec = getConceptOperationDec(exp.getName(), exp.getArguments());
        }
        else if (ebDec instanceof FacilityModuleDec) {
            opDec = getFacilityOperationDec(exp.getName(), exp.getArguments());
        }

        if (opDec == null) {
            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nSpec not found \n");
            VCBuffer.append(assertion.assertionToString());

            System.err.println("Error passed operation not found: "
                    + exp.getName().asString(1, 1));
            throw new RuntimeException();
        }
        /*		List<ProgramExp> exps = exp.getArguments();
         Exp req = null;
         for(int i=0; i < exps.size(); i++)
         {
         Exp tmp = invk_cond((Exp)exps.get(i).clone(), assertion);
         if(tmp != null)
         if(req == null)
         req = tmp;
         else
         req = formAndStmt(tmp, req);
         }*/

        requires =
                replacePreConditionVariables(opDec.getRequires(), exp
                        .getArguments(), opDec, assertion);
        if (requires != null && opDec.getRequires() != null) {
            Location loc = opDec.getRequires().getLocation();

            loc.setDetails("Requires Clause of " + opDec.getName());
            setLocation(requires, loc);
        }
        //	if(req != null)
        //		requires = formAndStmt(requires, req);

        return requires;
    }

    private ModuleDec getTheCurrentModuleDec() {
        /* Find Current ModuleID */
        Scope current = table.getCurrentScope();
        ScopeID sid = current.getScopeID();
        ModuleID mid = sid.getModuleID();

        if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
            ModuleID tid = mid.getConceptID();
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(tid);

            return tmp;
        }
        else if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
            ModuleID tid = mid.getEnhancementID();
            ModuleDec tmp = (ModuleDec) myInstanceEnvironment.getModuleDec(tid);

            return tmp;
        }
        else
            return null;
    }

    private VarExp getTrueVarExp() {
        Symbol trueSym = Symbol.symbol("true");
        VarExp trueExp = new VarExp();
        PosSymbol truePosSym = new PosSymbol();
        truePosSym.setSymbol(trueSym);
        trueExp.setName(truePosSym);

        trueExp.setType(BooleanType.INSTANCE);

        return trueExp;
    }

    // Returns the type of var
    private Type getType(Dec var) {
        Ty varTy = null;

        if (var instanceof VarDec)
            return getVarType((VarDec) var);
        if (var instanceof ParameterVarDec)
            varTy = ((ParameterVarDec) var).getTy();
        return getType(var, varTy);
    }

    // Gets the type of var associated with var
    private Type getType(Dec var, Ty varTy) {

        ModuleScope curr = table.getModuleScope();

        if (varTy instanceof ArrayTy) {
            ArrayTy ty = (ArrayTy) varTy;
            ArrayType type =
                    new ArrayType(curr.getModuleID(), var.getName(),
                            ty.getLo(), ty.getHi(), getType(var, ty
                                    .getEntryType()), getType(var, ty
                                    .getEntryType()));
            return type;
        }
        else if (varTy instanceof FunctionTy) {
            FunctionTy varFuncTy = (FunctionTy) varTy;
            Ty dom = varFuncTy.getDomain();
            Ty range = varFuncTy.getRange();
            Type domType = getType(var, dom);
            Type rangeType = getType(var, range);
            FunctionType funcType = new FunctionType(domType, rangeType);
            return funcType;
        }
        else if (varTy instanceof NameTy) {
            NameTy varNameTy = (NameTy) varTy;
            TypeHolder typeHold = curr.getTypeHolder();
            TypeID typeID = new TypeID(varNameTy.getName().getSymbol());
            Type type = typeHold.searchForType(typeID);
            return type;
        }
        else if (varTy instanceof TupleTy) {
            List<Ty> list = ((TupleTy) varTy).getFields();
            Iterator<Ty> it = list.iterator();
            List<FieldItem> typeList = new List<FieldItem>();
            while (it.hasNext()) {
                Ty tmp = it.next();
                Type tmpType = getType(var, tmp);
                typeList.add(new FieldItem(getName(tmp), tmpType));
            }
            TupleType tupType = new TupleType(typeList);
            return tupType;
        }

        else
            return null;
    }

    private TypeEntry getTypeEntry(PosSymbol qual, PosSymbol name) {
        Symbol qualSym = null;
        if (qual != null) {
            qualSym = qual.getSymbol();
        }
        return getTypeEntry(qualSym, name.getSymbol());
    }

    private TypeEntry getTypeEntry(Symbol qual, Symbol name) {
        //   ModuleScope modScope = null;
        TypeEntry typeEntry = null;
        TypeLocator typeLoc = null;
        TypeID typeId = new TypeID(qual, name, 0);
        if (table.getCurrentScope() instanceof OperationScope) {
            try {
                typeLoc =
                        new TypeLocator(table.getModuleScope(),
                                myInstanceEnvironment);
                typeEntry = typeLoc.locateProgramType(typeId);
            }
            catch (SymbolSearchException ex) {
                //System.out.println("have SymbolSearchException");
            }
            ;
        }
        else if (table.getCurrentScope() instanceof TypeScope) {
            try {
                typeLoc =
                        new TypeLocator(table.getModuleScope(),
                                myInstanceEnvironment);
                typeEntry = typeLoc.locateProgramType(typeId);
            }
            catch (SymbolSearchException ex) {
                //System.out.println("have SymbolSearchException");
            }
            ;
        }
        else {
            try {
                typeLoc =
                        new TypeLocator(table.getCurrentScope(),
                                myInstanceEnvironment);
                typeEntry = typeLoc.locateProgramType(typeId);
            }
            catch (SymbolSearchException ex) {
                //System.out.println("have SymbolSearchException");
            }
            ;
        }
        return typeEntry;
    }

    private Type getTypeFromTy(Ty ty) {
        TypeConverter converter =
                new TypeConverter(myInstanceEnvironment
                        .getSymbolTable(getCurrentModuleID()));

        Type result = converter.getMathType(ty);

        if (result == null) {
            throw new UnsupportedOperationException("No type for: " + ty);
        }

        return result;
    }

    private ModuleScope getTypeModuleScope(Symbol qual, Symbol name) {
        ModuleScope modScope = null;
        TypeEntry typeEntry = null;
        TypeLocator typeLoc = null;
        TypeID typeId = new TypeID(qual, name, 0);
        if (table.getCurrentScope() instanceof OperationScope) {
            try {
                typeLoc =
                        new TypeLocator(table.getModuleScope(),
                                myInstanceEnvironment);
                typeEntry = typeLoc.locateProgramType(typeId);
            }
            catch (SymbolSearchException ex) {
                //System.out.println("have SymbolSearchException");
            }
            ;
        }
        else if (table.getCurrentScope() instanceof TypeScope) {
            try {
                typeLoc =
                        new TypeLocator(table.getModuleScope(),
                                myInstanceEnvironment);
                typeEntry = typeLoc.locateProgramType(typeId);
            }
            catch (SymbolSearchException ex) {
                //System.out.println("have SymbolSearchException");
            }
            ;
        }
        else {
            try {
                typeLoc =
                        new TypeLocator(table.getCurrentScope(),
                                myInstanceEnvironment);
                typeEntry = typeLoc.locateProgramType(typeId);
            }
            catch (SymbolSearchException ex) {
                //System.out.println("have SymbolSearchException");
            }
            ;
        }
        if (typeEntry != null) {
            modScope = (ModuleScope) typeEntry.getScope();
        }
        return modScope;
    }

    private List<String> getTypeParms(ModuleID cid) {
        List<String> typeParms = new List<String>();
        ConceptModuleDec cDec =
                (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);
        List<ModuleParameter> mpList = cDec.getParameters();
        Iterator<ModuleParameter> mpIt = mpList.iterator();
        ModuleParameter mp = null;
        while (mpIt.hasNext()) {
            mp = mpIt.next();
            if (mp instanceof ConceptTypeParamDec) {
                typeParms.addUnique(((ConceptTypeParamDec) mp).getName()
                        .toString());
            }
        }
        return typeParms;
    }

    // Get the PosSymbol associated with the VariableExp left 
    private PosSymbol getVarName(VariableExp left) {
        PosSymbol name;
        if (left instanceof VariableNameExp) {
            name = ((VariableNameExp) left).getName();
        }
        else if (left instanceof VariableDotExp) {
            VariableRecordExp varRecExp =
                    (VariableRecordExp) ((VariableDotExp) left)
                            .getSemanticExp();
            name = varRecExp.getName();
        }
        else if (left instanceof VariableRecordExp) {
            VariableRecordExp varRecExp = (VariableRecordExp) left;
            name = varRecExp.getName();
        }
        else if (left instanceof VariableArrayExp) {
            name = ((VariableArrayExp) left).getName();
        }
        else {
            name = createPosSymbol("false");
        }
        return name;
    }

    // Get the string name associated with the VariableExp left 
    private String getVarNameStr(VariableExp left) {
        String name;
        if (left instanceof VariableNameExp) {
            name = ((VariableNameExp) left).getName().toString();
        }
        else if (left instanceof VariableDotExp) {
            VariableRecordExp varRecExp =
                    (VariableRecordExp) ((VariableDotExp) left)
                            .getSemanticExp();
            name = varRecExp.toString();
        }
        else if (left instanceof VariableRecordExp) {
            VariableRecordExp varRecExp = (VariableRecordExp) left;
            name = varRecExp.getName().toString();
        }
        else if (left instanceof VariableArrayExp) {
            name = ((VariableArrayExp) left).getName().toString();
        }
        else {
            name = new String("false");
        }
        return name;
    }

    private Type getVarType(VarDec varDec) {
        NameTy nameTy = (NameTy) varDec.getTy();
        Binding binding = table.getModuleScope().getBinding();
        Type type = binding.getType(nameTy.getQualifier(), nameTy.getName());
        return type;
    }

    private Exp getWhileRuleAssume(WhileStmt stmt, AssertiveCode assertion) {
        InfixExp assume = new InfixExp();
        Exp inv = getTrueVarExp();
        if (stmt.getMaintaining() != null) {
            inv = (Exp) stmt.getMaintaining().clone();
        }

        if (inv.getLocation() != null) {
            Location loc = (Location) inv.getLocation().clone();
            loc.setDetails("Invariant");
            setLocation(inv, loc);
        }
        assume.setLeft(inv);

        EqualsExp PExp = new EqualsExp();
        PExp.setType(BooleanType.INSTANCE);
        VarExp var = new VarExp();
        if (stmt.getDecreasing() == null)
            return inv;

        ConcType pval = getPVAL();
        pval = NQV(assertion.getFinalConfirm(), pval, assertion);
        var.setName(pval.getName());
        var.setType(pval.getType());

        PExp.setOperator(EqualsExp.EQUAL);
        PExp.setType(BooleanType.INSTANCE);

        PExp.setLeft((Exp) var.clone());
        PExp.setRight((Exp) (stmt.getDecreasing()).clone());
        assume.setOpName(createPosSymbol("and"));
        assume.setType(BooleanType.INSTANCE);

        PExp.setLocation((Location) stmt.getDecreasing().getLocation().clone());

        if (PExp.getLocation() != null) {
            PExp.getLocation().setDetails("Progress Metric for While Loop");
        }
        assume.setRight(PExp);

        return assume;
    }

    // ===========================================================
    // Public Methods - Non-declarative Constructs
    // ===========================================================

    private Exp getWhileRuleConfirm(WhileStmt stmt, AssertiveCode assertion) {
        InfixExp assume = new InfixExp();
        Exp inv = getTrueVarExp();
        if (stmt.getMaintaining() != null) {
            inv = (Exp) stmt.getMaintaining().clone();

            if (inv.getLocation() != null) {
                Location loc = (Location) inv.getLocation().clone();
                Dec myDec = getCurrentProcedure();
                String details = "";
                if (myDec != null) {
                    details = " in Procedure " + myDec.getName();
                }
                if (loc != null)
                    loc
                            .setDetails("Inductive Case of Invariant of While Statement"
                                    + details);
                setLocation(inv, loc);
            }
        }

        assume.setLeft(inv);
        VarExp var = new VarExp();
        ConcType pval = getPVAL();
        pval = NQV(assertion.getFinalConfirm(), pval, assertion);
        assertion.addFreeVar(pval);

        var.setName(pval.getName());
        var.setType(pval.getType());
        InfixExp PExp = new InfixExp();
        PExp.setLeft((Exp) stmt.getDecreasing().clone());

        PExp.setRight((Exp) var.clone());
        PExp.setOpName(createPosSymbol("<"));
        PExp.setType(BooleanType.INSTANCE);
        assume.setOpName(createPosSymbol("and"));
        assume.setType(BooleanType.INSTANCE);
        if (stmt.getDecreasing().getLocation() != null) {
            Location loc =
                    (Location) stmt.getDecreasing().getLocation().clone();
            Dec myDec = getCurrentProcedure();
            String details = "";
            if (myDec != null) {
                details = " in Procedure " + myDec.getName();
            }
            loc.setDetails("Termination of While Statement" + details);
            setLocation(PExp, loc);
        }

        assume.setRight(PExp);

        return assume;
    }

    private boolean inSimpleForm(Exp ensures, List<ParameterVarDec> list) {

        boolean simple = false;
        if (ensures instanceof EqualsExp) {
            if (inSimpleForm(((EqualsExp) ensures).getLeft(), list)
                    || inSimpleForm(((EqualsExp) ensures).getRight(), list))
                simple = true;
        }
        else if (ensures instanceof InfixExp) {
            if (((InfixExp) ensures).getOpName().equals("and")) {
                if (inSimpleForm(((InfixExp) ensures).getLeft(), list)
                        && inSimpleForm(((InfixExp) ensures).getRight(), list))
                    simple = true;
            }
        }
        else if (ensures instanceof VarExp) {
            Iterator<ParameterVarDec> i = list.iterator();
            while (i.hasNext()) {
                if (i.next().getName().toString().equals(
                        ((VarExp) ensures).getName().toString()))
                    simple = true;
            }
        }

        return simple;
    }

    private boolean inSimpleForm(Exp ensures, VarDec tmpVD) {

        boolean simple = false;
        if (ensures instanceof EqualsExp) {
            if (inSimpleForm(((EqualsExp) ensures).getLeft(), tmpVD)
                    || inSimpleForm(((EqualsExp) ensures).getRight(), tmpVD))
                simple = true;
        }
        else if (ensures instanceof InfixExp) {
            if (((InfixExp) ensures).getOpName().equals("and")) {
                if (inSimpleForm(((InfixExp) ensures).getLeft(), tmpVD)
                        && inSimpleForm(((InfixExp) ensures).getRight(), tmpVD))
                    simple = true;
            }
        }
        else if (ensures instanceof VarExp) {
            if (tmpVD.getName().toString().equals(
                    ((VarExp) ensures).getName().toString()))
                simple = true;

        }

        return simple;
    }

    private Exp invk_cond(Exp test, AssertiveCode assertion) {

        if (test instanceof ProgramParamExp) {
            return invk_cond(((ProgramParamExp) test).getSemanticExp(),
                    assertion);
        }
        else if (test instanceof ProgramFunctionExp) {
            Exp requires =
                    getRequiresForProgFuncExp(((ProgramFunctionExp) test),
                            assertion);

            List<ProgramExp> exps = ((ProgramFunctionExp) test).getArguments();
            Exp req = null;
            for (int i = 0; i < exps.size(); i++) {
                Exp tmp = invk_cond((Exp) exps.get(i).clone(), assertion);
                if (tmp != null && !isTrueExp(tmp))
                    if (req == null)
                        req = tmp;
                    else
                        req = InfixExp.formAndStmt(tmp, req);
            }

            if (req != null)
                requires = InfixExp.formAndStmt(requires, req);

            return requires;
        }
        else if (test instanceof ProgramOpExp) {
            ProgramOpExp exp = (ProgramOpExp) test;
            PosSymbol name = getProgramOpName(exp);
            if (name.toString().toLowerCase().equals("and")) {
                Exp exp1 = invk_cond(exp.getFirst(), assertion);
                Exp exp2 = invk_cond(exp.getSecond(), assertion);
                return InfixExp.formAndStmt(exp1, exp2);
            }
            else {
                ProgramFunctionExp tmp = corProgramFuncExp((ProgramOpExp) test);
                return getRequiresForProgFuncExp(((ProgramFunctionExp) tmp),
                        assertion);
            }
        }
        else if (test instanceof ProgramDotExp) {
            return invk_cond(((ProgramDotExp) test).getSemanticExp(), assertion);
        }

        return getTrueVarExp();
    }

    private boolean isConcParm(String name) {
        if (concParms == null) {
            return false;
        }
        return concParms.contains(name);
    }

    private boolean isCurrentConceptModuleDec(ConceptModuleDec dec) {
        ModuleDec myMDec = getTheCurrentModuleDec();
        if (myMDec instanceof ConceptModuleDec) {
            if (myMDec.getName().equals(dec.getName())) {
                return true;
            }
        }
        ModuleDec myMBdec = getCurrentBodyModuleDec();
        if (myMBdec instanceof ConceptBodyModuleDec) {
            ModuleID modID =
                    ModuleID.createConceptID(((ConceptBodyModuleDec) myMBdec)
                            .getConceptName());
            ConceptModuleDec moduleDec =
                    (ConceptModuleDec) myInstanceEnvironment
                            .getModuleDec(modID);
            if (myMBdec.getName().equals(dec.getName())) {
                return true;
            }
        }
        return false;

    }

    public boolean isLocalType(RepresentationDec dec) {
        //re-do;
        //essentially copied and added the following code from 
        //addTypeFamilyVariablesToScope from Populator.java
        boolean result = true;
        Symbol tsym = dec.getName().getSymbol();
        Iterator<ModuleID> i = table.getModuleScope().getSpecIterator();
        while (i.hasNext()) {
            ModuleScope scope = myInstanceEnvironment.getModuleScope(i.next());
            if (scope.containsLocalConcType(tsym)) {
                result = false;
            }
        }
        return result;
    }

    private boolean isParmOp(String name) {
        if (parmOpList == null) {
            return false;
        }
        return parmOpList.contains(name);
    }

    /*
     * returns True if the exp is the 'True' Exp
     */
    private boolean isTrueExp(Exp exp) {
        if (exp instanceof VarExp) {
            if (((VarExp) exp).getName().toString().equals(
                    getTrueVarExp().getName().toString())) {
                ;
                return true;
            }
        }
        return false;
    }

    private boolean isTypeParm(String name) {
        if (typeParms == null) {
            return false;
        }
        return typeParms.contains(name);
    }

    // Is name a verificatoin variable (PVAL, starts with ,?, etc..)
    private boolean isVerificationVar(Exp name) {
        if (name instanceof VarExp) {
            String strName = ((VarExp) name).getName().toString();
            char strChar = '?';

            if (strName.charAt(0) == (strChar)) {
                return true;
            }
            if (strName.equals(getPVAL())) {
                return true;
            }
        }
        else if (name instanceof DotExp) {
            List<Exp> names = ((DotExp) name).getSegments();
            return isVerificationVar(names.get(0));
        }
        return false;
    }

    private Exp modifyConfirmForClears(Exp init, Exp ensures) {
        InfixExp newEnsures;

        if (init instanceof EqualsExp) {
            if (ensures != null) {
                newEnsures = new InfixExp();
                newEnsures.setOpName(createPosSymbol("and"));
                newEnsures.setType(BooleanType.INSTANCE);
                newEnsures.setLeft(ensures);
                newEnsures.setRight(init);
                return newEnsures;
            }
            else {
                return init;
            }
        }
        else if (init instanceof DotExp) {
            if (ensures != null) {
                newEnsures = new InfixExp();
                newEnsures.setOpName(createPosSymbol("and"));
                newEnsures.setType(BooleanType.INSTANCE);
                newEnsures.setLeft(ensures);
                newEnsures.setRight(init);
                return newEnsures;
            }
            else {
                return init;
            }
        }
        else {
            if (ensures != null) {
                newEnsures = new InfixExp();
                newEnsures.setOpName(createPosSymbol("and"));
                newEnsures.setType(BooleanType.INSTANCE);
                newEnsures.setLeft(ensures);
                newEnsures.setRight(init);
                return newEnsures;
            }
            else {
                return init;
            }
        }
    }

    private Exp modifyConfirmForRestores(ParameterVarDec varDec, Exp ensures) {
        InfixExp newConf = new InfixExp();
        EqualsExp eExp = new EqualsExp();
        eExp.setType(BooleanType.INSTANCE);
        VarExp newExp = new VarExp();
        VarExp oldExp = new VarExp();
        OldExp oExp = new OldExp();
        String str = varDec.getName().toString();
        Symbol name = Symbol.symbol(str);
        Symbol newName = Symbol.symbol(str);
        PosSymbol newPS = new PosSymbol();
        newPS.setSymbol(name);
        PosSymbol oldPS = new PosSymbol();
        oldPS.setSymbol(newName);
        newExp.setName(newPS);
        oldExp.setName(oldPS);

        Type expType = getTypeFromTy(varDec.getTy());

        oldExp.setType(expType);

        newExp.setType(expType);

        oExp.setExp(oldExp);
        oExp.setType(expType);

        eExp.setOperator(EqualsExp.EQUAL);
        eExp.setRight(newExp);
        eExp.setLeft(oExp);
        eExp.setType(BooleanType.INSTANCE);

        if (ensures != null) {
            newConf.setLeft(ensures);
            PosSymbol opName = new PosSymbol();
            opName.setSymbol(Symbol.symbol("and"));
            newConf.setOpName(opName);
            newConf.setRight(eExp);
            newConf.setType(BooleanType.INSTANCE);

            return newConf;
        }
        else {
            return eExp;
        }

    }

    private Exp modifyEnsuresByParameters(OperationDec curOperation,
            AssertiveCode assertion, Exp ensures) {

        Iterator<ParameterVarDec> paramIter =
                curOperation.getParameters().iterator();
        while (paramIter.hasNext()) {
            ParameterVarDec tmpPVD = paramIter.next();
            if (tmpPVD.getMode() == Mode.EVALUATES) {
                VarExp exp = new VarExp();
                exp.setName(tmpPVD.getName());
                ensures = replace(ensures, exp, new OldExp(null, exp));
            }
            else if (tmpPVD.getMode() == Mode.CLEARS) {
                VarDec var = new VarDec();
                var.setName(tmpPVD.getName());
                var.setTy(tmpPVD.getTy());
                Exp init = getInitialExp(var);
                ensures = modifyConfirmForClears(init, ensures);
            }
        }
        return ensures;
    }

    private Exp modifyEnsuresForParameterModes(Exp ensures, OperationDec opDec) {

        List<ParameterVarDec> lst = opDec.getParameters();
        Iterator<ParameterVarDec> i = lst.iterator();

        while (i.hasNext()) {
            ParameterVarDec tmp = i.next();

            if (tmp.getMode() == Mode.CLEARS) {

            }
            else if (tmp.getMode() == Mode.PRESERVES) {

            }
            else if (tmp.getMode() == Mode.RESTORES) {
                ensures = modifyConfirmForRestores(tmp, ensures);

            }
        }

        return ensures;

    }

    private Exp modifyEnsuresForParameterModes(Exp ensures, OperationDec opDec,
            CallStmt stmt) {

        List<ParameterVarDec> lst = opDec.getParameters();
        Iterator<ParameterVarDec> i = lst.iterator();
        List<ProgramExp> list = stmt.getArguments();
        Iterator<ProgramExp> argIt = list.iterator();
        while (i.hasNext() && argIt.hasNext()) {
            ParameterVarDec tmp = i.next();
            ProgramExp tmpArg = argIt.next();
            if (tmp.getMode() == Mode.CLEARS) {
                VarDec tmpVD = new VarDec();

                if (tmpArg instanceof VariableNameExp) { //redo this
                    tmpVD.setTy(tmp.getTy());
                    PosSymbol t = createPosSymbol(tmp.getName().toString());
                    tmpVD.setName(t);
                    Exp init = getInitialExp(tmpVD);
                    if (isTrueExp(ensures))
                        ensures = init;
                    else
                        ensures = InfixExp.formAndStmt(init, ensures);
                }
                else if (tmpArg instanceof ProgramDotExp) { //redo this
                    tmpVD.setTy(tmp.getTy());
                    PosSymbol t = createPosSymbol(tmp.getName().toString());
                    tmpVD.setName(t);
                    Exp init = (Exp) getInitialExp(tmpVD).clone();
                    if (isTrueExp(ensures))
                        ensures = init;
                    else
                        ensures = InfixExp.formAndStmt(init, ensures);
                }
                else if (tmpArg instanceof VariableDotExp) { //redo this
                    tmpVD.setTy(tmp.getTy());
                    PosSymbol t = createPosSymbol(tmp.getName().toString());
                    tmpVD.setName(t);
                    Exp init = (Exp) getInitialExp(tmpVD).clone();
                    if (isTrueExp(ensures))
                        ensures = init;
                    else
                        ensures = InfixExp.formAndStmt(init, ensures);
                }

            }
            else if (tmp.getMode() == Mode.PRESERVES) {

            }
        }

        return ensures;

    }

    /* private FacilityDec getFacility(NameType nameType) {
         return getFacility(null, nameType.getName().getSymbol());
     }

     private FacilityDec getFacility(PosSymbol qual, PosSymbol name) {
         Symbol qualSym = null;
         if (qual != null) {
             qualSym = qual.getSymbol();
         }
          return getFacility(qualSym, name.getSymbol());
     }

     private FacilityDec getFacility(Symbol qual, Symbol name) {
         FacilityDec fDec = null;
         ModuleScope modScope = getTypeModuleScope(qual, name);
         if (modScope != null) {
             fDec = modScope.getFacilityDec();
         }
         return fDec;
     }*/

    // Modfies the Ensures Statements based on Restores and Clears parameters
    private Exp modifyEnsuresForProcedureDecRule(OperationDec curOperation,
            ProcedureDec dec) {
        Exp ensures = curOperation.getEnsures();
        if (ensures != null) {
            ensures = (Exp) ensures.clone();
        }

        Exp tmpEnsures = modifyEnsuresIfQuantified(ensures, dec);
        if (tmpEnsures != null)
            ensures = tmpEnsures;

        Iterator<ParameterVarDec> k = curOperation.getParameters().iterator();
        while (k.hasNext()) {
            ParameterVarDec varDec = k.next();

            if (varDec.getMode() == Mode.RESTORES) {
                ensures = modifyConfirmForRestores(varDec, ensures);
            }
            else if (varDec.getMode() == Mode.PRESERVES) {
                ensures = modifyConfirmForRestores(varDec, ensures);
            }

        }

        if (curOperation.getStateVars() != null) {
            Iterator<AffectsItem> iter = curOperation.getStateVars().iterator();
            ModuleDec sourceModule = (ModuleDec) getCurrentModuleDec();
            while (iter.hasNext()) {
                AffectsItem tmp = iter.next();
                ParameterVarDec tVD = new ParameterVarDec();
                tVD.setName(tmp.getName());
                tVD.setTy(getConceptualVariableTy(tmp.getName().getName()));

                if (tmp.getMode() == Mode.RESTORES) {
                    ensures = modifyConfirmForRestores(tVD, ensures);
                }
                else if (tmp.getMode() == Mode.PRESERVES) {
                    ensures = modifyConfirmForRestores(tVD, ensures);
                }
            }
        }

        return ensures;
    }

    // Modifes Ensures of the called Operation and Removes quantification and adds as free variable(s)
    private Exp modifyEnsuresIfCallingQuantified(Exp ensures, OperationDec dec,
            AssertiveCode assertion, Exp curEnsures) {
        if (ensures instanceof QuantExp /*&& (((QuantExp)ensures).getOperator() == QuantExp.FORALL)*/) {

            Exp tEns =
                    modifyEnsuresIfCallingQuantified(((QuantExp) ensures)
                            .getBody(), dec, assertion, ((QuantExp) ensures)
                            .getBody());

            if (((QuantExp) ensures).getVars() != null) {
                Iterator<MathVarDec> i =
                        ((QuantExp) ensures).getVars().iterator();
                while (i.hasNext()) {
                    MathVarDec tmpMVD = i.next();
                    ConcType tmp = getFreeVar(tmpMVD);
                    VarExp old = new VarExp();
                    old.setName(tmp.getName());
                    VarExp repl = new VarExp();
                    ConcType replCT =
                            NQV(InfixExp.formAndStmt(curEnsures, assertion
                                    .getFinalConfirm()), tmp, assertion);
                    assertion.addFreeVar(replCT);
                    tmpMVD.setName(replCT.getName());
                    repl.setName(replCT.getName());
                    tEns = tEns.replace(old, repl);

                }
            }
            if ((((QuantExp) ensures).getOperator() == QuantExp.FORALL)) {
                return tEns;
            }
            else {
                QuantExp tmpQE = (QuantExp) ensures.clone();
                tmpQE.setBody(tEns);

                return tmpQE;
            }
        }
        else if (ensures instanceof InfixExp) {
            if (((InfixExp) ensures).getLeft() != null) {
                Exp tmp =
                        modifyEnsuresIfCallingQuantified(((InfixExp) ensures)
                                .getLeft(), dec, assertion, ensures);
                if (tmp != null)
                    ((InfixExp) ensures).setLeft(tmp);
            }
            if (((InfixExp) ensures).getRight() != null) {
                Exp tmp =
                        modifyEnsuresIfCallingQuantified(((InfixExp) ensures)
                                .getRight(), dec, assertion, ensures);
                if (tmp != null)
                    ((InfixExp) ensures).setRight(tmp);
            }
        }
        return ensures;
    }

    // Removes quantification if Aux Variables Exist
    private Exp modifyEnsuresIfQuantified(Exp ensures, ProcedureDec dec) {
        if (ensures instanceof QuantExp) {
            List<AuxVarDec> lst = dec.getAuxVariables();

            Iterator<MathVarDec> quantVarsIt =
                    ((QuantExp) ensures).getVars().iterator();
            while (quantVarsIt.hasNext()) {
                Iterator<AuxVarDec> i = lst.iterator();
                MathVarDec tMVD = quantVarsIt.next();
                boolean found = false;
                while (i.hasNext()) {
                    AuxVarDec tmp = i.next();
                    if (same_Type(tMVD, tmp))
                        found = true;
                }
                if (found == false)
                    return null;
            }

            Exp tEns =
                    modifyEnsuresIfQuantified(((QuantExp) ensures).getBody(),
                            dec);

            if (tEns == null) {
                return ((QuantExp) ensures).getBody();
            }
            else {
                return (tEns);
            }
        }
        else if (ensures instanceof InfixExp) {
            if (((InfixExp) ensures).getLeft() != null) {
                Exp tmp =
                        modifyEnsuresIfQuantified(((InfixExp) ensures)
                                .getLeft(), dec);
                if (tmp != null)
                    ((InfixExp) ensures).setLeft(tmp);
            }
            if (((InfixExp) ensures).getRight() != null) {
                Exp tmp =
                        modifyEnsuresIfQuantified(((InfixExp) ensures)
                                .getRight(), dec);
                if (tmp != null)
                    ((InfixExp) ensures).setRight(tmp);
            }
            return ensures;
        }
        return null;
    }

    // This needs to be ReWritten
    private Exp modifyRequiresByGlobalMode(Exp requires,
            OperationDec curOperation, AssertiveCode assertion) {
        if (curOperation.getStateVars() == null)
            return requires;
        Iterator<AffectsItem> iter = curOperation.getStateVars().iterator();
        while (iter.hasNext()) {
            AffectsItem tmp = iter.next();
            //   	ConcType tmpCT = assertion.getFreeVar(tmp.getName());

            if (tmp != null) {

                /*	Exp init = getInitialExp(tmpVD); */
                //	if(tmp.getMode() == Mode.REPLACES && init != null){
                /*	if(requires != null){	
                		InfixExp tmp = new InfixExp();
                		tmp.setOpName(createPosSymbol("and"));
                		tmp.setLeft(init);
                		tmp.setRight(requires);
                		requires = tmp;
                	}
                	else
                		requires = init;
                 */
            }
            //	else{
            /*	if(requires != null && constr != null){	    			
            		InfixExp tmp = new InfixExp();
            		tmp.setOpName(createPosSymbol("and"));
            		tmp.setLeft(constr);
            		tmp.setRight(requires);
            		requires = tmp;
            	}
            	else
            		if(constr != null)
            			requires = constr;
             */
            //	}        		
        }
        return requires;
    }

    // Modfies Requires Statement based on Replaces parameter and adds parameters to free variables
    private Exp modifyRequiresByParameters(OperationDec curOperation,
            AssertiveCode assertion) {
        Exp requires = curOperation.getRequires();
        if (requires != null && requires.getLocation() != null) {
            Location loc = (Location) (requires.getLocation()).clone();
            loc.setDetails("Requirement for " + curOperation.getName());
            setLocation(requires, loc);
        }

        Exp ensures = curOperation.getEnsures();

        Iterator<ParameterVarDec> paramIter =
                curOperation.getParameters().iterator();
        while (paramIter.hasNext()) {
            ParameterVarDec tmpPVD = paramIter.next();
            VarDec tmpVD = toVarDec(tmpPVD);

            if (tmpVD != null) {
                Exp constr = getConstraints(tmpVD);
                Exp init = getInitialExp(tmpVD);
                if (tmpPVD.getMode() == Mode.REPLACES && init != null) {
                    if (curOperation.getRequires() != null) {
                        init.setLocation((Location) curOperation.getRequires()
                                .getLocation().clone());
                        init.getLocation().setDetails(
                                "Assumption from Replaces Parameter Mode");
                    }
                    if (requires != null) {
                        requires = InfixExp.formAndStmt(init, requires);
                    }
                    else {
                        requires = init;
                    }

                }
                else {
                    if (requires != null && constr != null
                            && !isTrueExp(constr) && !isTrueExp(requires)) {
                        requires = InfixExp.formAndStmt(constr, requires);
                    }
                    else if (constr != null && !isTrueExp(constr)) {
                        requires = constr;
                    }
                }
                if (tmpPVD.getMode() == Mode.EVALUATES) {
                    VarExp exp = new VarExp();
                    exp.setName(tmpPVD.getName());
                    ensures = replace(ensures, exp, new OldExp(null, exp));
                }
                addFreeVar(tmpPVD, assertion);
            }
        }

        return requires;
    }

    private Exp negateExp(Exp exp) {
        if (exp instanceof EqualsExp) {
            if (((EqualsExp) exp).getOperator() == EqualsExp.EQUAL)
                ((EqualsExp) exp).setOperator(EqualsExp.NOT_EQUAL);
            else
                ((EqualsExp) exp).setOperator(EqualsExp.EQUAL);
            return exp;
        }
        else if (exp instanceof PrefixExp) {
            if (((PrefixExp) exp).getSymbol().getName().toString()
                    .equals("not")) {
                exp = ((PrefixExp) exp).getArgument();
            }
            return exp;
        }
        else {
            PrefixExp tmp = new PrefixExp();
            setLocation(tmp, exp.getLocation());
            tmp.setArgument(exp);
            tmp.setSymbol(createPosSymbol("not"));
            tmp.setType(BooleanType.INSTANCE);
            return tmp;
        }
    }

    private ConcType NQV(Exp exp, ConcType oldVar) {
        return NQV(exp, oldVar, null);
    }

    private ConcType NQV(Exp exp, ConcType oldVar, AssertiveCode assertion) {

        String var = oldVar.getName().toString();
        String quesVar = "?" + var;
        ConcType newOldVar =
                new ConcType(oldVar.getModuleID(), createPosSymbol(quesVar),
                        oldVar.getType());

        if (exp.containsVar(oldVar.getName().toString(), false)) {

            return NQV(exp, newOldVar, assertion);
        }
        else if (exp.containsVar(newOldVar.getName().toString(), false)) {
            return NQV(exp, newOldVar, assertion);
        }
        else {
            if (var.charAt(0) != '?') {
                /* Need to add this new variable to the list of assertions - YS
                 *
                if(assertion != null){
                VarDec newVarDec = new VarDec();
                VarDec dec = assertion.getVariableDec(oldVar.getName().toString());
                if(dec != null){
                	newVarDec.setName(createPosSymbol(quesVar));
                	newVarDec.setTy(dec.getTy());
                
                	assertion.insertVariableDec(newVarDec);
                }
                }*/

                return newOldVar;
            }
        }

        return oldVar;
    }

    private void outputAsFile(String fileName, String fileContents) {
        try {
            FileWriter outFile = new FileWriter(getMainFileName());
            outFile.write("");
            outFile.append(fileContents);
            outFile.flush();

        }
        catch (IOException ex) {
            System.err.println("File I/O error when writing: " + fileName);
        }

    }

    public void outputAsrt() {
        Boolean WebOut =
                myInstanceEnvironment.flags
                        .isFlagSet(ResolveCompiler.FLAG_XML_OUT);
        String fileContents = getMainBuffer();
        String fileName = getMainFileName();
        if (fileContents != null && fileContents.length() > 0) {
            if (WebOut) {
                outputToReport(fileContents);
            }
            else {
                outputAsFile(fileName, fileContents);
            }
        }
    }

    private void outputToReport(String fileContents) {
        CompileReport report = myInstanceEnvironment.getCompileReport();
        StringBuffer outBuffer = new StringBuffer();
        outBuffer.append("<vcFile>{\"vcs\":[");
        outBuffer.append(fileContents.substring(0, fileContents.length() - 3));
        outBuffer.append("]}</vcFile>");
        report.setVcSuccess();
        report.setOutput(outBuffer.toString());
    }

    private Statement processIterateExitStmt(IterateStmt stmt,
            AssertiveCode assertion, Iterator<Statement> i, Statement tmp) {

        IterateExitStmt whenStmt = (IterateExitStmt) tmp;
        IfStmt if_stmt = new IfStmt();
        if_stmt.setTest(whenStmt.getTest());
        List<Statement> lst = whenStmt.getStatements();
        lst.add(new ConfirmStmt(null, assertion.getFinalConfirm()));
        if_stmt.setThenclause(lst);

        List<Statement> lst2 = new List<Statement>();

        while (i.hasNext()) {
            tmp = i.next();
            if (!(tmp instanceof IterateExitStmt)) {
                lst2.add(tmp);
            }
            else {
                lst2.add(processIterateExitStmt(stmt, assertion, i, tmp));
                if_stmt.setElseclause(lst2);

                return if_stmt;
            }
        }

        lst2.add(new ConfirmStmt(null, getIterateRuleConfirm(stmt, assertion)));
        if_stmt.setElseclause(lst2);
        return if_stmt;
    }

    // replace in exp, any instance of old with repl
    private Exp replace(Exp exp, Exp old, Exp repl) {

        Exp tmp = exp.replace((Exp) old.clone(), (Exp) repl.clone());
        if (tmp != null)
            return tmp;
        else
            return exp;
    }

    private Exp replaceAssumeRule(AssumeStmt stmt, Exp conf,
            AssertiveCode assertion) {
        Exp exp = stmt.getAssertion();
        conf = (Exp) conf.clone();
        boolean keepAssumption = false;
        if (exp instanceof EqualsExp
                && ((EqualsExp) exp).getOperator() == EqualsExp.EQUAL) {
            Exp tmp = null;
            boolean verif = false;
            if (((EqualsExp) exp).getOperator() == EqualsExp.EQUAL) {
                if (isVerificationVar(((EqualsExp) exp).getLeft())) {
                    verif = true;
                }
                if (((EqualsExp) exp).getLeft() instanceof VarExp) {
                    ConcType type =
                            getIfInFreeVarList(((VarExp) ((EqualsExp) exp)
                                    .getLeft()).getName().toString().replace(
                                    "?", ""), assertion);
                    if (type != null && type.getType() instanceof FunctionType) {
                        type = type;
                        keepAssumption = true;
                    }
                }
                tmp =
                        replace(conf, ((EqualsExp) exp).getLeft(),
                                ((EqualsExp) exp).getRight());
                Exp var = ((EqualsExp) exp).getLeft();
                if (var instanceof VarExp
                        && tmp.containsVar(((VarExp) var).getName().getName(),
                                false)) {
                    keepAssumption = true;
                }
            }
            else {
                if (isVerificationVar(((EqualsExp) exp).getLeft())) {
                    verif = true;
                }
                tmp =
                        replace(conf, ((EqualsExp) exp).getLeft(),
                                negateExp(((EqualsExp) exp).getRight()));
                Exp var = ((EqualsExp) exp).getLeft();
                if (var instanceof VarExp
                        && tmp.containsVar(((VarExp) var).getName().getName(),
                                false)) {
                    keepAssumption = true;
                }
            }

            if (tmp == null) {
                if (((EqualsExp) exp).getOperator() == EqualsExp.EQUAL) {
                    if (isVerificationVar(((EqualsExp) exp).getRight())) {
                        verif = true;
                    }
                    tmp =
                            replace(conf, ((EqualsExp) exp).getRight(),
                                    ((EqualsExp) exp).getLeft());

                    Exp var = ((EqualsExp) exp).getRight();
                    if (var instanceof VarExp
                            && tmp.containsVar(((VarExp) var).getName()
                                    .getName(), false)) {
                        keepAssumption = true;
                    }
                }
                else {
                    if (isVerificationVar(((EqualsExp) exp).getRight())) {
                        verif = true;
                    }
                    tmp =
                            replace(conf, ((EqualsExp) exp).getRight(),
                                    negateExp(((EqualsExp) exp).getLeft()));
                    Exp var = ((EqualsExp) exp).getRight();
                    if (var instanceof VarExp
                            && tmp.containsVar(((VarExp) var).getName()
                                    .getName(), false)) {
                        keepAssumption = true;
                    }
                }
            }

            if (verif && !keepAssumption) {
                exp = null;
            }

            if (tmp.asString(0, 0).equals(conf.asString(0, 0)))
                return conf;
            //	else
            //	exp = null;
            conf = tmp;

        }
        else if (exp instanceof InfixExp
                && ((InfixExp) exp).getOpName().toString().equals("and")) {
            AssumeStmt left =
                    new AssumeStmt((Exp) ((InfixExp) exp).getLeft().clone());
            AssumeStmt right =
                    new AssumeStmt((Exp) ((InfixExp) exp).getRight().clone());
            conf = replaceAssumeRule(left, conf, assertion);
            conf = replaceAssumeRule(right, conf, assertion);
            if (left.getAssertion() == null)
                if (right.getAssertion() == null)
                    exp = null;
                else
                    exp = right.getAssertion();
            else if (right.getAssertion() == null)
                exp = left.getAssertion();
            else
                exp =
                        InfixExp.formAndStmt(left.getAssertion(), right
                                .getAssertion());
        }
        stmt.setAssertion(exp);
        return conf;
    }

    private Exp replaceFacilityDeclarationVariables(Exp req, List facParam,
            List concParam) {

        for (int i = 0; i < facParam.size(); i++) {
            if (facParam.get(i) instanceof Dec
                    && (concParam.get(i) instanceof Dec)) {

                Dec facDec = (Dec) facParam.get(i);
                Dec concDec = (Dec) concParam.get(i);
                VarExp expToReplace = new VarExp();
                VarExp expToUse = new VarExp();
                expToReplace.setName(facDec.getName());
                expToUse.setName(concDec.getName());
                if (concDec instanceof ConstantParamDec) {
                    // Set Type
                }
                if (req != null) {
                    req = replace(req, expToReplace, expToUse);
                    req =
                            replace(req, new OldExp(null, expToReplace),
                                    new OldExp(null, expToUse));
                }
            }
            else if (facParam.get(i) instanceof Dec
                    && concParam.get(i) instanceof ModuleArgumentItem) {

                Dec facDec = (Dec) facParam.get(i);
                ModuleArgumentItem concItem =
                        (ModuleArgumentItem) concParam.get(i);
                VarExp expToReplace = new VarExp();
                VarExp expToUse = new VarExp();
                expToReplace.setName(facDec.getName());
                if (concItem.getName() != null) {
                    expToUse.setName(concItem.getName());
                }
                else {
                    expToUse.setName(createPosSymbol(concItem.getEvalExp()
                            .toString()));
                }
                if (req == null) {
                    req = getTrueVarExp();
                    req.setLocation(facDec.getName().getLocation());
                }
                req = replace(req, expToReplace, expToUse);
                req =
                        replace(req, new OldExp(null, expToReplace),
                                new OldExp(null, expToUse));
            }

        }

        return req;
    }

    private Exp replacePostConditionVariables(List<ProgramExp> argList,
            Exp ensures, OperationDec opDec, AssertiveCode assertion) {
        return replacePostConditionVariables(argList, ensures, opDec,
                assertion, false);
    }

    /* this totally needs to be rewritten */
    private Exp replacePostConditionVariables(List<ProgramExp> argList,
            Exp ensures, OperationDec opDec, AssertiveCode assertion,
            boolean simple) {
        if (opDec == null)
            return getTrueVarExp();

        List<ParameterVarDec> parList = new List<ParameterVarDec>();
        if (ensures == null) {
            Exp tmpEns = (Exp) ((OperationDec) opDec).getEnsures();
            if (tmpEns != null) {
                ensures = (Exp) tmpEns.clone();
            }
        }
        if (ensures == null) {
            ensures = getTrueVarExp();
        }

        parList = ((OperationDec) opDec).getParameters();

        Exp conf = assertion.getFinalConfirm();
        Iterator<ProgramExp> k = argList.iterator();
        Iterator<ParameterVarDec> j = parList.iterator();
        Iterator<AffectsItem> it =
                ((OperationDec) opDec).getStateVars().iterator();

        List<Exp> undRepList = new List<Exp>();
        List<Exp> replList = new List<Exp>();

        k = argList.iterator();
        j = parList.iterator();

        k = argList.iterator();
        j = parList.iterator();

        while ((j.hasNext() && k.hasNext()) || it.hasNext()) {
            conf = (Exp) assertion.getFinalConfirm().clone();
            if (it.hasNext()) {
                AffectsItem stateVar = it.next();
                if (stateVar.getMode() == Mode.UPDATES
                        || stateVar.getMode() == Mode.ALTERS
                        || stateVar.getMode() == Mode.REASSIGNS
                        || stateVar.getMode() == Mode.REPLACES) {

                    ConcType SV =
                            getIfInFreeVarList(stateVar.getName().toString(),
                                    assertion, true);
                    Exp sVar;
                    VarExp oldNamesVar = new VarExp();
                    if (SV != null) {

                        if (SV.getName().getName().contains(".")) {
                            sVar = new DotExp();
                            String[] array =
                                    SV.getName().getName().split("\\.");
                            String qualifier = array[0];
                            String name = array[1];
                            VarExp facName = new VarExp();
                            VarExp nameExp = new VarExp();
                            List<Exp> lst = new List();

                            nameExp.setName(createPosSymbol(name));
                            facName.setName(createPosSymbol(qualifier));

                            lst.add(facName);
                            lst.add(nameExp);
                            ((DotExp) sVar).setSegments(lst);
                        }
                        else {
                            sVar = new VarExp();
                            ((VarExp) sVar).setName(SV.getName());
                        }

                        if (!simple) {
                            //	conf = OldNQV(conf, SV, assertion);

                        }
                        oldNamesVar.setName(stateVar.getName());
                        ConcType quesSV =
                                getIfInFreeVarList(stateVar.getName()
                                        .toString(), assertion);

                        if (quesSV == null) {
                            quesSV =
                                    new ConcType(SV.getModuleID(),
                                            createPosSymbol(SV.getName()
                                                    .toString()), SV.getType());
                            quesSV =
                                    NQV(InfixExp.formAndStmt(ensures, conf),
                                            quesSV, assertion);
                            assertion.addFreeVar(quesSV);
                        }
                        else
                            quesSV =
                                    NQV(InfixExp.formAndStmt(ensures, conf),
                                            quesSV, assertion);
                        //assertion.addFreeVar(quesSV);

                        VarExp qsVar = new VarExp();
                        qsVar.setName(quesSV.getName());
                        qsVar = (VarExp) qsVar.clone();

                        OldExp osVar = new OldExp(null, (Exp) sVar.clone());
                        OldExp oldNameOSVar =
                                new OldExp(null, (Exp) oldNamesVar.clone());
                        ensures = replace(ensures, oldNamesVar, sVar);
                        ensures = replace(ensures, oldNameOSVar, osVar);

                        if (!simple) {

                            ensures = replace(ensures, sVar, qsVar);
                            ensures = replace(ensures, osVar, sVar);
                            conf = replace(conf, sVar, qsVar);
                        }
                    }
                    else {
                        sVar = new VarExp();
                        ((VarExp) sVar).setName(stateVar.getName());
                        VCBuffer.append(" WHY IS "
                                + ((VarExp) sVar).getName().toString()
                                + "not in the Free Variable List?");

                    }

                    assertion.setFinalConfirm(conf);
                }
            }
            else {
                ParameterVarDec specVar = (ParameterVarDec) j.next().clone();

                ProgramExp originalRealVar = k.next();
                ProgramExp realVar = (ProgramExp) originalRealVar.clone();

                Exp specVarExp = null, quesRep = null, replace = null, undqRep =
                        null;
                OldExp oSpecVar = null, oRealVar = null;
                String replacement = new String();
                oSpecVar = new OldExp();
                oRealVar = new OldExp();

                specVarExp = new VarExp();
                ((VarExp) specVarExp).setName(specVar.getName());

                replace = getReplacement(realVar, assertion);

                if (realVar instanceof VariableNameExp) {
                    replacement =
                            ((VariableNameExp) realVar).getName().toString();
                    String quesReplacement = "?".concat(replacement);
                    String undquesReplacement = "_?".concat(replacement);
                    //	specVarExp = new VarExp();
                    //((VarExp)specVarExp).setName(specVar.getName());
                    undqRep = new VarExp();
                    ((VarExp) undqRep)
                            .setName(createPosSymbol(undquesReplacement));
                    undqRep.setType(realVar.getType());

                    oSpecVar.setExp((Exp) specVarExp.clone());
                    quesRep = new VarExp();
                    ((VarExp) quesRep)
                            .setName(createPosSymbol(quesReplacement));
                    quesRep.setType(realVar.getType());

                    oRealVar.setExp((Exp) replace.clone());
                }
                else if (realVar instanceof ProgramIntegerExp) {
                    replacement =
                            Integer.toString(((ProgramIntegerExp) realVar)
                                    .getValue());
                    String quesReplacement = "?".concat(replacement);
                    String undquesReplacement = "_?".concat(replacement);
                    //	specVarExp = new VarExp();
                    //	((VarExp)specVarExp).setName(specVar.getName());
                    oSpecVar.setExp((Exp) specVarExp.clone());
                    quesRep = new VarExp();
                    ((VarExp) quesRep)
                            .setName(createPosSymbol(quesReplacement));
                    quesRep.setType(realVar.getType());

                    //	replace = new VarExp();
                    //	((VarExp)replace).setName(createPosSymbol(replacement));
                    Exp repl = getReplacement(realVar, assertion);
                    oRealVar.setExp((Exp) repl.clone());

                    undqRep = new VarExp();
                    ((VarExp) undqRep)
                            .setName(createPosSymbol(undquesReplacement));
                    undqRep.setType(realVar.getType());

                }
                else if (realVar instanceof ProgramDoubleExp) {

                }
                else if (realVar instanceof ProgramFunctionExp) {
                    replacement =
                            Integer.toString(((ProgramIntegerExp) realVar)
                                    .getValue());
                    String quesReplacement = "?".concat(replacement);
                    String undquesReplacement = "_?".concat(replacement);
                    // 	specVarExp = new VarExp();
                    // 	((VarExp)specVarExp).setName(specVar.getName());
                    replace = realVar;
                    oSpecVar.setExp((Exp) specVarExp.clone());
                    oRealVar.setExp((Exp) replace.clone());
                    quesRep = new VarExp();
                    ((VarExp) quesRep)
                            .setName(createPosSymbol(quesReplacement));
                    quesRep.setType(realVar.getType());

                    undqRep = new VarExp();
                    ((VarExp) undqRep)
                            .setName(createPosSymbol(undquesReplacement));
                    undqRep.setType(realVar.getType());
                }
                else if (realVar instanceof ProgramOpExp) {
                    replacement =
                            getProgramOpName(((ProgramOpExp) realVar))
                                    .toString();
                    String quesReplacement = "?".concat(replacement);
                    String undquesReplacement = "_?".concat(replacement);
                    // 	specVarExp = new VarExp();
                    // 	((VarExp)specVarExp).setName(specVar.getName());
                    //	            	replace = realVar;
                    oSpecVar.setExp((Exp) specVarExp.clone());
                    oRealVar.setExp((Exp) replace.clone());
                    quesRep = new VarExp();
                    ((VarExp) quesRep)
                            .setName(createPosSymbol(quesReplacement));
                    quesRep.setType(realVar.getType());

                    undqRep = new VarExp();
                    ((VarExp) undqRep)
                            .setName(createPosSymbol(undquesReplacement));
                    undqRep.setType(realVar.getType());
                }
                else if (realVar instanceof ProgramParamExp) {
                    replacement = new String(specVar.getName().toString());
                    String quesReplacement = "?".concat(replacement);
                    String undquesReplacement = "_?".concat(replacement);
                    // 	specVarExp = new VarExp();
                    // 	((VarExp)specVarExp).setName(specVar.getName());
                    oSpecVar.setExp((Exp) specVarExp.clone());
                    oRealVar.setExp((Exp) replace.clone());
                    quesRep = new VarExp();
                    ((VarExp) quesRep)
                            .setName(createPosSymbol(quesReplacement));
                    quesRep.setType(getTypeFromTy(specVar.getTy()));

                    undqRep = new VarExp();
                    ((VarExp) undqRep)
                            .setName(createPosSymbol(undquesReplacement));
                    undqRep.setType(getTypeFromTy(specVar.getTy()));
                }
                else if (realVar instanceof VariableDotExp) {
                    if (replace instanceof DotExp) {
                        Exp pE = ((DotExp) replace).getSegments().get(0);
                        replacement = pE.toString(0);

                        String undquesReplacement = "_?".concat(replacement);
                        oSpecVar.setExp((Exp) specVarExp.clone());
                        oRealVar.setExp((Exp) replace.clone());
                        quesRep = (DotExp) replace.clone();
                        ((DotExp) quesRep).getSegments().remove(0);
                        ((DotExp) quesRep).getSegments()
                                .add(
                                        0,
                                        ((VariableDotExp) realVar)
                                                .getSegments().get(0));

                        VariableNameExp undqNameRep = new VariableNameExp();
                        ((VariableNameExp) undqNameRep)
                                .setName(createPosSymbol(undquesReplacement));

                        undqRep = (DotExp) replace.clone();
                        ((DotExp) undqRep).getSegments().remove(0);
                        ((DotExp) undqRep).getSegments().add(0, undqNameRep);

                    }
                    else if (replace instanceof VariableDotExp) {
                        Exp pE =
                                ((VariableDotExp) replace).getSegments().get(0);
                        replacement = pE.toString(0);

                        String undquesReplacement = "_?".concat(replacement);
                        oSpecVar.setExp((Exp) specVarExp.clone());
                        oRealVar.setExp((Exp) replace.clone());
                        quesRep = (VariableDotExp) replace.clone();
                        ((VariableDotExp) quesRep).getSegments().remove(0);
                        ((VariableDotExp) quesRep).getSegments()
                                .add(
                                        0,
                                        ((VariableDotExp) realVar)
                                                .getSegments().get(0));

                        VariableNameExp undqNameRep = new VariableNameExp();
                        ((VariableNameExp) undqNameRep)
                                .setName(createPosSymbol(undquesReplacement));

                        undqRep = (VariableDotExp) replace.clone();
                        ((VariableDotExp) undqRep).getSegments().remove(0);
                        ((VariableDotExp) undqRep).getSegments().add(0,
                                undqNameRep);

                    }
                    else {
                        try {
                            replacement =
                                    new String(specVar.getName().toString());
                            String quesReplacement = "?".concat(replacement);
                            String undquesReplacement =
                                    "_?".concat(replacement);

                            oSpecVar.setExp((Exp) specVarExp.clone());
                            oRealVar.setExp((Exp) replace.clone());
                            quesRep = new VarExp();
                            ((VarExp) quesRep)
                                    .setName(createPosSymbol(quesReplacement));
                            undqRep = new VarExp();
                            ((VarExp) undqRep)
                                    .setName(createPosSymbol(undquesReplacement));
                            undqRep.setType(getTypeFromTy(specVar.getTy()));
                        }
                        catch (Exception ex) {
                            System.err
                                    .println("Need to work on replacePostConditionVariables");
                        }
                    }

                }
                else if (realVar instanceof VariableExp) {

                }
                else if (realVar instanceof ProgramDotExp) {
                    if (replace instanceof DotExp) {
                        Exp pE = ((DotExp) replace).getSegments().get(0);
                        replacement = pE.toString(0);

                        String undquesReplacement = "_?".concat(replacement);
                        oSpecVar.setExp((Exp) specVarExp.clone());
                        oRealVar.setExp((Exp) replace.clone());
                        quesRep = (DotExp) replace.clone();
                        ((DotExp) quesRep).getSegments().remove(0);
                        ((DotExp) quesRep).getSegments().add(0,
                                ((ProgramDotExp) realVar).getSegments().get(0));

                        VariableNameExp undqNameRep = new VariableNameExp();
                        ((VariableNameExp) undqNameRep)
                                .setName(createPosSymbol(undquesReplacement));

                        undqRep = (DotExp) replace.clone();
                        ((DotExp) undqRep).getSegments().remove(0);
                        ((DotExp) undqRep).getSegments().add(0, undqNameRep);

                    }
                    else {
                        //	System.err.println("Errors May have occurred while fixing up Post-Condition Variables for "+ opDec.getName().toString());
                        //	System.err.println("\t\t Real Variable: "+ (realVar.asString(0, 0)));
                        //	System.err.println("\t\t Replacement Variable: "+ (replace.asString(0, 0)));
                        try {
                            replacement =
                                    new String(specVar.getName().toString());
                            String quesReplacement = "?".concat(replacement);
                            String undquesReplacement =
                                    "_?".concat(replacement);

                            oSpecVar.setExp((Exp) specVarExp.clone());
                            oRealVar.setExp((Exp) replace.clone());
                            quesRep = new VarExp();
                            ((VarExp) quesRep)
                                    .setName(createPosSymbol(quesReplacement));
                            undqRep = new VarExp();
                            ((VarExp) undqRep)
                                    .setName(createPosSymbol(undquesReplacement));
                            undqRep.setType(getTypeFromTy(specVar.getTy()));
                        }
                        catch (Exception ex) {
                            System.err
                                    .println("Need to work on replacePostConditionVariables");
                        }

                    }
                }
                else {}

                Exp quesVar = new VarExp();

                if (specVarExp != null && quesRep != null && oSpecVar != null
                        && replace != null && oRealVar != null) {

                    if (specVar.getMode() == Mode.UPDATES
                            || specVar.getMode() == Mode.REPLACES
                            || specVar.getMode() == Mode.REASSIGNS
                            || specVar.getMode() == Mode.CLEARS
                            || specVar.getMode() == Mode.ALTERS) {

                        ConcType quesSV =
                                getIfInFreeVarList(replacement, assertion);

                        if (quesSV == null) {
                            quesSV = getFreeVar(specVar);
                            quesSV.setName(createPosSymbol(replacement));
                        }

                        quesSV =
                                NQV(InfixExp.formAndStmt(ensures, conf),
                                        quesSV, assertion);

                        if (realVar instanceof ProgramDotExp) {
                            quesVar = (DotExp) replace.clone();

                            VarExp tmpVar = new VarExp();
                            ((VarExp) tmpVar).setName(quesSV.getName());
                            ((DotExp) quesVar).getSegments().remove(0);
                            ((DotExp) quesVar).getSegments().add(0, tmpVar);

                        }
                        if (realVar instanceof VariableDotExp) {
                            quesVar = (DotExp) replace.clone();

                            VarExp tmpVar = new VarExp();
                            ((VarExp) tmpVar).setName(quesSV.getName());
                            ((DotExp) quesVar).getSegments().remove(0);
                            ((DotExp) quesVar).getSegments().add(0, tmpVar);

                        }
                        else {

                            quesVar = new VarExp();
                            ((VarExp) quesVar).setName(quesSV.getName());
                        }

                        assertion.addFreeVar(quesSV);
                        if (ensures.containsVar(specVar.getName().toString(),
                                true)
                                || ensures.containsVar(specVar.getName()
                                        .toString(), false)) {
                            ensures = replace(ensures, specVarExp, undqRep);
                            ensures = replace(ensures, oSpecVar, replace);
                            undRepList.add(undqRep);
                            replList.add(quesVar);

                        }
                        else {
                            ensures = replace(ensures, specVarExp, quesRep);
                            ensures = replace(ensures, oSpecVar, replace);
                        }

                        if (realVar instanceof VariableNameExp) {
                            ParameterVarDec varDec =
                                    (ParameterVarDec) specVar.clone();
                            varDec
                                    .setName(createPosSymbol(((VariableNameExp) realVar)
                                            .getName().toString()));
                            conf = replace(conf, replace, quesVar);
                            assertion.setFinalConfirm(conf);
                        }
                        else {

                            conf = replace(conf, replace, quesVar);
                            assertion.setFinalConfirm(conf);
                        }

                    }
                    else {
                        if (specVar.getMode() == Mode.PRESERVES) {
                            if (ensures.containsVar(specVar.getName()
                                    .toString(), true)
                                    || ensures.containsVar(specVar.getName()
                                            .toString(), false)) {
                                ensures = replace(ensures, specVarExp, undqRep);
                                ensures = replace(ensures, oSpecVar, undqRep);
                                undRepList.add(undqRep);
                                replList.add(replace);

                            }
                            else {
                                ensures = replace(ensures, specVarExp, replace);
                                ensures = replace(ensures, oSpecVar, replace);
                            }
                        }
                        else {
                            if (ensures.containsVar(specVar.getName()
                                    .toString(), true)
                                    || ensures.containsVar(specVar.getName()
                                            .toString(), false)) {

                                ensures = replace(ensures, specVarExp, undqRep);
                                ensures = replace(ensures, oSpecVar, oRealVar);
                                undRepList.add(undqRep);
                                replList.add(replace);
                            }
                            else {
                                ensures = replace(ensures, specVarExp, replace);
                                ensures = replace(ensures, oSpecVar, oRealVar);
                            }
                        }
                    }
                }
            }
        }

        Iterator<Exp> iUnd = undRepList.iterator();
        Iterator<Exp> iRepl = replList.iterator();
        while (iUnd.hasNext() && iRepl.hasNext()) {
            ensures = replace(ensures, iUnd.next(), iRepl.next());
        }

        return ensures;
    }

    private Exp replacePreConditionVariables(Exp requires,
            List<ProgramExp> argList, OperationDec opDec,
            AssertiveCode assertion) {
        char tempRepChar = '_';

        if (requires == null)
            requires = getTrueVarExp();
        else
            requires = (Exp) (requires.clone());

        /* List to Hold temp and real values of variables in case of duplicate spec and real var */
        List<Exp> undRepList = new List<Exp>();
        List<Exp> replList = new List<Exp>();

        /* Replace PreCondition Variables */
        Iterator<ProgramExp> k = argList.iterator(); // Parameters (to be replaced)
        Iterator<ParameterVarDec> j =
                ((OperationDec) opDec).getParameters().iterator(); // Arguments

        while (k.hasNext() && j.hasNext()) {
            ParameterVarDec varDec = j.next();
            ProgramExp exp = k.next();

            VarExp old = new VarExp();
            Exp repl = new VarExp();
            VarExp undRepl = new VarExp();

            repl = getReplacement(exp, assertion); // 	Real Replacement For Spec
            String name = varDec.getName().toString(); //	Spec Name
            old.setName(createPosSymbol(name)); //  Spec Var
            undRepl.setName(createPosSymbol(tempRepChar + name.toString())); // 	_Spec

            Exp tmp = requires.replace(old, undRepl);
            undRepList.add(undRepl);
            replList.add(repl);
            if (tmp != null)
                requires = tmp;
        }

        Iterator<Exp> undIt = undRepList.iterator();
        Iterator<Exp> repIt = replList.iterator();

        while (undIt.hasNext() && repIt.hasNext()) {
            Exp tmp = requires.replace(undIt.next(), repIt.next());
            if (tmp != null)
                requires = tmp;
        }
        return requires;
    }

    private Exp replaceSimplePostConditionVariables(Exp ensures,
            List<ProgramExp> argList, OperationDec opDec,
            AssertiveCode assertion, boolean simple) {
        if (opDec == null)
            return getTrueVarExp();

        List<ParameterVarDec> parList = new List<ParameterVarDec>();

        ensures = (Exp) ensures.clone();

        if (ensures == null) {
            ensures = getTrueVarExp();
        }

        parList = ((OperationDec) opDec).getParameters();

        Exp conf = assertion.getFinalConfirm();
        Iterator<ProgramExp> k = argList.iterator();
        Iterator<ParameterVarDec> j = parList.iterator();
        Iterator<AffectsItem> it =
                ((OperationDec) opDec).getStateVars().iterator();

        List<Exp> undRepList = new List<Exp>();
        List<Exp> replList = new List<Exp>();

        k = argList.iterator();
        j = parList.iterator();

        while ((j.hasNext() && k.hasNext()) || it.hasNext()) {
            conf = assertion.getFinalConfirm();

            if (it.hasNext()) {
                AffectsItem stateVar = it.next();
                if (stateVar.getMode() == Mode.UPDATES
                        || stateVar.getMode() == Mode.ALTERS
                        || stateVar.getMode() == Mode.REPLACES) {
                    // Logic Needs to be reworked for State Variables			
                    ConcType SV =
                            getIfInFreeVarList(stateVar.getName().toString(),
                                    assertion);
                    VarExp sVar = new VarExp(null, SV.getName(), SV.getName());

                    ConcType quesSV =
                            getIfInFreeVarList("?"
                                    + stateVar.getName().toString(), assertion);

                    if (quesSV == null) {
                        quesSV =
                                new ConcType(
                                        SV.getModuleID(),
                                        createPosSymbol("?"
                                                + stateVar.getName().toString()),
                                        SV.getType());
                        assertion.addFreeVar(quesSV);
                    }

                    VarExp qsVar = new VarExp();
                    qsVar.setName(quesSV.getName());

                    OldExp osVar = new OldExp(null, (Exp) sVar.clone());

                    ensures = replace(ensures, sVar, qsVar);
                    ensures = replace(ensures, osVar, sVar);

                    assertion.setFinalConfirm(conf);
                }
            }
            else {
                ParameterVarDec specVar = j.next();
                ProgramExp realVar = k.next();
                Exp specVarExp = null, replace = null;
                VarExp undSpec = new VarExp();
                OldExp oSpecVar = null, oRealVar = null;

                undSpec.setName(createPosSymbol("_"
                        + specVar.getName().toString()));

                specVarExp = new VarExp();
                oSpecVar = new OldExp();
                oRealVar = new OldExp();
                ((VarExp) specVarExp).setName(specVar.getName());
                oSpecVar.setExp((Exp) specVarExp.clone());
                replace = getReplacement(realVar, assertion);
                oRealVar.setExp((Exp) replace.clone());

                if (specVarExp != null && oSpecVar != null && replace != null) {

                    ensures = replace(ensures, specVarExp, undSpec);
                    ensures = replace(ensures, oSpecVar, undSpec);
                    undRepList.add(undSpec);
                    replList.add(replace);
                }
            }
        }

        Iterator<Exp> iUnd = undRepList.iterator();
        Iterator<Exp> iRepl = replList.iterator();
        while (iUnd.hasNext() && iRepl.hasNext()) {
            ensures = replace(ensures, iUnd.next(), iRepl.next());
        }

        return ensures;
    }

    private Exp replaceSimplePostConditionVariables(List<ProgramExp> argList,
            OperationDec opDec, AssertiveCode assertion, boolean simple) {
        return replaceSimplePostConditionVariables(opDec.getEnsures(), argList,
                opDec, assertion, simple);
    }

    // This needs to be worked on
    private boolean same_Type(MathVarDec mVD, AuxVarDec aVD) {
        if (mVD.getTy() instanceof NameTy && aVD.getTy() instanceof NameTy) {
            return mVD.getTy().asString(1, 1)
                    .equals(aVD.getTy().asString(1, 1));
        }
        else {
            TypeConverter TC =
                    new TypeConverter(myInstanceEnvironment
                            .getSymbolTable(getCurrentModuleID()));
            TC.getMathType(mVD.getTy());

            ModuleScope curr = table.getModuleScope();
            TypeHolder typeHold = curr.getTypeHolder();
            TypeID typeID =
                    new TypeID(((NameTy) aVD.getTy()).getName().getSymbol());
            Type auxType = typeHold.searchForType(typeID);

            if (auxType == null)
                auxType = TC.getMathType(aVD.getTy());

            if (auxType instanceof ConcType) {
                auxType = ((ConcType) auxType).getType();

            }
            Type quantType = TC.getMathType(mVD.getTy());
            if (auxType instanceof ConstructedType) {
                // Get Around issue with ModifiedString.Str(entry) vs Str(Entry)
                ((ConstructedType) auxType).setQualifier(null);
            }

            return auxType.toString().equals(quantType.toString());
        }
    }

    // set the location variable for exp
    private void setLocation(Exp exp, Location loc) {
        if (loc == null)
            return;
        if (exp instanceof InfixExp) {
            ((InfixExp) exp).setAllLocations(loc);
        }
        else {
            exp.setLocation(loc);
        }
    }

    private VarDec toVarDec(Dec param) {
        VarDec tmpVD = new VarDec();
        PosSymbol tmpPS = null;
        Ty tmpTy = null;
        if (param instanceof ParameterVarDec) {
            tmpPS = param.getName();
            tmpTy = ((ParameterVarDec) param).getTy();
        }
        else if (param instanceof MathVarDec) {
            tmpPS = param.getName();
            tmpTy = ((MathVarDec) param).getTy();
        }
        else if (param instanceof ConstantParamDec) {
            tmpPS = param.getName();
            tmpTy = ((ConstantParamDec) param).getTy();
        }
        tmpVD.setName(tmpPS);
        tmpVD.setTy(tmpTy);
        return tmpVD;
    }

    public void visitConceptBodyModuleDec(ConceptBodyModuleDec dec) {
        table.beginModuleScope();
        importList = new List<String>();
        parmOpList = new List<String>();

        ModuleID cid = ModuleID.createConceptID(dec.getConceptName());
        typeParms = getTypeParms(cid);
        concParms = getConcParms(cid);

        VCBuffer.append("Concept Name: ");
        VCBuffer.append(dec.getName().toString());
        VCBuffer.append("\n");
        visitProcedures(dec.getDecs());

        table.endModuleScope();
    }

    public void visitDec(Dec dec) {
        if (debug) {
            System.out.println("Visiting Dec:" + dec.getName());
        }
        dec.accept(this);
    }

    public void visitEBCodeRule(VerificationStatement code,
            AssertiveCode assertion) {
        applyProofRulesToAssertiveCode((Statement) code.getAssertion(),
                assertion);
        if (!(myInstanceEnvironment.flags.isFlagSet(FLAG_FINALVERB_VC) || myInstanceEnvironment.flags
                .isFlagSet(Verifier.FLAG_VERIFY_VC))) {
            outputAsrt();
            clearMainBuffer();
        }
    }

    public void visitEnhancementBodyModuleDec(EnhancementBodyModuleDec dec) {
        table.beginModuleScope();
        importList = new List<String>();
        parmOpList = new List<String>();

        ModuleID cid = ModuleID.createConceptID(dec.getConceptName());
        typeParms = getTypeParms(cid);
        concParms = getConcParms(cid);

        /* Are there any facility declarations, if so, we need to add them to the context 
         * create any assertions needed.
         */

        /* Get Corresponding EnhancementModuleDec */
        ModuleID eid =
                ModuleID.createEnhancementID(dec.getEnhancementName(), dec
                        .getConceptName());
        EnhancementModuleDec eDec =
                (EnhancementModuleDec) myInstanceEnvironment.getModuleDec(eid);

        VCBuffer.append("Enhancement Name: ");
        VCBuffer.append(eDec.getName().toString());
        VCBuffer.append("\n");

        List<UsesItem> list = eDec.getUsesItems();
        list.addAll(dec.getUsesItems());
        ConceptModuleDec cDec =
                (ConceptModuleDec) myInstanceEnvironment.getModuleDec(cid);

        list.addAll(cDec.getUsesItems());
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_ISABELLE_VC)) {
            if (!list.isEmpty()) {
                assrtBuf.append("\timports ");
                Iterator<UsesItem> it = list.iterator();
                while (it.hasNext()) {
                    UsesItem imports = it.next();
                    if (imports.getName().toString().endsWith("Theory")) {
                        String name =
                                imports.getName().toString().substring(
                                        0,
                                        imports.getName().toString()
                                                .lastIndexOf("_Theory"));
                        assrtBuf.append(name + " ");
                    }
                }
                assrtBuf.append("Main \n\n");
            }
            assrtBuf.append("\tbegin\n\n");
        }
        visitProcedures(dec.getDecs());
        table.endModuleScope();
    }

    public void visitExp(Exp exp) {
        if (exp == null) {
            return;
        }
        if (debug) {
            System.out.println("Visiting Exp:" + exp.toString());
        }
        exp.accept(this);
    }

    // This method generates VCs for Facility Declarations.
    public void visitFacilityDec(FacilityDec dec) {
        AssertiveCode assertion = new AssertiveCode(myInstanceEnvironment);

        PosSymbol concName = dec.getConceptName();
        List<ModuleArgumentItem> concParams = dec.getConceptParams();

        ModuleID currentMID = getCurrentModuleID();
        ModuleID curCID = currentMID.getConceptID();

        ModuleID cid = ModuleID.createConceptID(concName);
        ModuleDec facMDec = myInstanceEnvironment.getModuleDec(cid);
        ModuleDec curMDec = null; // We need the Current Concept Module Dec
        ConceptModuleDec curCDec = null;

        if (debug) {
            System.out.println("Visiting Facility Dec:" + dec.getName());
        }
        File file = null;
        try {
            curMDec = myInstanceEnvironment.getModuleDec(curCID);
            file = myInstanceEnvironment.getFile(curCID);
        }
        catch (Exception ex) {
            curMDec = myInstanceEnvironment.getModuleDec(currentMID);
            file = myInstanceEnvironment.getFile(currentMID);
        }

        if (curMDec == null) {
            // We didn't find where this facility declaration exist
            return;
        }
        else {
            if (curMDec instanceof FacilityModuleDec) {
                /* YS - Get the global requires clause and add it to our list
                   of assumes */
                Exp gRequires = ((FacilityModuleDec) curMDec).getRequirement();
                if (gRequires != null) {
                    if (gRequires.getLocation() != null) {
                        Location myLoc = gRequires.getLocation();
                        myLoc.setDetails("Requires Clause for "
                                + ((FacilityModuleDec) curMDec).getName());
                        setLocation(gRequires, myLoc);
                    }
                    assertion.addAssume(gRequires);
                }
            }
            else if (curMDec instanceof ConceptBodyModuleDec) {
                /* YS - Get the global requires clause and add it to our list
                   of assumes */
                Exp gRequires = ((ConceptBodyModuleDec) curMDec).getRequires();
                if (gRequires != null) {
                    if (gRequires.getLocation() != null) {
                        Location myLoc = gRequires.getLocation();
                        myLoc.setDetails("Requires Clause for "
                                + ((ConceptBodyModuleDec) curMDec).getName());
                        setLocation(gRequires, myLoc);
                    }
                    assertion.addAssume(gRequires);
                }
            }
            else if (curMDec instanceof EnhancementBodyModuleDec) {
                /* YS - Get the global requires clause and add it to our list
                   of assumes */
                Exp gRequires =
                        ((EnhancementBodyModuleDec) curMDec).getRequires();
                if (gRequires != null) {
                    if (gRequires.getLocation() != null) {
                        Location myLoc = gRequires.getLocation();
                        myLoc.setDetails("Requires Clause for "
                                + ((EnhancementBodyModuleDec) curMDec)
                                        .getName());
                        setLocation(gRequires, myLoc);
                    }
                    assertion.addAssume(gRequires);
                }
            }
        }

        // Not sure if this actually does much?
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_REPARG_VC)) {
            RepeatedArguments ra = new RepeatedArguments();
        }

        if (dec.getEnhancementBodies() != null) {
            Iterator it = dec.getEnhancementBodies().iterator();
            while (it.hasNext()) {
                EnhancementBodyItem eBI = (EnhancementBodyItem) it.next();
                PosSymbol enhBodyName = eBI.getBodyName();

                ModuleID ehmid =
                        ModuleID.createEnhancementID(eBI.getName(), concName);
                ModuleDec eSpecDec = myInstanceEnvironment.getModuleDec(ehmid);
                ModuleID eBmid =
                        ModuleID.createEnhancementBodyID(eBI.getBodyName(), eBI
                                .getName(), concName);
                ModuleDec myDec = myInstanceEnvironment.getModuleDec(eBmid);
                //    ModuleID eSpecID	= ehmid.getEnhancementID();
                //    ModuleDec eSpecDec 	= env.getModuleDec(eSpecID);

                if (myDec instanceof EnhancementBodyModuleDec) {

                    EnhancementBodyModuleDec curEnhDec =
                            (EnhancementBodyModuleDec) myDec;
                    EnhancementModuleDec curESpecDec =
                            (EnhancementModuleDec) eSpecDec;
                    generateVCsForOperationParameter(dec, eBI, curEnhDec,
                            curESpecDec);

                    /* Replace Requirements Params with Actuals */
                    Exp enh_req = null;//curESpecDec.getRequirement();
                    if (enh_req == null) {
                        enh_req = getTrueVarExp();
                        Location loc =
                                (Location) dec.getBodyName().getLocation()
                                        .clone();
                        enh_req.setLocation(loc);
                    }
                    Exp enh_body_req = null;//curEnhDec.getRequires();
                    if (enh_body_req == null) {
                        enh_body_req = getTrueVarExp();
                        Location loc =
                                (Location) dec.getBodyName().getLocation()
                                        .clone();
                        enh_body_req.setLocation(loc);
                    }

                    // Get the Spec for the Facility
                    if (curESpecDec instanceof EnhancementModuleDec) {
                        enh_req = curESpecDec.getRequirement();
                        enh_body_req = curEnhDec.getRequires();

                        if (enh_req != null) {
                            enh_req =
                                    replaceFacilityDeclarationVariables(
                                            enh_req, curESpecDec
                                                    .getParameters(), eBI
                                                    .getParams());

                            enh_req.getLocation().setDetails(
                                    "Requirement for Facility Declaration Rule for "
                                            + dec.getName());
                            assertion.addConfirm(enh_req);
                        }

                        if (enh_body_req != null) {
                            enh_body_req =
                                    replaceFacilityDeclarationVariables(
                                            enh_body_req, curEnhDec
                                                    .getParameters(), eBI
                                                    .getBodyParams());

                            enh_body_req.getLocation().setDetails(
                                    "Requirement for Facility Declaration Rule for "
                                            + dec.getName());
                            assertion.addConfirm(enh_body_req);
                        }
                        //Exp thisConcReq = curESpecDec.getRequirement();
                        //if(thisConcReq != null){
                        //	thisConcReq.getLocation().setDetails("Concept Declaration Requirement");            	
                        //  	assertion.addAssume(thisConcReq);
                        //}

                    }
                    else {
                        enh_req =
                                replaceFacilityDeclarationVariables(enh_req,
                                        curEnhDec.getParameters(), eBI
                                                .getParams());

                        enh_req.getLocation().setDetails(
                                "Facility Declaration Rule");
                        assertion.addConfirm(enh_req);
                    }

                    if (curEnhDec != null) {
                        for (int i = 0; i < curEnhDec.getParameters().size(); i++) {
                            if (curEnhDec.getParameters().get(i) instanceof Dec) {
                                //Parameters to Enhancement.
                                Dec paramDec =
                                        (Dec) curEnhDec.getParameters().get(i);

                                addFreeVar(paramDec, assertion);
                            }
                        }
                    }
                }

            }

        }

        if (facMDec instanceof ConceptModuleDec
                && (curMDec instanceof ConceptModuleDec || curMDec instanceof FacilityModuleDec)) {

            ConceptModuleDec facCDec = (ConceptModuleDec) facMDec;

            /* Replace Requirements Params with Actuals */
            Exp req = facCDec.getRequirement();
            if (req == null) {
                req = getTrueVarExp();
                Location loc =
                        (Location) dec.getBodyName().getLocation().clone();
                req.setLocation(loc);
            }

            // Get the Spec for the Facility
            if (curMDec instanceof ConceptModuleDec) {
                //curCDec = (ConceptModuleDec)curMDec;
                ModuleID facConceptID =
                        myInstanceEnvironment.getModuleID(facCDec.getName()
                                .getFile());
                curCDec =
                        (ConceptModuleDec) myInstanceEnvironment
                                .getModuleDec(facConceptID);
                if (req != null) {
                    req =
                            replaceFacilityDeclarationVariables(req, facCDec
                                    .getParameters(), curCDec.getParameters());

                    Location loc = dec.getName().getLocation();
                    loc
                            .setDetails("Requirement for Facility Declaration Rule for "
                                    + dec.getName());
                    setLocation(req, loc);

                    assertion.setFinalConfirm(req);
                }
                Exp thisConcReq = curCDec.getRequirement();
                if (thisConcReq != null) {
                    Location loc = (Location) thisConcReq.getLocation().clone();
                    loc.setDetails("Concept Declaration Requirement");
                    setLocation(thisConcReq, loc);
                    assertion.addAssume(thisConcReq);
                }

            }
            else {
                req =
                        replaceFacilityDeclarationVariables(req, facCDec
                                .getParameters(), concParams);

                req.getLocation().setDetails("Facility Declaration Rule");
                assertion.setFinalConfirm(req);
            }

            /* Check for Concept Realization Requires Clause */
            if (dec.getBodyName() != null) {
                ModuleID bid =
                        ModuleID.createConceptBodyID(dec.getBodyName(), dec
                                .getConceptName());
                ConceptBodyModuleDec bodyDec =
                        (ConceptBodyModuleDec) myInstanceEnvironment
                                .getModuleDec(bid);

                if (bodyDec != null) {
                    Exp breq = bodyDec.getRequires();
                    if (breq != null) {
                        Location loc = (Location) breq.getLocation().clone();
                        loc
                                .setDetails("Requirement for Facility Declaration Rule for "
                                        + dec.getName());
                        setLocation(breq, loc);
                        assertion.addConfirm(breq);
                    }
                }
            }

            /* What can we assume? */

            if (curCDec != null) {
                for (int i = 0; i < curCDec.getParameters().size(); i++) {
                    if (curCDec.getParameters().get(i) instanceof Dec) {
                        //Parameters to Concept.
                        Dec paramDec = (Dec) curCDec.getParameters().get(i);

                        addFreeVar(paramDec, assertion);
                    }
                }
            }

            // Add the parameters to the facility Concept to the free variable list
            for (int i = 0; i < facCDec.getParameters().size(); i++) {
                if (facCDec.getParameters().get(i) instanceof Dec) {
                    //Parameters to Concept.
                    Dec facilParmDec = (Dec) facCDec.getParameters().get(i);

                    addFreeVar(facilParmDec, assertion);
                }
            }

            VCBuffer.append("\n Facility Dec Name:\t");
            VCBuffer.append(dec.getName().getSymbol().toString());
            VCBuffer.append("\n");

            VCBuffer.append("\n_____________________ \n");
            VCBuffer.append("\nFacility Declaration Rule Applied: \n\n");
            VCBuffer.append(assertion.assertionToString());

            /* Applies Proof Rules */
            applyEBRules(assertion);
            VCBuffer.append("\n_____________________ \n\n");

        }

        //   table.endTypeScope();
    }

    public void visitFacilityModuleDec(FacilityModuleDec dec) {
        table.beginModuleScope();
        //      System.out.println("Facility Module Dec:" + dec.getName());
        myFacilityModuleDec = dec;
        buildListAvailableSpecs(dec);

        visitProcedures(dec.getDecs());
        myFacilityModuleDec = null;
        table.endModuleScope();
    }

    public void visitFacilityOperationDec(FacilityOperationDec dec) {

        AssertiveCode assertion = new AssertiveCode(myInstanceEnvironment);
        table.beginOperationScope();
        table.beginProcedureScope();
        table.bindProcedureTypeNames();

        ModuleDec moduleDec = getCurrentBodyModuleDec();

        Exp requires = new VarExp();
        Exp ensures = new VarExp();
        boolean concept = false;
        Exp moduleLevelRequires = null;

        Exp constraints =
                getConstraintsFromCurrentContext(assertion, false, false);
        Exp correspondence = null;
        Exp convention = null;
        VarExp exemplar = null, cExem = null;
        if (moduleDec != null && moduleDec instanceof ConceptBodyModuleDec) {
            concept = true;
            Iterator<Dec> decsIt =
                    ((ConceptBodyModuleDec) moduleDec).getDecs().iterator();
            while (decsIt.hasNext()) {
                Dec tmp = decsIt.next();
                if (tmp instanceof RepresentationDec) {
                    correspondence =
                            (Exp) ((RepresentationDec) tmp).getCorrespondence()
                                    .clone();
                    convention =
                            (Exp) ((RepresentationDec) tmp).getConvention()
                                    .clone();
                }
            }
            ConceptModuleDec cmDec = (ConceptModuleDec) getCurrentModuleDec();
            moduleLevelRequires = cmDec.getRequirement();

            Iterator<Dec> decs = cmDec.getDecs().iterator();
            while (decs.hasNext()) {
                Dec tmpDec = decs.next();
                if (tmpDec instanceof TypeDec) {

                    exemplar = new VarExp();

                    cExem = new VarExp();
                    exemplar.setName(((TypeDec) tmpDec).getExemplar());

                    Type exemType =
                            getTypeFromTy(((TypeDec) tmpDec).getModel());

                    cExem.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    cExem.setType(exemType);
                    VarDec concVar = new VarDec();
                    concVar.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    concVar.setTy(((TypeDec) tmpDec).getModel());
                    addFreeVar(concVar, assertion);
                }
            }

        }
        else if (moduleDec != null
                && moduleDec instanceof EnhancementBodyModuleDec) {

            moduleLevelRequires =
                    (Exp) ((EnhancementBodyModuleDec) moduleDec).getRequires();
            if (moduleLevelRequires != null) {
                moduleLevelRequires = (Exp) moduleLevelRequires.clone();
            }

            ModuleID enhancementID =
                    ModuleID.createEnhancementID(
                            ((EnhancementBodyModuleDec) moduleDec)
                                    .getEnhancementName(),
                            ((EnhancementBodyModuleDec) moduleDec)
                                    .getConceptName());

            EnhancementModuleDec eDec =
                    (EnhancementModuleDec) myInstanceEnvironment
                            .getModuleDec(enhancementID);

            if (moduleLevelRequires != null) {
                Location location =
                        (Location) moduleLevelRequires.getLocation().clone();
                location.setDetails("Requires from Enhancement Body: "
                        + ((EnhancementModuleDec) moduleDec).getName());
                setLocation(moduleLevelRequires, location);
            }
            Exp eDecRequire = getTrueVarExp();
            if (eDec.getRequirement() != null) {
                eDecRequire = eDec.getRequirement();
                if (eDecRequire != null) {
                    Location location =
                            (Location) eDecRequire.getLocation().clone();
                    location.setDetails("Requires from Enhancement Body: "
                            + eDec.getName());
                    setLocation(eDecRequire, location);
                }
            }

            if (moduleLevelRequires != null) {
                moduleLevelRequires =
                        InfixExp.formAndStmt(moduleLevelRequires, eDecRequire);
            }
            else {
                moduleLevelRequires = eDecRequire;
            }

        }
        else if (moduleDec != null && moduleDec instanceof FacilityModuleDec) {

        }

        /* YS - Cheap fix for when getCurrentBodyModuleDec doesn't return what we
         *      need. TODO: Use Hampton's new symbol table to fix it.
         */
        if (moduleDec == null && myFacilityModuleDec != null) {
            FacilityModuleDec fDec = myFacilityModuleDec;
            if (fDec instanceof FacilityModuleDec) {
                /* YS - Get the global requires clause and add it to our list
                   of assumes */
                Exp gRequires = fDec.getRequirement();
                if (gRequires != null) {
                    if (gRequires.getLocation() != null) {
                        Location myLoc = gRequires.getLocation();
                        myLoc.setDetails("Requires Clause for "
                                + fDec.getName());
                        setLocation(gRequires, myLoc);
                    }
                    assertion.addAssume(gRequires);
                }
            }
        }

        OperationDec curOperation = formOpDec(dec);

        /* OperationDec curOperation  = new OperationDec(
        			((FacilityOperationDec)dec).getName(),
        				((FacilityOperationDec)dec).getParameters(),
        				((FacilityOperationDec)dec).getReturnTy(),
        				((FacilityOperationDec)dec).getStateVars(),
        				(Exp)((FacilityOperationDec)dec).getRequires().clone(),
        				(Exp)((FacilityOperationDec)dec).getEnsures().clone());*/
        ensures = curOperation.getEnsures();

        requires = modifyRequiresByParameters(curOperation, assertion);
        Exp globalConstr = addGlobalsAsFreeVariables(curOperation, assertion);
        requires =
                modifyRequiresByGlobalMode(requires, curOperation, assertion);

        /* Adds constraints of current context */
        if (constraints != null) {
            if (concept == true) {
                constraints = replace(constraints, exemplar, cExem);
                constraints =
                        replace(constraints, new OldExp(null, exemplar),
                                new OldExp(null, cExem));
                assertion.addAssume(constraints);
            }
            else {
                assertion.addAssume(constraints);
            }
        }

        /* Adds constraints of global variables */
        if (globalConstr != null)
            assertion.addAssume(globalConstr);

        /* Are there any requires clauses on this module */
        if (moduleLevelRequires != null) {
            assertion.addAssume(moduleLevelRequires);
        }

        /* If Concept Operation, Assume Conventions, Assume Correspondence */

        if (correspondence != null)
            assertion.addAssume((Exp) correspondence.clone());
        if (convention != null)
            assertion.addAssume((Exp) convention.clone());

        /* Adds requires assumption */
        if (requires != null) {
            if (requires.getLocation() != null) {
                Location myLoc = requires.getLocation();
                myLoc.setDetails("Requires Clause for "
                        + curOperation.getName());
                setLocation(requires, myLoc);
            }
            if (concept == true) {
                requires = replace(requires, exemplar, cExem);
                requires =
                        replace(requires, new OldExp(null, exemplar),
                                new OldExp(null, cExem));
                assertion.addAssume(requires);
            }
            else {
                assertion.addAssume(requires);
            }
        }
        else {
            assertion.addAssume(getTrueVarExp());
        }
        assertion.addRemember();

        /* Adds Variable Declaration to assertive code */
        assertion.addVariableDecs(dec.getAllVariables());
        Iterator<VarDec> it = dec.getAllVariables().iterator();
        while (it.hasNext()) {
            VarDec mydec = it.next();

            addFreeVar((VarDec) mydec, assertion);

        }

        /* If Procedure is recursive, add P_Val = (decreasing clause) assumption */
        if (dec.getDecreasing() != null) {
            VarExp pval = new VarExp();
            ConcType pVAL = getPVAL();
            assertion.addFreeVar(pVAL);
            pval.setName(pVAL.getName());
            setLocation(pval, dec.getDecreasing().getLocation());

            EqualsExp recurs =
                    new EqualsExp(dec.getDecreasing().getLocation(), pval,
                            EqualsExp.EQUAL, dec.getDecreasing());
            Location recurLocation = dec.getDecreasing().getLocation();
            recurLocation.setDetails("Progress Metric for Recursive Procedure");
            setLocation(recurs, recurLocation);

            recurs.setType(BooleanType.INSTANCE);
            assertion.addAssume(recurs);
        }

        /* Add Statements to Assertive Code */
        assertion.addStatements(dec.getStatements());

        ProcedureDec pDec =
                new ProcedureDec(dec.getName(), dec.getParameters(), dec
                        .getReturnTy(), dec.getStateVars(),
                        dec.getDecreasing(), dec.getFacilities(), dec
                                .getVariables(), dec.getAuxVariables(), dec
                                .getStatements());
        /* Add the ensures clause to be confirmed*/
        ensures = modifyEnsuresForProcedureDecRule(curOperation, pDec);
        if (ensures == null)
            ensures = getTrueVarExp();

        /* If Concept Operation, Confirm Conventions, Assume Correspondence */
        if (convention != null) {
            assertion.addConfirm(convention);
        }
        if (correspondence != null) {
            assertion.addAssume(correspondence);
        }

        Location loc;
        if (curOperation.getEnsures() == null) {
            loc = (Location) curOperation.getName().getLocation().clone();
        }
        else {
            loc = (Location) curOperation.getEnsures().getLocation().clone();
        }
        if (loc != null) {
            loc.setDetails("Ensures Clause of " + dec.getName());
            setLocation(ensures, loc);
        }

        if (concept == true) {
            ensures = replace(ensures, exemplar, cExem);
            ensures =
                    replace(ensures, new OldExp(null, exemplar), new OldExp(
                            null, cExem));
            assertion.setFinalConfirm(ensures);
        }
        else {
            assertion.setFinalConfirm(ensures);
        }

        VCBuffer.append("\n Procedure Name:\t");
        VCBuffer.append(dec.getName().getSymbol().toString());
        VCBuffer.append("\n");

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nProcedure Declaration Rule Applied: \n\n");
        VCBuffer.append(assertion.assertionToString());

        /* Applies Proof Rules */
        applyEBRules(assertion);
        VCBuffer.append("\n_____________________ \n\n");
        table.endProcedureScope();
        table.endOperationScope();
    }

    //the following class has been improved to handle record type declarations
    public void visitFacilityTypeDec(FacilityTypeDec dec) {
        table.beginTypeScope();
        //     System.out.println(dec.getName() + " " + dec.toString());

        table.endTypeScope();
    }

    public void visitInitItem(InitItem item) {
        table.beginOperationScope();
        table.beginProcedureScope();
        //FIX visitFacilityDecList(item.getFacilities());
        //FIX visitStatementList(item.getStatements());
        table.endProcedureScope();
        table.endOperationScope();
    }

    public void visitFinalItem(FinalItem item) {
        table.beginOperationScope();
        table.beginProcedureScope();
        //FIX visitFacilityDecList(item.getFacilities());
        //FIX visitStatementList(item.getStatements());
        table.endProcedureScope();
        table.endOperationScope();
    }

    /*
     * 
     */
    public void visitModuleDec(ModuleDec dec) {
        if (debug) {
            System.out.println("Visiting Module Dec:" + dec.getName());
        }
        dec.accept(this);
    }

    //forms assertion from Procedure Declaration
    public void visitProcedureDec(ProcedureDec dec) {

        AssertiveCode assertion = new AssertiveCode(myInstanceEnvironment);
        table.beginOperationScope();
        table.beginProcedureScope();
        table.bindProcedureTypeNames();

        ModuleDec moduleDec = getCurrentBodyModuleDec();
        if (moduleDec == null) {
            ModuleID mid = getCurrentModuleID();
            if (mid.getModuleKind() == ModuleKind.ENHANCEMENT_BODY) {
                moduleDec =
                        (EnhancementBodyModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);

                ModuleID tid = mid.getEnhancementID();
                moduleDec = myInstanceEnvironment.getModuleDec(tid);

            }
            else if (mid.getModuleKind() == ModuleKind.CONCEPT_BODY) {
                moduleDec =
                        (ConceptBodyModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);
            }
            else if (mid.getModuleKind() == ModuleKind.FACILITY) {
                moduleDec =
                        (FacilityModuleDec) myInstanceEnvironment
                                .getModuleDec(mid);
            }
        }

        if (moduleDec instanceof EnhancementBodyModuleDec) {
            /* YS - Get the global requires clause and add it to our list
               of assumes */
            Exp gRequires =
                    ((EnhancementBodyModuleDec) moduleDec).getRequires();
            if (gRequires != null) {
                if (gRequires.getLocation() != null) {
                    Location myLoc = gRequires.getLocation();
                    myLoc.setDetails("Requires Clause for "
                            + ((EnhancementBodyModuleDec) moduleDec).getName());
                    setLocation(gRequires, myLoc);
                }
                assertion.addAssume(gRequires);
            }
        }
        else if (moduleDec instanceof ConceptBodyModuleDec) {
            /* YS - Get the global requires clause and add it to our list
               of assumes */
            Exp gRequires = ((ConceptBodyModuleDec) moduleDec).getRequires();
            if (gRequires != null) {
                if (gRequires.getLocation() != null) {
                    Location myLoc = gRequires.getLocation();
                    myLoc.setDetails("Requires Clause for "
                            + ((ConceptBodyModuleDec) moduleDec).getName());
                    setLocation(gRequires, myLoc);
                }
                assertion.addAssume(gRequires);
            }
        }

        Exp requires = new VarExp();
        Exp ensures = Exp.getTrueVarExp(); //new VarExp();  
        boolean thisConcept = false;

        if (moduleDec != null && moduleDec instanceof ConceptBodyModuleDec
                && decInCurConcept(dec)) {
            thisConcept = true;
        }

        Exp constraints =
                getConstraintsFromCurrentContext(assertion, false, thisConcept);
        Exp correspondence = null;
        Exp convention = null;
        Exp moduleLevelRequires = null;
        VarExp exemplar = null, cExem = null;
        if (moduleDec != null && moduleDec instanceof ConceptBodyModuleDec) {

            Iterator<Dec> decsIt =
                    ((ConceptBodyModuleDec) moduleDec).getDecs().iterator();
            while (decsIt.hasNext()) {
                Dec tmp = decsIt.next();
                if (tmp instanceof RepresentationDec) {
                    if (((RepresentationDec) tmp).getCorrespondence() != null) {
                        correspondence =
                                (Exp) ((RepresentationDec) tmp)
                                        .getCorrespondence().clone();
                    }

                    if (((RepresentationDec) tmp).getConvention() != null) {
                        convention =
                                (Exp) ((RepresentationDec) tmp).getConvention()
                                        .clone();
                    }
                }
            }
            Iterator<Exp> convIt =
                    ((ConceptBodyModuleDec) moduleDec).getConventions()
                            .iterator();
            while (convIt.hasNext()) {
                if (convention != null) {
                    convention =
                            InfixExp.formAndStmt(convention, (Exp) convIt
                                    .next());
                }
                else {
                    convention = (Exp) convIt.next().clone();
                }
            }
            if (convention == null) {
                convention = getTrueVarExp();
            }
            else {
                if (dec.getStateVars().size() > 0) {
                    Location newloc =
                            getLocationOfLastLine(dec.getStatements());
                    convention.setLocation(newloc);
                    convention.getLocation().setDetails(
                            "Convention for " + moduleDec.getName());
                }
            }
            ConceptModuleDec cmDec = (ConceptModuleDec) getCurrentModuleDec();
            moduleLevelRequires = cmDec.getRequirement();

            Iterator<Dec> decs = cmDec.getDecs().iterator();
            while (decs.hasNext()) {
                Dec tmpDec = decs.next();
                if (tmpDec instanceof TypeDec) {

                    exemplar = new VarExp();

                    cExem = new VarExp();
                    exemplar.setName(((TypeDec) tmpDec).getExemplar());

                    Type exemplarType =
                            getTypeFromTy(((TypeDec) tmpDec).getModel());

                    cExem.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    cExem.setType(exemplarType);

                    VarDec concVar = new VarDec();
                    concVar.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    concVar.setTy(((TypeDec) tmpDec).getModel());
                    addFreeVar(concVar, assertion);
                }
            }

            DotExp concDotExp = new DotExp();
            VarExp cName = new VarExp();
            cName.setName(createPosSymbol("Conc"));

            List<Exp> myList = new List<Exp>();
            myList.add(cName);
            myList.add(exemplar);

            concDotExp.setSegments(myList);

            if (correspondence != null) {
                correspondence = replace(correspondence, concDotExp, cExem);
            }

        }
        else if (moduleDec != null && moduleDec instanceof EnhancementModuleDec) {

            moduleLevelRequires =
                    (Exp) ((EnhancementModuleDec) moduleDec).getRequirement()
                            .clone();
            if (moduleLevelRequires != null) {

                Location location =
                        (Location) dec.getName().getLocation().clone();

                location.setDetails("Requires from Enhancement Spec: "
                        + ((EnhancementModuleDec) moduleDec).getName());
                setLocation(moduleLevelRequires, location);
            }

        }
        else if (moduleDec != null && moduleDec instanceof FacilityModuleDec) {

        }

        OperationDec curOperation = getCorOpDec(dec);
        if (curOperation == null) {
            return;
        }

        if (curOperation.getEnsures() != null) {
            ensures = (Exp) curOperation.getEnsures().clone();
        }

        requires = modifyRequiresByParameters(curOperation, assertion);

        Exp globalConstr = addGlobalsAsFreeVariables(curOperation, assertion);
        requires =
                modifyRequiresByGlobalMode(requires, curOperation, assertion);

        /* Adds constraints of current context */
        if (constraints != null) {
            if (thisConcept == true) {
                // 		constraints = replace(constraints, exemplar, cExem);       		
                constraints =
                        replace(constraints, new OldExp(null, exemplar),
                                new OldExp(null, cExem));
                if (correspondence instanceof EqualsExp) {
                    constraints =
                            replace(constraints, cExem,
                                    ((EqualsExp) correspondence).getRight());
                }
                assertion.addAssume(constraints);
            }
            else {
                assertion.addAssume(constraints);
            }
        }

        /* Adds constraints of global variables */
        if (globalConstr != null)
            assertion.addAssume(globalConstr);

        /* If Concept Operation, Assume Conventions, Assume Correspondence */

        //       if(correspondence != null)
        //       	assertion.addAssume((Exp)correspondence.clone());
        if (convention != null)
            assertion.addAssume((Exp) convention.clone());

        /* Are there any requires clauses on this module */
        //        if(moduleLevelRequires != null){
        //    		assertion.addAssume(moduleLevelRequires);        	
        //       }

        Exp keepCorrespondence = null;

        /* Add the ensures clause to be confirmed*/
        ensures = modifyEnsuresForProcedureDecRule(curOperation, dec);
        if (ensures == null)
            ensures = getTrueVarExp();

        ensures = modifyEnsuresByParameters(curOperation, assertion, ensures);
        if (thisConcept == true) {
            if (exemplar != null) {
                ensures = replace(ensures, exemplar, cExem);
                ensures =
                        replace(ensures, buildOldExp(exemplar),
                                buildOldExp(cExem));

                if (correspondence instanceof EqualsExp) {

                    ensures =
                            replace(ensures, cExem,
                                    ((EqualsExp) correspondence).getRight());
                    //ensures = replace(ensures, buildOldExp(cExem), buildOldExp(((EqualsExp)correspondence).getRight()));      				
                    if (ensures.containsVar(cExem.getName().getName(), false)) {

                        ConcType type =
                                getIfInFreeVarList(cExem.getName().getName(),
                                        assertion);

                        ConcType newcExemType =
                                NQV(correspondence, type, assertion);
                        VarExp newcExem = (VarExp) cExem.clone();
                        newcExem.setName(newcExemType.getName());
                        Exp newCorrespondence = (Exp) correspondence.clone();
                        newCorrespondence =
                                replace(newCorrespondence, cExem, newcExem);
                        ensures = replace(ensures, cExem, newcExem);

                        keepCorrespondence = newCorrespondence;
                        //assertion.addAssume(newCorrespondence);
                    }
                    if (ensures.containsVar(cExem.getName().getName(), true)) {

                        //VarExp newcExem = (VarExp)cExem.clone();

                        //Exp newCorrespondence = (Exp)correspondence.clone();
                        //newCorrespondence = replace(newCorrespondence, cExem, buildOldExp(newcExem));
                        //assertion.addAssume(newCorrespondence);    					
                    }
                    //ensures = replace(ensures, buildOldExp(cExem), buildOldExp(((EqualsExp)correspondence).getRight()));
                }
            }
        }

        if (requires == null) {
            requires = getTrueVarExp();
        }
        /* Adds requires assumption */
        if (requires != null) {
            if (requires.getLocation() != null) {
                Location myLoc = requires.getLocation();
                myLoc.setDetails("Requires Clause for "
                        + curOperation.getName());
                setLocation(requires, myLoc);
            }

            if (thisConcept == true) {
                requires = replace(requires, exemplar, cExem);
                requires =
                        replace(requires, new OldExp(null, exemplar),
                                new OldExp(null, cExem));
                if (correspondence instanceof EqualsExp) {
                    requires =
                            replace(requires, cExem,
                                    ((EqualsExp) correspondence).getRight());
                    if (requires.containsVar(cExem.getName().getName(), false)
                            || ensures.containsVar(cExem.getName().getName(),
                                    true)) {
                        //	keepCorrespondence = true;
                        assertion.addAssume(correspondence);
                    }

                }

                assertion.addAssume(requires);
            }
            else {
                assertion.addAssume(requires);
            }
        }
        else {
            assertion.addAssume(getTrueVarExp());
        }

        assertion.addRemember();

        Location loc;
        if (curOperation.getEnsures() == null) {
            loc = (Location) curOperation.getName().getLocation().clone();
        }
        else {
            loc = (Location) curOperation.getEnsures().getLocation().clone();
        }
        Location newloc = getLocationOfLastLine(dec.getStatements());
        if (newloc != null) {
            Pos pos = newloc.getPos();
            Pos newPos = new Pos(pos.getLine() + 1, pos.getColumn());
            loc = new Location(newloc.getFile(), newPos);

        }
        if (loc != null) {
            loc.setDetails("Ensures Clause of " + dec.getName());
            setLocation(ensures, loc);
        }

        if (correspondence != null && (!(correspondence instanceof EqualsExp)))
            assertion.addAssume(correspondence);

        /* Adds Variable Declaration to assertive code */
        if (dec.getReturnTy() != null) { //If a function
            assertion.addVariableDec(new VarDec(dec.getName(), dec
                    .getReturnTy()));
            addFreeVar(new VarDec(dec.getName(), dec.getReturnTy()), assertion);
        }
        assertion.addVariableDecs(dec.getAllVariables());

        Iterator<VarDec> it = dec.getAllVariables().iterator();
        while (it.hasNext()) {
            VarDec mydec = it.next();

            addFreeVar((VarDec) mydec, assertion);

        }

        /* If Procedure is recursive, add P_Val = (decreasing clause) assumption */
        if (dec.getDecreasing() != null) {
            VarExp pval = new VarExp();
            ConcType pVAL = getPVAL();
            assertion.addFreeVar(pVAL);
            pval.setName(pVAL.getName());
            setLocation(pval, dec.getDecreasing().getLocation());
            EqualsExp recurs =
                    new EqualsExp(dec.getDecreasing().getLocation(), pval,
                            EqualsExp.EQUAL, dec.getDecreasing());
            Location recurLocation = dec.getDecreasing().getLocation();
            recurLocation.setDetails("Progress Metric for Recursive Procedure");
            setLocation(recurs, recurLocation);
            recurs.setType(BooleanType.INSTANCE);
            assertion.addAssume(recurs);
        }

        /* Add Statements to Assertive Code */
        assertion.addStatements(dec.getStatements());

        /* If Concept Operation, Confirm Conventions, Assume Correspondence */
        if (convention != null) {
            assertion.addConfirm(convention);
        }

        if (keepCorrespondence != null) {

            assertion.addAssume(keepCorrespondence);
        }

        if (ensures != null) {
            assertion.setFinalConfirm(ensures);
        }

        VCBuffer.append("\n Procedure Name:\t");
        VCBuffer.append(dec.getName().getSymbol().toString());
        VCBuffer.append("\n");

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nProcedure Declaration Rule Applied: \n\n");
        VCBuffer.append(assertion.assertionToString());

        /* Applies Proof Rules */
        applyEBRules(assertion);
        VCBuffer.append("\n_____________________ \n\n");
        table.endProcedureScope();
        table.endOperationScope();
    }

    // unifying procedure handling in concept and enhancement bodies
    private void visitProcedures(List<Dec> decs) {
        Iterator<Dec> i = decs.iterator();
        while (i.hasNext()) {
            Dec dec = i.next();
            if (dec instanceof ProcedureDec) {
                visitProcedureDec((ProcedureDec) dec);
            }
            else if (dec instanceof FacilityOperationDec) {
                visitFacilityOperationDec((FacilityOperationDec) dec);
            }
            else if (dec instanceof RepresentationDec) {
                visitRepresentationDec((RepresentationDec) dec);
            }
            else if (dec instanceof FacilityDec) {
                visitFacilityDec((FacilityDec) dec);
            }
        }
    }

    public void visitRepresentationDec(RepresentationDec dec) {

        AssertiveCode assertion = new AssertiveCode(myInstanceEnvironment);

        /* Assume Conventions, Show Correspondence */

        assertion.setFinalConfirm(getTrueVarExp());
        MathVarDec var = new MathVarDec(), concVar = new MathVarDec();
        ModuleDec tmp = getCurrentModuleDec();
        InitItem init = null;
        Exp constraint = null;

        Exp initExp = null;

        //ConceptModuleDec cmDec= (ConceptModuleDec)getCurrentModuleDec();
        Exp moduleLevelRequires = null;

        if (tmp instanceof ConceptModuleDec) {
            ConceptModuleDec cmDec = (ConceptModuleDec) tmp;
            Exp cMDRequirement = cmDec.getRequirement();
            if (cMDRequirement != null) {
                Location loc =
                        (Location) (cMDRequirement.getLocation()).clone();
                loc.setDetails("Requirement for " + cmDec.getName().getName());
                setLocation(cMDRequirement, loc);
                assertion.addAssume(cMDRequirement);
            }

            if (cmDec != null && cmDec.getFacilityInit() != null) {
                Exp cmdFacInitRequires = cmDec.getFacilityInit().getRequires();
                if (cmdFacInitRequires != null) {
                    Location loc =
                            (Location) (cmdFacInitRequires.getLocation())
                                    .clone();
                    loc.setDetails("Requirement for Facility Initialization "
                            + cmDec.getName().getName());
                    setLocation(cmdFacInitRequires, loc);
                    assertion.addAssume(cmdFacInitRequires);
                }

            }

            Iterator<Dec> decs = cmDec.getDecs().iterator();
            while (decs.hasNext()) {
                Dec tmpDec = decs.next();

                if (tmpDec instanceof TypeDec) {

                    init = ((TypeDec) tmpDec).getInitialization();
                    init.getLocation().setDetails(
                            "Initialization for"
                                    + ((TypeDec) tmpDec).getName().getName());

                    initExp = (Exp) init.getEnsures().clone();
                    if (initExp != null && initExp.getLocation() != null) {
                        Location loc =
                                (Location) (initExp.getLocation()).clone();
                        loc.setDetails("Initialization Ensures for  "
                                + tmpDec.getName().getName());
                        setLocation(initExp, loc);
                    }

                    constraint = ((TypeDec) tmpDec).getConstraint();
                    if (constraint != null) {
                        ((Location) constraint.getLocation().clone())
                                .setDetails("Constraint for "
                                        + ((TypeDec) tmpDec).getName()
                                                .getName());
                        //	assertion.addAssume(constraint)
                    }
                    var.setName(((TypeDec) tmpDec).getExemplar());
                    var.setTy(new NameTy(null, ((TypeDec) tmpDec).getName()));
                    addFreeVar(var, assertion);

                    concVar.setName(createPosSymbol("Conc_"
                            + ((TypeDec) tmpDec).getExemplar().toString()));
                    concVar.setTy(((TypeDec) tmpDec).getModel());
                }

            }
            moduleLevelRequires = cmDec.getRequirement();

        }

        VarExp name = new VarExp();
        name.setName(var.getName());

        VarExp concName = new VarExp();
        concName.setName(concVar.getName());
        concName.setType(getTypeFromTy(concVar.getTy()));

        Exp corr = (Exp) dec.getCorrespondence().clone();
        if (corr != null) {
            DotExp concDotExp = new DotExp();
            VarExp cName = new VarExp();
            cName.setName(createPosSymbol("Conc"));

            List<Exp> myList = new List<Exp>();
            myList.add(cName);
            myList.add(name);

            concDotExp.setSegments(myList);

            corr = replace(corr, concDotExp, concName);
            corr.getLocation().setDetails(
                    "Correspondence for " + dec.getName().getName());
        }

        if (!(corr instanceof EqualsExp)) {
            if (corr != null && constraint != null) {
                /*VarExp cName = new VarExp();
                cName.setName(createPosSymbol("Conc_" + name));*/
                constraint = replace(constraint, name, concName);
            }
            Exp constraints =
                    getConstraintsFromCurrentContext(assertion, true, false);
            Exp globalConstr = addGlobalsAsFreeVariables(null, assertion);
            if (globalConstr != null)
                assertion.addAssume(globalConstr);

            if (constraints != null)
                assertion.addAssume(constraints);

            if (dec.getCorrespondence() != null) {
                List<MathVarDec> lst = new List<MathVarDec>();
                lst.add(concVar);
                Location corrLoc = (Location) corr.getLocation().clone();
                corrLoc.setDetails("Correspondence Rule for "
                        + dec.getName().getName());
                if (constraint != null) {

                    assertion.setFinalConfirm(new QuantExp(corrLoc,
                            QuantExp.EXISTS, lst, null, InfixExp.formAndStmt(
                                    corr, constraint)));
                }
                else {
                    assertion.setFinalConfirm(new QuantExp(corrLoc,
                            QuantExp.EXISTS, lst, null, corr));
                }
            }
        }
        else {

            //   if(corr != null && constraint != null){
            //   	constraint = replace(constraint, name, ((EqualsExp)corr).getRight());
            //   }
            if (constraint == null) {
                constraint = getTrueVarExp();
            }
            if (corr != null && constraint != null) {
                /*VarExp cName = new VarExp();
                cName.setName(createPosSymbol("Conc_" + name));*/
                constraint = replace(constraint, name, concName);
            }

            Exp constraints =
                    getConstraintsFromCurrentContext(assertion, true, false);
            if (constraints != null)
                assertion.addAssume(constraints);

            VarExp cName = new VarExp();
            cName.setName(createPosSymbol("Conc_" + name));
            cName.setType(getTypeFromTy(var.getTy()));
            //This doesn't need a type, since we're just using it as a search
            //pattern

            constraint =
                    replace(constraint, cName, ((EqualsExp) corr).getRight());
            Location corrLoc = (Location) corr.getLocation().clone();
            corrLoc.setDetails("Correspondence Rule for "
                    + dec.getName().getName());
            constraint.setLocation(corrLoc);
            assertion.setFinalConfirm(constraint);

        }

        if (dec.getConvention() != null) {
            List<MathVarDec> lst = new List<MathVarDec>();
            lst.add(var);
            Exp convention = dec.getConvention();

            Location loc = (Location) (convention.getLocation()).clone();
            loc.setDetails("Convention for " + dec.getName().getName());
            setLocation(convention, loc);

            assertion.addAssume(convention);
        }

        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nCorrespondence Rule Applied: \n");
        VCBuffer.append(assertion.assertionToString());
        applyEBRules(assertion);

        AssertiveCode initializeAssert =
                new AssertiveCode(myInstanceEnvironment);

        addFreeVar(var, initializeAssert);
        addFreeVar(concVar, initializeAssert);

        if (init == null)
            return;

        Ty type = dec.getRepresentation();

        if (dec.getInitialization() != null) {
            visitInitItem(dec.getInitialization());

            /* Add Statements to Assertive Code */
            initializeAssert.addStatements(dec.getInitialization()
                    .getStatements());
        }

        if (type instanceof RecordTy) {
            RecordTy record = (RecordTy) type;
            Iterator<VarDec> myIterator = record.getFields().iterator();
            while (myIterator.hasNext()) {
                VarDec myvar = myIterator.next();

                initializeAssert.addVariableDec(new VarDec(createPosSymbol(var
                        .getName()
                        + "." + myvar.getName().toString()), myvar.getTy()));
            }
        }
        else {
            // This this is wrong
            initializeAssert.addVariableDec(new VarDec(var.getName(), var
                    .getTy()));
        }

        Exp globalConstr = addGlobalsAsFreeVariables(null, initializeAssert);
        if (globalConstr != null)
            initializeAssert.addAssume(globalConstr);

        initExp = replace(initExp, name, concName);
        Location myLoc;
        try {
            myLoc = dec.getInitialization().getLocation();
        }
        catch (Exception ex) {
            myLoc = dec.getName().getLocation();
        }

        myLoc.setDetails("Initialization Rule for " + dec.getName().getName());
        setLocation(initExp, myLoc);
        initializeAssert.setFinalConfirm(initExp);

        if (moduleLevelRequires != null) {
            initializeAssert.addAssume(moduleLevelRequires);
        }
        if (dec.getConvention() != null) {
            List<MathVarDec> lst = new List<MathVarDec>();
            lst.add(var);
            Exp convForInit = dec.getConvention();
            Location loc = convForInit.getLocation();

            loc.setDetails("Convention for " + dec.getName().getName());

            //	convForInit = replace( convForInit, name, concName );
            appendToLocation(convForInit, " generated by intialization rule");
            initializeAssert.addConfirm(convForInit);
        }

        if (dec.getCorrespondence() != null) {
            DotExp concDotExp = new DotExp();
            VarExp cName = new VarExp();
            cName.setName(createPosSymbol("Conc"));

            List<Exp> myList = new List<Exp>();
            myList.add(cName);
            myList.add(name);

            concDotExp.setSegments(myList);

            //List<MathVarDec> lst = new List<MathVarDec>();
            //lst.add(concVar);
            Exp corrForInit = (Exp) dec.getCorrespondence().clone();
            corrForInit.getLocation().setDetails(
                    "Correspondence for " + dec.getName().getName());
            corrForInit = replace(corrForInit, concDotExp, concName);
            initializeAssert.addAssume(corrForInit);
        }

        VCBuffer.append("\n_____________________ \n\n");
        VCBuffer.append("\n_____________________ \n");
        VCBuffer.append("\nInitialization Rule Applied: \n");
        VCBuffer.append(initializeAssert.assertionToString());
        initializationRule = true;
        applyEBRules(initializeAssert);
        initializationRule = false;
        VCBuffer.append("\n_____________________ \n");

    }

    public void visitShortFacilityModuleDec(ShortFacilityModuleDec dec) {
        table.beginModuleScope();
        //   System.out.println("Short Facility Module Dec: " +dec.getName());       
        table.endModuleScope();
    }

    public void visitTypeDec(TypeDec dec) {
        table.beginTypeScope();
        if (dec.getInitialization() != null) {
            visitInitItem(dec.getInitialization());
        }
        if (dec.getFinalization() != null) {
            visitFinalItem(dec.getFinalization());
        }
        table.endTypeScope();
    }

    /* Murali's first attempt to make a change to the compiler and 
     * fix the problem with uses list that includes concept names */
    public void visitUsesItem(UsesItem item) {
        //No changes here.
        ModuleID id = ModuleID.createFacilityID(item.getName());

        if (myInstanceEnvironment.contains(id)) {

            ModuleDec dec = myInstanceEnvironment.getModuleDec(id);
            if (dec instanceof ShortFacilityModuleDec) {
                ShortFacilityModuleDec sdec = (ShortFacilityModuleDec) (dec);
                FacilityDec fdec = sdec.getDec();
                PosSymbol cname = fdec.getConceptName();
                ModuleID cid = ModuleID.createConceptID(cname);
                if (!isInInterface) {
                    visitFacilityDec(sdec.getDec());
                }
                else {
                    String importStr =
                            formJavaImport(myInstanceEnvironment.getFile(cid));
                    if (!checkImportDup(importStr)) {
                        usesItemBuf.append(importStr);
                        importList.addUnique(importStr);
                    }
                }
            }
        }
        //Added the part below.
        ModuleID cid = ModuleID.createConceptID(item.getName());
        if (myInstanceEnvironment.contains(cid)) {
            String importStr =
                    formJavaImport(myInstanceEnvironment.getFile(cid));
            if (!checkImportDup(importStr)) {
                usesItemBuf.append(importStr);
                importList.addUnique(importStr);
            }
        }
    }
}
