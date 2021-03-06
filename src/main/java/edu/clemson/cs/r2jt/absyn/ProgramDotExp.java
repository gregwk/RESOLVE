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
 * ProgramDotExp.java
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
import edu.clemson.cs.r2jt.type.Type;
import edu.clemson.cs.r2jt.analysis.TypeResolutionException;
import edu.clemson.cs.r2jt.collections.Iterator;

public class ProgramDotExp extends ProgramExp {

    // ===========================================================
    // Variables
    // ===========================================================

    /** The location member. */
    private Location location;

    /** The segments member. */
    private List<ProgramExp> segments;

    /** The semanticExp member. */
    private ProgramExp semanticExp;

    // ===========================================================
    // Constructors
    // ===========================================================

    public ProgramDotExp() {};

    public ProgramDotExp(Location location, List<ProgramExp> segments,
            ProgramExp semanticExp) {
        this.location = location;
        this.segments = segments;
        this.semanticExp = semanticExp;
    }

    public Exp substituteChildren(java.util.Map<Exp, Exp> substitutions) {
        List<ProgramExp> newSegments = new List<ProgramExp>();
        for (ProgramExp e : segments) {
            newSegments.add((ProgramExp) substitute(e, substitutions));
        }

        return new ProgramDotExp(location, newSegments,
                (ProgramExp) substitute(semanticExp, substitutions));
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

    /** Returns the value of the segments variable. */
    public List<ProgramExp> getSegments() {
        return segments;
    }

    /** Returns the value of the semanticExp variable. */
    public ProgramExp getSemanticExp() {
        return semanticExp;
    }

    // -----------------------------------------------------------
    // Set Methods
    // -----------------------------------------------------------

    /** Sets the location variable to the specified value. */
    public void setLocation(Location location) {
        this.location = location;
    }

    /** Sets the segments variable to the specified value. */
    public void setSegments(List<ProgramExp> segments) {
        this.segments = segments;
    }

    /** Sets the semanticExp variable to the specified value. */
    public void setSemanticExp(ProgramExp semanticExp) {
        this.semanticExp = semanticExp;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /** Accepts a ResolveConceptualVisitor. */
    public void accept(ResolveConceptualVisitor v) {
        v.visitProgramDotExp(this);
    }

    /** Accepts a TypeResolutionVisitor. */
    public Type accept(TypeResolutionVisitor v) throws TypeResolutionException {
        return v.getProgramDotExpType(this);
    }

    /** Returns a formatted text string of this class. */
    public String asString(int indent, int increment) {

        StringBuffer sb = new StringBuffer();

        printSpace(indent, sb);
        sb.append("ProgramDotExp\n");

        if (segments != null) {
            sb.append(segments.asString(indent + increment, increment));
        }

        if (semanticExp != null) {
            sb.append(semanticExp.asString(indent + increment, increment));
        }

        return sb.toString();
    }

    /** Returns a formatted text string of this class. */
    public String toString(int indent) {

        StringBuffer sb = new StringBuffer();
        printSpace(indent, sb);

        sb.append(segmentsToString(this.segments));

        return sb.toString();
    }

    private String segmentsToString(List<ProgramExp> segments) {
        StringBuffer sb = new StringBuffer();
        if (segments != null) {
            Iterator<ProgramExp> i = segments.iterator();

            while (i.hasNext()) {
                sb.append(i.next().toString(0));
                if (i.hasNext())
                    sb.append(".");
            }
        }
        return sb.toString();
    }

    /** Returns true if the variable is found in any sub expression   
        of this one. **/
    public boolean containsVar(String varName, boolean IsOldExp) {
        if (segments != null) {
            Iterator<ProgramExp> i = segments.iterator();
            while (i.hasNext()) {
                ProgramExp temp = i.next();
                if (temp != null) {
                    if (temp.containsVar(varName, IsOldExp)) {
                        return true;
                    }
                }
            }
        }
        if (semanticExp != null) {
            if (semanticExp.containsVar(varName, IsOldExp)) {
                return true;
            }
        }
        return false;
    }

    public Object clone() {
        ProgramDotExp clone = new ProgramDotExp();
        clone.setSemanticExp((ProgramExp) this.getSemanticExp().clone());
        clone.setLocation(this.getLocation());
        if (segments != null) {
            Iterator<ProgramExp> i = segments.iterator();
            List<ProgramExp> newSegments = new List<ProgramExp>();
            while (i.hasNext()) {
                newSegments.add((ProgramExp) i.next().clone());
            }
            clone.setSegments(newSegments);
        }
        return clone;
    }

    public List<Exp> getSubExpressions() {
        List<Exp> list = new List<Exp>();
        Iterator<ProgramExp> segmentsIt = segments.iterator();
        while (segmentsIt.hasNext()) {
            list.add((Exp) (segmentsIt.next()));
        }
        return list;
    }

    public void setSubExpression(int index, Exp e) {
        segments.set(index, (ProgramExp) e);
    }

    public Exp replace(Exp old, Exp replacement) {
        if (old instanceof ProgramDotExp) {
            if (old.equals(this)) {
                return replacement;
            }
        }

        if ((old instanceof VarExp || old instanceof OldExp)) {
            Iterator<ProgramExp> it = segments.iterator();

            if (it.hasNext()) {
                Exp name = it.next();
                if (old instanceof VarExp && name instanceof VarExp) {
                    if (((VarExp) old).getName().toString().equals(
                            ((VarExp) name).getName().toString())) {
                        segments.remove(0);
                        segments.add(0, (ProgramExp) (replacement.clone()));

                        return this;
                    }
                }
                else if (old instanceof OldExp && name instanceof OldExp) {
                    name = name.replace(old, replacement);
                    if (name != null) {
                        segments.remove(0);
                        segments.add(0, (ProgramExp) (name.clone()));
                        return this;
                    }
                }
            }

            if (it.hasNext()) {
                Exp name = it.next();
                name = name.replace(old, replacement);
                if (name != null) {
                    segments.remove(1);
                    segments.add(1, (ProgramExp) (name.clone()));
                    return this;
                }
            }
        }

        return this;
    }

}
