
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

package com.gowan.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class JUnit3Visitor extends ASTVisitor {
	private final JUnit3Builder builder = JUnit3Builder.newInstance();
	private static final Pattern testMethod = Pattern.compile("^test");
	private static final Pattern junit3Import = Pattern.compile("junit.framework.TestCase");
	
	public static class JUnit3Builder {
		private MethodDeclaration setUp;
		private List<MethodDeclaration> test = new ArrayList<MethodDeclaration>();
		private MethodDeclaration tearDown;
		private MethodDeclaration suite;
		private ImportDeclaration testCaseImport;
		private TypeDeclaration klass;
		
		private JUnit3Builder(){}
		public static JUnit3Builder newInstance(){
			return new JUnit3Builder();
		}
		
		public JUnit3Builder setUp(MethodDeclaration setUp){
			this.setUp = setUp;
			return this;
		}
		public JUnit3Builder test(MethodDeclaration test){
			this.test.add(test);
			return this;
		}
		public JUnit3Builder suite(MethodDeclaration suite){
			this.suite = suite;
			return this;
		}
		public JUnit3Builder tearDown(MethodDeclaration tearDown){
			this.tearDown = tearDown;
			return this;
		}
		public JUnit3Builder testCaseImport(ImportDeclaration i){
			this.testCaseImport = i;
			return this;
		}
		public JUnit3Builder klass(TypeDeclaration klass){
			this.klass = klass;
			return this;
		}
		public JUnit3 build(){
			return new JUnit3(setUp, test, tearDown, suite, testCaseImport, klass);
		}
		
	}
	public static class JUnit3{
		private final MethodDeclaration setUp;
		private final List<MethodDeclaration> test;
		private final MethodDeclaration tearDown;
		private final MethodDeclaration suite;
		private final ImportDeclaration testCaseImport;
		private final TypeDeclaration klass;
		
		JUnit3(MethodDeclaration setUp, List<MethodDeclaration> test,
				MethodDeclaration tearDown, MethodDeclaration suite,
				ImportDeclaration testCaseImport, TypeDeclaration klass) {
			super();
			this.setUp = setUp;
			this.test = test;
			this.tearDown = tearDown;
			this.suite = suite;
			this.testCaseImport = testCaseImport;
			this.klass = klass;
		}

		public MethodDeclaration getSetUp() {
			return setUp;
		}

		public List<MethodDeclaration> getTest() {
			return test;
		}

		public MethodDeclaration getTearDown() {
			return tearDown;
		}

		public MethodDeclaration getSuite() {
			return suite;
		}

		public ImportDeclaration getTestCaseImport() {
			return testCaseImport;
		}

		public TypeDeclaration getKlass() {
			return klass;
		}

		@Override
		public String toString() {
			return "JUnit3 [setUp=" + setUp + ", test=" + test.toString() + ", tearDown="
					+ tearDown + ", suite=" + suite + ", testCaseImport="
					+ testCaseImport + ", klass=" + klass + "]";
		}
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		final String name = node.getName().toString();

		if( name != null && testMethod.matcher(name).find() ){
			builder.test(node);
		}else if( "setUp".equals(name) ){
			builder.setUp(node);
		}else if( "tearDown".equals(name) ){
			builder.tearDown(node);
		}else if( "suite".equals(name) ){
			builder.suite(node);
		}
		
		return super.visit(node);
	}


	@Override
	public boolean visit(ImportDeclaration node) {
		final String name = node.getName().toString();
		if( junit3Import.matcher(name).matches() ){
			builder.testCaseImport(node);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		final Type superKlass = node.getSuperclassType();
		String qualifiedName= null;
		if( superKlass != null ){
			qualifiedName = superKlass.resolveBinding().getQualifiedName();
		}
		if(superKlass != null && "junit.framework.TestCase".equals( qualifiedName )){
			builder.klass(node);
		}
		return super.visit(node);
	}
	
	public JUnit3 getJUnit3(){
		return builder.build();
	}
	
}
