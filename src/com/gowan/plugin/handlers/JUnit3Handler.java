/*
Copyright (C) 2013  Jason Gowan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.gowan.plugin.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gowan.plugin.JUnit3Visitor;
import com.gowan.plugin.JUnit3Visitor.JUnit3;
import com.gowan.plugin.SelectionToICompilationUnitList;


public class JUnit3Handler extends AbstractHandler {
	private static final String[] AFTER = new String[] {"org", "junit", "After"};
	private static final String[] BEFORE = new String[] {"org", "junit", "Before"};
	private static final String[] TEST = new String[] {"org", "junit", "Test"};

	  private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";

	  @Override
	  public Object execute(ExecutionEvent event) throws ExecutionException {
		  Shell shell = HandlerUtil.getActiveShell(event);
		  ISelection sel = HandlerUtil.getActiveMenuSelection(event);
		  IStructuredSelection selection = null;
		  List<ICompilationUnit> units = null;
		  try{
			  selection = (IStructuredSelection) sel; 
			  units = new SelectionToICompilationUnitList(selection);
		  }catch(ClassCastException e){
			  MessageDialog.openInformation(shell, "Info", "Please select a Java Source file");
			  return null;
		  }

		  try {
			  for (ICompilationUnit iCompilationUnit : units) {
				  createAST(iCompilationUnit);
			  }
			} catch (JavaModelException e) {
				e.printStackTrace();
			}

	    return null;
	  }
	  /**
	   * 
	   * @param unit
	   * @throws JavaModelException
	   */
	  private void createAST(ICompilationUnit unit)
	      throws JavaModelException {
		  CompilationUnit parse = parse(unit);
		  JUnit3Visitor visitor = new JUnit3Visitor();
		  parse.accept(visitor);
		  
		  IDocument doc = new Document(unit.getSource());
		  AST ast = parse.getAST();
		  ASTRewrite rewrite = ASTRewrite.create(ast);
		  JUnit3 junit = visitor.getJUnit3();

		  TypeDeclaration td = (TypeDeclaration) parse.types().get(0);
		  ITrackedNodePosition tdLocation = rewrite.track(td);
		  
		  if( junit.getKlass() != null ){
			  rewrite.replace(td.getSuperclassType(), null, null);
		  }else{
			  return; // Skip if the class does not extend junit.framework.TestCase
		  }
		  
		//	      imports
		  ImportDeclaration afterImport = ast.newImportDeclaration();
		  afterImport.setName(ast.newName(AFTER));
		  ImportDeclaration beforeImport = ast.newImportDeclaration();
		  beforeImport.setName(ast.newName(BEFORE));
		  ImportDeclaration testImport = ast.newImportDeclaration();
		  testImport.setName(ast.newName(TEST));
		  
		  
		  ListRewrite lrw = rewrite.getListRewrite(parse, CompilationUnit.IMPORTS_PROPERTY);
		  if( junit.getTestCaseImport() != null){
			  lrw.remove(junit.getTestCaseImport(), null);
			  
		      lrw.insertLast(afterImport, null);
		      lrw.insertLast(beforeImport, null);
		      lrw.insertLast(testImport, null);
		  }
		  
		  if( junit.getSetUp() != null ) {
			  transformSetUp(ast, rewrite, junit);
		  }
		  if( junit.getTearDown() != null ){
			  transformTearDown(ast, rewrite, junit);
		  }
		  if( junit.getTest() != null && !junit.getTest().isEmpty() ){
			  transformTest(ast, rewrite, junit);
		  }
		  		  
		  TextEdit edits = rewrite.rewriteAST(doc, null);
		  
		  unit.applyTextEdit(edits, null);

	  }

	private void transformTest(AST ast, ASTRewrite rewrite, JUnit3 junit) {
		final List<MethodDeclaration> original = junit.getTest();
		
		for (MethodDeclaration methodDeclaration : original) {
			transformMethod(ast, rewrite, methodDeclaration, "Test");
		}
	}

	private void transformSetUp(AST ast, ASTRewrite rewrite, JUnit3 junit) {
		final MethodDeclaration original = junit.getSetUp();
		transformMethod(ast, rewrite, original, "Before");
	}
	private void transformTearDown(AST ast, ASTRewrite rewrite, JUnit3 junit) {
		final MethodDeclaration original = junit.getTearDown();
		transformMethod(ast, rewrite, original, "After");
	}
	
	private void transformMethod(AST ast, ASTRewrite rewrite,MethodDeclaration original, String annotationName){
		  ListRewrite setupRw = rewrite.getListRewrite(original, MethodDeclaration.MODIFIERS2_PROPERTY);
		  MarkerAnnotation annotation = ast.newMarkerAnnotation();
		  annotation.setTypeName(ast.newSimpleName(annotationName));
		  setupRw.insertFirst(annotation, null);
		  
		  List<IExtendedModifier> modifiers = original.modifiers();
		  Annotation override = null;
		  for (IExtendedModifier iExtendedModifier : modifiers) {
			if( iExtendedModifier.isAnnotation() ){
				Annotation localAnnotation = (Annotation) iExtendedModifier;
				String fullName = localAnnotation.getTypeName().getFullyQualifiedName();
				if( "java.lang.Override".equals(fullName) || "Override".equals(fullName) ){
					override = localAnnotation;
					break;
				}
			}
		  }
		  try{
			  setupRw.remove(override, null);
		  }catch(IllegalArgumentException e){}
	}
  
	/**
	   * Reads a ICompilationUnit and creates the AST DOM for manipulating the
	   * Java source file
	   * 
	   * @param unit
	   * @return
	   */
	  private static CompilationUnit parse(ICompilationUnit unit) {
	    ASTParser parser = ASTParser.newParser(AST.JLS3);
	    parser.setKind(ASTParser.K_COMPILATION_UNIT);
	    parser.setSource(unit);
	    parser.setResolveBindings(true);
	    return (CompilationUnit) parser.createAST(null); // parse
	  }
}
