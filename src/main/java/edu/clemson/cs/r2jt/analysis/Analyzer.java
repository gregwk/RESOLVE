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
 * Analyzer.java
 * 
 * The Resolve Software Composition Workbench Project
 * 
 * Copyright (c) 1999-2005
 * Reusable Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.analysis;

// replica
import edu.clemson.cs.r2jt.absyn.*;
import edu.clemson.cs.r2jt.collections.Iterator;
import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.collections.Stack;
import edu.clemson.cs.r2jt.data.Location;
import edu.clemson.cs.r2jt.data.Mode;
import edu.clemson.cs.r2jt.data.ModuleID;
import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.data.Symbol;
import edu.clemson.cs.r2jt.entry.*;
import edu.clemson.cs.r2jt.errors.ErrorHandler;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.init.Environment;
import edu.clemson.cs.r2jt.location.DefinitionLocator;
import edu.clemson.cs.r2jt.location.ProofLocator;
import edu.clemson.cs.r2jt.location.SymbolSearchException;
import edu.clemson.cs.r2jt.location.TheoremLocator;
import edu.clemson.cs.r2jt.location.VariableLocator;
import edu.clemson.cs.r2jt.proofchecking.ProofChecker;
import edu.clemson.cs.r2jt.proving.Prover;
import edu.clemson.cs.r2jt.sanitycheck.SanityCheckException;
import edu.clemson.cs.r2jt.scope.*;
import edu.clemson.cs.r2jt.translation.PrettyJavaTranslator;
import edu.clemson.cs.r2jt.type.*;
import edu.clemson.cs.r2jt.utilities.Flag;

import java.util.Collection;

/*
 * XXX : TO DO:
 * 
 * Currently, when building up entries for definitions, you only get one entry
 * for an inductive definition. Originally it overwrote part (i) with part (ii)
 * of the definition. I have a hacked in work around, but really they should
 * each be a separate entry (at least the way ProofChecker is doing things).
 * -HwS
 */

public class Analyzer extends ResolveConceptualVisitor {

    private final static String SECTION_NAME = "General";

    /**
     * <p>Turns on full mathematical typechecking.  Currently typechecking is
     * performed on a best-effort basis in many places to support the prover,
     * but this flag will cause it to be performed everywhere and turn off the
     * "best effort" part, generating errors when its not possible.</p>
     */
    private final static String FLAG_DESC_TYPECHECK =
            "Perform full mathematical typechecking.";

    /**
     * <p>Turns on a number of sanity checks, including checking to make sure
     * that function calls use the correct number of arguments, and realizations
     * use appropriate parameter modes given their specifications.</p>
     */
    private final static String FLAG_DESC_SANITYCHECK =
            "Check for common errors.";

    public static final Flag FLAG_TYPECHECK =
            new Flag(SECTION_NAME, "typecheck", FLAG_DESC_TYPECHECK);
    public static final Flag FLAG_SANITYCHECK =
            new Flag(SECTION_NAME, "sanitycheck", FLAG_DESC_SANITYCHECK);

    // Variables 

    private ErrorHandler err;

    //private Environment env;
    private final CompileEnvironment myInstanceEnvironment;

    private SymbolTable table;

    private TypeMatcher tm = new TypeMatcher();

    private MathExpTypeResolver metr;
    private ProgramExpTypeResolver petr;

    private ProofChecker pc;

    private int origErrorCount;

    private List<String> myEncounteredProcedures;

    //HwS - Filthy hack.  These global variables keeps a pointer to the symbol
    //table of the concept associated with a realization that is currently
    //being parsed.  The name associated with the concept is also stored so
    //that the table can be retrieved lazily.
    private PosSymbol myCurrentModuleName = null;
    private SymbolTable myAssociatedConceptSymbolTable = null;

    // Stack of WhileStmts used for building the changing list
    private Stack<WhileStmt> whileStmts = new Stack<WhileStmt>();

    // Constructors

    /**
     * Constructs an analyzer.
     */
    public Analyzer(SymbolTable table, CompileEnvironment instanceEnvironment) {
        myInstanceEnvironment = instanceEnvironment;
        //env = new Environment(instanceEnvironment);
        this.table = table;
        this.err = instanceEnvironment.getErrorHandler();
        this.metr = new MathExpTypeResolver(table, tm, instanceEnvironment);
        this.petr = new ProgramExpTypeResolver(table, instanceEnvironment);

        if (myInstanceEnvironment.flags.isFlagSet(ProofChecker.FLAG_PROOFCHECK)) {

            pc = new ProofChecker(table, tm, metr, myInstanceEnvironment);
            this.metr.setPC(pc);
        }
        origErrorCount = err.getErrorCount();

        myEncounteredProcedures = new List<String>();
        err = instanceEnvironment.getErrorHandler();
    }

    public static void setUpFlags() {

    }

    public MathExpTypeResolver getMathExpTypeResolver() {
        return metr;
    }

    public TypeMatcher getTypeMatcher() {
        return tm;
    }

    // Public Methods - Abstract Visit Methods

    public void visitModuleDec(ModuleDec dec) {
        //          TypeHolder holder = table.getTypeHolder();
        //          if (!holder.containsTypeB()) {
        //              String msg = mandMathTypeMessage();
        //              err.error(dec.getName().getLocation(), msg);
        //              return;
        //          }
        //          if (  dec instanceof ConceptBodyModuleDec ||
        //                dec instanceof EnhancementBodyModuleDec ||
        //                dec instanceof FacilityModuleDec) {
        //              if (  !holder.containsTypeN() ||
        //                    !holder.containsTypeBoolean() ||
        //                    !holder.containsTypeInteger()) {
        //                  String msg = mandProgTypeMessage();
        //                  err.error(dec.getName().getLocation(), msg);
        //                  return;
        //              }
        //          }
        dec.accept(this);
    }

    public void visitDec(Dec dec) {
        dec.accept(this);
    }

    public void visitStatement(Statement stmt) {
        stmt.accept(this);
    }

    //      public void visitExp(Exp exp) {
    //          if (exp == null) { return; }
    //          exp.accept(this);
    //      }

    public void visitModuleParameter(ModuleParameter par) {
        par.accept(this);
    }

    // Public Methods - Declarations

    // -----------------------------------------------------------
    // Module Declarations
    // -----------------------------------------------------------

