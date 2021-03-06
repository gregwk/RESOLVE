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
 * MathExpTypeResolver.java
 * 
 * The Resolve Software Composition Workbench Project
 * 
 * Copyright (c) 1999-2006
 * Reusable Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.analysis;

import edu.clemson.cs.r2jt.absyn.*;
import edu.clemson.cs.r2jt.collections.*;
import edu.clemson.cs.r2jt.data.*;
import edu.clemson.cs.r2jt.entry.*;
import edu.clemson.cs.r2jt.errors.ErrorHandler;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.init.Environment;
import edu.clemson.cs.r2jt.location.OperationLocator;
import edu.clemson.cs.r2jt.location.DefinitionLocator;
import edu.clemson.cs.r2jt.location.ProofLocator;
import edu.clemson.cs.r2jt.location.QualifierLocator;
import edu.clemson.cs.r2jt.location.TheoremLocator;
import edu.clemson.cs.r2jt.location.VariableLocator;
import edu.clemson.cs.r2jt.location.SymbolSearchException;
import edu.clemson.cs.r2jt.location.TypeLocator;
import edu.clemson.cs.r2jt.proofchecking.*;
import edu.clemson.cs.r2jt.scope.Binding;
import edu.clemson.cs.r2jt.scope.ModuleScope;
import edu.clemson.cs.r2jt.scope.OperationScope;
import edu.clemson.cs.r2jt.scope.Scope;
import edu.clemson.cs.r2jt.scope.ScopeID;
import edu.clemson.cs.r2jt.scope.SymbolTable;
import edu.clemson.cs.r2jt.scope.TypeHolder;
import edu.clemson.cs.r2jt.scope.TypeID;
import edu.clemson.cs.r2jt.type.*;

public class MathExpTypeResolver extends TypeResolutionVisitor {

    // ===========================================================
    // Variables 
    // ===========================================================

    private SymbolTable table;

    private ProofChecker pc = null;

    private TypeMatcher tm;

    private CompileEnvironment myInstanceEnvironment;

    private ErrorHandler err;

    private Type B = null;

    private Type Char = null;

    private Type N = null;

    private Type Z = null;

    private Type R = null;

    private Type Str = null;

    private boolean printVarErrors = true;

    // In the case of indexed expressions within an inductive
    //     definition and return statements on operations/proc-
    //     edures, we may encounter this:
    //     " Operation And(b1, b2: B): B
    //           ensures And = b1 and b2; "
    //     In this case, we want the equals expression to return
    //     the type of either its LHS or RHS (and not boolean).
    //     This value can be set true by an inductive definition
    //     or a value-returning function.
    private boolean specialEqlCase = false;

    // In the case of a maintaining clause, any program variables
    //     that are encountered will need to be mechanically transformed
    //     to their math equivalents (by searching for the type def.
    //     that describes them.  This flag makes identifying this
    //     case much simpler.
    //    private boolean maintainingClause = false;

    // ===========================================================
    // Constructors
    // ===========================================================

    public MathExpTypeResolver(SymbolTable table, TypeMatcher tm,
            CompileEnvironment instanceEnvironment) {
        myInstanceEnvironment = instanceEnvironment;
        this.table = table;
        this.tm = tm;
        this.err = instanceEnvironment.getErrorHandler();
    }

    public void setPC(ProofChecker pc) {
        this.pc = pc;
    }

    public TypeMatcher getTypeMatcher() {
        return tm;
    }

    public SymbolTable getSymbolTable() {
        return table;
    }

