package com.armedia.caliente.cli.parser.token;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.armedia.caliente.cli.parser.Parameter;

public final class BasicParserErrorPolicy implements TokenErrorPolicy {

	public static enum Error {
		//
		MISSING_VALUES, TOO_MANY_VALUES, UNKNOWN_PARAMETER, UNKNOWN_SUBCOMMAND,
		//
		;
	}

	private final Set<Error> errors;

	public BasicParserErrorPolicy() {
		this.errors = EnumSet.allOf(Error.class);
	}

	public BasicParserErrorPolicy(Error... errors) {
		this.errors = EnumSet.noneOf(Error.class);
		if (errors != null) {
			for (Error e : errors) {
				if (e != null) {
					this.errors.add(e);
				}
			}
		}
	}

	public BasicParserErrorPolicy(Collection<Error> errors) {
		this.errors = EnumSet.noneOf(Error.class);
		if (errors != null) {
			for (Error e : errors) {
				if (e != null) {
					this.errors.add(e);
				}
			}
		}
	}

	public boolean enable(Error... errors) {
		if (errors == null) { return false; }
		return enable(Arrays.asList(errors));
	}

	public boolean enable(Collection<Error> errors) {
		boolean changed = false;
		if (errors != null) {
			for (Error e : errors) {
				if (e != null) {
					changed |= this.errors.add(e);
				}
			}
		}
		return changed;
	}

	public boolean disable(Error... errors) {
		if (errors == null) { return false; }
		return disable(Arrays.asList(errors));
	}

	public boolean disable(Iterable<Error> errors) {
		boolean changed = false;
		if (errors != null) {
			for (Error e : errors) {
				if (e != null) {
					changed |= this.errors.remove(e);
				}
			}
		}
		return changed;
	}

	public boolean toggle(Error... errors) {
		if (errors == null) { return false; }
		return toggle(Arrays.asList(errors));
	}

	public boolean toggle(Iterable<Error> errors) {
		boolean changed = false;
		if (errors != null) {
			for (Error e : errors) {
				if (e != null) {
					if (this.errors.add(e) || this.errors.remove(e)) {
						changed |= true;
					}
				}
			}
		}
		return changed;
	}

	public boolean set(Error... errors) {
		if (errors == null) { return false; }
		return set(Arrays.asList(errors));
	}

	public boolean set(Iterable<Error> errors) {
		if (errors == null) {
			boolean empty = this.errors.isEmpty();
			this.errors.clear();
			return !empty;
		}

		Set<Error> newSet = EnumSet.noneOf(Error.class);
		for (Error e : errors) {
			if (e != null) {
				newSet.add(e);
			}
		}
		return this.errors.retainAll(newSet) || this.errors.addAll(newSet);
	}

	public boolean isEnabled(Error error) {
		if (error == null) { throw new IllegalArgumentException("Must provide an error to check for"); }
		return this.errors.contains(error);
	}

	@Override
	public boolean isErrorMissingValues(Token token, Parameter parameter, List<String> values) {
		return isEnabled(Error.MISSING_VALUES);
	}

	@Override
	public boolean isErrorTooManyValues(Token token, Parameter parameter, List<String> values) {
		return isEnabled(Error.TOO_MANY_VALUES);
	}

	@Override
	public boolean isErrorUnknownParameterFound(Token token) {
		return isEnabled(Error.UNKNOWN_PARAMETER);
	}

	@Override
	public boolean isErrorUnknownSubCommandFound(Token token) {
		return isEnabled(Error.UNKNOWN_SUBCOMMAND);
	}
}