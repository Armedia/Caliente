package com.armedia.caliente.cli.help;

import java.util.Arrays;

import org.junit.Test;

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