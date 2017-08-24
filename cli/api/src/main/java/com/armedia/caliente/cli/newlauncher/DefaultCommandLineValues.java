package com.armedia.caliente.cli.newlauncher;

import java.util.Iterator;
import java.util.List;

import com.armedia.caliente.cli.CommandLineValues;
import com.armedia.caliente.cli.ParameterDefinition;
import com.armedia.caliente.cli.ParameterWrapper;
import com.armedia.caliente.cli.parser.CommandLineParameter;

public class DefaultCommandLineValues implements CommandLineValues {

	public DefaultCommandLineValues() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Iterator<CommandLineParameter> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isHelpRequested() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getHelpMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<CommandLineParameter> shortOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommandLineParameter getParameter(char shortOpt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasParameter(char shortOpt) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<CommandLineParameter> longOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommandLineParameter getParameter(String longOpt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasParameter(String longOpt) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDefined(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CommandLineParameter getParameter(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasHelpParameter() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CommandLineParameter getHelpParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean getBoolean(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getBoolean(ParameterDefinition param, boolean def) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Boolean> getAllBooleans(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getInteger(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInteger(ParameterDefinition param, int def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Integer> getAllIntegers(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getLong(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLong(ParameterDefinition param, long def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Long> getAllLongs(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getFloat(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getFloat(ParameterDefinition param, float def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Float> getAllFloats(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getDouble(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getDouble(ParameterDefinition param, double def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Double> getAllDoubles(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(ParameterDefinition param, String def) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getAllStrings(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getAllStrings(ParameterDefinition param, List<String> def) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPresent(ParameterDefinition param) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> getPositionalValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDefined(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CommandLineParameter getParameter(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean getBoolean(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getBoolean(ParameterWrapper wrapper, boolean def) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Boolean> getAllBooleans(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getInteger(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInteger(ParameterWrapper wrapper, int def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Integer> getAllIntegers(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getLong(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLong(ParameterWrapper wrapper, long def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Long> getAllLongs(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getFloat(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getFloat(ParameterWrapper wrapper, float def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Float> getAllFloats(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getDouble(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getDouble(ParameterWrapper wrapper, double def) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Double> getAllDoubles(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(ParameterWrapper wrapper, String def) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getAllStrings(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getAllStrings(ParameterWrapper wrapper, List<String> def) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPresent(ParameterWrapper wrapper) {
		// TODO Auto-generated method stub
		return false;
	}

}
