package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.util.List;

public interface TokenSource {

	public List<String> getTokenStrings() throws IOException;

	public String getKey();
}