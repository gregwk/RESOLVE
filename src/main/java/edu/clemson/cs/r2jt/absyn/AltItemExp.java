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
 * AltItemExp.java
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

public class AltItemExp extends Exp {

    // ===========================================================
    // Variables
    // ===========================================================

    /** The location member. */
    private Location location;

    /** The test member. */
    private Exp test;

    /** The assignment member. */
    private Exp assignment;

    // ===========================================================
    // Constructors
    // ===========================================================

    public AltItemExp() {
    // Empty
    }

    public AltItemExp(Location location, Exp test, Exp assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Cannot have null assignment.");
        }

        this.location = location;
        this.test = test;
        this.assignment = assignment;
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

    /** Returns the value of the test variable. */
    public Exp getTest() {
        return test;
    }

    /** Returns the value of the assignment variable. */
    public Exp getAssignment() {
        return assignment;
    }

    // -----------------------------------------------------------
    // Set Methods
    // -----------------------------------------------------------

    /** Sets the location variable to the specified value. */
    public void setLocation(Location location) {
        this.location = location;
    }

    /** Sets the test variable to the specified value. */
    public void setTest(Exp test) {
        this.test = test;
    }

    /** Sets the assignment variable to the specified value. */
    public void setAssignment(Exp assignment) {

        if (assignment == null) {
            throw new IllegalArgumentException("Cannot have null assignment.");
        }

        this.assignment = assignment;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    public boolean equivalent(Exp e) {
        boolean result = e instanceof AltItemExp;

        if (result) {
            AltItemExp eAsAltItemExp = (AltItemExp) e;

            result = eAsAltItemExp.test.equivalent(test);
            result &= eAsAltItemExp.assignment.equivalent(assignment);
        }

        return result;
    }

    public Exp substituteChildren(java.util.Map<Exp, Exp> substitutions) {
        return new AltItemExp(location, substitute(test, substitutions),
                substitute(assignment, substitutions));
    }

    /** Accepts a ResolveConceptualVisitor. */
    public void accept(ResolveConceptualVisitor v) {
        v.visitAltItemExp(this);
    }

    /** Accepts a TypeResolutionVisitor. */
    public Type accept(TypeResolutionVisitor v) throws TypeResolutionException {
        return v.getAltItemExpType(this);
    }

    /** Returns a formatted text string of this class. */
    public String asString(int indent, int increment) {

        StringBuffer sb = new StringBuffer();

        printSpace(indent, sb);
        sb.append("AltItemExp\n");

        if (test != null) {
            sb.append(test.asString(indent + increment, increment));
        }

        if (assignment != null) {
            sb.append(assignment.asString(indent + increment, increment));
        }

        return sb.toString();
    }

    /** Returns true if the variable is found in any sub expression   
        of this one. **/
    public boolean containsVar(String varName, boolean IsOldExp) {
        Boolean found = false;
        if (test != null) {
            found = test.containsVar(varName, IsOldExp);
        }
        if (!found && assignment != null) {
            found = assignment.containsVar(varName, IsOldExp);
        }
        return found;
    }

    public List<Exp> getSubExpressions() {
        List<Exp> list = new List<Exp>();
        list.add(test);
        list.add(assignment);
        return list;
    }

    public void setSubExpression(int index, Exp e) {
        switch (index) {
        case 0:
            test = e;
            break;
        case 1:
            assignment = e;
            if (assignment == null) {
                throw new IllegalArgumentException(
                        "Cannot have null assignment.");
            }
            break;
        }
    }

    public boolean shallowCompare(Exp e2) {
        if (!(e2 instanceof AltItemExp)) {
            return false;
        }
        return true;
    }

    public String toString(int index) {
        StringBuffer sb = new StringBuffer();
        sb.append(assignment.toString(0));

        if (test != null) {
            sb.append(" if ");
            sb.append(test.toString(0));
        }
        else {
            sb.append(" otherwise");
        }
        return sb.toString();
    }

    public void prettyPrint() {
        assignment.prettyPrint();
        if (test != null) {
            System.out.print(" if ");
            test.prettyPrint();
        }
        else {
            System.out.print(" otherwise");
        }
    }

    public Exp replace(Exp old, Exp replacement) {
        AltItemExp result = (AltItemExp) copy();

        if (test != null) {
            result.test = test.replace(old, replacement);
        }

        if (assignment != null) {
            if (result.assignment == null) {
                throw new IllegalArgumentException(
                        "Cannot have null assignment.");
            }

            Exp oldAssignment = assignment;

            result.assignment = assignment.replace(old, replacement);

            if (result.assignment == null) {
                result.assignment = oldAssignment;
            }
        }

        return result;
    }

    public Exp copy() {
        Exp newTest = test;
        if (newTest != null) {
            newTest = newTest.copy();
        }

        Exp newAssignment = assignment;
        if (newAssignment != null) {
            newAssignment = newAssignment.copy();
        }

        Exp result = new AltItemExp(null, newTest, newAssignment);
        result.setType(type);

        return result;
    }

    public Object clone() {
        Exp newTest = test;
        if (newTest != null) {
            newTest = (Exp) newTest.clone();
        }

        Exp newAssignment = assignment;
        if (newAssignment != null) {
            newAssignment = (Exp) newAssignment.clone();
        }

        Exp result = new AltItemExp(null, newTest, newAssignment);
        result.setType(type);

        return result;
    }

    public Exp remember() {

        if (test instanceof OldExp)
            this.setTest(((OldExp) (test)).getExp());

        if (test != null)
            test = test.remember();

        if (assignment instanceof OldExp)
            this.setAssignment(((OldExp) (assignment)).getExp());

        if (assignment != null)
            assignment = assignment.remember();

        return this;
    }

}
