package com.armedia.cmf.engine.tools;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import com.armedia.cmf.engine.tools.grammar.BooleanGrammar;
import com.armedia.cmf.engine.tools.grammar.ParseException;

public class StringExpression implements BooleanExpression {

	private class Grammar extends BooleanGrammar {

		public Grammar(InputStream stream) {
			super(stream);
		}

		public Grammar(InputStream stream, String encoding) {
			super(stream, encoding);
		}

		public Grammar(Reader reader) {
			super(reader);
		}

		@Override
		protected BooleanExpression buildNamedExpression(String name) {
			return new NameExistsExpression(name);
		}
	}

	private final BooleanExpression expression;

	public StringExpression(String expression) throws ParseException {
		this(new StringReader(expression));
	}

	public StringExpression(InputStream in) throws ParseException {
		this(in, Charset.defaultCharset());
	}

	public StringExpression(InputStream in, Charset encoding) throws ParseException {
		this(in, (encoding != null ? encoding : Charset.defaultCharset()).name());
	}

	public StringExpression(InputStream in, String encoding) throws ParseException {
		if (encoding == null) {
			encoding = Charset.defaultCharset().name();
		}
		this.expression = new Grammar(in, encoding).parse();
	}

	public StringExpression(Reader r) throws ParseException {
		this.expression = new Grammar(r).parse();
	}

	@Override
	public final boolean evaluate(BooleanContext c) {
		if (c == null) { throw new IllegalArgumentException("Must provide an evaluation context"); }
		return this.expression.evaluate(c);
	}
}