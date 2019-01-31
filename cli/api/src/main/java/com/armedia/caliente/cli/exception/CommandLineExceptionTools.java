package com.armedia.caliente.cli.exception;

public class CommandLineExceptionTools {

	/**
	 * Unwinds the given throwable into one of the 7 subclasses of
	 * {@link CommandLineSyntaxException}. If the given throwable is either {@code null}, or not one
	 * of these classes, this method does nothing and execution continues normally. This is useful
	 * in the event that closer analysis of a {@link CommandLineSyntaxException} is desired. The API
	 * is designed to make it easy to catch these exceptions and process them simply, but if more
	 * detail is required, this method is useful.
	 *
	 * @param t
	 * @throws UnknownOptionException
	 * @throws UnknownCommandException
	 * @throws TooManyPositionalValuesException
	 * @throws TooManyOptionValuesException
	 * @throws InsufficientPositionalValuesException
	 * @throws InsufficientOptionValuesException
	 * @throws MissingRequiredOptionsException
	 * @throws MissingRequiredCommandException
	 */
	public static void unwindSyntaxException(final Throwable t) throws UnknownOptionException, UnknownCommandException,
		TooManyPositionalValuesException, TooManyOptionValuesException, InsufficientPositionalValuesException,
		InsufficientOptionValuesException, MissingRequiredOptionsException, MissingRequiredCommandException {
		if (t == null) { return; }
		if (UnknownOptionException.class.isInstance(t)) { throw UnknownOptionException.class.cast(t); }
		if (UnknownCommandException.class.isInstance(t)) { throw UnknownCommandException.class.cast(t); }
		if (TooManyPositionalValuesException.class.isInstance(t)) {
			throw TooManyPositionalValuesException.class.cast(t);
		}
		if (TooManyOptionValuesException.class.isInstance(t)) { throw TooManyOptionValuesException.class.cast(t); }
		if (InsufficientPositionalValuesException.class.isInstance(t)) {
			throw InsufficientPositionalValuesException.class.cast(t);
		}
		if (InsufficientOptionValuesException.class.isInstance(t)) {
			throw InsufficientOptionValuesException.class.cast(t);
		}
		if (MissingRequiredOptionsException.class.isInstance(t)) {
			throw MissingRequiredOptionsException.class.cast(t);
		}
		if (MissingRequiredCommandException.class.isInstance(t)) {
			throw MissingRequiredCommandException.class.cast(t);
		}
	}

}