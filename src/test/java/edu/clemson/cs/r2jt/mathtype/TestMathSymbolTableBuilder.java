package edu.clemson.cs.r2jt.mathtype;

import java.io.File;

import java.util.List;
import static org.junit.Assert.*;

import org.junit.Test;

import edu.clemson.cs.r2jt.absyn.CallStmt;
import edu.clemson.cs.r2jt.absyn.MathModuleDec;
import edu.clemson.cs.r2jt.absyn.OutfixExp;
import edu.clemson.cs.r2jt.absyn.ResolveConceptualElement;
import edu.clemson.cs.r2jt.absyn.VariableNameExp;
import edu.clemson.cs.r2jt.data.Location;
import edu.clemson.cs.r2jt.data.Pos;
import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.data.Symbol;
import edu.clemson.cs.r2jt.mathtype.DuplicateSymbolException;
import edu.clemson.cs.r2jt.mathtype.MTProper;
import edu.clemson.cs.r2jt.mathtype.MTType;
import edu.clemson.cs.r2jt.mathtype.MathSymbolTable;
import edu.clemson.cs.r2jt.mathtype.MathSymbolTableBuilder;
import edu.clemson.cs.r2jt.mathtype.MathSymbolTableEntry;
import edu.clemson.cs.r2jt.mathtype.ModuleIdentifier;
import edu.clemson.cs.r2jt.mathtype.NoSuchSymbolException;
import edu.clemson.cs.r2jt.mathtype.ScopeBuilder;

public class TestMathSymbolTableBuilder {

    private PosSymbol myPosSymbol1 =
            new PosSymbol(new Location(new File("/some/file"), new Pos(1, 1)),
                    Symbol.symbol("x"));
    private PosSymbol myPosSymbol2 =
            new PosSymbol(new Location(new File("/some/file"), new Pos(1, 1)),
                    Symbol.symbol("y"));
    private PosSymbol myPosSymbol3 =
            new PosSymbol(new Location(new File("/some/file"), new Pos(1, 1)),
                    Symbol.symbol("z"));
    private PosSymbol myPosSymbol4 =
            new PosSymbol(new Location(new File("/some/file"), new Pos(1, 1)),
                    Symbol.symbol("w"));
    private ResolveConceptualElement myConceptualElement1 =
            new VariableNameExp();
    private ResolveConceptualElement myConceptualElement2 = new OutfixExp();
    private ResolveConceptualElement myConceptualElement3 = new CallStmt();
    private MTType myType1 = new MTProper();
    private MTType myType2 = new MTProper();
    private MTType myType3 = new MTProper();

