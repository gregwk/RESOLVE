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
 * PrefixExp.java
 * 
 * The Resolve Software Composition Workbench Project
 * 
 * Copyright (c) 1999-2005
 * Reusable Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.absyn;

import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.data.Location;
import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.type.Type;
import edu.clemson.cs.r2jt.analysis.TypeResolutionException;

public class PrefixExp extends Exp {

    // ===========================================================
    // Variables
    // ===========================================================

    /** The location member. */
    private Location location;

    /** The symbol member. */
    private PosSymbol symbol;

    /** The argument member. */
    private Exp argument;

    // ===========================================================
    // Constructors
    // ===========================================================

    public PrefixExp() {};

    public PrefixExp(Location location, PosSymbol symbol, Exp argument) {
        this.location = location;
        this.symbol = symbol;
        this.argument = argument;
    }

    // special constructor to use when we can determine the statement return 
    // type while building the symbol table in RBuilder.g
    public PrefixExp(Location location, PosSymbol symbol, Exp argument,
            Type bType) {
        this.location = location;
        this.symbol = symbol;
        this.argument = argument;
        super.bType = bType;
    }

    public Object clone() {
        PrefixExp clone = new PrefixExp();
        clone.setLocation(this.getLocation());
        clone.symbol = this.symbol.copy();
        if (this.argument != null) {
            clone.argument = (Exp) this.argument.clone();
        }

        clone.setType(this.bType);
        return clone;
    }

    public Exp substituteChildren(java.util.Map<Exp, Exp> substitutions) {
        PrefixExp retval =
                new PrefixExp(location, symbol, substitute(argument,
                        substitutions));

        retval.setType(type);

        return retval;
    }

    // ===========================================================
    // Accessor Methods
    // ===========================================================

    // -----------------------------------------------------------
    // Get Methods
    // -----------------------------------------------------------

    /** Returns the value of the location variable. */
    public Location getLocation() {
        return location;
    }

    /** Returns the value of the symbol variable. */
    public PosSymbol getSymbol() {
        return symbol;
    }

    /** Returns the value of the argument variable. */
    public Exp getArgument() {
        return argument;
    }

    // -----------------------------------------------------------
    // Set Methods
    // -----------------------------------------------------------

    /** Sets the location variable to the specified value. */
    public void setLocation(Location location) {
        this.location = location;
    }

    /** Sets the symbol variable to the specified value. */
    public void setSymbol(PosSymbol symbol) {
        this.symbol = symbol;
    }

    /** Sets the argument variable to the specified value. */
    public void setArgument(Exp argument) {
        this.argument = argument;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /** Accepts a ResolveConceptualVisitor. */
    public void accept(ResolveConceptualVisitor v) {
        v.visitPrefixExp(this);
    }

    /** Accepts a TypeResolutionVisitor. */
    public Type accept(TypeResolutionVisitor v) throws TypeResolutionException {
        return v.getPrefixExpType(this);
    }

    /** Returns a formatted text string of this class. */
    public String asString(int indent, int increment) {

        StringBuffer sb = new StringBuffer();

        printSpace(indent, sb);
        sb.append("PrefixExp\n");

        if (symbol != null) {
            sb.append(symbol.asString(indent + increment, increment));
        }

        if (argument != null) {
            sb.append(argument.asString(indent + increment, increment));
        }

        return sb.toString();
    }

    public String toString(int indent) {
        StringBuffer sb = new StringBuffer();
        printSpace(indent, sb);
        if (symbol != null)
            sb.append(symbol.getName().toString());
        if (argument != null)
            sb.append("(" + argument.toString(0) + ")");
        return sb.toString();
    }

    public String toIsabelleString(int indent) {
        StringBuffer sb = new StringBuffer();
        printSpace(indent, sb);
        if (symbol != null)
            sb.append(symbol.getName().toString());
        if (argument != null)
            sb.append("(" + argument.toString(0) + ")");
        return sb.toString();
    }

    /** Returns true if the variable is found in any sub expression   
        of this one. **/
    public boolean containsVar(String varName, boolean IsOldExp) {
        if (argument != null) {
            return argument.containsVar(varName, IsOldExp);
        }
        return false;
    }

    public List<Exp> getSubExpressions() {
        List<Exp> list = new List<Exp>();
        list.add(argument);
        return list;
    }

    public void setSubExpression(int index, Exp e) {
        argument = e;
    }

    public boolean shallowCompare(Exp e2) {
        if (!(e2 instanceof PrefixExp)) {
            return false;
        }
        if (!(symbol.equals(((PrefixExp) e2).getSymbol().getName()))) {
            return false;
        }
        return true;
    }

    public boolean equivalent(Exp e) {
        boolean retval = (e instanceof PrefixExp);

        if (retval) {
            PrefixExp eAsPrefixExp = (PrefixExp) e;
            retval =
                    Exp.posSymbolEquivalent(symbol, eAsPrefixExp.symbol)
                            && Exp.equivalent(argument, eAsPrefixExp.argument);
        }

        return retval;
    }

    public void prettyPrint() {
        System.out.print(symbol.getName() + "(");
        argument.prettyPrint();
        System.out.print(")");
    }

    public Exp copy() {
        PrefixExp retval;

        PosSymbol newSymbol = symbol.copy();
        Exp newArgument = argument.copy();

        retval = new PrefixExp(null, newSymbol, newArgument);
        retval.setType(type);
        return retval;
    }

    public Exp replace(Exp old, Exp replacement) {
        if (!(old instanceof PrefixExp)) {
            if (this.argument != null) {
                Exp newArgument = argument.replace(old, replacement);
                if (newArgument != null) {
                    this.setArgument(newArgument);
                }
            }
            if (this.symbol != null && old instanceof VarExp)
                if (symbol.toString().equals(
                        ((VarExp) old).getName().toString())) {
                    if (replacement instanceof VarExp)
                        symbol = ((VarExp) replacement).getName();
                }
        }
        else {}
        //
        return this;
    }

    public Exp remember() {
        if (argument instanceof OldExp)
            this.setArgument(((OldExp) (argument)).getExp());
        else {
            if (argument != null) {
                argument = argument.remember();
            }
        }
        return this;
    }

    public Exp simplify() {
        if (argument instanceof EqualsExp) {
            if (((EqualsExp) argument).getOperator() == EqualsExp.EQUAL)
                ((EqualsExp) argument).setOperator(EqualsExp.NOT_EQUAL);
            else
                ((EqualsExp) argument).setOperator(EqualsExp.EQUAL);
            return argument;
        }
        else
            return this;
    }
}