    private void storeValue(Dec dec, Exp value) {

        //Once upon a time, all of this code was not executed if proofchecking
        //was on.  That check was removed because I could find no reason why it
        //mattered.  If we start having problems, try removing this code.
        //   -HwS Jun 21, 2010

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

    // ===========================================================
    // Public Methods - Abstract Visit Methods
    // ===========================================================

    public Type getMathExpType(Exp exp) throws TypeResolutionException {

        Type retval = exp.getType();

        if (retval == null) {
            retval = exp.accept(this);
        }

        exp.setType(retval);

        return retval;
    }

    public void setEqlCase() {
        specialEqlCase = true;
    }

    public void unsetEqlCase() {
        specialEqlCase = false;
    }

    //    public void setMaintainingClause() { maintainingClause = true; }

    //    public void unsetMaintainingClause() { maintainingClause = false; }

    // ===========================================================
    // Public Methods - Math Expressions
    // ===========================================================

    public boolean matchConstructedTypes(Location loc, ConstructedType t1,
            ConstructedType t2, boolean quiet, boolean strict)
            throws TypeResolutionException {
        if (t1.getQualifier() != null && t2.getQualifier() != null) {
            if (!(t1.getQualifier().getName().equals(t2.getQualifier()
                    .getName())))
                return false;
        }
        if (!(t1.getName().getName().equals(t2.getName().getName())))
            return false;
        List<Type> args1 = t1.getArgs();
        List<Type> args2 = t2.getArgs();
        if (args1.size() != args2.size())
            return false;
        Iterator<Type> args1It = args1.iterator();
        Iterator<Type> args2It = args2.iterator();
        while (args1It.hasNext()) {
            if (!matchTypes(loc, args1It.next(), args2It.next(), quiet, strict))
                return false;
        }
        return true;
    }

    // Matches two math types t1 & t2
    public boolean matchTypes(Location loc, Type t1, Type t2, boolean quiet,
            boolean strict) throws TypeResolutionException {

        if (t1 == null || t2 == null)
            return true;
        List<Type> t1Correspondences = table.getTypeCorrespondences(t1, tm);
        Iterator<Type> t1It = t1Correspondences.iterator();
        List<Type> t2Correspondences;
        if (strict) {
            t2Correspondences = new List<Type>();
            t2Correspondences.add(t2);
        }
        else {
            t2Correspondences = table.getTypeCorrespondences(t2, tm);
        }

        /*if (t1.asString().equals("*N") && t2.asString().equals("*Z")) {
        	System.out.println("Analysis.MathExpTypeResolver.matchTypes reports:");
        	System.out.println("#### N type correspondences:");
        	System.out.println(t1Correspondences);
        	System.out.println("#### Z type correspondences:");
        	System.out.println(t2Correspondences);
        }*/

        Iterator<Type> t2It = null;
        Type temp1 = null;
        Type temp2 = null;
        while (t1It.hasNext()) {
            temp1 = t1It.next();
            t2It = t2Correspondences.iterator();
            while (t2It.hasNext()) {
                temp2 = t2It.next();
                if (tm.mathMatches(temp1, temp2)) {
                    return true;
                }
            }
        }
        if (t1 instanceof ConstructedType && t2 instanceof ConstructedType) {
            return matchConstructedTypes(loc, (ConstructedType) t1,
                    (ConstructedType) t2, quiet, strict);
        }
        if (!quiet) {
            String msg = expectedDiffTypeMessage(t1.toString(), t2.toString());
            err.error(loc, msg);
            throw new TypeResolutionException();
        }
        else {
            return false;
        }
    }

    //    private boolean confirmTypeObligations(Type t1, Type t2) {
    //    	TypeLocator tl = new TypeLocator(table);
    //    	try {
    //    		TypeID tid1 = buildTypeID(t1);
    //    		TypeID tid2 = buildTypeID(t2);
    //    		TypeEntry te1 = tl.locateMathType(tid1);
    //    		TypeEntry te2 = tl.locateMathType(tid2);
    //    		if(te1.hasObligation() && !checkSingleTypeObligation(new PosSymbol(te2.getLocation(),
    //    				te2.getSymbol()), te2.getType(), te1.getLocal(), te1.getObligation())) {
    //    			return false;
    //    		}
    //    		if(te2.hasObligation() && !checkSingleTypeObligation(new PosSymbol(te1.getLocation(),
    //    				te1.getSymbol()), te1.getType(), te2.getLocal(), te2.getObligation())) {
    //    			return false;
    //    		}
    //    		return true;
    //    	}
    //    	catch(SymbolSearchException ssex) {
    //    		return true;
    //    	}
    //    }
    //    
    //    private boolean checkSingleTypeObligation(PosSymbol name, Type t, MathVarDec local, Exp obligation) {
    //    	if(obligation instanceof EqualsExp) {
    //        	obligation = pc.unwrapQuantExp(obligation);
    //        	// Create var
    //        	VarExp var = new VarExp(local.getName().getLocation(), null, local.getName());
    //        	TypeConverter tc = new TypeConverter(table);
    //        	var.setType(tc.getMathType(local.getTy()));
    //        	// Create exp
    //        	VarExp exp = new VarExp(name.getLocation(), null, name);
    //        	var.setType(t);
    //        	// Create variable-expression binding
    //    	    VariableExpBinding veb = new VariableExpBinding(var, exp);
    //    	    List<VariableExpBinding> vars = new List<VariableExpBinding>();
    //    	    vars.add(veb);
    //    	    return pc.equals(((EqualsExp)obligation).getLeft(), ((EqualsExp)obligation).getRight(), null, null, true, false, false, vars);
    //    	}
    //    	else {
    //    		// Not finished...
    //    		return true;
    //    	}
    //    }

    public void setInstanceVar(String var, Type t) {
        if (var.equals("B"))
            B = t;
        else if (var.equals("N"))
            N = t;
        else if (var.equals("Z"))
            Z = t;
        else if (var.equals("Char"))
            Char = t;
        else if (var.equals("Str"))
            Str = t;
    }

    // If XXX_Theory.X is visible in the symbol table,
    //     it retrieves a copy of it - only have to check
    //     for X's visibility once per module
    // NOTE: Unfortunately, can't just use a VariableLocator
    //     to find XXX_Theory.X -- same problem if
    //     XXX_Theory is not already compiled
    public Type getType(String theory, String var, Exp exp, boolean quiet)
            throws TypeResolutionException {
        // If we have already concluded that the type is visible, return
        //     that type's value (held in the instance variable of the
        //     same name)
        if (var.equals("B") && B != null)
            return B;
        if (var.equals("N") && N != null)
            return N;
        if (var.equals("Z") && Z != null)
            return Z;
        if (var.equals("Char") && Char != null)
            return Char;
        if (var.equals("Str") && Str != null)
            return Str;

        if (theory.equals("Boolean_Theory") && var.equals("B")) {
            //B = new BooleanType();
            return BooleanType.INSTANCE;
        }

        ModuleID tid =
                ModuleID.createTheoryID(new PosSymbol(null, Symbol
                        .symbol(theory)));
        if (myInstanceEnvironment.contains(tid)) {
            // Make sure XXX_Theory has math type "X"
            PosSymbol ps = new PosSymbol(null, Symbol.symbol(var));
            SymbolTable st = myInstanceEnvironment.getSymbolTable(tid);
            ModuleScope scope = null;
            if (st != null) {
                // XXX_Theory has already been successfully compiled
                scope = myInstanceEnvironment.getModuleScope(tid);
            }
            else if (table.getModuleScope().getModuleID().equals(tid)) {
                // XXX_Theory has not fully been compiled yet,
                //     check to see if *this* is XXX_Theory, in
                //     which case we can get X from our own ST
                scope = table.getModuleScope();
                if (scope.containsType(ps.getSymbol())) {
                    // If X exists in the current scope
                    Type t = scope.getType(ps.getSymbol()).getType();
                    setInstanceVar(var, t);
                    return t;
                }
                else {
                    if (!quiet) {
                        err.error(exp.getLocation(), "Module " + theory
                                + " does not contain math type " + var + ".");
                        throw new TypeResolutionException();
                    }
                    return null;
                }
            }
            else {
                if (!quiet) {
                    err.error(exp.getLocation(), "Module " + theory
                            + " is not visible from "
                            + table.getModuleID().getFilename() + ".");
                    throw new TypeResolutionException();
                }
                return null;
            }
            if (scope.containsType(ps.getSymbol())) {
                // XXX_Theory has already been successfully compiled
                Type t = scope.getType(ps.getSymbol()).getType();
                setInstanceVar(var, t);
                return t;
            }
            else {
                if (!quiet) {
                    err.error(exp.getLocation(), "Module " + theory
                            + " does not contain math type " + var + ".");
                    throw new TypeResolutionException();
                }
                return null;
            }
        }
        else {
            if (!quiet) {
                err.error(exp.getLocation(), "Module " + theory
                        + " is not visible from "
                        + table.getModuleID().getFilename() + ".");
                throw new TypeResolutionException();
            }
            return null;
        }
    }

    // Returns the type of a QuantExp ("For all x...")
    public Type getQuantExpType(QuantExp exp) throws TypeResolutionException {
        table.beginExpressionScope();
        try {
            if (exp.getWhere() != null) {
                // Typecheck the where clause but throw away it's value
                getMathExpType(exp.getWhere());
            }
            Type t2 = getMathExpType(exp.getBody());
            exp.getBody().setType(t2);
            table.endExpressionScope();
            exp.setType(t2);
            return t2;
        }
        catch (Exception e) {
            table.endExpressionScope();
            throw new TypeResolutionException();
        }
    }

    // Returns the type of an IfExp
    public Type getIfExpType(IfExp exp) throws TypeResolutionException {
        Type t1 = getMathExpType(exp.getTest());
        // Check the type of the test clause against B
        matchTypes(exp.getLocation(), t1, getType("Boolean_Theory", "B", exp,
                false), false, false);
        Type t2 = getMathExpType(exp.getThenclause());
        if (exp.getElseclause() != null) {
            Type t3 = getMathExpType(exp.getElseclause());
            matchTypes(exp.getLocation(), t2, t3, false, false);
        }
        exp.setType(t2);
        return t2;
    }

    // Returns the type of an EqualsExp
    public Type getEqualsExpType(EqualsExp exp) throws TypeResolutionException {

        getMathExpType(exp.getLeft());
        getMathExpType(exp.getRight());

        return BooleanType.INSTANCE;

        /*
        boolean rememberEqlCase = false;
        if(specialEqlCase) {
        	rememberEqlCase = specialEqlCase;
        	specialEqlCase = false;
        }
        List<Type> argtypes = new List<Type>();
        argtypes.add(getMathExpType(exp.getLeft()));
          argtypes.add(getMathExpType(exp.getRight()));
          if(argtypes.size() != 2) {
          	String msg = "Could not resolve type of LHS or RHS of equals expression.";
          	err.error(exp.getLocation(), msg);
          	throw new TypeResolutionException();
          }
          if(exp.getRight() instanceof LambdaExp){
          	typeCheckLambdaExp(exp);
          }
          if(exp.getRight() instanceof LambdaExp){
          	typeCheckLambdaExp(exp);
          	//matchTypes(exp.getLocation(), argtypes.get(0).toMath(), argtypes.get(1), false, false);
          }
        matchTypes(exp.getLocation(), argtypes.get(0), argtypes.get(1), false, false);
        if(rememberEqlCase) {
        	specialEqlCase = true;
        	exp.setType(argtypes.get(0));
            return argtypes.get(0);
        }
        Type bool = null;
        if(exp.getBType() != null){
        	bool = exp.getBType();
        }
        exp.setType(bool);
        return bool;*/
    }

    private void typeCheckLambdaExp(EqualsExp exp)
            throws TypeResolutionException {
        //System.out.println("Lambda checking: "+exp);
        Exp leftExp = exp.getLeft();
        LambdaExp lambdaExp = (LambdaExp) exp.getRight();
        Type leftType = leftExp.getType().toMath();
        //System.out.println(leftType);
        Type lambdaType = lambdaExp.getType();
        //System.out.println(lambdaType);
        if (!leftType.toString().equals(lambdaType.toString())) {
            //err.setIgnore(false);
            //String msg = "LHS does not match lambda expression -=> " + leftType + " != " + lambdaType;
            err.error(lambdaExp.getLocation(), expectedDiffTypeMessage(leftType
                    .toString(), lambdaType.toString()));
            throw new TypeResolutionException();
            //System.err.println(msg);
        }
    }

    public Type getSetExpType(SetExp exp) throws TypeResolutionException {
        table.beginExpressionScope();
        try {
            if (exp.getWhere() != null) {
                Type t1 = getMathExpType(exp.getWhere());
                Type t2 = getType("Boolean_Theory", "B", exp, false);
                matchTypes(exp.getLocation(), t1, t2, false, false);
            }
            if (exp.getBody() != null) {
                Type t1 = getMathExpType(exp.getBody());
                Type t2 = getType("Boolean_Theory", "B", exp, false);
                matchTypes(exp.getLocation(), t1, t2, false, false);
            }
            Type t1 = null;
            List<Type> list = new List<Type>();
            if (exp.getVar() != null) {
                t1 = getMathType(exp.getVar().getTy());
                list.add(t1);
            }
            else if (!exp.getVars().isEmpty()) {
                List<VarExp> vars = exp.getVars();
                t1 = getVarExpType(vars.get(0));
                list.add(t1);
            }
            PosSymbol name = new PosSymbol(null, Symbol.symbol("Set"));
            // Binding will be null on this new type
            ConstructedType retType =
                    new ConstructedType(null, name, list, table.getBinding());
            table.endExpressionScope();
            exp.setType(retType);
            return retType;
        }
        catch (Exception e) {
            table.endExpressionScope();
            throw new TypeResolutionException();
        }
    }

    public Type getMathType(Ty ty) {
        TypeConverter tc = new TypeConverter(table);
        return tc.getMathType(ty);
    }

    // If Character_Theory is visible, returns type Character_Theory.Char
    public Type getCharExpType(CharExp exp) throws TypeResolutionException {
        Type c = getType("Char_Template", "Char", exp, false);
        exp.setType(c);
        return c;
    }

    // Returns the type of a DotExp ("Boolean_Theory.B", "S.Contents(i)", etc.)
    public Type getDotExpType(DotExp exp) throws TypeResolutionException {

        if (exp.getSemanticExp() == null) {
            exp.setSemanticExp(extractSemanticExp(exp));
        }
        Exp semExp = exp.getSemanticExp();

        if (semExp == null) {
            Exp exp1 = exp.getSegments().get(0);

            if (exp1 instanceof VarExp || exp1 instanceof OldExp) {
                if (exp1 instanceof VarExp) {
                    // Check to see if this VarExp is really a *type name*
                    try {
                        Exp exp2 = exp.getSegments().get(1);
                        if (checkIs_InitialCall(exp2)) {
                            // "Entry.Is_Initial(SR.Contents(i))", etc.
                            // Right now Is_Initial is the only function callable
                            //     on types
                            return handleIsInitialCall(exp);
                        }
                    }
                    catch (Exception ex) {
                        if (ex instanceof TypeResolutionException) {
                            // take the current EPN off the stack and throw it away
                            throw new TypeResolutionException();
                        }
                    }
                }
                return getRecordFieldType(null, exp, 0);
            }
            else if (exp1 instanceof FunctionExp) {
                Type returnType = getFunctionExpType((FunctionExp) exp1);

                Exp fieldExp = exp.getSegments().get(1);

                if (fieldExp instanceof VarExp) {
                    VarExp fieldVarExp = (VarExp) fieldExp;
                    PosSymbol fieldName = fieldVarExp.getName();
                    FieldItem correspondingField =
                            findFieldItem(returnType, fieldName);

                    if (correspondingField != null) {
                        return correspondingField.getType();
                    }
                    else {
                        throw new TypeResolutionException();
                    }
                }
                else {
                    throw new TypeResolutionException();
                }

                //if (returnType instanceOf )
            }
        }

        return getMathExpType(semExp);
    }

    public boolean checkIs_InitialCall(Exp exp) {
        if (exp instanceof FunctionExp) {
            if (((FunctionExp) exp).getName().getName().equals("Is_Initial")) {
                return true;
            }
        }
        return false;
    }

    // handle a call of the form: "Entry.Is_Initial(E)"
    private Type handleIsInitialCall(DotExp exp) throws Exception {
        Exp exp1 = exp.getSegments().get(0);
        Exp exp2 = exp.getSegments().get(1);
        TypeID tid = new TypeID(null, ((VarExp) exp1).getName(), 0);
        TypeLocator tlocator =
                new TypeLocator(table, false, myInstanceEnvironment);
        TypeEntry tentry = tlocator.locateProgramType(tid);
        //                           < ------------------------------ (1) put a flag here to tell if its local?
        ((VarExp) exp1).setType(tentry.getType());
        FunctionExp fe = (FunctionExp) exp2;
        List<FunctionArgList> params = fe.getParamList();
        // check the number of parameters
        if (params.size() != 1) {
            String msg =
                    "Cannot pass more than one argument in a call to Is_Initial().";
            err.error(exp2.getLocation(), msg);
            throw new TypeResolutionException();
        }
        List<Exp> params2 = params.get(0).getArguments();
        if (params2.size() != 1) {
            String msg =
                    "Cannot pass more than one argument in a call to Is_Initial().";
            err.error(exp2.getLocation(), msg);
            throw new TypeResolutionException();
        }
        Exp onlyParam = params2.get(0);
        Type t2 = getMathExpType(onlyParam);
        matchTypes(exp2.getLocation(), tentry.getType(), t2, false, false);
        Type bool = getType("Boolean_Theory", "B", exp, false);
        ((FunctionExp) exp2).setType(bool);
        exp.setType(bool);
        return bool;
    }

    public Type getIntegerExpType(IntegerExp exp)
            throws TypeResolutionException {
        Symbol s = Symbol.symbol(String.valueOf(exp.getValue()));
        Symbol thisModule = table.getModuleID().getName();
        if (exp.getQualifier() != null
                && !(exp.getQualifier().getName().equals(thisModule.getName()))) {
            ModuleScope qualModule =
                    table.getModuleScope().getMathVisibleModule(
                            exp.getQualifier().getSymbol());
            VarEntry var = qualModule.getVariable(s);
            Type t = var.getType();
            exp.setType(t);
            return t;
        }
        else if (table.getModuleScope().containsVariable(s)) {
            VarEntry var = table.getModuleScope().getVariable(s);
            Type t = var.getType();
            exp.setType(t);
            return t;
        }
        else {
            // Return a natural number if possible
            if (exp.getValue() >= 0) {
                Type N = getType("Natural_Number_Theory", "N", exp, true);
                if (N != null) {
                    exp.setType(N);
                    return N;
                }
            }
            Type Z = getType("Integer_Theory", "Z", exp, true);
            if (Z != null) {
                exp.setType(Z);
                return Z;
            }
            String msg =
                    cantFindType("Integer_Theory.Z or Natural_Number_Theory.N");
            err.error(exp.getLocation(), msg);
            throw new TypeResolutionException();
        }
    }

    // -----------------------------------------------------------
    // DotExp Methods
    // -----------------------------------------------------------

    /*
     * Returns null when the dot expression is indexing into a Cartesian
     * Product.
     */
    private Exp extractSemanticExp(DotExp exp) throws TypeResolutionException {

        Exp exp1 = exp.getSegments().get(0);
        Exp exp2 = exp.getSegments().get(1);
        PosSymbol qual = assignQual(exp1);

        if (qual == null
                && (exp1 instanceof FunctionExp || exp1 instanceof VarExp || exp1 instanceof OldExp)) {
            // Return null, indicating to the calling method (getDotExpType())
            //     to try looking for the qualifier as a variable of type CartProd
            return null;
        }

        if (exp2 instanceof VarExp) {
            return getQualifiedName(qual, (VarExp) exp2);
        }
        else if (exp2 instanceof FunctionExp) {
            return getQualifiedFunction(qual, (FunctionExp) exp2);
        }
        else {
            assert false : "exp2 is an unexpected type: "
                    + exp2.getClass().toString();
            throw new TypeResolutionException();
        }
    }

    private PosSymbol assignQual(Exp exp1) throws TypeResolutionException {
        QualifierLocator locator = new QualifierLocator(table, err);
        PosSymbol qual = null;
        if (isMathQualifier(exp1)) {
            qual = getMathQualifier(exp1);
            try {
                locator.locateMathModule(qual);
                return qual;
            }
            catch (SymbolSearchException ssex) {
                String msg = theoryNotFound(qual.getName());
                err.error(exp1.getLocation(), msg);
                throw new TypeResolutionException();
            }
        }
        else if (isProgramQualifier(exp1)) {
            qual = getProgramQualifier(exp1);
            try {
                // Was locator.locateMathModule(qual); (???)
                locator.locateProgramModule(qual);
                return qual;
            }
            catch (SymbolSearchException ssex) {
                String msg = conceptNotFound(qual.getName());
                err.error(exp1.getLocation(), msg);
                throw new TypeResolutionException();
            }
        }

        return null;
    }

    private FieldItem findFieldItem(Type tuple, PosSymbol name) {
        List<FieldItem> fields = null;
        if (tuple instanceof TupleType) {
            fields = ((TupleType) tuple).getFields();
        }
        else if (tuple instanceof RecordType) {
            fields = ((RecordType) tuple).getFields();
        }
        else
            return null;
        Iterator<FieldItem> fieldIt = fields.iterator();
        while (fieldIt.hasNext()) {
            FieldItem item = fieldIt.next();
            if (item.getName().getName().equals(name.getName())) {
                return item;
            }
        }
        return null;
    }

    private Type getRecordFieldType(Type t, DotExp exp, int index)
            throws TypeResolutionException {
        Exp currentSegment = exp.getSegments().get(index);
        if (currentSegment instanceof OldExp) {
            currentSegment = ((OldExp) currentSegment).getExp();
        }
        // Case 1: currentSegment is a FunctionExp
        if (currentSegment instanceof FunctionExp) {
            if (t instanceof TupleType || t instanceof RecordType) {
                FunctionExp fe = (FunctionExp) currentSegment;
                FieldItem fi = findFieldItem(t, fe.getName());
                if (fi == null) {
                    genericError(currentSegment.getLocation(), "Field "
                            + fe.getName().getName()
                            + " not found within record or tuple.");
                    throw new TypeResolutionException();
                }
                else {
                    Type retType =
                            convertSegmentToTuple(fi, currentSegment, exp);
                    currentSegment.setType(retType);
                    return retType;
                }
            }
            else {
                System.out
                        .println("DEBUG: currentSegment not an instanceof TupleType (1)");
                throw new TypeResolutionException();
            }
        }
        // Case 2: currentSegment is a VarExp
        else if (currentSegment instanceof VarExp) {
            VarExp vexp = (VarExp) currentSegment;
            if (index == 0) {
                // If the VarExp is "Conc" then cut it off here
                if (vexp.getName().getName().equalsIgnoreCase("Conc")) {
                    Type retType = handleConcKeyword(index, vexp, exp);
                    currentSegment.setType(retType);
                    if (retType != null)
                        return retType;
                }
                try {
                    // Not a "Conc"...
                    // Try looking up as a math variable
                    Type retType = findSegmentAsMathVariable(exp, t, index);
                    currentSegment.setType(retType);
                    return retType;
                }
                catch (Exception ex) {
                    // Try looking up as a program variable
                    try {
                        Type retType =
                                findSegmentAsProgramVariable(exp, t, index);
                        currentSegment.setType(retType);
                        return retType;
                    }
                    catch (Exception ex2) {
                        throw new TypeResolutionException();
                    }
                }
            }
            else if (index == exp.getSegments().size() - 1) {
                // the last segment
                Type retType = handleLastSegment(exp, currentSegment, t, index);
                currentSegment.setType(retType);
                return retType;
            }
            else {
                Type retType =
                        handleMiddleSegment(exp, currentSegment, t, index);
                currentSegment.setType(retType);
                return retType;
            }
        }
        // Case 3: type of currentSegment is currently unhandled
        else {
            System.out
                    .println("DEBUG: Unhandled type in MathExpTypeResolver.getRecordFieldType()");
            throw new TypeResolutionException();
        }
    }

    private Type convertSegmentToTuple(FieldItem fi, Exp currentSegment,
            DotExp exp) throws TypeResolutionException {
        if (fi.getType() instanceof FunctionType) {
            return ((FunctionType) (fi.getType())).getRange();
        }
        else if (fi.getType() instanceof ArrayType) {
            List<FunctionArgList> felist =
                    ((FunctionExp) currentSegment).getParamList();
            if (felist.size() > 0) {
                List<Exp> args = felist.get(0).getArguments();
                if (args.size() > 0) {
                    Exp arg = args.get(0);
                    Type t1 = getMathExpType(arg);
                    ArrayType atype = ((ArrayType) (fi.getType()));
                    matchTypes(exp.getLocation(), t1, atype.getIndex(), false,
                            false);
                    return atype.getEntry();
                }
            }
        }

        // We changed all ArrayType to IndirectType due to the fact that arrays are now Facilities of 
        // Static_Array_Template, so maybe the above is not necessary anymore? - YS 
        else if (fi.getType() instanceof IndirectType) {
            // Type cast to IndirectType
            IndirectType itype = ((IndirectType) fi.getType());

            // Check if it is a NameType containing the type we want
            if (itype.getType() instanceof NameType) {
                // Type cast to NameType
                NameType nType = ((NameType) itype.getType());

                // Check if the type of nType is a FunctionType (The actual type of this IndirectType)
                if (nType.getType() instanceof FunctionType) {
                    return ((FunctionType) (nType.getType())).getRange();
                }
            }
        }
        return fi.getType();
    }

    // If the VarExp takes the form "Conc.X", attempt to resolve
    //     the type of the conceptual variable
    private Type handleConcKeyword(int index, VarExp vexp, DotExp exp)
            throws TypeResolutionException {
        Exp exp2 = exp.getSegments().get(index + 1);
        // What other expression types are acceptable here?
        if (exp2 instanceof VarExp || exp2 instanceof FunctionExp) {
            // Conc.X.Y -> X.Y
            Type t = handleConcAsExp(index, exp2, exp);
            exp.setType(t);
            return t;
        }
        else {
            String msg =
                    "The keyword \"Conc\" must be followed by a variable or function name.";
            err.error(exp.getLocation(), msg);
            throw new TypeResolutionException();
        }
    }

    private Type handleConcAsExp(int index, Exp vexp2, DotExp dexp)
            throws TypeResolutionException {
        ModuleScope module = table.getModuleScope();
        Iterator<ModuleID> specs = module.getSpecIterator();
        while (specs.hasNext()) {
            ModuleID mid = specs.next();
            ModuleScope mscope = myInstanceEnvironment.getModuleScope(mid);
            Map<Symbol, TypeEntry> map = mscope.getTypes();
            Iterator<Symbol> keyIt = map.keyIterator();
            TypeEntry te = null;
            while (keyIt.hasNext()) {
                te = map.get(keyIt.next());
                if (te.getExemplar() != null) {
                    if (vexp2 instanceof VarExp) {
                        if (te.getExemplar().getName().equals(
                                ((VarExp) (vexp2)).getName().getName())) {
                            if (dexp.getSegments().size() > index + 2) {
                                // If we are looking at an expression with more than 2 segments
                                //      (i.e. Conc.P vs. Conc.P.Accepting)
                                Type t =
                                        getRecordFieldType(((ConcType) (te
                                                .getType())).getType(), dexp,
                                                index + 2);
                                return t;
                            }
                            Type t = te.getType();
                            return t;
                        }
                    }
                    else if (vexp2 instanceof FunctionExp) {
                        // Won't handle something of the form: Conc.X(i).Y, just Conc.X(i) (?)
                        if (te.getExemplar().getName().equals(
                                ((FunctionExp) (vexp2)).getName().getName())) {
                            FunctionType tx =
                                    resolveToFunctionType(te.getType());
                            if (tx != null) {
                                Type t = tx.getRange();
                                vexp2.setType(t);
                                return t;
                            }
                        }
                    }
                }
            }
        }
        String msg =
                "The math value of " + vexp2.toString(0, 2)
                        + " could not be resolved.";
        err.error(vexp2.getLocation(), msg);
        throw new TypeResolutionException();
        //    	return null;
    }

    // Added an else if to unwrap the IndirectType if it is a NameType (Chuck)
    private Type findSegmentAsMathVariable(DotExp exp, Type t, int index)
            throws Exception {
        VariableLocator vlocator = new VariableLocator(table, false, err);

        VarExp vexp;
        try {
            vexp = (VarExp) (exp.getSegments().get(index));
        }
        catch (ClassCastException e) {
            vexp = (VarExp) ((OldExp) (exp.getSegments().get(index))).getExp();
        }

        VarEntry ventry = vlocator.locateMathVariable(vexp.getName());
        if (ventry.getType() instanceof IndirectType) {
            IndirectType it = (IndirectType) (ventry.getType());
            t = it.getType();
            if (t instanceof ConcType) {
                t = ((ConcType) t).getType();
            }
            else if (t instanceof NameType) {
                t = ((NameType) t).getType();
            }
            vexp.setType(t);
            return getRecordFieldType(t, exp, index + 1);
        }
        vexp.setType(ventry.getType());
        return getRecordFieldType(ventry.getType(), exp, index + 1);
    }

    private Type findSegmentAsProgramVariable(DotExp exp, Type t, int index)
            throws Exception {
        VariableLocator vlocator = new VariableLocator(table, true, err);

        VarExp vexp;
        try {
            vexp = (VarExp) (exp.getSegments().get(index));
        }
        catch (ClassCastException e) {
            vexp = (VarExp) ((OldExp) (exp.getSegments().get(index))).getExp();
        }
        VarEntry ventry2 = vlocator.locateProgramVariable(vexp.getName());
        if (ventry2.getType() instanceof IndirectType) {
            IndirectType it = (IndirectType) (ventry2.getType());
            t = it.getType();
            if (t instanceof ConcType) {
                t = ((ConcType) t).getType();
            }
            vexp.setType(t);
            return getRecordFieldType(t, exp, index + 1);
        }
        vexp.setType(t);
        return getRecordFieldType(ventry2.getType(), exp, index + 1);
    }

    private Type handleLastSegment(DotExp exp, Exp currentSegment, Type t,
            int index) throws TypeResolutionException {
        VarExp vexp = (VarExp) (exp.getSegments().get(index));
        if (t instanceof ConcType) {
            t = ((ConcType) t).getType();
        }
        if (t instanceof TupleType || t instanceof RecordType) {
            FieldItem fi = null;
            fi = findFieldItem(t, vexp.getName());
            // ERROR CASE
            if (fi == null) {
                String msg =
                        "Field " + vexp.getName().getName()
                                + " not found within record or tuple.";
                err.error(currentSegment.getLocation(), msg);
                throw new TypeResolutionException();
            }
            else {
                Type t2 = fi.getType();
                vexp.setType(t2);
                return t2;
            }
        }
        else if (t instanceof IndirectType) {
            IndirectType it = (IndirectType) t;
            Type it2 = it.getType();
            vexp.setType(it2);
            return it2;
        }
        else {
            System.out
                    .println("DEBUG: currentSegment not an instanceof TupleType (2)");
            throw new TypeResolutionException();
        }
    }

    private Type handleMiddleSegment(DotExp exp, Exp currentSegment, Type t,
            int index) throws TypeResolutionException {
        VarExp vexp = (VarExp) (exp.getSegments().get(index));
        if (t instanceof ConcType) {
            t = ((ConcType) t).getType();
        }
        if (t instanceof TupleType || t instanceof RecordType) {
            FieldItem fi = findFieldItem(t, vexp.getName());
            // ERROR CASE
            if (fi == null) {
                String msg =
                        "Field " + vexp.getName().getName()
                                + " not found within record or tuple.";
                err.error(currentSegment.getLocation(), msg);
                throw new TypeResolutionException();
            }
            else {
                Type t2 = getRecordFieldType(fi.getType(), exp, index + 1);
                vexp.setType(t2);
                return t2;
            }
        }
        else if (t instanceof IndirectType) {
            IndirectType it = (IndirectType) t;
            Type t2 = it.getType();
            vexp.setType(t2);
            return t2;
        }
        else {
            System.out
                    .println("DEBUG: currentSegment not an instanceof TupleType (3)");
            System.out.println("DEBUG: " + t.getClass().toString());
            throw new TypeResolutionException();
        }
    }

    private VarExp getQualifiedName(PosSymbol qual, VarExp exp) {
        return new VarExp(exp.getLocation(), qual, exp.getName());
    }

    private FunctionExp getQualifiedFunction(PosSymbol qual, FunctionExp exp) {
        return new FunctionExp(exp.getLocation(), qual, exp.getName(), exp
                .getNatural(), exp.getParamList());
    }

    private boolean isMathQualifier(Exp exp) {
        if (exp instanceof VarExp) {
            PosSymbol qual = ((VarExp) exp).getName();
            QualifierLocator locator = new QualifierLocator(table, err);
            return locator.isMathQualifier(qual);
        }
        return false;
    }

    private PosSymbol getMathQualifier(Exp exp) {
        if (exp instanceof VarExp) {
            return ((VarExp) exp).getName();
        }
        assert false : "exp is not an instance of VarExp";
        System.out.println("DEBUG: exp is not an instance of VarExp");
        return null;
    }

    private boolean isProgramQualifier(Exp exp) {
        if (exp instanceof VarExp) {
            PosSymbol qual = ((VarExp) exp).getName();
            QualifierLocator locator = new QualifierLocator(table, err);
            return locator.isProgramQualifier(qual);
        }
        return false;
    }

    private PosSymbol getProgramQualifier(Exp exp) {
        if (exp instanceof VarExp) {
            return ((VarExp) exp).getName();
        }
        assert false : "exp is not an instance of VarExp";
        System.out.println("DEBUG: exp is not an instance of VarExp");
        return null;
    }

    public Type getPrefixExpType(PrefixExp exp) throws TypeResolutionException {
        List<Exp> args = new List<Exp>();
        args.add(exp.getArgument());
        List<FunctionArgList> args2 = new List<FunctionArgList>();
        args2.add(new FunctionArgList(args));
        FunctionExp fe = null;
        if (exp.getBType() == null) {
            fe =
                    new FunctionExp(exp.getLocation(), null, exp.getSymbol(),
                            null, args2);
        }
        else {
            fe =
                    new FunctionExp(exp.getLocation(), null, exp.getSymbol(),
                            null, args2, exp.getBType());
        }
        Type t = getFunctionExpType(fe);
        exp.setType(t);
        return t;
    }

    public Type getOutfixExpType(OutfixExp exp) throws TypeResolutionException {
        List<Exp> args = new List<Exp>();
        args.add(exp.getArgument());
        List<FunctionArgList> args2 = new List<FunctionArgList>();
        args2.add(new FunctionArgList(args));
        PosSymbol ps = null;
        switch (exp.getOperator()) {
        case OutfixExp.ANGLE:
            ps = new PosSymbol(null, Symbol.symbol("<_>"));
            break;
        case OutfixExp.DBL_ANGLE:
            ps = new PosSymbol(null, Symbol.symbol("<<_>>"));
            break;
        case OutfixExp.BAR:
            ps = new PosSymbol(null, Symbol.symbol("|_|"));
            break;
        case OutfixExp.DBL_BAR:
            ps = new PosSymbol(null, Symbol.symbol("||_||"));
            break;
        case OutfixExp.SQUARE:
            ps = new PosSymbol(null, Symbol.symbol("[_]"));
            break;
        case OutfixExp.DBL_SQUARE:
            break;
        }
        FunctionExp fe =
                new FunctionExp(exp.getLocation(), null, ps, null, args2);
        Type t = getFunctionExpType(fe);
        exp.setType(t);
        return t;
    }

    public Type getInfixExpType(InfixExp exp) throws TypeResolutionException {
        Type t = null;
        List<Exp> args = new List<Exp>();
        args.add(exp.getLeft());
        args.add(exp.getRight());
        List<FunctionArgList> args2 = new List<FunctionArgList>();
        args2.add(new FunctionArgList(args));
        FunctionExp fe = null;
        if (exp.getBType() == null) {
            fe =
                    new FunctionExp(exp.getLocation(), null, exp.getOpName(),
                            null, args2);
        }
        else {
            fe =
                    new FunctionExp(exp.getLocation(), null, exp.getOpName(),
                            null, args2, exp.getBType());
        }

        t = getFunctionExpType(fe);
        exp.setType(t);

        /*if(exp.getBType() == null){
        	FunctionExp fe = new FunctionExp(exp.getLocation(), null, exp.getOpName(),
        			null, args2);
        	t = getFunctionExpType(fe);
        	exp.setType(t);
        }
        else{
        	//exp.getType().setArgs(args2);
        	t = getKnownExpType(exp);
        	//t = exp.getBType();
        	exp.setType(t);
        }*/
        return t;
    }

    /*public Type getKnownExpType(InfixExp exp)
    	throws TypeResolutionException {
    	
    	Type retType = null;
    	if(exp.getOpName().getName().equals("is_in")){
    		return getIsInType(exp);
    	}
    	return retType;
    }*/

    // -----------------------------------------------------------
    // getFunctionExpType() Methods
    // -----------------------------------------------------------

    public Type getFunctionExpType(FunctionExp exp)
            throws TypeResolutionException {

        String expName = exp.getName().getName();
        if (expName.equals("is_in") || expName.equals("is_not_in")) {
            //if(expName.equals("is_in")) {
            //return getIsInType(exp);
        }
        else if (expName.equals("and")) {
            //System.out.println("and");
        }
        List<Type> argtypes = getFunctionExpParamList(exp);
        Type retType = null;
        if (exp.getBtype() == null) {
            retType = tryParamTypeConfiguration(exp, argtypes, 0);
        }
        else {
            retType = exp.getBtype();
        }
        if (retType == null) {
            String msg =
                    "Could not resolve the type of the function expression.";
            err.error(exp.getLocation(), msg);
            throw new TypeResolutionException(exp);
        }
        else {
            return retType;
        }
    }

    /*
     * Get the type of the is_in statement.
     * Check that the RHS is a set and that the LHS matches
     * the type of the elements in the set
     */
    //private Type getIsInType(InfixExp exp)
    //throws TypeResolutionException {
    //List<Type> argtypes = getFunctionExpParamList(exp);
    /*if(argtypes.size() != 2) {
    	String msg = "Only two parameters may be given for is_in.";
    	err.error(exp.getLocation(), msg);
    	throw new TypeResolutionException();
    }*/
    //Type t1 = argtypes.get(0);
    //Type t2 = argtypes.get(1);
    /*if(exp.getBType() instanceof IsInType){
    	Type t1 = getMathExpType(exp.getLeft());
    	Type t2 = getMathExpType(exp.getRight());
    	
    	// if t2 made using powerset_expression, need to convert it
    	// to the math type (ConstructedType)
    	if(t2 instanceof IndirectType){
    		t2 = t2.toMath();
    	}

    	// if t2 made using set_constructor
    	if(t2 instanceof ConstructedType) {
    		if(((ConstructedType)t2).getName().getName().equalsIgnoreCase("Set") ||
    		   ((ConstructedType)t2).getArgs().size() != 1) {
    			// We no longer check the type within the set, could be anything
    			/*Type t2SubType = ((ConstructedType)t2).getArgs().get(0);
    			if(matchTypes(exp.getLocation(), t2SubType, t1, false, false)) {
    				// Manually set the return type of is_in statement
    				Type b = getType("Boolean_Theory", "B", exp, false);
    				exp.setType(b);
    				return b;
    			}*/
    // Manually set the return type of is_in statement
    /*Type b = getType("Boolean_Theory", "B", exp, false);
    exp.setType(b);
    return b;
    //Type b = ((IsInType)exp.getBType()).getRetType();
    //exp.setType(b);
    //return b;
    }
    }
    // t2 is not a set at all
    else {
    String msg = "The second parameter to is_in must be a set, found: " + exp.getRight().toString();
    err.error(exp.getLocation(), msg);
    throw new TypeResolutionException();
    }
    }
    
    return null;
    }*/

    public Type tryParamTypeConfiguration(FunctionExp exp, List<Type> argtypes,
            int whichArg) {

        try {
            if (exp.getName().getName().equals("NB")) {
                System.out
                        .println("MathExpTypeResolver.tryParamTypeConfiguration: "
                                + argtypes);
            }

            Type retType = getFunctionExpType2(exp, argtypes, true);
            if (retType != null) {
                return retType;
            }
            else {
                throw new TypeResolutionException();
            }
        }
        catch (TypeResolutionException trex) {
            if (whichArg >= argtypes.size()) {
                return null;
            }
            List<Type> whichArgTypes =
                    table.getTypeCorrespondences(argtypes.get(whichArg), tm);
            Iterator<Type> it = whichArgTypes.iterator();
            List<Type> newArgs = null;
            Type retValue = null;
            while (it.hasNext()) {
                newArgs = new List<Type>();
                newArgs.addAll(argtypes);
                newArgs.set(whichArg, it.next());
                retValue =
                        tryParamTypeConfiguration(exp, newArgs, whichArg + 1);
                if (retValue != null)
                    return retValue;
            }
            return null;
        }
    }

    public Type getFunctionExpType2(FunctionExp exp, List<Type> argtypes,
            boolean error) throws TypeResolutionException {

        if (exp.getName().getName().equals("zz")) {
            System.out.println("MathExpTypeResolver.getFunctionExpType2");
        }

        // Get a list of types from the arguments
        //    	List<Type> argtypes = getFunctionExpParamList(exp);
        try {
            // If the function is defined in the parameter of another
            //     definition, we have to store it in the symbol table
            //     as a VarEntry (not a DefinitionEntry), so look in
            //     that scope first
            // i.e. "functor(b: B x B -> B, a, c: B) = b(a, c);"
            // Look up as a VarEntry of the form name: B x B -> B first
            Type range = getFunctionExpRangeFromParameters(exp, argtypes);

            exp.setType(range);
            return range;
        }
        catch (SymbolSearchException ex1) {
            // If the function was not found as a parameter, look it up as a standard
            //     DefinitionEntry in the ST
            try {
                DefinitionLocator locator =
                        new DefinitionLocator(table, false, tm, err);
                DefinitionEntry def =
                        locator.locateDefinition(exp.getQualifier(), exp
                                .getName(), argtypes);
                Type t = def.getType();
                exp.setType(t);
                return t;
            }
            catch (SymbolSearchException ex2) {
                //            	try {
                //            		OperationLocator oplocator = new OperationLocator(table, false);
                //            		OperationEntry op = oplocator.locateOperation(exp.getQualifier(),
                //            				                        exp.getName(), argtypes);
                //            		return op.getType();
                //            	}
                //            	catch (SymbolSearchException ex3) {
                // One last try: what if it is a FunctionExp?
                // First - look up M as a variable and get its type
                VarExp ve = new VarExp(exp.getLocation(), null, exp.getName());
                printVarErrors = false;
                Type tempT = null;
                try {
                    tempT = getMathExpType(ve);
                    printVarErrors = true;
                }
                catch (TypeResolutionException trex) {
                    printVarErrors = true;
                    err.error(exp.getLocation(), trex.toString());
                    throw trex;
                }
                //Why is finding a variable with a function type bad? 
                //Quantifiers over functions will give this behavior.
                //if(tempT instanceof FunctionType || tempT instanceof TupleType) {
                //	throw new TypeResolutionException();
                if (tempT instanceof FunctionType) {
                    return ((FunctionType) tempT).getRange();
                }
                else if (tempT instanceof TupleType) {
                    throw new TypeResolutionException();
                }
                else {
                    TypeConverter tc = new TypeConverter(table);
                    TypeID tid = tc.buildTypeID(tempT);
                    TypeLocator tlocator =
                            new TypeLocator(table, myInstanceEnvironment);
                    try {
                        TypeEntry te = tlocator.locateMathType(tid);
                        Type retType = getFunctionTypeIfType(exp, te);
                        exp.setType(retType);
                        return retType;
                    }
                    catch (Exception ex4) {
                        try {
                            TypeEntry te = tlocator.locateProgramType(tid);
                            Type retType = getFunctionTypeIfType(exp, te);
                            exp.setType(retType);
                            return retType;
                        }
                        catch (Exception ex5) {
                            //                                printFunctionError(exp, argtypes);
                            throw new TypeResolutionException();
                        }
                    }
                }
                //            	}
            }
        }
    }

    /*
     * See my note at getFunctionExpDomain.  This function preserved in case
     * we need to revert to the old version of that method. -HwS
     * 
     * A little helper function to take a <code>List</code> of 
     * <code>Type</code>s and convert it into a single type: either the type
     * of its sole element if the list is of size one or a TupleType that is the
     * Cartesian product of all the elements in the list if it is of size > 1.
     * 
     * @param types A <code>List</code> of <code>Type</code>s to be transformed
     *              into a single type.
     *              
     * @return A TupleType that is the Cartesian product of the types in the
     *         list if the given list's size is > 1, otherwise the type of the
     *         singleton member of the list.
     */
    /*
    private Type makeCrossOfTypes(List<Type> types) {
    	Type retval;
    	
    	if (types.size() > 1) {
        	List<FieldItem> elements = new List<FieldItem>();
        	
        	for (Type curType : types) {
        		elements.add(new FieldItem(new PosSymbol(), curType));
        	}

        	retval = new TupleType(elements);
    	}
    	else if (types.size() == 1) {
    		retval = types.get(0);
    	}
    	else {
    		//If we got here it means a definition references a function from
    		//its own parameter list whose domain is of size 0.  That is,
    		//a function that takes nothing.  My assumption is that this is
    		//impossible.
    		System.out.print("ERROR: Got somewhere I never expected in " +
    				"MathExpTypeResolver.makeCrossOfTypes.");
    		throw new RuntimeException();
    	}
    	
    	return retval;
    }
     */

    /*
     * See my note below at getFunctionExpDomain.  A version of this method
     * compatible with the old version of getFunctionExpDomain has likewise
     * been preserved.  -HwS
     * 
     * This function is only called from getFunctionExpType2 when determining
     * the type of a function that is listed in the parameters of the definition
     * in which it is used.  For instance:
     * 
     *   Definition D(E : N -> N) : N = E(0)
     *   
     * It takes as parameters the expression we're looking for, and a list of
     * the types of the arguments forming the domain of the function we're 
     * looking for.  In the above example, the expression is "E" and the list 
     * of argument types is: N.
     * 
     * Returns the type of the range of the expression.  Throws a 
     * SymbolSearchException if no such expression can be found.
     * 
     * @param exp      The function expression to search for.
     * @param argtypes A <code>List</code> of <code>Type</code>s forming the
     *                 arguments of the function call, to be used in matching
     *                 against the appropriate function.
     *                 
     * @return The <code>Type</code> of range of the expression.
     * 
     * @throws SymbolSearchException If no matching expression can be found.
     */
    private Type getFunctionExpRangeFromParameters(FunctionExp exp,
            List<Type> argtypes) throws SymbolSearchException {

        //Since we're only looking in the parameters of a definition, no
        //'function' with 0 or more than 1 parameters would be there.  All
        //mathematical functions take exactly one parameter--it just may be the
        //Cartesian product of many types.
        if (argtypes.size() != 1) {
            throw new SymbolSearchException();
        }

        Type range = null;

        VariableLocator vlocator = new VariableLocator(table, false, err);
        VarEntry ventry = vlocator.locateMathVariable(exp.getName());

        if (ventry.getType() instanceof FunctionType) {
            range = ((FunctionType) ventry.getType()).getRange();
            Type domainType = getFunctionExpDomain(ventry);

            if (!tm.mathMatches(argtypes.get(0), domainType)) {
                throw new SymbolSearchException();
            }
        }
        // We changed all ArrayType to IndirectType due to the fact that arrays are now Facilities of 
        // Static_Array_Template so maybe the above is not necessary? - YS 
        else if (ventry.getType() instanceof IndirectType) {
            // Type cast to IndirectType
            IndirectType itype = ((IndirectType) ventry.getType());

            // Check if it's type is of ConcType
            if (itype.getType() instanceof ConcType) {
                // Type cast to ConcType
                ConcType ctype = (ConcType) itype.getType();

                // Check if ctype's type is of FunctionType
                if (ctype.getType() instanceof FunctionType) {
                    range = ((FunctionType) ctype.getType()).getRange();
                    Type domainType =
                            ((FunctionType) ctype.getType()).getDomain();

                    // Check the type of the domain
                    if (!tm.mathMatches(argtypes.get(0), domainType)) {
                        throw new SymbolSearchException();
                    }
                }
            }
            else if (itype.getType() instanceof IndirectType) {
                // Type cast to IndirectType
                IndirectType ttype = (IndirectType) itype.getType();

                if (ttype.getType() instanceof NameType) {
                    // Type cast to NameType
                    NameType ntype = (NameType) ttype.getType();

                    // Check if ctype's type is of FunctionType
                    if (ntype.getType() instanceof FunctionType) {
                        range = ((FunctionType) ntype.getType()).getRange();
                        Type domainType =
                                ((FunctionType) ntype.getType()).getDomain();

                        // Check the type of the domain
                        if (!tm.mathMatches(argtypes.get(0), domainType)) {
                            throw new SymbolSearchException();
                        }
                    }
                }
            }
        }

        // Throw Exception
        if (range == null) {
            throw new SymbolSearchException();
        }

        return range;
    }

    /*
    private Type getFunctionExpRange(FunctionExp exp, List<Type> argtypes)
        throws SymbolSearchException {
    	VariableLocator vlocator = new VariableLocator(table, false);
    	VarEntry ventry = vlocator.locateMathVariable(exp.getName());
    	
    	if(ventry.getType() instanceof FunctionType) {
    		Type range = ((FunctionType)ventry.getType()).getRange();
    		// Create a list of the parameters of this VarEntry to
    		//    compare with those in argtypes
    		List<Type> domainTypes = getFunctionExpDomain(ventry);
    		
    		//For the reason of irritating this programmer, the domain types
    		//come to us as a List of Types rather than a TupleType, while the
    		//argument types come to us as a tuple.  Here we convert the list
    		//into a more useful tuple, or take out the one type if its a list
    		//of only one. (EDIT: turns out the above function MADE them this
    		//way!)
    		List<Type> finalDomainTypes = new List<Type>();
    		finalDomainTypes.add(makeCrossOfTypes(domainTypes));
    		
    		// Check to see parameters match
    		TypeMatcher tmatcher = tm;
    		if(argtypes.size() == finalDomainTypes.size()) {
        		Iterator<Type> argumentTypeIterator = argtypes.iterator();
        		Iterator<Type> domainTypeIterator = finalDomainTypes.iterator();
        		Type t;
        		while(argumentTypeIterator.hasNext()) {
        			t = argumentTypeIterator.next();
        			if(!tmatcher.mathMatches(t, domainTypeIterator.next())) {
        				throw new SymbolSearchException();
        			}
        		}
        		return range;
    		}
    		else {
    			throw new SymbolSearchException();
    		}
    	}
    	else {
    		throw new SymbolSearchException();
    	}
    }*/

    /*
     * Originally, this method took the domain and transformed it into a list,
     * either from the original tuple into a list with multiple elements or from
     * the original other type to a list of one element.  I don't know WHY it
     * did this.  This took an extra step to take a form that was useful and
     * transform it into a form that breaks definitions that reference functions
     * in their own parameters.  I can only assume that something in the 
     * underlying representation changed.  I have preserved the original 
     * function after this one in case it becomes obvious once again why it was 
     * the way it was.
     *      -HwS
     *      
     * Takes a <code>VarEntry</code> representing a function and returns the
     * type of its domain.
     * 
     * @param VarEntry The <code>VarEntry</code> in question.
     * 
     * @return The <code>Type</code> of the VarEntry's domain.
     */
    private Type getFunctionExpDomain(VarEntry ventry) {
        return ((FunctionType) ventry.getType()).getDomain();
    }

    // Transforms the domain of the FunctionExp to a list
    //      of Types
    /*private List<Type> getFunctionExpDomain(VarEntry ventry) {
    	Type domain = ((FunctionType)ventry.getType()).getDomain();
    	List<Type> domainTypes = new List<Type>();
    	if(domain instanceof TupleType) {
    		TupleType t1 = (TupleType)domain;
    	    List<FieldItem> fields = t1.getFields();
    	    Iterator<FieldItem> i2 = fields.iterator();
    	    while(i2.hasNext()) {
    	    	FieldItem next = i2.next();
    	    	domainTypes.add(next.getType());
    	    }
    	}
    	else {
    		domainTypes.add(domain);
    	}
    	return domainTypes;
    }*/

    // Returns a list of the FunctionExp's argument types
    private List<Type> getFunctionExpParamList(FunctionExp fe)
            throws TypeResolutionException {
        List<Type> argtypes = new List<Type>();
        List<Exp> args = new List<Exp>();
        List<FunctionArgList> faExp = fe.getParamList();
        Iterator<FunctionArgList> fi = faExp.iterator();
        while (fi.hasNext()) {
            args.addAll(fi.next().getArguments());
        }
        Iterator<Exp> i = args.iterator();
        while (i.hasNext()) {
            Exp argexp = i.next();
            Type argtype = getMathExpType(argexp);
            argexp.setType(argtype);
            argtypes.add(argtype);
        }
        return argtypes;
    }

    // Return the type of the TypeEntry if the TypeEntry and the FunctionExp
    //     match
    private Type getFunctionTypeIfType(FunctionExp fe, TypeEntry te)
            throws TypeResolutionException {
        Type retType = matchToTypeEntry(te, fe);
        if (retType != null)
            return retType;
        else
            throw new TypeResolutionException();
    }

    // Prints a detailed error if the FunctionExp's type cannot be resolved
    public void printFunctionError(FunctionExp exp, List<Type> argtypes)
            throws TypeResolutionException {
        //    	System.out.println("DEBUG: ");
        //    	System.out.println("COULDN'T FIND: " + exp.getName().getName());
        //        Iterator<Type> ix = argtypes.iterator();
        //        while(ix.hasNext()) {
        //            System.out.println(ix.next().toString());
        //        }
        //        System.out.println("FOR MODULE: " + table.getModuleScope().getModuleID().getName());
        //        Iterator<ModuleScope> mi1 = table.getModuleScope().getProgramVisibleModules();
        //        System.out.println("Program Visible Modules:");
        //        while(mi1.hasNext()) {
        //            System.out.println(" - " + mi1.next().getModuleID().getName());
        //        }
        //        Iterator<ModuleScope> mi2 = table.getModuleScope().getMathVisibleModules();
        //        System.out.println("Math Visible Modules:");
        //        while(mi2.hasNext()) {
        //            System.out.println(" - " + mi2.next().getModuleID().getName());
        //        }
        String msg1 = exp.getName().getName() + " (";
        if (argtypes.size() > 0) {
            Iterator<Type> it = argtypes.iterator();
            if (it.hasNext()) {
                msg1 += it.next().toString();
            }
            while (it.hasNext()) {
                msg1 += ", " + it.next().toString();
            }
        }
        msg1 += ")";
        String msg = defNotFound(msg1);
        err.error(exp.getLocation(), msg);
        throw new TypeResolutionException();
    }

    // Builds a new TypeID from the given Type
    //    public TypeID buildNewTypeID(Type tempT) {
    //    	PosSymbol qual = null;
    //   		PosSymbol name = null;
    //   		if(tempT instanceof ArrayType) {
    //   			name = ((ArrayType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof ConcType) {
    //   			name = ((ConcType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof ConstructedType) {
    //   			qual = ((ConstructedType)tempT).getQualifier();
    //   			name = ((ConstructedType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof FieldItem) {
    //   			name = ((FieldItem)tempT).getName();
    //   		}
    //   		else if(tempT instanceof FormalType) {
    //   			name = ((FormalType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof IndirectType) {
    //   			qual = ((IndirectType)tempT).getQualifier();
    //   			name = ((IndirectType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof MathFormalType) {
    //   			name = ((MathFormalType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof NameType) {
    //   			name = ((NameType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof PrimitiveType) {
    //   			name = ((PrimitiveType)tempT).getName();
    //   		}
    //   		else if(tempT instanceof RecordType) {
    //   			name = ((RecordType)tempT).getName();
    //   		}
    //   		return new TypeID(qual, name, 0);
    //    }

    // Attempt to cast the given Type to a FunctionType if possible
    public FunctionType resolveToFunctionType(Type t) {
        if (t instanceof FunctionType) {
            return (FunctionType) t;
        }
        else if (t instanceof ConcType) {
            return resolveToFunctionType(((ConcType) t).getType());
        }
        else if (t instanceof FieldItem) {
            return resolveToFunctionType(((FieldItem) t).getType());
        }
        else if (t instanceof NameType) {
            return resolveToFunctionType(((NameType) t).getType());
        }
        else {
            return null;
        }
    }

    // Matches a FunctionExp to a TypeEntry
    public Type matchToTypeEntry(TypeEntry te, FunctionExp fe)
            throws TypeResolutionException {
        FunctionType ft = resolveToFunctionType(te.getType());
        if (ft == null)
            return null;
        Type domain = ft.getDomain();
        Type returnValue = ft.getRange();
        List<Type> args = new List<Type>();
        // Fill in the list of argument types of the TypeEntry
        if (domain instanceof TupleType) {
            TupleType domain2 = (TupleType) domain;
            Iterator<FieldItem> it = domain2.getFields().iterator();
            FieldItem tempFI = null;
            while (it.hasNext()) {
                tempFI = it.next();
                args.add(tempFI.getType());
            }
        }
        else {
            args.add(domain);
        }
        // Create a list of arguments types from the FunctionExp
        List<FunctionArgList> feList = fe.getParamList();
        if (feList.size() == 0 && args.size() == 0) {
            return returnValue;
        }
        Iterator<Exp> it1 = feList.get(0).getArguments().iterator();
        List<Type> list2 = new List<Type>();
        Exp e1 = null;
        while (it1.hasNext()) {
            e1 = it1.next();
            Type t = getMathExpType(e1);
            // may already be set?
            e1.setType(t);
            list2.add(t);
        }
        // Match argument types
        if (matchTypeLists(args, list2)) {
            return returnValue;
        }
        else {
            return null;
        }
    }

    // Match two lists of arguments
    public boolean matchTypeLists(List<Type> l1, List<Type> l2) {
        if (l1.size() != l2.size())
            return false;
        Iterator<Type> i1 = l1.iterator();
        Iterator<Type> i2 = l2.iterator();
        Type t1 = null;
        Type t2 = null;
        while (i1.hasNext()) {
            t1 = i1.next();
            t2 = i2.next();
            if (!(tm.mathMatches(t1, t2))) {
                if (!(tm.programMatches(t1, t2))) {
                    return false;
                }
            }
        }
        return true;
    }

    // -----------------------------------------------------------
    // getVarExpType() Methods
    // -----------------------------------------------------------

    //    private Type convertProgramTypeToMathType(VarExp exp)
    //        throws SymbolSearchException {
    //    	// Locate the program variable and get its type
    //    	VariableLocator vloc = new VariableLocator(table, false);
    //    	VarEntry ve = vloc.locateProgramVariable(exp.getQualifier(), exp.getName());
    //		Type t1 = ve.getType();
    //		// Locate that type
    //		TypeLocator tloc = new TypeLocator(table);
    //		TypeID tid = buildTypeID(t1);
    //		if(tid == null) {
    //			throw new SymbolSearchException();
    //		}
    //        TypeEntry te = tloc.strictLocateMathType(tid);
    //		return te.getType();
    //    }

    public Type getVarExpType(VarExp exp) throws TypeResolutionException {

        if (exp.getBType() != null) {
            return exp.getBType();
        }

        if (exp.getName().getName().equals("bbb")) {
            System.out.println("MathExpTypeResolver.getVarExpType");
        }

        // First - look and see if an explicit math type has been set by
        // means of a "Confirm Math Type..." statement
        Iterator<MathVarDec> it =
                table.getModuleScope().getAlternateVarTypes().iterator();
        MathVarDec temp;
        while (it.hasNext()) {
            temp = it.next();
            if (temp.getName().getName().equals(exp.getName().getName())) {
                // We have found an override for the math type of this variable
                TypeConverter tc = new TypeConverter(table);
                return tc.getMathType(temp.getTy());
            }
        }
        // Case 1: the variable is just a standard math variable in the ST
        VariableLocator locator = new VariableLocator(table, false, err);
        try {
            VarEntry entry =
                    locator.locateMathVariable(exp.getQualifier(), exp
                            .getName(), exp);
            //            if(maintainingClause && !(entry.getMode().isMathMode())) {
            //               Type t = convertProgramTypeToMathType(exp);
            //               	if(t != null) return t;
            //            }
            Type t = entry.getType();
            exp.setType(t);
            return t;
        }
        catch (SymbolSearchException ex) {
            // Case 2: the variable is actually the name of the operation
            //         whose scope we are in
            // i.e. "Oper And(a, b: Boolean):B ensures And = a and b;"
            try {
                Type retType = getVarTypeIfOperation(exp);
                exp.setType(retType);
                if (retType != null)
                    return retType;
                throw new SymbolSearchException();
            }
            catch (SymbolSearchException ex1) {
                // Case 3: the variable is actually the name of a Definition with
                //         no parameters (the grammar allows this, so we need to
                //         handle it)
                try {
                    Type retType = getVarTypeIfDefinition(exp);
                    exp.setType(retType);
                    return retType;
                }
                catch (SymbolSearchException ex2) {
                    // Case 4: matching a VarEntry of the form "func: B x B -> B" with
                    //         a definition of the form "func(x, y: B): B"
                    try {
                        Type retType = getVarTypeIfFunction(exp);
                        exp.setType(retType);
                        return retType;
                    }
                    catch (SymbolSearchException ex3) {
                        // Case 5: the variable is a program variable whose math type
                        //         needs to be derived
                        try {
                            Type retType = getVarTypeIfProgramVar(exp);
                            exp.setType(retType);
                            return retType;
                        }
                        catch (SymbolSearchException ex4) {
                            try {
                                // Case 6: the variable actually denotes a named type
                                //     i.e. calling N refers to the set of all natural
                                //     numbers (so return a set of type N)
                                ConstructedType retType = getVarTypeIfType(exp);
                                exp.setType(retType);
                                return retType;
                            }
                            catch (SymbolSearchException ex5) {
                                if (printVarErrors) {
                                    String s =
                                            variableNotFound(exp.getName()
                                                    .getName());
                                    err.error(exp.getLocation(), s);
                                }
                                throw new TypeResolutionException(exp);
                            }
                        }
                    }
                }
            }
        }
    }

    // Looks for the VarExp as an Operation whose scope we are within;
    //     if found, returns the return type of that Operation
    private Type getVarTypeIfOperation(VarExp exp) {
        Scope s1 = table.getCurrentScope();
        if (s1 instanceof OperationScope) {
            Symbol sym1 = exp.getName().getSymbol();
            ScopeID id1 = ((OperationScope) s1).getScopeID();
            if (id1.isOperation()) {
                if (sym1.equals(id1.getOperation().getName())) {
                    ModuleScope m1 = table.getModuleScope();
                    OperationEntry oentry = m1.getOperation(sym1);
                    return oentry.getType().toMath();
                }
            }
        }
        return null;
    }

    // Looks for the VarExp as a DefinitionEntry with no parameters;
    //     if found, returns it's return type
    private Type getVarTypeIfDefinition(VarExp exp)
            throws SymbolSearchException {
        DefinitionLocator olocator =
                new DefinitionLocator(table, false, tm, err);
        List<Type> params = new List<Type>();
        DefinitionEntry entry =
                olocator.locateDefinition(exp.getQualifier(), exp.getName(),
                        params);
        return entry.getType().toMath();
    }

    // Looks for the VarExp as a function; if found, returns the math
    //     type of that function
    private Type getVarTypeIfFunction(VarExp exp) throws SymbolSearchException {
        DefinitionLocator olocator =
                new DefinitionLocator(table, false, tm, err);
        DefinitionEntry entry = olocator.locateDefinition(exp.getName());
        // Return as a FunctionType
        Iterator<VarEntry> paramIt = entry.getParameters();
        List<FieldItem> domainList = new List<FieldItem>();
        while (paramIt.hasNext()) {
            VarEntry ve = paramIt.next();
            FieldItem fi = new FieldItem(ve.getName(), ve.getType());
            domainList.add(fi);
        }
        TupleType domain = new TupleType(domainList);
        FunctionType ftype = new FunctionType(domain, entry.getType());
        Type retType = ftype.toMath();
        return retType;
    }

    // Looks for the VarExp as a *program* variable; if found it attempts
    //     to return the math type of that program variable; if toMath()
    //     can't resolve the math type, the program type is returned
    private Type getVarTypeIfProgramVar(VarExp exp)
            throws SymbolSearchException {
        VariableLocator vlocator = new VariableLocator(table, false, err);
        VarEntry entry =
                vlocator.locateProgramVariable(exp.getQualifier(), exp
                        .getName(), exp);
        return entry.getType().toMath();
    }

    // Looks for the VarExp as a TypeEntry; if found returns a set of
    //     that Type
    private ConstructedType getVarTypeIfType(VarExp exp)
            throws SymbolSearchException {
        TypeLocator tlocator = new TypeLocator(table, myInstanceEnvironment);
        TypeID tid = new TypeID(exp.getQualifier(), exp.getName(), 0);
        TypeEntry tentry = tlocator.locateMathType(tid);
        List<Type> args = new List<Type>();
        args.add(tentry.getType());
        // Should we give this the module scope or the current scope?
        ConstructedType ctype =
                new ConstructedType(exp.getQualifier(), new PosSymbol(exp
                        .getLocation(), Symbol.symbol("Set")), args,
                        new Binding(table.getModuleScope(),
                                myInstanceEnvironment));
        return ctype;
    }

    // -----------------------------------------------------------
    // Other Expression Types
    // -----------------------------------------------------------

    public Type getAlternativeExpType(AlternativeExp exp)
            throws TypeResolutionException {
        Iterator<AltItemExp> i = exp.getAlternatives().iterator();
        Type t1 = getMathExpType(i.next());
        Type t2 = null;
        AltItemExp alt = null;
        while (i.hasNext()) {
            alt = i.next();
            t2 = getMathExpType(alt);
            matchTypes(alt.getLocation(), t1, t2, false, false);
        }
        exp.setType(t1);
        return t1;
    }

    public Type getAltItemExpType(AltItemExp exp)
            throws TypeResolutionException {
        if (exp.getTest() != null) {
            Type t1 = getMathExpType(exp.getTest());
            Type t2 = getType("Boolean_Theory", "B", exp.getTest(), false);
            matchTypes(exp.getTest().getLocation(), t1, t2, false, false);
        }
        Type t1 = getMathExpType(exp.getAssignment());
        exp.setType(t1);
        return t1;
    }

    public Type getBetweenExpType(BetweenExp exp)
            throws TypeResolutionException {
        Iterator<Exp> i = exp.getLessExps().iterator();
        Type tOriginal = null;
        Type t = null;
        Exp temp = null;
        if (i.hasNext()) {
            tOriginal = getMathExpType(i.next());
        }
        while (i.hasNext()) {
            temp = i.next();
            t = getMathExpType(temp);
            matchTypes(temp.getLocation(), tOriginal, t, false, false);
        }
        exp.setType(tOriginal);
        return t;
    }

    public Type getRealExpType(DoubleExp exp) throws TypeResolutionException {
        Type r = getType("Real_Theory", "Real", exp, false);
        exp.setType(r);
        return r;
    }

    public Type getGoalExpType(GoalExp exp) throws TypeResolutionException {
        Type t1 = getMathExpType(exp.getExp());
        matchTypes(exp.getExp().getLocation(), t1, getType("Boolean_Theory",
                "B", exp.getExp(), false), false, false);
        exp.setType(t1);
        return t1;
    }

    public Type getSuppositionExpType(SuppositionExp exp)
            throws TypeResolutionException {
        Type t1 = null;
        Type b = getType("Boolean_Theory", "B", exp, false);
        if (exp.getExp() != null) {
            t1 = getMathExpType(exp.getExp());
            matchTypes(exp.getExp().getLocation(), t1, b, false, false);
        }
        exp.setType(b);
        return b;
    }

    public Type getDeductionExpType(DeductionExp exp)
            throws TypeResolutionException {
        Type t1 = getMathExpType(exp.getExp());
        matchTypes(exp.getExp().getLocation(), t1, getType("Boolean_Theory",
                "B", exp.getExp(), false), false, false);
        exp.setType(t1);
        return t1;
    }

    public Type getLambdaExpType(LambdaExp exp) throws TypeResolutionException {
        table.beginExpressionScope();
        try {
            Type t1 = getMathType(exp.getTy());
            Type t2 = getMathExpType(exp.getBody());
            Type lambdaType = new FunctionType(t1, t2);
            exp.setType(lambdaType);
            table.endExpressionScope();
            return lambdaType;
        }
        catch (Exception e) {
            table.endExpressionScope();
            throw new TypeResolutionException();
        }
    }

    public Type getFieldExpType(FieldExp exp) throws TypeResolutionException {
        // This Exp type never used?
        assert false : "getFieldExpType() not fully implemented";
        throw new TypeResolutionException();
        //return null;
    }

    public Type getIterativeExpType(IterativeExp exp)
            throws TypeResolutionException {
        table.beginExpressionScope();
        try {
            Type t1 = getMathExpType(exp.getWhere());
            matchTypes(exp.getWhere().getLocation(), t1, getType(
                    "Boolean_Theory", "B", exp.getWhere(), false), false, false);
            Type t2 = getMathExpType(exp.getBody());
            exp.setType(t2);
            table.endExpressionScope();
            return t2;
        }
        catch (Exception e) {
            table.endExpressionScope();
            throw new TypeResolutionException();
        }
    }

    public Type getJustifiedExpType(JustifiedExp exp)
            throws TypeResolutionException {
        Type t1 = getMathExpType(exp.getExp());
        matchTypes(exp.getExp().getLocation(), t1, getType("Boolean_Theory",
                "B", exp.getExp(), false), false, false);
        exp.setType(t1);
        return t1;
    }

    public Type getOldExpType(OldExp exp) throws TypeResolutionException {
        Type t = getMathExpType(exp.getExp());
        exp.setType(t);
        return t;
    }

    public Type getProofDefinitionExpType(ProofDefinitionExp exp)
            throws TypeResolutionException {
        DefinitionDec def = exp.getExp();
        Type retType = getMathType(def.getReturnTy());
        if (def.getBase() != null) {
            specialEqlCase = true;
            Type t1 = getMathExpType(def.getBase());
            matchTypes(def.getBase().getLocation(), t1, retType, false, false);
            storeValue(exp.getExp(), exp.getExp().getBase());
            if (def.getHypothesis() != null) {
                t1 = getMathExpType(def.getHypothesis());
                matchTypes(def.getHypothesis().getLocation(), t1, retType,
                        false, false);
                storeValue(exp.getExp(), exp.getExp().getHypothesis());
            }
        }
        else if (def.getDefinition() != null) {
            Type t1 = getMathExpType(def.getDefinition());
            matchTypes(def.getDefinition().getLocation(), t1, retType, false,
                    false);
            storeValue(exp.getExp(), exp.getExp().getDefinition());
        }
        exp.setType(retType);
        return retType;
    }

    // Returns Boolean right?
    public Type getSuppositionDeductionExpType(SuppositionDeductionExp exp)
            throws TypeResolutionException {
        table.beginExpressionScope();
        Type b = getType("Boolean_Theory", "B", exp.getSupposition(), false);
        try {
            Type t1 = null;
            if (exp.getSupposition().getExp() != null) {
                t1 = getMathExpType(exp.getSupposition());
                matchTypes(exp.getSupposition().getLocation(), t1, b, false,
                        false);
            }
            Iterator<Exp> i = exp.getBody().iterator();
            while (i.hasNext()) {
                getMathExpType(i.next());
            }
            t1 = getMathExpType(exp.getDeduction());
            matchTypes(exp.getDeduction().getLocation(), t1, b, false, false);
            exp.setType(b);
            table.endExpressionScope();
            return b;
        }
        catch (TypeResolutionException trex) {
            exp.setType(b);
            table.endExpressionScope();
            throw trex;
        }
    }

    // OK to leave the names of the FieldItem's null?
    public Type getTupleExpType(TupleExp exp) throws TypeResolutionException {
        List<FieldItem> fitems = new List<FieldItem>();
        Iterator<Exp> i = exp.getFields().iterator();
        FieldItem f1 = null;
        while (i.hasNext()) {
            f1 = new FieldItem(null, getMathExpType(i.next()));
            fitems.add(f1);
        }
        Type t = new TupleType(fitems);
        exp.setType(t);
        return t;
    }

    public Type getTypeFunctionExpType(TypeFunctionExp exp)
            throws TypeResolutionException {
        System.out.println("DEBUG: getTypeFunctionExpType() not implemented!");
        throw new TypeResolutionException();
        //return null;
    }

    public Type getUnaryMinusExpType(UnaryMinusExp exp)
            throws TypeResolutionException {
        Type t1 = getMathExpType(exp.getArgument());
        try {
            // Try matching it to type Real
            matchTypes(exp.getArgument().getLocation(), t1, getType(
                    "Real_Theory", "Real", exp.getArgument(), true), true,
                    false);
            exp.setType(t1);
            return t1;
        }
        catch (TypeResolutionException trex) {
            // If matching to type Real fails, try matching to type Integer
            matchTypes(exp.getArgument().getLocation(), t1, getType(
                    "Integer_Theory", "Z", exp.getArgument(), false), false,
                    false);
            exp.setType(t1);
            return t1;
        }
    }

    // -----------------------------------------------------------
    // Error Related Methods
    // -----------------------------------------------------------

    private String defNotFound(String name) {
        return "The definition " + name
                + " with the given types is not visible from this module.";
    }

    private String cantFindType(String name) {
        return "The type " + name + " is not visible from this module.";
    }

    private String facilityNotFound(String name) {
        return "The facility " + name + " is not visible from this module.";
    }

    private String theoryNotFound(String name) {
        return "The theory module " + name
                + " is not visible from this module.";
    }

    private String conceptNotFound(String name) {
        return "The concept module " + name
                + " is not visible from this module.";
    }

    private String variableNotFound(String name) {
        return "The variable " + name + " is not visible from this module.";
    }

    private String expectedDiffTypeMessage(String t1, String t2) {
        return "  Expected type: " + t1 + "\n" + "  Found type: " + t2;
    }

    private void genericError(Location loc, String msg)
            throws TypeResolutionException {
        err.error(loc, msg);
    }

}
