/**
 * Copyright 2012  David Llewellyn-Jones <D.Llewellyn-Jones@ljmu.ac.uk>
 * Liverpool John Moores University <http://www.ljmu.ac.uk/cmp/>
 * Aniketos Project <http://www.aniketos.eu>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package eu.aniketos.wp2;

import eu.aniketos.DeclType;

/**
 * Declaration class wraps the autogenerated JAXB DeclType class
 * Provides better access to the underlying data
 * We do this, rather than amending the DeclType class directly,
 * since otherwise regenerating the JAXB classes will cause problems
 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
 *
 */
public class Declaration {
	/**
	 * The original element created by JAXB that we're providing a wrapper to
	 */
	private DeclType declaration;
	
	/**
	 * Class initialiser
	 * Create a Declaration object from the DeclType object created automatically by JAXB 
	 * @param declaration The DeclType objected created by JAXB
	 */
	public Declaration (DeclType declaration) {
		this.declaration = declaration;
	}

	/**
	 * Return the type of the declaration as a string
	 * @return The declaration type, which should be one of int, bool or string 
	 */
	public String getType () {
		return declaration.getType();
	}

	/**
	 * Return the identifier of the declaration
	 * which is basically the name of a variable that forms part of the security state
	 * @return The declaration identifier
	 */
	public String getIdentifier () {
		return declaration.getIdentifier();
	}
	
	/**
	 * Return the value expression associated with the declaration
	 * This is usually a complex nested algebraic or boolean expression that represents the value of the variable
	 * @return The declaration value expression
	 */
	public Expression getExpression() {
		return new Expression(declaration.getValue().getExpType());
	}
}
