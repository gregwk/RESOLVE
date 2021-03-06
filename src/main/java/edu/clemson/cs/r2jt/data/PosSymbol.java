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
 * PosSymbol.java
 * 
 * The Resolve Software Composition Workbench Project
 * 
 * Copyright (c) 1999-2005
 * Reusable Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.data;

import java.io.File;

public class PosSymbol implements AsStringCapability {

    // ===========================================================
    // Variables
    // ===========================================================

    private Location location;

    private Symbol symbol;

    // ===========================================================
    // Constructors
    // ===========================================================

    public PosSymbol() {};

    public PosSymbol(Location location, Symbol symbol) {
        this.location = location;
        this.symbol = symbol;
    }

    // ===========================================================
    // Accessor Methods
    // ===========================================================

    /** Returns the value of the location variable. */
    public Location getLocation() {
        return location;
    }

    /** Returns the value of the symbol variable. */
    public Symbol getSymbol() {
        return symbol;
    }

    public File getFile() {
        assert location != null : "PosSymbol's location is null";
        return location.getFile();
    }

    public Pos getPos() {
        assert location != null : "PosSymbol's location is null";
        return location.getPos();
    }

    public String getName() {
        return symbol.getName();
    }

    // -----------------------------------------------------------
    // Set Methods
    // -----------------------------------------------------------

    /** Sets the location variable to the specified value. */
    public void setLocation(Location location) {
        this.location = location;
    }

    /** Sets the symbol variable to the specified value. */
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    public boolean equals(Symbol sym) {
        //return symbol == sym;
        return symbol.getName().equals(sym.getName());
    }

    /**
     * Returns true if the symbol field matches the symbol created
     * by the specified string.
     */
    public boolean equals(String str) {
        return symbol == Symbol.symbol(str);
    }

    /** Returns the string representation of the associated symbol. */
    public String toString() {
        String retval;

        if (symbol == null) {
            retval = "[No Symbol]";
        }
        else {
            retval = symbol.toString();
        }

        return retval;
    }

    /** Returns a formatted text string of this class. */
    public String asString(int indent, int increment) {
        StringBuffer sb = new StringBuffer();
        sb.append(printSpace(indent));
        sb.append(symbol.toString());
        sb.append("\n");
        return sb.toString();
    }

    // ===========================================================
    // Private Methods
    // ===========================================================

    private String printSpace(int n) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < n; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public PosSymbol copy() {
        String name = symbol.getName();
        return new PosSymbol(location, Symbol.symbol(name));
    }
}