    public void visitMathModuleDec(MathModuleDec dec) {
        table.beginModuleScope();
        visitModuleParameterList(dec.getParameters());
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_TYPECHECK)
                || myInstanceEnvironment.flags.isFlagSet(Prover.FLAG_PROVE)
                || myInstanceEnvironment.flags
                        .isFlagSet(Prover.FLAG_LEGACY_PROVE)) {

            visitDecList(dec.getDecs());
        }
        table.endModuleScope();
    }

    public void visitProofModuleDec(ProofModuleDec dec) {
        table.beginModuleScope();
        visitModuleParameterList(dec.getModuleParams());
        if (myInstanceEnvironment.flags.isFlagSet(FLAG_TYPECHECK)) {
            visitDecList(dec.getDecs());
        }
        table.endModuleScope();
    }

    public void visitConceptModuleDec(ConceptModuleDec dec) {
        myCurrentModuleName = dec.getName();
        table.beginModuleScope();
        visitModuleParameterList(dec.getParameters());
        visitAssertion(dec.getRequirement());
        visitAssertionList(dec.getConstraints());
        if (dec.getFacilityInit() != null) {
            visitInitItem(dec.getFacilityInit());
        }
        if (dec.getFacilityFinal() != null) {
            visitFinalItem(dec.getFacilityFinal());
        }

        visitDecList(dec.getDecs());

        table.endModuleScope();
        myCurrentModuleName = null;
    }

    public void visitEnhancementModuleDec(EnhancementModuleDec dec) {
        table.beginModuleScope();
        visitModuleParameterList(dec.getParameters());
        visitAssertion(dec.getRequirement());

        visitDecList(dec.getDecs());

        table.endModuleScope();
    }

    public void visitConceptBodyModuleDec(ConceptBodyModuleDec dec) {
        myCurrentModuleName = dec.getName();
        //We're going to need this deeper in the tree, so save it to a global
        //variable.  Would like to do this functionally, but I don't want to
        //break things by changing public method type signatures.  -HwS
        ModuleID id = ModuleID.createConceptID(dec.getConceptName());
        myAssociatedConceptSymbolTable =
                myInstanceEnvironment.getSymbolTable(id);

        table.beginModuleScope();
        visitModuleParameterList(dec.getParameters());
        visitAssertion(dec.getRequires());
        visitAssertionList(dec.getConventions());
        visitAssertionList(dec.getCorrs());
        if (dec.getFacilityInit() != null) {
            visitInitItem(dec.getFacilityInit());
        }
        if (dec.getFacilityFinal() != null) {
            visitFinalItem(dec.getFacilityFinal());
        }

        visitDecList(dec.getDecs());

        table.endModuleScope();
        myCurrentModuleName = null;
    }

    public void visitEnhancementBodyModuleDec(EnhancementBodyModuleDec dec) {

        myCurrentModuleName = dec.getName();
        //We're going to need this deeper in the tree, so save it to a global
        //variable.  Would like to do this functionally, but I don't want to
        //break things by changing public method type signatures.  -HwS
        ModuleID id =
                ModuleID.createEnhancementID(dec.getEnhancementName(), dec
                        .getConceptName());
        myAssociatedConceptSymbolTable =
                myInstanceEnvironment.getSymbolTable(id);

        table.beginModuleScope();
        // check instantiation of enhancement bodies
        //visitEnhancementBodyItemList(dec.getEnhancementBodies());
        visitModuleParameterList(dec.getParameters());
        visitAssertion(dec.getRequires());
        visitAssertionList(dec.getConventions());
        visitAssertionList(dec.getCorrs());
        if (dec.getFacilityInit() != null) {
            visitInitItem(dec.getFacilityInit());
        }
        if (dec.getFacilityFinal() != null) {
            visitFinalItem(dec.getFacilityFinal());
        }

        visitDecList(dec.getDecs());

        table.endModuleScope();
        myCurrentModuleName = null;

    }

    public void visitFacilityModuleDec(FacilityModuleDec dec) {
        myCurrentModuleName = dec.getName();
        table.beginModuleScope();
        if (dec.getFacilityInit() != null) {
            visitInitItem(dec.getFacilityInit());
        }
        if (dec.getFacilityFinal() != null) {
            visitFinalItem(dec.getFacilityFinal());
        }

        visitDecList(dec.getDecs());

        table.endModuleScope();
        myCurrentModuleName = null;
    }

    public void visitShortFacilityModuleDec(ShortFacilityModuleDec dec) {
        table.beginModuleScope();
        visitFacilityDec(dec.getDec());
        table.endModuleScope();
    }

    // -----------------------------------------------------------
    // Math Declarations
    // -----------------------------------------------------------

    private Boolean isDecAVar(DefinitionDec dec) {
        if (dec.getParameters() != null) {
            List<MathVarDec> params1 = dec.getParameters();
            Iterator<MathVarDec> i = params1.iterator();
            if (!i.hasNext()
                    && !(getMathType(dec.getReturnTy()) instanceof FunctionType)) {
                if (dec.getDefinition() == null && dec.getBase() == null
                        && dec.getHypothesis() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    //    private List<Type> convertMathVarDecsToTypes(List<MathVarDec> decParams) {
    //    	List<Type> params = new List<Type>();
    //    	Iterator<MathVarDec> paramsIt = decParams.iterator();
    //    	while(paramsIt.hasNext()) {
    //    		MathVarDec currentParam = paramsIt.next();
    //    		Type currentType = getMathType(currentParam.getTy());
    //    		params.add(currentType);
    //    	}
    //    	return params;
    //    }

    private void matchExpressions(Exp exp1, Type returnType, boolean equalCase,
            boolean strict) throws TypeResolutionException {

        if (equalCase)
            metr.setEqlCase();
        Type t = metr.getMathExpType(exp1);
        if (equalCase)
            metr.unsetEqlCase();
        matchTypes(exp1.getLocation(), t, returnType, strict);
    }

    public void visitDefinitionDec(DefinitionDec dec) {

        /*if (dec.getName().getName().equals("N") && !env.debugOff()) {
        	System.out.println("analysis.Analyzer.visitDefinitionDec found N");
        }*/

        Type returnType = getDefinitionReturnType(dec);

        table.beginDefinitionScope();

        doBaseStuff(dec, returnType);

        doHypothesisStuff(dec, returnType);

        if (dec.getDefinition() != null) {
            // If the defn. is null, don't typecheck
            try {
                matchExpressions(dec.getDefinition(), returnType, false, true);
                storeValue(dec, dec.getDefinition());
            }
            catch (TypeResolutionException trex) {
                // Error already reported; do nothing
            }
        }
        table.endDefinitionScope();
    }

    private Exp unwrapQuantExp(Exp e) {
        if (e instanceof QuantExp) {
            return unwrapQuantExp(((QuantExp) e).getBody());
        }
        return e;
    }

    private boolean checkSelfReference(String name, Exp exp) {
        Exp newExp = unwrapQuantExp(exp);
        if (newExp instanceof IfExp) {
            return (findReference(name, ((IfExp) newExp).getThenclause()))
                    && ((findReference(name, ((IfExp) newExp).getElseclause())));
        }
        else if (newExp instanceof AlternativeExp) {
            Iterator<AltItemExp> it =
                    ((AlternativeExp) newExp).getAlternatives().iterator();
            while (it.hasNext()) {
                if (findReference(name, it.next().getAssignment()) == false)
                    return false;
            }
            return true;
        }
        return findReference(name, newExp);
    }

    private boolean findReference(String name, Exp exp) {
        if (exp == null || !(exp instanceof EqualsExp)) {
            return false;
        }
        if (((EqualsExp) exp).getOperator() != EqualsExp.EQUAL) {
            return false;
        }
        Exp LHS = ((EqualsExp) exp).getLeft();
        if (LHS instanceof VarExp) {
            return (name.equals(((VarExp) LHS).getName().getName()));
        }
        else if (LHS instanceof FunctionExp) {
            return (name.equals(((FunctionExp) LHS).getName().getName()));
        }
        else if (LHS instanceof InfixExp) {
            return (name.equals(((InfixExp) LHS).getOpName().getName()));
        }
        else if (LHS instanceof PrefixExp) {
            return (name.equals(((PrefixExp) LHS).getSymbol().getName()));
        }
        else if (LHS instanceof OutfixExp) {
            return matchOutfixOp(name, (OutfixExp) LHS);
        }
        return false;
    }

    private boolean matchOutfixOp(String name, OutfixExp exp) {
        return ((name.equals("<_>") && exp.getOperator() == OutfixExp.ANGLE)
                || (name.equals("<<_>>") && exp.getOperator() == OutfixExp.DBL_ANGLE)
                || (name.equals("[_]") && exp.getOperator() == OutfixExp.SQUARE)
                || (name.equals("[[_]]") && exp.getOperator() == OutfixExp.DBL_SQUARE)
                || (name.equals("|_|") && exp.getOperator() == OutfixExp.BAR) || (name
                .equals("||_||") && exp.getOperator() == OutfixExp.DBL_BAR));
    }

    private DefinitionEntry getDefinitionEntry(DefinitionDec d) {
        DefinitionEntry retval;

        DefinitionLocator locator = new DefinitionLocator(table, true, tm, err);
        PosSymbol searchKey = d.getName();
        String msg = "definition " + searchKey;

        try {
            retval = locator.locateDefinition(searchKey);
        }
        catch (SymbolSearchException e) {
            err.error("Could not located " + msg);
            retval = null;
        }

        return retval;
    }

    private void storeValue(Dec dec, Exp value) {
        if (myInstanceEnvironment.flags.isFlagSet(ProofChecker.FLAG_PROOFCHECK)) {

            String msg = null;
            try {
                if (dec instanceof DefinitionDec) {
                    DefinitionDec dec1 = ((DefinitionDec) dec);
                    DefinitionLocator locator =
                            new DefinitionLocator(table, true, tm, err);
                    PosSymbol searchKey = dec1.getName();
                    msg = "definition " + searchKey;
                    DefinitionEntry entry = locator.locateDefinition(searchKey);
                    entry.setValue(value);
                }
                else if (dec instanceof MathAssertionDec) {
                    MathAssertionDec dec1 = ((MathAssertionDec) dec);
                    TheoremLocator locator = new TheoremLocator(table, tm, err);
                    PosSymbol searchKey = dec1.getName();
                    int kind = dec1.getKind();
                    switch (kind) {
                    case MathAssertionDec.AXIOM:
                        msg = "axiom ";
                        break;
                    case MathAssertionDec.COROLLARY:
                        msg = "corollary ";
                        break;
                    case MathAssertionDec.LEMMA:
                        msg = "lemma ";
                        break;
                    case MathAssertionDec.PROPERTY:
                        msg = "property ";
                        break;
                    case MathAssertionDec.THEOREM:
                        msg = "theorem ";
                        break;
                    default:
                        msg = "math assertion ";
                        break;
                    }
                    msg += searchKey;
                    TheoremEntry entry = locator.locateTheorem(searchKey);
                    entry.setValue(value);
                }
                else if (dec instanceof ProofDec) {
                    ProofDec dec1 = ((ProofDec) dec);
                    ProofLocator locator = new ProofLocator(table, tm, err);
                    PosSymbol searchKey = dec1.getName();
                    msg = "proof " + searchKey;
                    ProofEntry entry = locator.locateProof(searchKey);
                    entry.setValue(value);
                }
                else {
                    return;
                }
            }
            catch (SymbolSearchException ex) {
                err.error("Could not locate the " + msg);
            }
        }
    }

    public void matchTypes(Location loc, Type t1, Type t2, boolean strict) {
        if (t1 == null || t2 == null)
            return;
        try {
            if (!(metr.matchTypes(loc, t1, t2, true, strict))) {
                String msg =
                        expectedDiffTypeMessage(t2.toString(), t1.toString());
                err.error(loc, msg);
            }
        }
        catch (TypeResolutionException e) {
            String msg = expectedDiffTypeMessage(t2.toString(), t1.toString());
            err.error(loc, msg);
        }
    }

    public void visitMathAssertionDec(MathAssertionDec dec) {
        table.beginExpressionScope();
        MathExpTypeResolver metr =
                new MathExpTypeResolver(table, tm, myInstanceEnvironment);
        try {
            Type t1 = metr.getMathExpType(dec.getAssertion());
            Type t2 = BooleanType.INSTANCE;
            matchTypes(dec.getAssertion().getLocation(), t1, t2, true);
            storeValue(dec, dec.getAssertion());
        }
        catch (TypeResolutionException trex) {

        }
        finally {
            table.endExpressionScope();
        }
    }

    public void visitMathTypeDec(MathTypeDec dec) {
        ;
    }

    public void visitMathTypeFormalDec(MathTypeFormalDec dec) {
        ;
    }

    // -----------------------------------------------------------
    // Type Declarations
    // -----------------------------------------------------------

    public void visitFacilityTypeDec(FacilityTypeDec dec) {
        table.beginTypeScope();
        visitAssertion(dec.getConvention());
        if (dec.getInitialization() != null) {
            visitInitItem(dec.getInitialization());
        }
        if (dec.getFinalization() != null) {
            visitFinalItem(dec.getFinalization());
        }
        table.endTypeScope();
    }

    public void visitTypeDec(TypeDec dec) {
        table.beginTypeScope();
        visitAssertion(dec.getConstraint());
        if (dec.getInitialization() != null) {
            visitInitItem(dec.getInitialization());
        }
        if (dec.getFinalization() != null) {
            visitFinalItem(dec.getFinalization());
        }
        table.endTypeScope();
    }

    public void visitRepresentationDec(RepresentationDec dec) {
        table.beginTypeScope();
        visitAssertion(dec.getConvention());
        visitAssertion(dec.getCorrespondence());
        if (dec.getInitialization() != null) {
            visitInitItem(dec.getInitialization());
        }
        if (dec.getFinalization() != null) {
            visitFinalItem(dec.getFinalization());
        }
        table.endTypeScope();
    }

    // -----------------------------------------------------------
    // Operation Declarations
    // -----------------------------------------------------------

    public void visitFacilityOperationDec(FacilityOperationDec dec) {

        table.beginOperationScope();
        visitAssertion(dec.getRequires());
        visitAssertion(dec.getEnsures());
        table.beginProcedureScope();
        table.bindProcedureTypeNames();
        visitProgressMetric(dec.getDecreasing());
        visitFacilityDecList(dec.getFacilities());
        visitStatementList(dec.getStatements());
        table.endProcedureScope();
        table.endOperationScope();
    }

    public void visitOperationDec(OperationDec dec) {

        table.beginOperationScope();
        visitAssertion(dec.getRequires());
        visitAssertion(dec.getEnsures());
        table.endOperationScope();
    }

    public void visitProcedureDec(ProcedureDec dec) {

        //Perform procedure parameter sanity checking.
        // remove: these same checks are perfomed in the SanityCheck
        //	sanityCheckProcedure(dec);

        table.beginOperationScope();
        table.beginProcedureScope();
        table.bindProcedureTypeNames();

        //This was added to do sanity check work, but this work is now handled
        //by sanityCheckProcedureArguments:
        //visitParameterVarDecList(dec.getParameters());
        visitProgressMetric(dec.getDecreasing());
        visitFacilityDecList(dec.getFacilities());
        visitStatementList(dec.getStatements());
        table.endProcedureScope();
        table.endOperationScope();
    }

    // -----------------------------------------------------------
    // Proof Declarations
    // -----------------------------------------------------------

    private void visitProofExp(Exp e) {
        // type check
        visitAssertion(e);
        // proof check
        if (origErrorCount != err.getErrorCount())
            return;

        if (myInstanceEnvironment.flags.isFlagSet(ProofChecker.FLAG_PROOFCHECK)) {

            pc.proofcheck(e);
        }
    }

    public void visitProofDec(ProofDec dec) {
        table.beginProofScope();
        table.bindProofTypeNames();
        Iterator<Exp> it = dec.getStatements().iterator();
        Exp e = null;
        //    	try {
        while (it.hasNext()) {
            e = it.next();
            if (e instanceof ProofDefinitionExp) {
                visitProofDefinitionExp((ProofDefinitionExp) e);
            }
            else {
                visitProofExp(e);
            }
        }
        if (dec.getBaseCase().size() != 0) {
            it = dec.getBaseCase().iterator();
            while (it.hasNext()) {
                e = it.next();
                if (e instanceof ProofDefinitionExp) {
                    visitProofDefinitionExp((ProofDefinitionExp) e);
                }
                else {
                    visitProofExp(e);
                }
            }
        }
        if (dec.getInductiveCase().size() != 0) {
            it = dec.getInductiveCase().iterator();
            while (it.hasNext()) {
                e = it.next();
                if (e instanceof ProofDefinitionExp) {
                    visitProofDefinitionExp((ProofDefinitionExp) e);
                }
                else {
                    visitProofExp(e);
                }
            }
        }
        //    	}
        //    	catch(TypeResolutionException trex) {
        //    		// Error already handled; do nothing
        //    	}
        table.endProofScope();
    }

    public void visitProofDefinitionExp(ProofDefinitionExp dec) {
        visitDefinitionDec(dec.getExp());
    }

    // -----------------------------------------------------------
    // Facility Declarations
    // -----------------------------------------------------------

    public void visitFacilityDec(FacilityDec dec) {

        sanityCheckFacilityDeclarationParameters(dec);

        // check instantiation AND arguments that are
        // program expressions.
        /*ConceptModuleDec cDec = null;
        ConceptBodyModuleDec bDec = null;
        ModuleID cid = ModuleID.createConceptID(dec.getConceptName());
        if (myInstanceEnvironment.contains(cid))
            cDec = (ConceptModuleDec)myInstanceEnvironment.getModuleDec(cid);
        else {
            String msg = facDecErrorMessage(dec.getName().getName());
            err.error(dec.getName().getLocation(), msg);
            return;
        };
        ModuleID bid = ModuleID.createConceptBodyID(dec.getBodyName(),
                dec.getConceptName());
        if (myInstanceEnvironment.contains(bid))
            bDec = (ConceptBodyModuleDec)myInstanceEnvironment.getModuleDec(cid);
        else {
            String msg = facDecErrorMessage(dec.getName().getName());
            err.error(dec.getName().getLocation(), msg);
            return;
        }*/

    }

    // -----------------------------------------------------------
    // Module Parameter Declarations
    // -----------------------------------------------------------

    public void visitConceptTypeParamDec(ConceptTypeParamDec dec) {
        ;
    }

    public void visitConstantParamDec(ConstantParamDec dec) {
        ;
    }

    public void visitRealizationParamDec(RealizationParamDec dec) {
    //FIX: Postpone this till later, since it doesn't even
    //     work like we want it to.
    }

    // Public Methods - Non-declarative Constructs

    public void visitAffectsItem(AffectsItem item) {
    // locate variables - make sure they are permitted here.
    }

    public void visitChoiceItem(ChoiceItem item) {
    // figure out what this does
    }

    public void visitConditionItem(ConditionItem item) {
        visitCondition(item.getTest());
        visitStatementList(item.getThenclause());
    }

    public void visitEnhancementBodyItem(EnhancementBodyItem item) {
    // check argument lists
    }

    public void visitEnhancementItem(EnhancementItem item) {
    // check argument lists
    }

    public void visitFinalItem(FinalItem item) {

        table.beginOperationScope();
        visitAssertion(item.getRequires());
        visitAssertion(item.getEnsures());
        table.beginProcedureScope();
        visitFacilityDecList(item.getFacilities());
        visitStatementList(item.getStatements());
        table.endProcedureScope();
        table.endOperationScope();
    }

    public void visitFunctionArgList(FunctionArgList list) {
    //visitExpList(list.getArguments());
    }

    public void visitInitItem(InitItem item) {
        table.beginOperationScope();
        visitAssertion(item.getRequires());
        visitAssertion(item.getEnsures());
        table.beginProcedureScope();
        visitFacilityDecList(item.getFacilities());
        visitStatementList(item.getStatements());
        table.endProcedureScope();
        table.endOperationScope();
    }

    /* Called instead of visitAssertion */
    public void visitMaintainingClause(Exp exp) {
        //metr.setMaintainingClause();
        visitAssertion(exp);
        //metr.unsetMaintainingClause();
    }

    public void visitRenamingItem(RenamingItem item) {
    // check if item renamed is in the facility ?
    }

    public void visitUsesItem(UsesItem item) {
        ;
    }

    // Statements

    public void visitCallStmt(CallStmt stmt) {

        //ProgramExpTypeResolver petr = new ProgramExpTypeResolver(table);
        TypeMatcher matcher = new TypeMatcher();
        Location loc =
                (stmt.getQualifier() == null) ? stmt.getName().getLocation()
                        : stmt.getQualifier().getLocation();
        ProgramFunctionExp exp =
                new ProgramFunctionExp(loc, stmt.getQualifier(),
                        stmt.getName(), stmt.getArguments());

        try {
            Type rtype = petr.getProgramExpType(exp);
            if (!matcher.programMatches(new VoidType(), rtype)) {
                String msg = expectedProcMessage();
                err.error(stmt.getName().getLocation(), msg);
            }
        }
        catch (TypeResolutionException trex) {
            err.error(exp.getLocation(),
                    "Cannot locate an operation with this name.");
            // do nothing - the error was already reported
        }
    }

    /*
     * XXX : Why is a FuncAssignStmt different from an ordinary AssignStmt???
     *               -HwS
     */
    public void visitFuncAssignStmt(FuncAssignStmt stmt) {

        //XXX : This is the only way to get the internal SemanticExp set for
        //      a VariableDotExp and so it must happen regardless of whether
        //      or not we're typechecking if we want to translate.  This seems
        //      like an odd restriction.  Can setting up SemanticExps be moved
        //      to a more rational location?  -HwS
        //ProgramExpTypeResolver petr = new ProgramExpTypeResolver(table);
        TypeMatcher matcher = new TypeMatcher();
        try {
            Type vtype = petr.getVariableExpType(stmt.getVar());

            Type atype = petr.getProgramExpType(stmt.getAssign()); // Problem is here

            if (myInstanceEnvironment.flags.isFlagSet(FLAG_TYPECHECK)) {
                if (!matcher.programMatches(vtype, atype)) {
                    Location loc = stmt.getAssign().getLocation();
                    String msg =
                            expectedDiffTypeMessage(vtype.getRelativeName(loc),
                                    atype.getRelativeName(loc));
                    err.error(loc, msg);
                    return;
                }
            }
            if (!myInstanceEnvironment.flags
                    .isFlagSet(PrettyJavaTranslator.FLAG_TRANSLATE)) {
                // Making sure that we do not have something of VariableExp on the right hand side
                // in a function assignment. - YS
                if (petr.isVariable(stmt.getAssign())) {
                    String msg =
                            "Right hand side of the function assignment cannot be a variable expression! ";
                    err.error(stmt.getAssign().getLocation(), msg);
                    throw new TypeResolutionException();
                }

                // Making sure that for any entry replica call for arrays, we have a replica function
                // defined for that type. - YS
                if (stmt.getAssign() instanceof ProgramParamExp) {
                    stmt.getAssign().accept(this);
                }
            }
        }
        catch (TypeResolutionException trex) {
            // do nothing - the error was already reported
            // err.error(stmt.getLocation(), "TypeResolutionException");            
        }
    }

    public void visitIfStmt(IfStmt stmt) {

        visitCondition(stmt.getTest());
        visitStatementList(stmt.getThenclause());
        visitConditionItemList(stmt.getElseifpairs());
        visitStatementList(stmt.getElseclause());
    }

    public void visitIterateStmt(IterateStmt stmt) {
        table.beginStatementScope();
        visitMaintainingClause(stmt.getMaintaining());
        visitProgressMetric(stmt.getDecreasing());
        visitStatementList(stmt.getStatements());
        table.endStatementScope();
    }

    public void visitIterateExitStmt(IterateExitStmt stmt) {
        visitCondition(stmt.getTest());
        visitStatementList(stmt.getStatements());
    }

    public void visitMemoryStmt(MemoryStmt stmt) {
        ;
    }

    //      public void visitSelectionStmt(SelectionStmt stmt) {
    //          visitVariableExp(stmt.getVar());
    //          visitChoiceItemList(stmt.getWhenpairs());
    //          visitStatementList(stmt.getDefaultclause());
    //      }

    public void visitSwapStmt(SwapStmt stmt) {
        //XXX : This is the only way to get the internal SemanticExp set for
        //      a VariableDotExp and so it must happen regardless of whether
        //      or not we're typechecking if we want to translate.  This seems
        //      like an odd restriction.  Can setting up SemanticExps be moved
        //      to a more rational location?  -HwS
        Type vtype1 = null;
        Type vtype2 = null;
        //ProgramExpTypeResolver petr = new ProgramExpTypeResolver(table);
        try {
            vtype1 = petr.getVariableExpType(stmt.getLeft());
            vtype2 = petr.getVariableExpType(stmt.getRight());

            if (myInstanceEnvironment.flags.isFlagSet(FLAG_TYPECHECK)) {
                TypeMatcher matcher = new TypeMatcher();

                if (!matcher.programMatches(vtype1, vtype2)) {
                    Location loc = stmt.getRight().getLocation();
                    String msg =
                            expectedDiffTypeMessage(
                                    vtype1.getRelativeName(loc), vtype2
                                            .getRelativeName(loc));
                    err.error(loc, msg);
                }
            }
        }
        catch (TypeResolutionException e) {
            err.error(stmt.getLocation(), "TypeResolutionException");
            //do nothing - the error was already reported
        }
    }

    public void visitWhileStmt(WhileStmt stmt) {

        table.beginStatementScope();
        visitCondition(stmt.getTest());
        visitMaintainingClause(stmt.getMaintaining());
        visitProgressMetric(stmt.getDecreasing());

        // We will now dynamically build the changing statement for the user.
        whileStmts.push(stmt);
        visitStatementList(stmt.getStatements());
        whileStmts.pop();
        table.endStatementScope();
    }

    // Public Methods - Expressions

    /*
     * Expressions are visited for the purpose of getting their
     * types. Therefore, expressions are analyzed in the
     * ExpTypeResolver, a class that extends TypeResolutionVisitor,
     * whose signature differs from ResolveConceptualVisitor in that
     * it returns a Type or throws a TypeResolutionException if a type
     * cannot be determined.
     */

    // Private Methods

    /*
     * XXX : I have no idea what the heck this code is meant to do.  I don't 
     * know what a definition's "base" is, just moving it here from
     * visitDefinitionDec to improve clarity. -HwS
     */
    private void doBaseStuff(DefinitionDec dec, Type definitionReturnType) {
        if (dec.getBase() != null) {
            try {
                matchExpressions(dec.getBase(), BooleanType.INSTANCE, true,
                        true);

                if (!checkSelfReference(dec.getName().getName(), dec.getBase())) {
                    String msg = noSelfReference();
                    err.error(dec.getBase().getLocation(), msg);
                }

                DefinitionEntry definition = getDefinitionEntry(dec);

                if (definition != null) {
                    definition.setBaseDefinition(dec.getBase());
                    //storeValue(dec, dec.getBase());
                }
            }
            catch (TypeResolutionException trex) {
                metr.unsetEqlCase();
                // Error already reported; do nothing
            }
        }
    }

    /*
     * XXX : Again, no idea what this means.  Moved out of visitDefinitionDec.
     *          -HwS
     */
    private void doHypothesisStuff(DefinitionDec dec, Type definitionReturnType) {
        if (dec.getHypothesis() != null) {
            try {
                matchExpressions(dec.getHypothesis(), BooleanType.INSTANCE,
                        true, true);

                if (!checkSelfReference(dec.getName().getName(), dec
                        .getHypothesis())) {

                    String msg = noSelfReference();
                    err.error(dec.getHypothesis().getLocation(), msg);
                }
                storeValue(dec, dec.getHypothesis());
            }
            catch (TypeResolutionException trex) {
                metr.unsetEqlCase();
                // Error already reported; do nothing
            }
        }
    }

    /**
     * A helper function for visitDefinitionDec.
     * 
     * Takes a DefinitionDec and returns the math type of the returned by that
     * definition.  
     * 
     * XXX : I don't really understand this one, just separating it
     * out from visitDefinitionDec for clarity.  -HwS
     */
    private Type getDefinitionReturnType(DefinitionDec dec) {
        Type returnType = null;
        //List<Type> params = convertMathVarDecsToTypes(dec.getParameters());

        // Set the return type of the definition (must be matched with contents
        //     of the def.
        if (isDecAVar(dec)) {
            // If the definition has no params, look it up as a VarEntry
            try {
                VariableLocator vlocator = new VariableLocator(table, err);
                VarEntry vEntry = vlocator.locateMathVariable(dec.getName());
                returnType = vEntry.getType();
            }
            catch (SymbolSearchException ssex) {
                // Error already reported by VariableLocator; do nothing
            }
        }
        else if (getMathType(dec.getReturnTy()) instanceof FunctionType) {
            FunctionType ftype =
                    (FunctionType) (getMathType(dec.getReturnTy()));
            returnType = ftype.getRange();
        }
        else {
            returnType = getMathType(dec.getReturnTy());
        }

        return returnType;
    }

    // -----------------------------------------------------------
    // Symbol Table searching methods
    // -----------------------------------------------------------
    private DefinitionEntry getDefinitionByName(Symbol name)
            throws NotFoundException {
        DefinitionEntry retval;

        ModuleScope curModuleScope = table.getModuleScope();
        if (curModuleScope.containsDefinition(name)) {
            retval = curModuleScope.getDefinition(name);
        }
        else {
            throw new NotFoundException("Couldn't find definition: " + name);
        }

        return retval;
    }

    private VarEntry getVariableByName(Symbol name) throws NotFoundException {
        VarEntry retval;

        ModuleScope curModuleScope = table.getModuleScope();
        if (curModuleScope.containsVariable(name)) {
            retval = curModuleScope.getVariable(name);
        }
        else {
            throw new NotFoundException("Couldn't find variable: " + name);
        }

        return retval;
    }

    // -----------------------------------------------------------
    // Iterative Visit Methods
    // -----------------------------------------------------------

    private void visitAssertionList(List<Exp> exps) {
        Iterator<Exp> i = exps.iterator();
        while (i.hasNext()) {
            visitAssertion(i.next());
        }
    }

    private void visitChoiceItemList(List<ChoiceItem> items) {
        Iterator<ChoiceItem> i = items.iterator();
        while (i.hasNext()) {
            visitChoiceItem(i.next());
        }
    }

    private void visitConditionItemList(List<ConditionItem> items) {
        Iterator<ConditionItem> i = items.iterator();
        while (i.hasNext()) {
            visitConditionItem(i.next());
        }
    }

    private void visitConditionList(List<ProgramExp> exps) {
        Iterator<ProgramExp> i = exps.iterator();
        while (i.hasNext()) {
            visitCondition(i.next());
        }
    }

    private void visitDecList(List<Dec> decs) {
        Iterator<Dec> i = decs.iterator();
        while (i.hasNext()) {
            visitDec(i.next());
        }
    }

    //      private void visitExpList(List<Exp> exps) {
    //          Iterator<Exp> i = exps.iterator();
    //          while (i.hasNext()) {
    //              visitExp(i.next());
    //          }
    //      }

    private void visitFacilityDecList(List<FacilityDec> decs) {
        Iterator<FacilityDec> i = decs.iterator();
        while (i.hasNext()) {
            visitFacilityDec(i.next());
        }
    }

    private void visitModuleParameterList(List<ModuleParameter> pars) {
        Iterator<ModuleParameter> i = pars.iterator();
        while (i.hasNext()) {
            visitModuleParameter(i.next());
        }
    }

    private void visitStatementList(List<Statement> stmts) {
        if (stmts != null) {
            Iterator<Statement> i = stmts.iterator();
            while (i.hasNext()) {
                visitStatement(i.next());
            }
        }
    }

    private void visitUsesItemList(List<UsesItem> items) {
        Iterator<UsesItem> i = items.iterator();
        while (i.hasNext()) {
            visitUsesItem(i.next());
        }
    }

    // -----------------------------------------------------------
    // Expression Related Methods
    // -----------------------------------------------------------

    /* Skipping analysis of math expressions for the time being. */
    //    private void visitExpOfType(Exp exp, Type type) { ; }

    //    private void visitAssertion(Exp exp) { }

    /**
     * Visits all ProgramOpExp subexpressions
     */
    public void visitProgramOpExp(ProgramOpExp exp) {
        Iterator<Exp> it = exp.getSubExpressions().iterator();
        while (it.hasNext()) {
            it.next().accept(this);
        }
    }

    /**
     * Visits ProgramParamExp
     */
    public void visitProgramParamExp(ProgramParamExp exp) {
        /* Check if the call statement is a Entry_Replica call */
        if (exp.getName().getName().equals("Entry_Replica")) {
            /* List of arguments */
            List<ProgramExp> argList = exp.getArguments();

            try {
                /* Call the ProgramExp type checker to go find a Replica operation */
                Type xtype = petr.getProgramExpType(argList.get(0));
                petr.checkReplica(argList.get(0).getLocation(), xtype);
            }
            catch (TypeResolutionException e) {
                /* Catch the error and print the message */
                String msg = "Illegal array access, use swap instead.";
                err.error(exp.getLocation(), msg);
            }
        }
    }

    private void visitAssertion(Exp exp) {
        if (exp != null) {
            //For the moment, determine types on a best-effort basis
            boolean errorState = err.getIgnore();
            err.setIgnore(true);

            try {
                Type t1 = metr.getMathExpType(exp);
                exp.setType(t1);

                if (myInstanceEnvironment.flags.isFlagSet(FLAG_TYPECHECK)) {
                    //Make sure the assertion is a boolean expression
                    Type B = BooleanType.INSTANCE;
                    matchTypes(exp.getLocation(), B, t1, false);
                }
            }
            catch (TypeResolutionException trex) {
                //System.out.println("Couldn't determine a type inside this assertion:");
                // The code below was added to aid in troubleshooting
                // TypeResolutionExceptions. It can be safely uncommented
                // for troubleshooting purposes.
                /*try {
                	Type t1 = metr.getMathExpType(exp);
                }
                catch (TypeResolutionException trex2) {
                	int i = 1;
                }*/
                throw new RuntimeException(trex);
            }

            err.setIgnore(errorState);
        }
    }

    private void visitProgressMetric(Exp exp) {
        if (exp != null) {
            //For the moment, determine types on a best-effort basis
            boolean errorState = err.getIgnore();
            err.setIgnore(true);

            try {
                Type t1 = metr.getMathExpType(exp);
                exp.setType(t1);
            }
            catch (TypeResolutionException trex) {}

            err.setIgnore(errorState);
        }
    }

    private void visitProgramExpOfType(ProgramExp exp, Type type) {
        //ProgramExpTypeResolver petr = new ProgramExpTypeResolver(table);
        TypeMatcher matcher = new TypeMatcher();
        try {
            Type etype = petr.getProgramExpType(exp);
            if (!matcher.programMatches(etype, type)) {
                Location loc = exp.getLocation();
                String msg =
                        expectedDiffTypeMessage(etype.getRelativeName(loc),
                                type.getRelativeName(loc));
                err.error(loc, msg);
            }
            exp.setType(etype);
        }
        catch (TypeResolutionException trex) {
            err.error(exp.getLocation(), "TypeResolutionException");
            // do nothing - the error was already reported
        }
    }

    private void visitCondition(ProgramExp exp) {
        TypeHolder holder = table.getTypeHolder();
        if (holder.containsTypeBoolean()) {
            visitProgramExpOfType(exp, holder.getTypeBoolean());
        }
        else {
            String msg = cantFindType("Std_Boolean_Fac.Boolean");
            err.error(exp.getLocation(), msg);
        }

        // Making sure that for any entry replica call for arrays, we have a replica function
        // defined for that type. - YS
        exp.accept(this);
    }

    public Type getMathType(Ty ty) {
        TypeConverter tc = new TypeConverter(table);
        return tc.getMathType(ty);
    }

    // -----------------------------------------------------------
    // Error Related Methods
    // -----------------------------------------------------------

    /**
     * Returns a well-formatted error message indicating that the wrong number
     * of arguments were provided.
     * 
     * @return The error message.
     */
    private String wrongNumberOfArgumentsMessage() {
        return "Wrong number of arguments.";
    }

    /**
     * Returns a well-formatted error message indicating that no enhancement
     * realization exists with the given name for the given enhancement and
     * concept.
     * 
     * @param realization The realization that cannot be found.
     * @param enhancement The enhancement for which it cannot be found.
     * @param concept The name of the concept for which it cannot be found.
     *
     * @return The error message.
     */
    private String noSuchEnhancementRealization(String realizationName,
            String enhancementName, String conceptName) {

        return "Cannot find a realization called " + realizationName
                + " for enhancement " + enhancementName + " to concept "
                + conceptName + ".";
    }

    /**
     * Returns a well-formatted error message indicating that no enhancement
     * exists with the given name for the given concept.
     * 
     * @param enhancement The enhancement that cannot be found.
     * @param concept The name of the concept for which it cannot be found.
     * 
     * @return The error message.
     */
    private String noSuchEnhancementConcept(String enhancementName,
            String conceptName) {

        return "Cannot find an enhancement " + enhancementName + " to "
                + " concept " + conceptName + ".";
    }

    private String expectedProcMessage() {
        return "Found a function where a procedure was expected.";
    }

    private String expectedDiffTypeMessage(String t1, String t2) {
        return "  Expected type: " + t1 + "\n" + "  Found type: " + t2;
    }

    //      private String mandMathTypeMessage() {
    //          return "The type B must be visible to all modules.";
    //      }

    //      private String mandProgTypeMessage() {
    //          return "The types N, Boolean, and Integer must be visible "
    //              + "to all implementation modules.";
    //      }

    private String cantFindType(String name) {
        return "The type " + name + " is not visible from this module.";
    }

    private String facDecErrorMessage(String name) {
        return "Facility declaration error: " + name;
    }

    private String noSelfReference() {
        return "The inductive definition does not reference itself.";
    }

    /** 
     * Returns a well-formatted error message indicating that a parameter
     * of a procedure is defined using a mode incompatible with the
     * corresponding parameter of the corresponding operation definition.
     * 
     * Assumes <code>myCurrentConceptName</code> has been set appropriately.
     * 
     * @param operationMode The mode of the corresponding parameter in the
     *                      corresponding operation.
     * @param procedureMode The mode of the procedure parameter whose mode is
     *                      incompatible.
     *                      
     * @return The error message.
     */
    private String incompatibleParameterModes(Mode operationMode,
            Mode procedureMode) {

        return "Corresponding parameter in " + myCurrentModuleName
                + " is in mode '" + operationMode + "'.  Here, this parameter "
                + "is implemented with mode '" + procedureMode + "'.  This is "
                + "not allowed.";
    }

    /**
     * Returns a well-formatted error message indicating that a parameter
     * of a procedure is defined with an incompatible type to its corresponding
     * parameter in the corresponding operation definition.
     * 
     * Assumes <code>myCurrentConceptName</code> has been set appropriately.
     * 
     * @param operationType The type of the corresponding parameter in the
     *                      corresponding operation.
     * @param procedureType The type of the procedure parameter whose 
     *                      <code>Type</code> is incompatible.
     *                      
     * @return The error message.
     */
    private String incompatibleParameterTypes(String iName, Type operationType,
            Type procedureType) {

        return "Incorrect parameter type " + procedureType.toString()
                + " in procedure, does not match " + "type "
                + operationType.toString() + " in corresponding operation of "
                + iName + ":";
        /*return "Corresponding parameter in " + myCurrentConceptName +
        	" is of type " + operationType + " and does not match the " +
        	"type used here (" + procedureType + ").";*/
    }

    /**
     * Returns a well-formatted error message indicating that the number of
     * parameters given for a procedure does not match the number of parameters
     * given for its corresponding operation.
     * 
     * @return The error message.
     */
    private String procedureParameterCountDoesNotMatchOperation(String iName) {
        return "The number of arguments in this procedure does not match those in "
                + "the corresponding operation in " + iName + ":";
        /*return "Corresponding operation in " + 
        	myCurrentConceptName + " does not have the same number of " +
        	"arguments as this procedure.";*/
    }

    /**
     * Returns a well-formatted error message indicating that the procedure
     * with the given name does not have a corresponding operation definition.
     * 
     * @return The error message.
     */
    private String noMatchingOperation(Symbol name) {
        return "No operation named " + name + " to match this procedure in "
                + "concept " + myCurrentModuleName + ".";
    }

    /**
     * Returns a well-formatted error message indicating that an expression
     * was provided as an argument where a definition was expeted.
     * 
     * @return The error message.
     */
    private String expressionGivenWhereDefinitionExpected() {
        return "Expression given where a definition argument was expected.";
    }

    /**
     * Returns a well-formatted error message indicating that an expression
     * was provided as an argument where a type was expeted.
     * 
     * @return The error message.
     */
    private String expressionGivenWhereTypeExpected() {
        return "Expression given where a type name was expected.";
    }

    /**
     * Returns a well-formatted error message indicating that a variable was
     * given for a parameter that expected a definition name, but no such
     * definition exists.
     */
    private String noSuchDefinition(Symbol name) {
        return "This parameter expects a definition.  No definition found "
                + "named '" + name + "'.";
    }

    /**
     * Returns a well-formatted error message indicating that a variable was
     * given for a parameter, but no variable with that name can be found.
     * 
     * @return The error message.
     */
    private String noSuchVariableName(Symbol name) {
        return "This parameter expects a variable.  No variable found "
                + "named '" + name + "'.";
    }

    /**
     * Returns a well-formatted error message indicating that a definition
     * provided as an argument is not of the proper type.
     * 
     * @return The error message.
     */
    private String incompatibleDefinitionTypesMessage(Type expected, Type given) {
        return "Expected a definition with type: " + expected + "\n"
                + "Given a definition with type: " + given;
    }

    /** Returns a well-formatted error message indicating that an expression
     * was provided where an operation was expected.
     * 
     * @return The error message.
     */
    private String expressionFoundWhereOperationExpectedMessage() {
        return "Found an expression where a definition was expected.";
    }

    /**
     * Returns a well-formatted error message indicating that an operation
     * has too few arguments.
     * 
     * @return The error message.
     */
    private String operationHasTooFewArgumentsMessage() {
        return "Operation has too few arguments.";
    }

    /**
     * Returns a well-formatted error message indicating that an operation
     * has too many arguments.
     * 
     * @return The error message.
     */
    private String operationHasTooManyArgumentsMessage() {
        return "Operation has too many arguments.";
    }

    /**
     * Returns a well-formatted error message that encapsulates a secondary
     * error message indicating that one of the parameters of an operation
     * provided as an argument raised an error.
     * 
     * @param problemParameterIndex The index of the parameter that caused the
     *                              trouble, indexed from 1.
     * @param problem The inner error message.
     * 
     * @return The full error message.
     */
    private String problemWithProvidedOperationMessage(
            int problemParameterIndex, String problem) {
        return "Parameter " + problemParameterIndex + " (counting from 1) "
                + "of provided operation caused the following error:\n"
                + problem;
    }

    /** 
     * Returns a well-formatted error message indicating that the name of a
     * definition was expected.
     * 
     * @return The error message.
     */
    private String expectedDefinitionMessage() {
        return "Expected definition name.";
    }

    /**
     * Returns a well-formatted error message indicating that a definition was
     * provided where none was expected.
     * 
     * @return The error message.
     */
    private String noDefinitionExpectedMessage() {
        return "Not expecting a definition.";
    }

    private String changeNotPermitted(String varName) {
        return varName + " does no appear in the changing clause and "
                + "therefore cannot be modified.";
    }

    /*private String operationNotFoundMessage(String enhancementRealizationName) {
    	return "This operation not implemented in the enhancement " +
    		"realization " + enhancementRealizationName + ".";
    }*/

    /**
     * This method creates an error message made up of the name of the realization
     * and the prototype(s) of the missing procedure(s).
     * 
     * @param name Name of realization with missing procedure(s)
     * @param iName Name of the concept/enhancement this realization is associated with
     * @param syms List of missing procedures
     * @return
     */
    private String foundMissingProceduresMessage(String name, String iName,
            List<OperationEntry> syms) {
        String msg = "\n" + name;
        Boolean plural = (syms.size() != 1);
        if (!plural)
            msg += " is missing a required procedure; ";
        else
            msg += " is missing required procedures; ";
        if (!plural)
            msg += iName + " also requires an implementation of:\n";
        else
            msg += iName + " also requires implementations of:\n";
        OperationEntry entry;
        Iterator<OperationEntry> it = syms.iterator();
        while (it.hasNext()) {
            entry = it.next();
            msg +=
                    err.printErrorLine(entry.getLocation().getFile(), entry
                            .getLocation().getPos());
        }
        return msg;
    }

    // -----------------------------------------------------------
    // Various Sanity-Check Helper Methods
    // -----------------------------------------------------------

    /**
     * Returns the mathematical types associated with a series of parameters
     * stored in a Collection of ParameterVarDecs.  Since the type information
     * inside a ParameterVarDec is of type 'Ty', each is coerced into something
     * of type 'Type' by the <code>getMathType(Ty t)</code> method of the
     * analyzer's MathExpTypeResolver (which is a global variable called 'metr'.
     * 
     * @param parameters A Collection of ParameterVarDecs representing each
     *                   of the parameters in order.
     *                   
     * @return A <code>List</code> of <code>Type</code>s wherein the first 
     *         element corresponds to the type of the first parameter in 
     *         <code>parameters</code>, the second to the second, and so forth.
     */
    private List<Type> getParameterVarDecTypes(
            Collection<ParameterVarDec> parameters) {

        List<Type> parameterTypes = new List<Type>();

        Type wrkType = null;

        for (ParameterVarDec p : parameters) {
            wrkType = metr.getMathType(p.getTy());
            parameterTypes.add(wrkType);
        }

        return parameterTypes;
    }

    /**
     * Returns the parameter modes associated with a series of parameters
     * stored in a Collection of ParameterVarDecs.  
     * 
     * @param parameters A Collection of ParameterVarDecs representing each
     *                   of the parameters in order.
     *                   
     * @return A <code>List</code> of <code>Mode</code>s wherein the first 
     *         element corresponds to the mode of the first parameter in 
     *         <code>parameters</code>, the second to the second, and so forth.
     */
    private List<Mode> getParameterVarDecModes(
            Collection<ParameterVarDec> parameters) {

        List<Mode> parameterModes = new List<Mode>();

        for (ParameterVarDec p : parameters) {
            parameterModes.add(p.getMode());
        }

        return parameterModes;
    }

    /**
     * Returns the types associated with a series of parameters
     * stored in an <code>Iterator</code> of <code>VarEntry</code>s.  Since the 
     * type information inside a <code>VarEntry</code> is already of type 
     * <code>Type</code>, it is copied directly into the final list (contrast
     * this with the method used in getParameterVarDecTypes.)
     * 
     * @param parameters A <code>Collection</code> of <code>VarEntry</code>s 
     *                   representing each of the parameters in order.
     *                   
     * @return A <code>List</code> of <code>Type</code>s wherein the first 
     *         element corresponds to the type of the first parameter in 
     *         <code>parameters</code>, the second to the second, and so forth.
     */
    private List<Type> getParameterVarEntryTypes(Iterator<VarEntry> parameters) {

        List<Type> parameterTypes = new List<Type>();

        VarEntry curParameter;
        while (parameters.hasNext()) {
            curParameter = parameters.next();
            parameterTypes.add(curParameter.getType());
        }

        return parameterTypes;
    }

    /**
     * Returns the parameter modes associated with a series of parameters
     * stored in an <code>Iterator</code> of <code>VarEntry</code>s.
     * 
     * @param parameters A <code>Collection</code> of <code>VarEntry</code>s 
     *                   representing each of the parameters in order.
     *                   
     * @return A <code>List</code> of <code>Mode</code>s wherein the first 
     *         element corresponds to the type of the first parameter in 
     *         <code>parameters</code>, the second to the second, and so forth.
     */
    private List<Mode> getParameterVarEntryModes(Iterator<VarEntry> parameters) {

        List<Mode> parameterModes = new List<Mode>();

        VarEntry curParameter;
        while (parameters.hasNext()) {
            curParameter = parameters.next();
            parameterModes.add(curParameter.getMode());
        }

        return parameterModes;
    }

    /**
     * This method retrieves an operation as an <code>OperationEntry</code>
     * from the current associated concept symbol table (which means that 
     * makeAssociatedConceptSymbolTableAvailable MUST be called before calling
     * this method.)  Throws a SanityCheckException if the named operation is
     * not found in the symbol table.
     * 
     * @param name The name of the operation to retrieve, as a 
     *             <code>Symbol</code>.
     *             
     * @return The OperationEntry associated with <code>name</code> in the
     *         symbol table of the current associated concept.
     *         
     * @throws SanityCheckException If an operation with the given name cannot
     *                              be found.
     */
    private OperationEntry getConceptOperation(Symbol name)
            throws SanityCheckException {

        ModuleScope conceptModuleScope =
                myAssociatedConceptSymbolTable.getModuleScope();

        OperationEntry operation = null;

        if (conceptModuleScope.containsOperation(name)) {
            operation = conceptModuleScope.getOperation(name);
        }
        else {
            //throw new SanityCheckException(noMatchingOperation(name));
            return null;
        }

        return operation;
    }

    /*
     * Error (Sanity) checking methods
     */

    /**
     * General sanity-check method to make sure that the arguments provided
     * for a given operation or module match the formal modes and types of the 
     * parameters.  Assumes that the length of the given parameters and the
     * length of the given arguments is the same.  Raises a SanityCheckException
     * if the number of arguments is inconsistent.  Evokes any other errors 
     * through <code>err</code>.
     * 
     * @param parameters A <code>List</code> of <code>Entry</code>s representing
     *                   the formal parameters of the operation or module.
     * @param arguments A <code>List</code> of <code>ModuleArgumentItem</code>s
     *                  representing the arguments provided, in the same order
     *                  as <code>parameters</code>.
     *                  
     * @throw SanityCheckException Thrown if the number of arguments is
     *                             inconsistent with the number of parameters.
     */
    private void sanityCheckProvidedArguments(List<Entry> parameters,
            List<ModuleArgumentItem> arguments) throws SanityCheckException {
        //This eventually checks the param types so it is not being moved to VisitorSanityCheck

        //Check to make sure we have the right number of arguments
        if (parameters.size() != arguments.size()) {
            throw new SanityCheckException(wrongNumberOfArgumentsMessage());
        }

        Entry curParameter;
        ModuleArgumentItem curArgument;

        int parametersSize = parameters.size();
        for (int curParameterIndex = 0; curParameterIndex < parametersSize; curParameterIndex++) {

            curArgument = arguments.get(curParameterIndex);
            curParameter = parameters.get(curParameterIndex);

            sanityCheckArgumentType(curParameter, curArgument);
        }
    }

    /**
     * The top-level sanity check method for the enhancement clause of a
     * facility declaration.  Simply evokes any errors using <code>err</code>.
     * 
     * @param enhancement The name of the enhancement as found in the facility
     *                    declaration.
     * @param realization The name of the realization of 
     *                    <code>enhancement</code> as found in the facility
     *                    declaration.
     * @param conceptName The name of the concept to which the enhancement is
     *                    being applied.
     */
    private void sanityCheckEnhancementClause(PosSymbol enhancementName,
            EnhancementBodyItem realization, PosSymbol conceptName) {
        //This eventually checks the param types so it is not being moved to VisitorSanityCheck

        ModuleID enhancementID =
                ModuleID.createEnhancementID(enhancementName, conceptName);

        ModuleID enhancementBodyID =
                ModuleID.createEnhancementBodyID(realization.getBodyName(),
                        enhancementName, conceptName);

        if (myInstanceEnvironment.contains(enhancementID)) {
            SymbolTable enhancementTable =
                    myInstanceEnvironment.getSymbolTable(enhancementID);

            List<Entry> enhancementParameters =
                    enhancementTable.getModuleScope().getModuleParameters();

            try {
                sanityCheckProvidedArguments(enhancementParameters, realization
                        .getParams());
            }
            catch (Exception e) {
                err.error(realization.getName().getLocation(), e.getMessage());
            }

            if (myInstanceEnvironment.contains(enhancementBodyID)) {
                SymbolTable enhancementBodyTable =
                        myInstanceEnvironment.getSymbolTable(enhancementBodyID);

                List<Entry> enhancementBodyParameters =
                        enhancementBodyTable.getModuleScope()
                                .getModuleParameters();

                try {
                    sanityCheckProvidedArguments(enhancementBodyParameters,
                            realization.getBodyParams());
                }
                catch (Exception e) {
                    err.error(realization.getBodyName().getLocation(), e
                            .getMessage());
                }
            }
            else {
                //We didn't find a realization with the given name
                err.error(realization.getName().getLocation(),
                        noSuchEnhancementRealization(
                                "" + realization.getName(), ""
                                        + enhancementName.getName(), ""
                                        + conceptName.getName()));
            }
        }
        else {
            //We didn't find an enhancement concept with the given name
            err.error(enhancementName.getLocation(), noSuchEnhancementConcept(
                    "" + enhancementName, "" + conceptName.getName()));
        }
    }

    /**
     * Sanity checks the parameters provided for the realization of a concept
     * as part of a facility declaration.  Simply evokes any errors using
     * <code>err</code>.
     * 
     * @param dec The facility declaration. 
     */
    private void sanityCheckBodyParameters(FacilityDec dec) {
        //This eventually checks the param types so it is not being moved to VisitorSanityCheck
        ModuleID bodyId =
                ModuleID.createConceptBodyID(dec.getBodyName(), dec
                        .getConceptName());

        if (myInstanceEnvironment.contains(bodyId)) {
            SymbolTable bodyTable =
                    myInstanceEnvironment.getSymbolTable(bodyId);
            if (bodyTable != null) {
                List<Entry> bodyParameters =
                        bodyTable.getModuleScope().getModuleParameters();

                List<ModuleArgumentItem> declarationArguments =
                        dec.getBodyParams();

                try {
                    sanityCheckProvidedArguments(bodyParameters,
                            declarationArguments);
                }
                catch (Exception e) {
                    err.error(dec.getBodyName().getLocation(), e.getMessage());
                }
            }
        }
        else {
            //System.err.println("Sanity Check skipping realization arguments " + 
            //"for " + dec.getName() + " because no code for " +
            //dec.getBodyName() + " is available.");
        }
    }

    /**
     * Sanity checks the parameters provided for the concept used as part of
     * a facility declaration.  Simply evokes any errors using <code>err</code>.
     * 
     * @param dec The facility declaration.
     */
    private void sanityCheckConceptParameters(FacilityDec dec) {
        //This eventually checks the param types so it is not being moved to VisitorSanityCheck
        ModuleID conceptId = ModuleID.createConceptID(dec.getConceptName());

        SymbolTable conceptTable =
                myInstanceEnvironment.getSymbolTable(conceptId);
        List<Entry> conceptParameters =
                conceptTable.getModuleScope().getModuleParameters();

        List<ModuleArgumentItem> declarationArguments = dec.getConceptParams();

        try {
            sanityCheckProvidedArguments(conceptParameters,
                    declarationArguments);
        }
        catch (Exception e) {
            err.error(dec.getName().getLocation(), e.getMessage());
        }
    }

    /**
     * The top-level sanity check method for checking a facility declaration's
     * arguments for consistency with their associated concept, realization,
     * or extension parameters.  Simply invokes any errors using 
     * <code>err</code>.
     * 
     * @param dec The <code>FacilityDec</code> containing the facility 
     *            declaration to be sanity checked.
     */
    private void sanityCheckFacilityDeclarationParameters(FacilityDec dec) {
        //This eventually checks the concept, body, and enhancement parameter types so it is not being removed to VisitorSanityCheck

        sanityCheckConceptParameters(dec);
        sanityCheckBodyParameters(dec);

        List<EnhancementItem> enhancements = dec.getEnhancements();
        List<EnhancementBodyItem> enhancementBodies =
                dec.getEnhancementBodies();

        //Check each enhancement/realization pair individually
        //EnhancementItem curEnhancement;
        EnhancementBodyItem curEnhancementBody, curEnhancement;
        PosSymbol curEnhancementName;
        for (int curEnhancementIndex = 0; curEnhancementIndex < enhancementBodies
                .size(); curEnhancementIndex++) {

            //curEnhancement = enhancements.get(curEnhancementIndex);
            curEnhancementBody = enhancementBodies.get(curEnhancementIndex);
            curEnhancementName = curEnhancementBody.getName();

            sanityCheckEnhancementClause(curEnhancementName,
                    curEnhancementBody, dec.getConceptName());
        }
    }

    /** The top level sanity check method for checking the parameters of a
     * procedure against the parameters of its corresponding operation.  Simply
     * invokes any errors using <code>err</code>.
     * 
     * @param dec The <code>ProcedureDec</code> containing the procedure
     *            declaration to be sanity checked.
     */
    private void sanityCheckProcedure(ProcedureDec dec) {

        //Get the name of the procedure as a Symbol
        Symbol procedureSymbol = dec.getName().getSymbol();

        myEncounteredProcedures.add(procedureSymbol.getName());

        try {

            //Get the operation of the same name from the concept
            OperationEntry operation = getConceptOperation(procedureSymbol);

            //Make sure they have the same number and types of arguments
            if (operation != null) {
                sanityCheckProcedureParameters(operation, dec);
            }
        }
        catch (SanityCheckException e) {
            //We couldn't find an operation corresponding to the procedure
            //under consideration
            err.error(dec.getName().getLocation(), e.getMessage());
        }
    }

    /**
     * This method simply invokes an error through the global variable 
     * <code>err</code> if the parameters of the provided operation and the
     * provided procedure do not match in number or type signature.  Returns
     * true iff the number of parameters was correct (even if some of the types
     * were wrong).
     * <br><br>
     * ASSUMES: That the number of parameters in the operation and procedure
     * 			are the same or already reported as an error in 
     * 			<code>VisitorSanityCheck.preProcedureDec()</code>
     * 
     * @param operation The operation whose parameter types and number should
     *                  be checked.
     * @param procedure The procedure whose parameter types and number should
     *                  be checked.
     *                  
     * @return True iff the number of parameters was correct.
     */
    private void sanityCheckProcedureParameters(OperationEntry operation,
            ProcedureDec procedure) {

        //Now that we have the operation, get its parameter types
        List<Type> operationParameterTypes =
                getParameterVarEntryTypes(operation.getParameters());

        //Get the mathematical type of each procedure parameter.
        List<Type> procedureParameterTypes =
                getParameterVarDecTypes(procedure.getParameters());

        sanityCheckProcedureParameterTypes(operationParameterTypes,
                procedureParameterTypes, operation, procedure);
    }

    /**
     * This method simply invokes and error with <code>err</code> for each 
     * parameter of a procedure declaration whose type does not match the
     * corresponding parameter of the corresponding operation.
     * 
     * Assumes that the same number of parameters is in each list.
     * 
     * @param operationParameterTypes A <code>List</code> of <code>Type</code>s
     *                                representing the parameters of the
     *                                corresponding operation in order.
     * @param procedureParameterTypes A <code>List</code> of <code>Type</code>s
     *                                representing the parameters of the
     *                                procedure in order.
     * @param procedure The procedure declaration itself.
     */
    private void sanityCheckProcedureParameterTypes(
            List<Type> operationParameterTypes,
            List<Type> procedureParameterTypes, OperationEntry operation,
            ProcedureDec procedure) {

        //Check each parameter against its sister, report any errors
        Type curOperationType, curProcedureType;
        for (int curArgumentIndex = 0; curArgumentIndex < operationParameterTypes
                .size(); curArgumentIndex++) {

            curOperationType = operationParameterTypes.get(curArgumentIndex);
            curProcedureType = procedureParameterTypes.get(curArgumentIndex);

            try {
                sanityCheckParameterType(curOperationType, curProcedureType,
                        procedure);
            }
            catch (SanityCheckException e) {
                //If we got here, the types did not match up correctly
                String iName =
                        myAssociatedConceptSymbolTable.getModuleID().getName()
                                .toString();
                Location l1 =
                        procedure.getParameters().get(curArgumentIndex)
                                .getName().getLocation();
                Iterator<VarEntry> it = operation.getParameters();
                for (int i = 0; i < curArgumentIndex; i++)
                    it.next();
                Location l2 = it.next().getName().getLocation();

                err.error(l1, l2, incompatibleParameterTypes(iName,
                        curOperationType, curProcedureType));
            }
        }
    }

    /**
     * This method simply throws a <code>SanityCheckException</code> if the 
     * provided types do not match.  "Matching" is currently defined as 
     * having the same TypeName.getName().
     * 
     * XXX : This definition of matching may need to be examined.
     * 
     * @param operationType The type of the parameter to the operation.
     * @param procedureType The type of the parameter to the procedure.
     * @param procedure The <code>Dec</code> of the declaration under
     *                  consideration.
     *                  
     * @throws SanityCheckException If the <code>procedureType</code> and the
     *                              <code>operationType</code> are not
     *                              equivalent.
     */
    private void sanityCheckParameterType(Type operationType,
            Type procedureType, Dec procedure) throws SanityCheckException {

        if (!(operationType.toString().equals(procedureType.toString()))) {
            String iName =
                    myAssociatedConceptSymbolTable.getModuleID().getName()
                            .toString();
            throw new SanityCheckException(incompatibleParameterTypes(iName,
                    operationType, procedureType));
        }
    }

    /*private void sanityCheckParameterMode
    		(Mode operationMode, Mode procedureMode, Dec procedure)
    		throws SanityCheckException {
    	
    	String operationModeName = operationMode.getModeName();
    	String procedureModename = procedureMode.getModeName();
    	
    	if(!(operationModeName.equals(procedureModename))){
    		String iName = myAssociatedConceptSymbolTable.getModuleID().getName().toString();
    		throw new SanityCheckException(
    				incompatibleParameterModes(iName, operationMode, procedureMode));
    	}
    }*/

    /**
     * This method simply invokes an error using <code>err</code> for each
     * parameter of <code>procedure</code> whose mode is not appropriate for
     * implementing the corresponding parameter mode in the corresponding 
     * <code>operation</code>.  Assumes that <code>operation</code> and
     * <code>procedure</code> have the same number of parameters, so this
     * should be checked ahead of time with 
     * <code>sanityCheckProcedureArgumentsNumberAndTypes</code>.
     * 
     * @param operation The operation corresponding to the procedure in 
     *                  <code>procedure</code>.
     * @param procedure The procedure corresponding to the operation in
     *                  <code>operation</code>.
     */
    /*private void sanityCheckProcedureParameterModes
    		(OperationEntry operation, ProcedureDec procedure) {
    	
    	//Get the operation's parameter modes
    	List<Mode> operationParameterModes =
    		getParameterVarEntryModes(operation.getParameters());
    	
    	//Get the procedure's parameter modes
    	List<Mode> procedureParameterModes =
    		getParameterVarDecModes(procedure.getParameters());
    	
    	//Test each parameter against its sister and make sure the modes are ok
    	Mode curOperationMode, curProcedureMode;
    	for (int curArgumentIndex = 0; 
    	     curArgumentIndex < operationParameterModes.size();
    	     curArgumentIndex++) {
    		
    		curOperationMode = operationParameterModes.get(curArgumentIndex);
    		curProcedureMode = procedureParameterModes.get(curArgumentIndex);
    		
    		try {
    			sanityCheckParameterModeStrength
    				(curOperationMode, curProcedureMode);
    		}
    		catch (SanityCheckException e) {
    			//If we got here, the modes did not match up correctly
    			PosSymbol p =
    				procedure.getParameters().get(curArgumentIndex).getName();
    			err.error(p.getLocation(), e.getMessage());
    		}
    	}
    }*/

    /** Takes a <code>DefinitionEntry</code> and builds a <code>Type</code>
     * corresponding to the type of that entry that is appropriate for comparing
     * with the type of a definition parameter provided in a VarEntry.
     */
    private Type buildDefinitionEntryType(DefinitionEntry definition) {
        Type retval;

        if (definition.getParameters().hasNext()) {
            List<FieldItem> parameterTuple = new List<FieldItem>();

            Iterator<VarEntry> varEntries = definition.getParameters();
            VarEntry curVarEntry;
            while (varEntries.hasNext()) {
                curVarEntry = varEntries.next();
                parameterTuple.add(new FieldItem(curVarEntry.getName(),
                        curVarEntry.getType()));
            }

            TupleType domain = new TupleType(parameterTuple);
            retval = new FunctionType(domain, definition.getType());
        }
        else {
            retval = null;
        }

        return retval;
    }

    /** Sanity checks the signature of a definition provided as the argument to
     * a parameter that expects a definition.
     */
    private void sanityCheckProvidedDefinition(VarEntry parameter,
            DefinitionEntry argument) {
        Type argumentType = buildDefinitionEntryType(argument);

        try {
            Location errorLocation = argument.getName().getLocation();
            if (!metr.matchTypes(errorLocation, parameter.getType(),
                    argumentType, true, false)) {

                err.error(errorLocation, incompatibleDefinitionTypesMessage(
                        parameter.getType(), argumentType));
            }
        }
        catch (Exception e) {}
    }

    /** Sanity checks the argument provided for a parameter that is meant to be
     * an Definition.  Evokes any errors using <code>err</code>.
     */
    private void sanityCheckDefinitionArgument(VarEntry parameter,
            ModuleArgumentItem argument) {

        //Since, to my knowledge, you cannot define Definitions in-place, the
        //argument BETTER be a variable with a name, rather than an expression.
        PosSymbol argumentName = argument.getName();
        if (argumentName == null) {
            err.error(argument.getEvalExp().getLocation(),
                    expressionGivenWhereDefinitionExpected());
        }
        else {
            try {
                DefinitionEntry definition =
                        getDefinitionByName(argumentName.getSymbol());

                sanityCheckProvidedDefinition(parameter, definition);
            }
            catch (NotFoundException e) {
                err.error(argumentName.getLocation(),
                        noSuchDefinition(argumentName.getSymbol()));
            }
        }
    }

    /** Sanity checks the argument provided for a parameter that is a mundane
     * value.  Evokes any errors using <code>err</code>.
     */
    private void sanityCheckVariableArgument(VarEntry parameter,
            ModuleArgumentItem argument) {

        Location errorLocation;

        //ProgramExpTypeResolver petr = new ProgramExpTypeResolver(table);

        //This is the type to match against
        Type parameterType = parameter.getType();

        //This big if just determines the type of whatever has been provided,
        //or evokes an error if the type cannot be determined
        Type argumentType = null;
        PosSymbol argumentName = argument.getName();
        if (argumentName == null) {
            errorLocation = argument.getEvalExp().getLocation();
            try {
                argumentType = petr.getProgramExpType(argument.getEvalExp());
            }
            catch (TypeResolutionException e) {
                err.error(errorLocation,
                        "Could not resolve the type of this expression.");
            }
        }
        else {
            errorLocation = argumentName.getLocation();
            try {
                VarEntry variable = getVariableByName(argumentName.getSymbol());
                argumentType = variable.getType();
            }
            catch (NotFoundException e) {
                err.error(errorLocation, noSuchVariableName(argumentName
                        .getSymbol()));
            }
        }

        //Check that the types match
        if (argumentType != null) {
            if (!parameterType.getProgramName().equals(
                    argumentType.getProgramName())) {

                err.error(errorLocation, expectedDiffTypeMessage(""
                        + parameterType.getProgramName(), ""
                        + argumentType.getProgramName()));
            }
        }
    }

    /** Sanity checks the individual parameter of an operation that was itself
     * provided as an argument.
     * 
     * @param parameterOperationParameter The a parameter of the operation
     *                                    parameter.
     * @param argumentOperationParameter The corresponding parameter for the
     *                                   operation provided as an argument.
     */
    private void sanityCheckOperationsParameterMatch(
            VarEntry parameterOperationParameter,
            VarEntry argumentOperationParameter, int parameterIndex)
            throws SanityCheckException {

        //Make sure we were given a definition if we wanted one.
        if (parameterOperationParameter.getMode() == Mode.DEFINITION
                && argumentOperationParameter.getMode() != Mode.DEFINITION) {
            throw new SanityCheckException(problemWithProvidedOperationMessage(
                    parameterIndex, expectedDefinitionMessage()));
        }

        //Make sure we WERE'NT given a definition if we DIDN'T want one.
        if (parameterOperationParameter.getMode() != Mode.DEFINITION
                && argumentOperationParameter.getMode() == Mode.DEFINITION) {
            throw new SanityCheckException(problemWithProvidedOperationMessage(
                    parameterIndex, noDefinitionExpectedMessage()));
        }

        //Make sure the mode is compatible.
        //if (!Mode.implementsCompatible(parameterOperationParameter.getMode(), 
        //argumentOperationParameter.getMode())) {
        if (!Mode.implementsCompatible(argumentOperationParameter.getMode(),
                parameterOperationParameter.getMode())) {
            throw new SanityCheckException(problemWithProvidedOperationMessage(
                    parameterIndex, incompatibleParameterModes(
                            parameterOperationParameter.getMode(),
                            argumentOperationParameter.getMode())));
        }

        //Make sure the type is right.
        //if (!parameterOperationParameter.getType().getProgramName().equals(
        //argumentOperationParameter.getType().getProgramName())) {
        if (!sanityCheckOperationParameterTypesMatch(
                parameterOperationParameter.getType(),
                argumentOperationParameter.getType())) {
            throw new SanityCheckException(problemWithProvidedOperationMessage(
                    parameterIndex, expectedDiffTypeMessage(
                            parameterOperationParameter.getType().asString(),
                            argumentOperationParameter.getType().asString())));
        }
    }

    private boolean sanityCheckOperationParameterTypesMatch(Type p1, Type p2) {
        boolean isMatch = false;
        if (p1.getProgramName().equals(p2.getProgramName())) {
            isMatch = true;
        }
        else if (p1.getProgramName().getName().getName().equals("Entry")) {
            isMatch = true;
        }
        return isMatch;
    }

    /** Sanity checks an operation named as an argument.  Evokes errors on
     * <code>err</code> if the operation does not match the type signature of 
     * the operation as required by the formal parameter.
     * 
     * @param parameter The formal operation parameter.
     * @param argument The operation provided.
     */
    private void sanityCheckOperationsMatch(OperationEntry parameter,
            OperationEntry argument) throws SanityCheckException {

        Iterator<VarEntry> parameterOperationParameters =
                parameter.getParameters();

        Iterator<VarEntry> argumentOperationParameters =
                argument.getParameters();

        //Check each argument individually against its corresponding parameter
        int curIndex = 1;
        boolean errors = false;
        VarEntry curParameterOperationParameter, curArgumentOperationParameter;
        while (parameterOperationParameters.hasNext() && !errors) {
            curParameterOperationParameter =
                    parameterOperationParameters.next();

            if (argumentOperationParameters.hasNext()) {
                curArgumentOperationParameter =
                        argumentOperationParameters.next();

                sanityCheckOperationsParameterMatch(
                        curParameterOperationParameter,
                        curArgumentOperationParameter, curIndex);
            }
            else {
                throw new SanityCheckException(
                        operationHasTooFewArgumentsMessage());
            }

            curIndex++;
        }

        if (argumentOperationParameters.hasNext()) {
            throw new SanityCheckException(
                    operationHasTooManyArgumentsMessage());
        }
    }

    /** Sanity checks the argument provided for a parameter that is meant to be
     * an Operation.  Evokes any errors using <code>err</code>.
     */
    private void sanityCheckOperationArgument(OperationEntry parameter,
            ModuleArgumentItem argument) {

        PosSymbol argumentName = argument.getName();
        if (argument.getName() == null) {
            err.error(argument.getEvalExp().getLocation(),
                    expressionFoundWhereOperationExpectedMessage());
        }
        else {
            ModuleScope curModuleScope = table.getModuleScope();
            if (argument.getQualifier() != null) {
                ModuleID qualID =
                        ModuleID.createFacilityID(argument.getQualifier());
                if (myInstanceEnvironment.contains(qualID)) {
                    ModuleScope qualModuleScope =
                            myInstanceEnvironment.getSymbolTable(qualID)
                                    .getModuleScope();
                    curModuleScope = qualModuleScope;
                }
            }
            if (curModuleScope
                    .containsOperation(argument.getName().getSymbol())) {

                OperationEntry argumentEntry =
                        curModuleScope.getOperation(argumentName.getSymbol());

                try {
                    sanityCheckOperationsMatch(parameter, argumentEntry);
                }
                catch (SanityCheckException e) {
                    err.error(argumentName.getLocation(), e.getMessage());
                }
            }
            else {
                String msg =
                        "Unknown argument: \"" + argumentName.getName() + "\"";
                err.error(argumentName.getLocation(), msg);
            }
        }
    }

    /** Sanity checks the argument provided for a parameter that is meant to be
     * a Type.  Evokes any errors using <code>err</code>.
     * 
     * XXX : Currently does not check to make sure that the provided identifier
     *       does, in fact, name a valid type.
     */
    private void sanityCheckTypeArgument(TypeEntry parameter,
            ModuleArgumentItem argument) {

        PosSymbol argumentName = argument.getName();

        if (argumentName == null) {
            err.error(argument.getEvalExp().getLocation(),
                    expressionGivenWhereTypeExpected());
        }
        else {
            /*Symbol argumentNameSymbol = argumentName.getSymbol();
            ModuleScope curModuleScope = table.getModuleScope();
            if (!curModuleScope.containsType(argumentNameSymbol)) {
            	err.error(argumentName.getLocation(),
            			cantFindType(argumentNameSymbol.toString()));
            }*/
        }
    }

    /** Checks a given argument against its corresponding parameter.  Evokes an
     * error using <code>err</code> if the argument type or mode is
     * inconsistent.
     * 
     * @param parameter The parameter.
     * @param argument The argument.
     * */
    private void sanityCheckArgumentType(Entry parameter,
            ModuleArgumentItem argument) {

        if (parameter instanceof VarEntry) {
            VarEntry parameterAsVarEntry = (VarEntry) parameter;
            Mode parameterMode = parameterAsVarEntry.getMode();

            if (parameterMode == Mode.DEFINITION) {
                // This fails while checking with generic entries, so commented it out for now (Chuck)
                //sanityCheckDefinitionArgument(parameterAsVarEntry, argument);
            }
            else {
                //XXX : For the moment we assume if we've gotten here then the
                //parameter in question is an ordinary variable parameter
                sanityCheckVariableArgument(parameterAsVarEntry, argument);
            }
        }
        else if (parameter instanceof OperationEntry) {
            OperationEntry parameterAsOperationEntry =
                    (OperationEntry) parameter;

            sanityCheckOperationArgument(parameterAsOperationEntry, argument);
        }
        else if (parameter instanceof TypeEntry) {
            TypeEntry parameterAsTypeEntry = (TypeEntry) parameter;
            sanityCheckTypeArgument(parameterAsTypeEntry, argument);
        }
        else {
            System.err.println("sanityCheckArgumentType - "
                    + parameter.getClass().toString());
            //Not sure what would get you here for the moment.
        }
    }

    /*
    private void sanityCheckFinalEnhancementBody(EnhancementBodyModuleDec dec) {
    	PosSymbol enhancementName = dec.getEnhancementName();
    	PosSymbol conceptName = dec.getConceptName();
    	ModuleID enhancementID = ModuleID.createEnhancementID(enhancementName,
    			conceptName);
    	
    	SymbolTable enhancementSymbolTable = env.getSymbolTable(enhancementID);
    	
    	ModuleScope enhancementScope = enhancementSymbolTable.getModuleScope();
    	
    	List<Symbol> requiredOperations = 
    		enhancementScope.getLocalOperationNames();
    	
    	String operationName;
    	OperationEntry operationEntry;
    	for (Symbol s : requiredOperations) {
    		operationName = s.getName();
    		if (!myEncounteredProcedures.contains(operationName)) {
    			operationEntry = enhancementScope.getOperation(s);
    			
    			err.error(operationEntry.getLocation(), 
    					operationNotFoundMessage(dec.getName().getName()));
    		}
    	}
    }*/
}
