package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.ast.Errors.BINDING_INCONSISTENT;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class Binding extends Element {
	public Binding(Location loc, NonTerminal nt, List<Element> l) {
		super(loc);
		nonTerminal = nt;
		elements = l;
	}

	
  @Override
  public int hashCode() {
    return nonTerminal.hashCode() + elements.hashCode();
  }

	@Override
  public boolean equals(Object obj) {
	  if (!(obj instanceof Binding)) return false;
	  Binding b = (Binding)obj;
	  return nonTerminal.equals(b.nonTerminal) && elements.equals(b.elements);
  }


  public NonTerminal getNonTerminal() { return nonTerminal; }
	public List<Element> getElements() { return elements; }
	public ElemType getElemType() { return nonTerminal.getType(); }
	public Symbol getGrmSymbol() {
		return nonTerminal.getGrmSymbol();
		//throw new RuntimeException("not implemented");
	}

	public String getTerminalSymbolString() {
		return nonTerminal.getType().getTermSymbolString();
	}

	private List<Element> elements;
	private NonTerminal nonTerminal;

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		//System.err.println("binding.prettyPrint for " + (ctx == null ? "" : ctx.term));
		if (ctx != null) {
			ctx.boundVars = new ArrayList<String>(ctx.boundVars);
			String varName = "<aVar" + ctx.boundVarCount++ + ">";
			ctx.boundVars.add(varName);
			Term t = ctx.term;
			if (t instanceof Abstraction)
				t = ((Abstraction)t).getBody();
			nonTerminal.prettyPrint(out, new PrintContext(t, ctx));
		} else {
			nonTerminal.prettyPrint(out);
			for (Element e : elements) {
				out.print('[');
				e.prettyPrint(out);
				out.print(']');
			}
		}
	}
	public Element typecheck(Context ctx) {
		Element e = nonTerminal.typecheck(ctx);
		if (!(e instanceof NonTerminal))
			ErrorHandler.report(Errors.BINDING_NOT_NONTERMINAL, "A binder must have a nonterminal as the thing bound in", nonTerminal);
		nonTerminal = (NonTerminal) e;
		for (int i = 0; i < elements.size(); ++i) {
			Element e2 = elements.get(i).typecheck(ctx);
			// convert lonely terminals to clauses
			if (e2 instanceof Terminal) {
				Clause c = new Clause(e2);
				c.getElements().add(e2);
				e2 = c;
			}
			if (e2 instanceof Clause) {
				Clause c = (Clause) e2;
				Element cu = c.computeClause(ctx, true);//new ClauseUse(c, ctx.parseMap);
				e2 = cu;
			}
			elements.set(i, e2);			
		}
		return this;
	}

	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		FreeVar t = (FreeVar) nonTerminal.computeTerm(varBindings);
		List<Term> argList = new ArrayList<Term>();
		List<Term> argTypes = new ArrayList<Term>();
		//int index = varBindings.size()-elements.size();
		for (Element e : elements) {
			Term argT = e.computeTerm(varBindings);
			argList.add(argT);
			argTypes.add(argT.getType(varBindings));
			//index++;
		}
		Term varType = t.getType();
		varType = Term.wrapWithLambdas(varType, argTypes);
		t.setType(varType);
		
		return new Application(t, argList);
	}

	@Override
	void checkBindings(Map<String, List<ElemType>> bindingTypes, Node nodeToBlame) {
		List<ElemType> myType = new ArrayList<ElemType>();
		for (Element e : elements) {
			myType.add(e.getElemType());
			e.checkBindings(bindingTypes, nodeToBlame);
		}
		List<ElemType> prevType = bindingTypes.get(this.getNonTerminal().getSymbol());
		debug("binding for " + this + " is " + bindingTypes);
		if (prevType == null) {
			bindingTypes.put(this.getNonTerminal().getSymbol(), myType);
		} else {
			if (!prevType.equals(myType))
				ErrorHandler.report(BINDING_INCONSISTENT, "meta-variable " + nonTerminal + " must have consistent numbers and types of bindings throughout a rule or branch of a theorem", nodeToBlame,
				    "(" + prevType + " != " + myType + ")");
		}
	}
}