    @Test(expected = IllegalStateException.class)
    public void testFreshMathSymbolTableBuilder1() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        b.endScope();
    }

    @Test(expected = IllegalStateException.class)
    public void testFreshMathSymbolTableBuilder2() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        b.getInnermostActiveScope();
    }

    @Test(expected = NoSuchSymbolException.class)
    public void testFreshMathSymbolTableBuilder3() throws NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        b.getModuleScope(new ModuleIdentifier("NonExistent"));
    }

    @Test(expected = IllegalStateException.class)
    public void testFreshMathSymbolTableBuilder4() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        b.startScope(new VariableNameExp());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFreshMathSymbolTableBuilder5() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        b.startModuleScope(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testFreshMathSymbolTableBuilder6() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        b.addModuleImport(new ModuleIdentifier("Theory_X"));
    }

    @Test
    public void testStartModuleScope1() throws NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.getInnermostActiveScope();
        assertTrue(s.getDefiningElement() == m);

        s = b.getModuleScope(new ModuleIdentifier("x"));
        assertTrue(s.getDefiningElement() == m);
    }

    @Test(expected = IllegalStateException.class)
    public void testStartModuleScope2() throws NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);
        b.startModuleScope(m);
    }

    @Test
    public void testStartModuleScope3() throws NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier("Theory_X"));

        b.startScope(myConceptualElement1);
        b.addModuleImport(new ModuleIdentifier("Theory_Y"));

        b.endScope();
        b.endScope();
    }

    @Test(expected = NoSuchSymbolException.class)
    public void testScopeGetInnermostBinding1() throws NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.getInnermostActiveScope();

        s.getInnermostBinding("NonExistent");
    }

    @Test
    public void testScopeGetInnermostBinding2()
            throws NoSuchSymbolException,
                DuplicateSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.getInnermostActiveScope();

        s.addBinding("E", myConceptualElement1, myType1);

        MathSymbolTableEntry e = s.getInnermostBinding("E");

        assertEquals(e.getDefiningElement(), myConceptualElement1);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType1);
    }

    @Test(expected = DuplicateSymbolException.class)
    public void testScopeGetInnermostBinding3()
            throws NoSuchSymbolException,
                DuplicateSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.getInnermostActiveScope();

        s.addBinding("E", myConceptualElement1, myType1);
        s.addBinding("E", myConceptualElement1, myType1);
    }

    @Test
    public void testScopeGetInnermostBinding4()
            throws NoSuchSymbolException,
                DuplicateSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.getInnermostActiveScope();
        s.addBinding("E", myConceptualElement1, myType1);

        s = b.startScope(myConceptualElement1);
        s.addBinding("E", myConceptualElement2, myType2);

        s = b.startScope(myConceptualElement3);
        s.addBinding("G", myConceptualElement3, myType3);

        MathSymbolTableEntry e = s.getInnermostBinding("E");
        assertEquals(e.getDefiningElement(), myConceptualElement2);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType2);
    }

    @Test
    public void testScopeAllBindings1() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.getInnermostActiveScope();

        List<MathSymbolTableEntry> bindings = s.getAllBindings("NonExistent");
        assertEquals(bindings.size(), 0);

        s.buildAllBindingsList("NonExistent", bindings);
        assertEquals(bindings.size(), 0);
    }

    @Test
    public void testScopeAllBindings2() throws DuplicateSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.getInnermostActiveScope();
        s.addBinding("E", myConceptualElement1, myType1);

        s = b.startScope(myConceptualElement1);
        s.addBinding("E", myConceptualElement2, myType2);

        s = b.startScope(myConceptualElement2);
        s.addBinding("G", myConceptualElement3, myType3);

        s = b.startScope(myConceptualElement3);
        s.addBinding("H", myConceptualElement1, myType1);
        s.addBinding("E", myConceptualElement3, myType3);
        s.addBinding("J", myConceptualElement2, myType2);

        List<MathSymbolTableEntry> bindings = s.getAllBindings("E");
        assertEquals(bindings.size(), 3);

        bindings.clear();
        s.buildAllBindingsList("E", bindings);
        assertEquals(bindings.size(), 3);
    }

    @Test
    public void testGetModuleScope1() throws NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        ScopeBuilder s = b.startScope(myConceptualElement1);

        s = b.getModuleScope(new ModuleIdentifier("x"));
        assertTrue(s.getDefiningElement() == m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartScope1() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);

        b.startScope(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testEndScope() {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);
        b.startScope(myConceptualElement1);

        try {
            b.endScope();
            b.endScope();
        }
        catch (Exception e) {
            fail();
        }

        b.endScope();
    }

    @Test
    public void testGetInnermostActiveScope1() throws DuplicateSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        b.startModuleScope(m);
        assertEquals(m, b.getInnermostActiveScope().getDefiningElement());

        b.startScope(myConceptualElement1);
        assertEquals(myConceptualElement1, b.getInnermostActiveScope()
                .getDefiningElement());

        b.startScope(myConceptualElement2);
        assertEquals(myConceptualElement2, b.getInnermostActiveScope()
                .getDefiningElement());

        b.endScope();
        assertEquals(myConceptualElement1, b.getInnermostActiveScope()
                .getDefiningElement());

        b.endScope();
        assertEquals(m, b.getInnermostActiveScope().getDefiningElement());
    }

    @Test
    public void testImportBehavior1()
            throws DuplicateSymbolException,
                NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        ScopeBuilder s = b.startModuleScope(m);

        s.addBinding("E", myConceptualElement1, myType1);

        s = b.startScope(myConceptualElement1);
        s.addBinding("E", myConceptualElement2, myType2);

        b.endScope();
        b.endScope();
        //There is now a module "x" with two "E"s, one at the top level
        m = new MathModuleDec(myPosSymbol2, null, null, null);
        s = b.startModuleScope(m);
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();
        //There is now a module "y" with a single "E" (at the top level)
        m = new MathModuleDec(myPosSymbol3, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol1.getName()));
        b.addModuleImport(new ModuleIdentifier(myPosSymbol2.getName()));
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();
        //There is now a module "z" that imports both "x" and "y" and has a 
        //single "E", which is at the top level
        m = new MathModuleDec(myPosSymbol4, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol3.getName()));
        s.addBinding("E", myConceptualElement3, myType3);

        //There is now a module "w" that imports "z" and has a single "E", which
        //is at the top level.  This scope is still open.
        MathSymbolTableEntry e =
                s.getInnermostBinding("E",
                        MathSymbolTable.ImportStrategy.IMPORT_NONE);
        assertEquals(e.getDefiningElement(), myConceptualElement3);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType3);

        e =
                s.getInnermostBinding("E",
                        MathSymbolTable.ImportStrategy.IMPORT_NAMED);
        assertEquals(e.getDefiningElement(), myConceptualElement3);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType3);

        e =
                s.getInnermostBinding("E",
                        MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE);
        assertEquals(e.getDefiningElement(), myConceptualElement3);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType3);

        List<MathSymbolTableEntry> bindings =
                s.getAllBindings("E",
                        MathSymbolTable.ImportStrategy.IMPORT_NONE);
        assertEquals(1, bindings.size());

        bindings =
                s.getAllBindings("E",
                        MathSymbolTable.ImportStrategy.IMPORT_NAMED);
        assertEquals(2, bindings.size());

        bindings =
                s.getAllBindings("E",
                        MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE);
        assertEquals(bindings.size(), 4);

        bindings.clear();
        s.buildAllBindingsList("E", bindings,
                MathSymbolTable.ImportStrategy.IMPORT_NONE);
        assertEquals(bindings.size(), 1);

        bindings.clear();
        s.buildAllBindingsList("E", bindings,
                MathSymbolTable.ImportStrategy.IMPORT_NAMED);
        assertEquals(bindings.size(), 2);

        bindings.clear();
        s.buildAllBindingsList("E", bindings,
                MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE);
        assertEquals(bindings.size(), 4);
    }

    @Test(expected = NoSuchSymbolException.class)
    public void testImportBehavior2()
            throws DuplicateSymbolException,
                NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        ScopeBuilder s = b.startModuleScope(m);

        s.addBinding("E", myConceptualElement1, myType1);

        s = b.startScope(myConceptualElement1);
        s.addBinding("E", myConceptualElement2, myType2);

        b.endScope();
        b.endScope();
        //There is now a module "x" with two "E"s, one at the top level
        m = new MathModuleDec(myPosSymbol2, null, null, null);
        s = b.startModuleScope(m);
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();
        //There is now a module "y" with a single "E" (at the top level)
        m = new MathModuleDec(myPosSymbol3, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol1.getName()));
        b.addModuleImport(new ModuleIdentifier(myPosSymbol2.getName()));
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();
        //There is now a module "z" that imports both "x" and "y" and has a 
        //single "E", which is at the top level
        m = new MathModuleDec(myPosSymbol4, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol3.getName()));

        //There is now a module "w" that imports "z" and has a no "E".  This 
        //scope is still open.
        MathSymbolTableEntry e =
                s.getInnermostBinding("E",
                        MathSymbolTable.ImportStrategy.IMPORT_NONE);
        assertEquals(e.getDefiningElement(), myConceptualElement3);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType3);
    }

    @Test
    public void testImportBehavior3()
            throws DuplicateSymbolException,
                NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        ScopeBuilder s = b.startModuleScope(m);

        s.addBinding("E", myConceptualElement1, myType1);

        s = b.startScope(myConceptualElement1);
        s.addBinding("E", myConceptualElement2, myType2);

        b.endScope();
        b.endScope();
        //There is now a module "x" with two "E"s, one at the top level
        m = new MathModuleDec(myPosSymbol2, null, null, null);
        s = b.startModuleScope(m);
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();
        //There is now a module "y" with a single "E" (at the top level)
        m = new MathModuleDec(myPosSymbol3, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol1.getName()));
        b.addModuleImport(new ModuleIdentifier(myPosSymbol2.getName()));
        s.addBinding("E", myConceptualElement3, myType3);
        b.endScope();
        //There is now a module "z" that imports both "x" and "y" and has a 
        //single "E", which is at the top level
        m = new MathModuleDec(myPosSymbol4, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol3.getName()));

        //There is now a module "w" that imports "z" and has a no "E".  This 
        //scope is still open.
        MathSymbolTableEntry e =
                s.getInnermostBinding("E",
                        MathSymbolTable.ImportStrategy.IMPORT_NAMED);
        assertEquals(e.getDefiningElement(), myConceptualElement3);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType3);
    }

    @Test(expected = DuplicateSymbolException.class)
    public void testImportBehavior4()
            throws DuplicateSymbolException,
                NoSuchSymbolException {
        MathSymbolTableBuilder b = new MathSymbolTableBuilder();
        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        ScopeBuilder s = b.startModuleScope(m);

        s.addBinding("E", myConceptualElement1, myType1);

        s = b.startScope(myConceptualElement1);
        s.addBinding("E", myConceptualElement2, myType2);

        b.endScope();
        b.endScope();
        //There is now a module "x" with two "E"s, one at the top level
        m = new MathModuleDec(myPosSymbol2, null, null, null);
        s = b.startModuleScope(m);
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();
        //There is now a module "y" with a single "E" (at the top level)
        m = new MathModuleDec(myPosSymbol3, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol1.getName()));
        b.addModuleImport(new ModuleIdentifier(myPosSymbol2.getName()));
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();
        //There is now a module "z" that imports both "x" and "y" and has a 
        //single "E", which is at the top level
        m = new MathModuleDec(myPosSymbol4, null, null, null);
        s = b.startModuleScope(m);
        b.addModuleImport(new ModuleIdentifier(myPosSymbol3.getName()));
        //There is now a module "w" that imports "z" and has a no "E".  This 
        //scope is still open.
        s.getInnermostBinding("E",
                MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE);
    }
}
