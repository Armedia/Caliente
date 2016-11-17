package com.armedia.caliente.cli.parser;

import java.io.IOException;
import java.util.List;

public interface TokenSource {

	public List<String> getTokens() throws IOException;

	public String getKey();
}