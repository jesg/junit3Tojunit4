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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;

public class SelectionToICompilationUnitList implements List<ICompilationUnit> {
	private List<ICompilationUnit> list;
	
	public SelectionToICompilationUnitList(IStructuredSelection selection){
		List<ICompilationUnit> localList = new ArrayList<ICompilationUnit>();
		List<Object> selectionList = selection.toList();
		for (Object object : selectionList) {
			localList.addAll( delegate(object) );
		}
		this.list = localList;
	}
	protected List<ICompilationUnit> delegate(Object selected){
		List<ICompilationUnit> compilationUnits = new ArrayList<ICompilationUnit>();
		if( selected instanceof ICompilationUnit ){
			compilationUnits.add((ICompilationUnit)selected);
		}else if ( selected instanceof IPackageFragment){
			compilationUnits.addAll(get((IPackageFragment)selected));
		}else if( selected instanceof IPackageFragmentRoot){
			compilationUnits.addAll(get((IPackageFragmentRoot)selected));
		}else if( selected instanceof IJavaProject){
			compilationUnits.addAll(get((IJavaProject)selected));
		}
		return compilationUnits;
	}
	protected List<ICompilationUnit> get(IJavaProject selected) {
		List<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		IPackageFragmentRoot[] packageRoots;
		try {
			packageRoots = selected.getAllPackageFragmentRoots();
			for(int i=0; i<packageRoots.length; i++){
				result.addAll(get(packageRoots[i]));
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return result;
	}

	protected List<ICompilationUnit> get(IPackageFragment frag){
		List<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		try {
			result = Arrays.asList(frag.getCompilationUnits());
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return result;
	}
	protected List<ICompilationUnit> get(IPackageFragmentRoot root){
		List<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		try {
			for (IJavaElement frag : Arrays.asList(root.getChildren())) {
				if( frag instanceof IPackageFragment ){
					result.addAll(get((IPackageFragment) frag));
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return result;
	}
	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public Iterator<ICompilationUnit> iterator() {
		return list.iterator();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	@Override
	public boolean add(ICompilationUnit e) {
		return list.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.contains(c);
	}

	@Override
	public boolean addAll(Collection<? extends ICompilationUnit> c) {
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends ICompilationUnit> c) {
		return list.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public ICompilationUnit get(int index) {
		return list.get(index);
	}

	@Override
	public ICompilationUnit set(int index, ICompilationUnit element) {
		return list.set(index, element);
	}

	@Override
	public void add(int index, ICompilationUnit element) {
		list.add(index, element);
	}

	@Override
	public ICompilationUnit remove(int index) {
		return list.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<ICompilationUnit> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<ICompilationUnit> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public List<ICompilationUnit> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

}
