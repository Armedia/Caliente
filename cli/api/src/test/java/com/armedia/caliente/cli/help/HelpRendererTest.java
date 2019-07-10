/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.help;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.caliente.cli.filter.StringValueFilter;

public class HelpRendererTest {

	@Test
	public void testRenderHelp() throws Exception {
		CommandScheme cs = new CommandScheme("Test", false);

		cs.add( //
			new OptionImpl() //
				.setRequired(true) //
				.setShortOpt('h') //
				.setLongOpt("hell") //
				.setMinArguments(1) //
				.setMaxArguments(10) //
				.setArgumentName("hi") //
				.setDescription("The number is 666") //
				.setDefaults(Arrays.asList("a", "b", "c")) //
				.setValueFilter(new StringValueFilter("x", "y", "z")))
			.add( //
				new OptionImpl() //
					.setRequired(false) //
					.setShortOpt('i') //
					.setLongOpt("importance") //
					.setMaxArguments(0) //
					.setDescription("The important importantish") //
			).add( //
				new OptionImpl() //
					.setRequired(false) //
					.setLongOpt("joke") //
					.setArgumentName("joke") //
					.setDescription("The joke to joker with") //
			).setDescription("THE BIG KAHUNA");

		Command c = null;

		c = new Command("first", Arrays.asList("primero", "premier", "1st"));
		c.add( //
			new OptionImpl() //
				.setRequired(true) //
				.setShortOpt('a') //
				.setLongOpt("absolute") //
				.setMinArguments(1) //
				.setMaxArguments(10) //
				.setArgumentName("absoluteValue") //
				.setDescription("The absolute value to absolutize") //
				.setDefault("AAA") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setShortOpt('b') //
				.setLongOpt("base") //
				.setMaxArguments(0) //
				.setArgumentName("baseValue") //
				.setDescription("The base value to basicate") //
				.setDefault("BBB") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setLongOpt("count") //
				.setArgumentName("counter") //
				.setDescription("The count to count") //
				.setDefault("CCC") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setShortOpt('d') //
				.setDescription("The 'd' value to demonstrate") //
				.setDefault("DDDD") //
		).setDescription("EL PRIMER COMANDO");
		cs.addCommand(c);

		c = new Command("second", Arrays.asList("segundo", "seconde", "2nd"));
		c.add( //
			new OptionImpl() //
				.setRequired(true) //
				.setShortOpt('x') //
				.setLongOpt("xanax") //
				.setMinArguments(2) //
				.setMaxArguments(9) //
				.setArgumentName("xValue") //
				.setDescription("The xxxxx") //
				.setDefault("XXX") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setShortOpt('y') //
				.setLongOpt("yoke") //
				.setMinArguments(0) //
				.setMaxArguments(0) //
				.setArgumentName("yyyValue") //
				.setDescription("The yy yyy yyyy") //
				.setDefault("YY") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setLongOpt("zed") //
				.setArgumentName("zette") //
				.setMinArguments(3) //
				.setMaxArguments(999) //
				.setDescription("ZZZ ZZzz zzZZ zzz") //
				.setDefault("ZZZZZZZZ") //
		).setDescription("SECOND COMMAND ON THE LIST");
		cs.addCommand(c);
		try {
			throw new HelpRequestedException(null, cs, c, null);
		} catch (HelpRequestedException e) {
			HelpRenderer.renderHelp("TEST PROGRAM", e, System.out);
		}
	}
}