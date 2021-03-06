/*
 * This softare is released under the new BSD 2006 license.
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
 * ConstructedTy.java
 * 
 * The Resolve Software Composition Workbench Project
 * 
 * Copyright (c) 1999-2005
 * Reusable Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.absyn;

import edu.clemson.cs.r2jt.collections.Iterator;
import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.type.Type;
import edu.clemson.cs.r2jt.analysis.TypeResolutionException;

public class ConstructedTy extends Ty {

    // ===========================================================
    // Variables
    // ===========================================================

    /** The qualifier member. */
    private PosSymbol qualifier;

    /** The name member. */
    private PosSymbol name;

    /** The args member. */
    private List<Ty> args;

    // ===========================================================
    // Constructors
    // ===========================================================

    public ConstructedTy() {};

    public ConstructedTy(PosSymbol qualifier, PosSymbol name, List<Ty> args) {
        this.qualifier = qualifier;
        this.name = name;
        this.args = args;
    }

    // ===========================================================
    // Accessor Methods
    // ===========================================================

    // -----------------------------------------------------------
    // Get Methods
    // -----------------------------------------------------------

    /** Returns the value of the qualifier variable. */
    public PosSymbol getQualifier() {
        return qualifier;
    }

    /** Returns the value of the name variable. */
    public PosSymbol getName() {
        return name;
    }

    /** Returns the value of the args variable. */
    public List<Ty> getArgs() {
        return args;
    }

    // -----------------------------------------------------------
    // Set Methods
    // -----------------------------------------------------------

    /** Sets the qualifier variable to the specified value. */
    public void setQualifier(PosSymbol qualifier) {
        this.qualifier = qualifier;
    }

    /** Sets the name variable to the specified value. */
    public void setName(PosSymbol name) {
        this.name = name;
    }

    /** Sets the args variable to the specified value. */
    public void setArgs(List<Ty> args) {
        this.args = args;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /** Accepts a ResolveConceptualVisitor. */
    public void accept(ResolveConceptualVisitor v) {
        v.visitConstructedTy(this);
    }

    /** Accepts a TypeResolutionVisitor. */
    public Type accept(TypeResolutionVisitor v) throws TypeResolutionException {
        return v.getConstructedTyType(this);
    }

    /** Returns a formatted text string of this class. */
    public String asString(int indent, int increment) {

        StringBuffer sb = new StringBuffer();

        printSpace(indent, sb);
        sb.append("ConstructedTy\n");

        if (qualifier != null) {
            sb.append(qualifier.asString(indent + increment, increment));
        }

        if (name != null) {
            sb.append(name.asString(indent + increment, increment));
        }

        if (args != null) {
            sb.append(args.asString(indent + increment, increment));
        }

        return sb.toString();
    }

    public void prettyPrint() {
        if (qualifier != null)
            System.out.print(qualifier.getName() + ".");
        System.out.print(name.getName());
        Iterator<Ty> it = args.iterator();
        System.out.print("(");
        if (it.hasNext()) {
            it.next().prettyPrint();
        }
        while (it.hasNext()) {
            System.out.print(", ");
            it.next().prettyPrint();
        }
        System.out.print(")");
    }

    public String toString(int indent) {
        StringBuffer sb = new StringBuffer();
        if (qualifier != null)
            sb.append(qualifier.getName() + ".");
        sb.append(name.getName());
        Iterator<Ty> it = args.iterator();
        sb.append("(");
        if (it.hasNext()) {
            sb.append(it.next().toString(0));
        }
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(it.next().toString(0));
        }
        sb.append(")");
        return sb.toString();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (qualifier != null)
            sb.append(qualifier.getName() + ".");
        sb.append(name.getName());
        Iterator<Ty> it = args.iterator();
        sb.append("(");
        if (it.hasNext()) {
            sb.append(it.next().toString(0));
        }
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(it.next().toString(0));
        }
        sb.append(")");
        return sb.toString();
    }

    public Ty copy() {
        PosSymbol newQualifier = null;
        if (qualifier != null)
            newQualifier = qualifier.copy();
        PosSymbol newName = name.copy();
        Iterator<Ty> it = args.iterator();
        List<Ty> newArgs = new List<Ty>();
        while (it.hasNext()) {
            newArgs.add(it.next().copy());
        }
        return new ConstructedTy(newQualifier, newName, newArgs);
    }
}
