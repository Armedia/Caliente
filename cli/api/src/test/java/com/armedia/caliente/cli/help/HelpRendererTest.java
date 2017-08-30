package com.armedia.caliente.cli.help;

import java.util.Arrays;

import org.junit.Test;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.exception.HelpRequestedException;

public class HelpRendererTest {

	@Test
	public void testRenderHelp() throws Exception {
		CommandScheme cs = new CommandScheme("Test", false);
		Command c = null;

		c = new Command("first", Arrays.asList("primero", "premier", "1st"));
		c.add( //
			new OptionImpl() //
				.setRequired(true) //
				.setShortOpt('a') //
				.setLongOpt("absolute") //
				.setMinValueCount(1) //
				.setMaxValueCount(10) //
				.setValueName("absoluteValue") //
				.setDescription("The absolute value to absolutize") //
				.setDefault("AAA") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setShortOpt('b') //
				.setLongOpt("base") //
				.setMaxValueCount(0) //
				.setValueName("baseValue") //
				.setDescription("The base value to basicate") //
				.setDefault("BBB") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setLongOpt("count") //
				.setValueName("counter") //
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
				.setMinValueCount(2) //
				.setMaxValueCount(4) //
				.setValueName("xValue") //
				.setDescription("The xxxxx") //
				.setDefault("XXX") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setShortOpt('y') //
				.setLongOpt("yoke") //
				.setMaxValueCount(0) //
				.setValueName("yyyValue") //
				.setDescription("The yy yyy yyyy") //
				.setDefault("YY") //
		).add( //
			new OptionImpl() //
				.setRequired(false) //
				.setLongOpt("zed") //
				.setValueName("zette") //
				.setDescription("ZZZ ZZzz zzZZ zzz") //
				.setDefault("ZZZZZZZZ") //
		).setDescription("SECOND COMMAND ON THE LIST");
		cs.addCommand(c);
		try {
			throw new HelpRequestedException(cs, c);
		} catch (HelpRequestedException e) {
			new HelpRenderer().renderHelp("TEST PROGRAM", e, System.out);
		}
	}
}