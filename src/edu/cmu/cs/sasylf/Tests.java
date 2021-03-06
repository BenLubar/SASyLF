package edu.cmu.cs.sasylf;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import edu.cmu.cs.sasylf.ast.Errors;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;

public class Tests {
	@Test
	public void good() throws IOException {
		int errors = 0;
		
		File regression = new File("regression");
		for (File f : regression.listFiles()) {
			if (f.getName().startsWith("good")) {
				try {
					if (!DSLToolkitParser.read(f).typecheck())
						errors++;
				} catch (ParseException e) {
					System.err.println(f.toString());
					e.printStackTrace();
					errors++;
				}
			}
		}
		if (errors != 0)
			fail();
	}

	@Test
	public void bad() throws IOException, ParseException {
		boolean hasError = false;
		
		PrintStream err = System.err;
		try {
			System.setErr(new PrintStream("regression.log"));
			File regression = new File("regression");
			for (File f : regression.listFiles()) {
				if (f.getName().startsWith("bad")) {
					if (DSLToolkitParser.read(f).typecheck()) {
						err.println("Test case " + f + " did not have errors.");
						hasError = true;
					}
				}
			}
		} finally {
			System.setErr(err);
		}

		Set<Errors> seen = new HashSet<Errors>();
		for (ErrorReport report : ErrorHandler.getReports()) {
			if (report.errorType == null) {
				System.err.println("\"" + report.getMessage() + "\" has no errorType");
				hasError = true;
			} else {
				seen.add(report.errorType);
			}
		}
		for (Errors error : Errors.values()) {
			if (!seen.contains(error)) {
				System.err.println("Error " + error.name() + " not tested");
				hasError = true;
			}
		}

		if (hasError) fail();
	}
}
