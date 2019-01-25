package com.armedia.caliente.cli;

import java.util.function.Supplier;

/**
 * <p>
 * This is a utility interface that helps in the use of classes (like {@link Enum Enums}, for
 * instance) that can be used as a {@link Option} analog because they contain a single option, and
 * thus reduce the amount of code written to support a simple coding strategy.
 * </p>
 *
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public interface OptionWrapper extends Supplier<Option> {

}