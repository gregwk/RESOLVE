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

package edu.clemson.cs.r2jt.treewalk;

import java.lang.reflect.*;
import java.util.*;

import edu.clemson.cs.r2jt.absyn.*;
import edu.clemson.cs.r2jt.collections.List;

/**
 * <p>The <code>TreeWalker</code> is used to apply the visitor pattern to the
 * RESOLVE abstract syntax tree. The visitor logic is implemented as a
 * <code>TreeWalkerVisitor</code>.
 */
public class TreeWalker {

    private TreeWalkerVisitor myVisitor;

    /**
     * Constructs a new <code>TreeWalker</code> that applies the logic of
     * <code>TreeWalkerVisitor</code> to a RESOLVE abstract syntax tree.
     * @param twv	An instance of TreeWalkerVisitor which implements
     * 				visitor methods to be applied to nodes of the AST.
     */
    public TreeWalker(TreeWalkerVisitor visitor) {
        this.myVisitor = visitor;
    }

    /**
     * Visits the node <code>e</code> by calling pre- visitor methods, recursively
     * visiting child nodes, and calling post- visitor methods.
     * If the <code>TreeWalkerVisitor</code> contains a method named
     * <code>walk[className]</code>, that method is called instead of visiting the children.
     * @param e	The RESOLVE abstract syntax tree node to visit/walk
     */
    public void visit(ResolveConceptualElement e) {
        if (e != null) {
            try {
                // invoke the "pre" visitor method(s)
                invokeVisitorMethods("pre", e);

                // are we overriding the walking for this element?
                if (!walkOverride(e)) {
                    List<ResolveConceptualElement> children = e.getChildren();

                    // if we have children, iterate over them
                    if (children.size() > 0) {
                        Iterator<ResolveConceptualElement> iter =
                                children.iterator();

                        ResolveConceptualElement prevChild = null, nextChild =
                                null;
                        while (iter.hasNext()) {
                            // invoke the "mid" visitor method
                            prevChild = nextChild;
                            nextChild = iter.next();
                            invokeVisitorMethods("mid", e, prevChild, nextChild);

                            // recursively visit the child
                            visit(nextChild);
                        }
                        invokeVisitorMethods("mid", e, nextChild, null);
                    }
                }

                // invoke the "post" visitor method(s)
                invokeVisitorMethods("post", e);
            }
            catch (Exception ex) {
                // if there is any exception, it is a bug
                ex.printStackTrace();
            }
        }
    }

    private void invokeVisitorMethods(String prefix,
            ResolveConceptualElement... e) {
        boolean pre = prefix.equals("pre"), post = prefix.equals("post"), mid =
                prefix.equals("mid"), list = (e[0] instanceof VirtualListNode);

        // Invoke generic visitor methods (preAny, postAny)
        if (pre) {
            myVisitor.preAny(e[0]);
        }
        else if (post) {
            myVisitor.postAny(e[0]);
        }

        // Get the heirarchy of classes from which this node inherits
        // e.g., [ConceptModuleDec, ModuleDec, Dec, ResolveConceptualElement]
        Class<?> elementClass = e[0].getClass();
        ArrayList<Class<?>> classHierarchy = new ArrayList<Class<?>>();

        if (list) {
            classHierarchy.add(((VirtualListNode) e[0]).getParent().getClass());
        }
        else if (pre || post) {
            while (elementClass != ResolveConceptualElement.class) {
                if (post) {
                    classHierarchy.add(elementClass);
                }
                else {
                    classHierarchy.add(0, elementClass);
                }
                elementClass = elementClass.getSuperclass();
            }
        }
        else {
            classHierarchy.add(elementClass);
        }

        // Iterate over the class hierarchy
        Iterator<Class<?>> iter = classHierarchy.iterator();
        while (iter.hasNext()) {
            Class<?> currentClass = iter.next();

            // Construct name of method
            String className, methodName;
            if (!list) {
                className = currentClass.getSimpleName();
            }
            else {
                className = ((VirtualListNode) e[0]).getNodeName();
            }
            methodName = prefix + className;

            // Get parent and child types if this is a list node
            Class<?> paramType = ResolveConceptualElement.class;
            if (list) {
                paramType = ((VirtualListNode) e[0]).getListType();
                e[0] = ((VirtualListNode) e[0]).getParent();
            }

            // Now try to obtain the proper visitor method
            try {
                Method visitorMethod;
                if (pre || post) { // pre and post methods
                    visitorMethod =
                            this.myVisitor.getClass().getMethod(methodName,
                                    currentClass);
                }
                else { // mid methods
                    visitorMethod =
                            this.myVisitor.getClass().getMethod(methodName,
                                    currentClass, paramType, paramType);
                }

                // Invoking the visitor method now!!!
                visitorMethod.invoke(this.myVisitor, (Object[]) e);
            }
            catch (InvocationTargetException ex1) {
                // unpack  and display the exception which
                // occurred inside the invoked method
                ex1.getCause().printStackTrace();
                throw new RuntimeException();
            }
            catch (NoSuchMethodException ex2) {
                System.err.println("Tree Walker error: method not found.");
                System.err
                        .println("The most likely cause of this error is that the TreeWalkerVisitor class"
                                + "is out of date and needs to be regenerated.");
            }
            catch (Exception exn) {
                // this is probably a "method not found" or invocation
                // exception, which either indicates a bug or that the
                // TreeWalkerVisitor class needs to be regenerated
                exn.printStackTrace();
            }
        }
    }

    private Boolean walkOverride(ResolveConceptualElement e) {
        Class<?> elementClass = e.getClass();
        ArrayList<Class<?>> classHierarchy = new ArrayList<Class<?>>();
        while (elementClass != ResolveConceptualElement.class) {
            classHierarchy.add(0, elementClass);
            elementClass = elementClass.getSuperclass();
        }

        Iterator<Class<?>> iter = classHierarchy.iterator();
        while (iter.hasNext()) {
            Class<?> c = iter.next();
            String walkMethodName = "walk" + c.getSimpleName();
            try {
                Method walkMethod =
                        this.myVisitor.getClass().getMethod(walkMethodName, c);
                walkMethod.invoke(this.myVisitor, e);
                return true;
            }
            catch (Exception ex) { /* do nothing */
            }
        }
        return false;
    }
}