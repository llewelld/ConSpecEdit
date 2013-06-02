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

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import eu.aniketos.Binary;
import eu.aniketos.InvocationType;
import eu.aniketos.Unary;
import eu.aniketos.InvocationType.Argument;

/**
 * A class for containing and managing potentially complex algebraic or boolean expressions
 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
 *
 */
public class Expression {
	/**
	 * Class initialiser
	 * Effectively turns a JAXBElement with restricted type information into a more manageable Expression class
	 * We provide this to avoid having to alter the generated classes directly 
	 * @param value The JAXBElement object hierarchy to wrap up as an Expression class
	 */
	public Expression (JAXBElement<?> value) {
		this.value = value;
	}

	/**
	 * Parses an expression provided as a string into a JAXBElement object hierarchy that can be used by JAXB.
	 * @param value The expression in human-readable form to be parsed.
	 */
	public Expression (String value) {
		this.value = evaluateString(value).complete;
	}

	/**
	 * Return the parsed JAXBElement object hierarchy.
	 * @return The root of the JAXBElement expression hierarchy. 
	 */
	public JAXBElement<?> getValue() {
		return value;
	}
	
	/**
	 * The JAXBElement object that we're wrapping up.
	 */
	private JAXBElement<?> value;

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		// Convert the JAXBElement object hierarchy into a human-readable string expression.,
		return ConvertExpression (value);
	}

	/**
	 * Converts the expression into a string.
	 * The method is called recursively to construct the string from the object hierarchy.
	 * @param value The JAXBElement object hierarchy to convert into a string.
	 * @return String representing the expression.
	 */
	@SuppressWarnings("unchecked")
	private static String ConvertExpression (JAXBElement<?> value) {
		// The string representing the converted expression.
		String valueText;

		// Check whether the value exists.
		if (value != null) {
			// Get the type of expression.
			String name = value.getName().getLocalPart();

			// We have to convert into different strings depending on the name.
			// Then recursively call this method to deal with any subexpressions.
			switch (name) {
			case "sum":
				valueText = ConvertBinary ("+", value);
				break;
			case "morequalthan":
				valueText = ConvertBinary (">=", value);
				break;
			case "lessequalthan":
				valueText = ConvertBinary ("<=", value);
				break;
			case "equal":
				valueText = ConvertBinary ("==", value);
				break;
			case "or":
				valueText = ConvertBinary ("||", value);
				break;
			case "append":
				valueText = ConvertBinary ("|", value);
				break;
			case "notequal":
				valueText = ConvertBinary ("!=", value);
				break;
			case "mod":
				valueText = ConvertBinary ("%", value);
				break;
			case "and":
				valueText = ConvertBinary ("&&", value);
				break;
			case "dif":
				valueText = ConvertBinary ("-", value);
				break;
			case "mul":
				valueText = ConvertBinary ("*", value);
				break;
			case "morethan":
				valueText = ConvertBinary (">", value);
				break;
			case "lessthan":
				valueText = ConvertBinary ("<", value);
				break;
			case "iconst":
				// Integer constant.
				valueText = ((JAXBElement<BigInteger>)value).getValue().toString();
				break;
			case "invocation":
				// Invocations are a bit more complex, so need to be presented slightly differently.
				InvocationType invocation = ((JAXBElement<InvocationType>)value).getValue();
				valueText = "Invocation\t" + invocation.getIdentifier() + "\t arguments:\n";
				Iterator<Argument> argumentIter = invocation.getArgument().iterator();
				while (argumentIter.hasNext()) {
					Argument argument = argumentIter.next();
					valueText += "\t\t" + ConvertExpression(argument.getExpType()) + "\n";
				}
				break;
			case "not":
				valueText = "(!" + ConvertExpression(((JAXBElement<Unary>)value).getValue().getExpType()) + ")";
				break;
			case "sconst":
				// String constant.
				valueText = "\"" + ((JAXBElement<String>)value).getValue() + "\"";
				break;
			case "bconst":
				// Boolean constant.
				valueText = ((JAXBElement<Boolean>)value).getValue().toString();
				break;
			case "identifier":
				valueText = ((JAXBElement<String>)value).getValue();
				break;
			default:
				// This shouldn't happen, but we need it just in case.
				valueText = "[UNKNOWN]";
				break;
			}
		}
		else {
			// There is no name; this shouldn't happen, but we need it just in case.
			valueText = "[NULL]";
		}

		// Return the resulting string.
		return valueText;
	}

	/**
	 * Most of the expressions are binary expressions, so this is a utility method to deal with them.
	 * @param name The name of the binary function; this will be inserted directly into the string.
	 * @param value The element list containing exactly two elements representing the LHS and RHS of the binary operation.
	 * @return The string value representing the binary operation.
	 */
	private static String ConvertBinary (String name, JAXBElement<?> value) {
		String valueText;

		// JAXBElements lack full type information, so we need to leave typechecking until runtime.
		@SuppressWarnings("unchecked")
		List<JAXBElement<?>> params = ((JAXBElement<Binary>)value).getValue().getExpType();
		// Get the LHS element form the list.
		JAXBElement<?> lhs = params.get(0);
		// Get the RHS element form the list.
		JAXBElement<?> rhs = params.get(1);
		// Construct the expression using recursion for the subexpressions.
		valueText = "(" + ConvertExpression(lhs) + " " + name + " " + ConvertExpression(rhs) + ")";

		// Return the reconstructed expression.
		return valueText;
	}
	
	/**
	 * Private class used for returning the state of the current recursive conversion operation.
	 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
	 *
	 */
	static private class ConvertResult {
		/**
		 * The current hierarchy that has already been generated from the parsing process.
		 */
		public JAXBElement<?> complete = null;
		/**
		 * The remaining text go be parsed.
		 */
		public String remaining = "";
	}

	/**
	 * Populate the JAXBElement object hierarchy from a human-readable string expression.
	 * @param expression The string to parse.
	 */
	public void fromString (String expression) {
		value = evaluateString(expression).complete;
	}

	/**
	 * Convert a human-readable string expression into a JAXBElement object hierarchy.
	 * @param expression The string to parse.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed. 
	 */
	public static ConvertResult evaluateString (String expression) {
		// Set up the return structure.
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		if ((expression == null) || (expression.length() <= 0)) {
			result.remaining = "0";
		}

		// Recursively deconstruct the string into expressions
		// Remove whitespace
		StringBuffer clean = new StringBuffer();
		StringTokenizer split = new StringTokenizer(result.remaining, " ");
		while (split.hasMoreElements()) {
			clean.append(split.nextToken());
		}
		result.remaining = clean.toString();

		// Parse from left to right greedily
		result = checkConcat(result.remaining);

		// Return the result of the parsing process
		return result;
	}
	
	/**
	 * Convert any string concatenation operators into an object hierarchy.
	 * Will first check if there's a logical operator with precedence and convert this first if there is.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ConvertResult checkConcat (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		result = checkLogic (result.remaining);
		//JAXBElement<?> result = checkLogic (expression);
		boolean more = true;
		
		while (more && (result.remaining.length() > 1)) {
			if (result.remaining.charAt(0) == '|') {
				result.remaining = result.remaining.substring(2);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkLogic(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("append");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else {
				more = false;
			}
		}

		return result;
	}	
	
	/**
	 * Convert any logical operators (and/or) into an object hierarchy.
	 * Will first check if there's a comparison (without equivalence) with precedence and convert this first if there is.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ConvertResult checkLogic (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		result = checkCompare (result.remaining);
		//JAXBElement<?> result = checkCompare (expression);
		boolean more = true;
		
		while (more && (result.remaining.length() > 1)) {
			if (result.remaining.startsWith("||")) {
				result.remaining = result.remaining.substring(2);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkCompare(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("or");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else if (result.remaining.startsWith("&&")) {
				result.remaining = result.remaining.substring(2);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkCompare(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("and");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else {
				more = false;
			}
		}

		return result;
	}
		
	/**
	 * Convert any comparisons (without equivalence) into an object hierarchy.
	 * Will first check if there's a comparison (with equivalence) with precedence and convert this first if there is.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ConvertResult checkCompare (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		result = checkCompareEq (result.remaining);
		//JAXBElement<?> result = checkCompareEq (expression);
		boolean more = true;
		
		while (more && (result.remaining.length() > 0)) {
			if (result.remaining.charAt(0) == '>') {
				result.remaining = result.remaining.substring(1);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkCompareEq(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("morethan");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else if (result.remaining.charAt(0) == '<') {
				result.remaining = result.remaining.substring(1);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkCompareEq(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("lessthan");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else {
				more = false;
			}
		}

		return result;
	}
	
	/**
	 * Convert any comparisons (with equivalence) into an object hierarchy.
	 * Will first check if there's a sum with precedence and convert this first if there is.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ConvertResult checkCompareEq (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		result = checkSum (result.remaining);
		//JAXBElement<?> result = checkSum (expression);
		boolean more = true;
		
		while (more && (result.remaining.length() > 1)) {
			if (result.remaining.startsWith(">=")) {
				result.remaining = result.remaining.substring(2);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkSum(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("morequalthan");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else if (result.remaining.startsWith("<=")) {
				result.remaining = result.remaining.substring(2);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkSum(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("lessequalthan");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else if (result.remaining.startsWith("==")) {
				result.remaining = result.remaining.substring(2);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkSum(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("equal");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else if (result.remaining.startsWith("!=")) {
				result.remaining = result.remaining.substring(2);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkSum(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("notequal");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else {
				more = false;
			}
		}

		return result;
	}
	
	/**
	 * Convert any additions or subtractions into an object hierarchy.
	 * Will first check if there's a multiplication with precedence and convert this first if there is.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ConvertResult checkSum (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		result = checkMul (result.remaining);
		//JAXBElement<?> result = checkMul (expression);
		boolean more = true;
		
		while (more && (result.remaining.length() > 0)) {
			if (result.remaining.charAt(0) == '+') {
				result.remaining = result.remaining.substring(1);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkMul(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("sum");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else if (result.remaining.charAt(0) == '-') {
				result.remaining = result.remaining.substring(1);
				Binary binary= new Binary();
				binary.getExpType().add(result.complete);
				result = checkMul(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("dif");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else {
				more = false;
			}
		}

		return result;
	}

	/**
	 * Convert a multiplications or moduluses into an object hierarchy.
	 * Will first check if there's a unary operation with precedence and convert this first if there is.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ConvertResult checkMul (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		// TODO: Check for other binary relations (logical operations, conditions, etc.) before checking for unary operations.
		result = checkUnary (result.remaining);
		boolean more = true;
		
		while (more && (result.remaining.length() > 0)) {
			if (result.remaining.charAt(0) == '*') {
				result.remaining = result.remaining.substring(1);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkUnary(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("mul");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else if (result.remaining.charAt(0) == '%') {
				result.remaining = result.remaining.substring(1);
				Binary binary = new Binary();
				binary.getExpType().add(result.complete);
				result = checkUnary(result.remaining);
				binary.getExpType().add(result.complete);
				QName name = new QName("mod");
				result.complete = new JAXBElement(name, Binary.class, binary);
			}
			else {
				more = false;
			}
		}

		return result;
	}
	
	/**
	 * Convert a unary operation into an object hierarchy.
	 * If it's not a unary operation, will check whether there are brackets that should be removed from the expression.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ConvertResult checkUnary (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;

		if ((result.remaining.indexOf("!") == 0) && ((result.remaining.length() <= 1) || (result.remaining.charAt(1) != '='))) {
			result.remaining = result.remaining.substring(1);
			Unary unary = new Unary();
			result = checkUnary(result.remaining);
			unary.setExpType(result.complete);
			QName name = new QName("not");
			result.complete = new JAXBElement(name, Unary.class, unary);
		}
		else {
			result = checkBrackets(result.remaining);
		}
		
		return result;
	}
	
	/**
	 * Remove brackets from an expression to allow it to be converted into an object hierarchy.
	 * If there are no brackets to remove, will check whether it's a constant value.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	private static ConvertResult checkBrackets (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;

		if (result.remaining.charAt(0) == '(') {
			result.remaining = result.remaining.substring(1);
			result = checkConcat(result.remaining);
			if (result.remaining.length() > 0) {
				result.remaining = result.remaining.substring(1);
			}
		}
		else {
			result = checkConstant(result.remaining);
		}
		
		return result;
	}

	/**
	 * Convert a constant value into an object.
	 * At this stage we already checked for everything else, so if it's not a number, whatever's left must be an identifier.
	 * @param expression The expression to be converted.
	 * @return The result of the parsing, containing the converted JAXBElement object hierarchy and the remaining string to be parsed.
	 */
	private static ConvertResult checkConstant (String expression) {
		ConvertResult result = new ConvertResult();
		result.remaining = expression;
		String value = "";

		// TODO: Check first whether this is a number, a bool or a string and convert it appropriately.
		while ((result.remaining.length() > 0) && ("+-*%><=!|&()".indexOf(result.remaining.charAt(0)) < 0)) {
			value += result.remaining.charAt(0);
			result.remaining = result.remaining.substring(1);
		}
		if (value.equals("true") || value.equals("false")) {
			QName name = new QName("bconst");
			result.complete = new JAXBElement<Boolean>(name, Boolean.class, (value.equals("true")));
		}
		else if ((value.length() >= 2) && value.startsWith("\"") && value.endsWith("\"")) {
			QName name = new QName("sconst");
			result.complete = new JAXBElement<String>(name, String.class, value.substring(1, (value.length() - 1)));
		}
		else {
			try {
				long num = Integer.parseInt(value);
				QName name = new QName("iconst");
				result.complete = new JAXBElement<BigInteger>(name, BigInteger.class, BigInteger.valueOf(num));
			}
			catch (NumberFormatException e) {
				QName name = new QName("identifier");
				result.complete = new JAXBElement<String>(name, String.class, value);
			}
		}
		
		return result;
	}
}
