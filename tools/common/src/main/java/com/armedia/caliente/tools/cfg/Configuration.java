package com.armedia.caliente.tools.cfg;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

import com.armedia.commons.utilities.Tools;

public class Configuration {

	public interface Setting {

		/**
		 * Return the string literal which is used in configuration files to set the configuration
		 * in question (i.e. the actual text label from the configuration file, NOT the
		 * user-friendly label)
		 *
		 * @return the string literal which is used in configuration files to set the configuration
		 *         in question
		 */
		public String getLabel();

		/**
		 * Return the default value for the configuration setting, in case it's not set at all.
		 *
		 * @return the default value for the configuration setting, in case it's not set at all.
		 */
		public Object getDefault();
	}

	public static interface Value {

		/**
		 * Return {@code true} if this value is set (i.e. non-{@code null}), {@code false}
		 * otherwise.
		 *
		 * @return {@code true} if this value is set (i.e. non-{@code null}), {@code false}
		 *         otherwise.
		 */
		public boolean isSet();

		/**
		 * Return {@code true} if a default value is defined for this configuration, or
		 * {@code false} otherwise. If this method returns {@code true}, then {@link #getDefault()}
		 * will never return {@code null}. Likewise, if this method returns {@code false}, then
		 * {@link #getDefault()} will always return {@code null}.
		 *
		 * @return {@code true} if a default value is defined for this configuration, or
		 *         {@code false} otherwise
		 */
		public boolean hasDefault();

		/**
		 * Return this setting's default value, or {@code null} if no default is set. If this method
		 * returns {@code null}, then {@link #hasDefault()} must also return {@code false}.
		 *
		 * @return this setting's default value, or {@code null} if no default is set
		 */
		public Value getDefault();

		/**
		 * Returns {@code true} if this value <b>is</b> the setting's default value, as distinct
		 * from whether the returned value is <b>equal to</b> the established default value. Note
		 * that this will only return {@code true} if the setting has a default value defined (i.e.
		 * {@link #hasDefault()} returns {@code true}), and this instance is the value returned by
		 * {@link #getDefault()}. If a superseding configuration sets the setting's value to a value
		 * <b>equal</b> to the default value, this method will return {@code false} in spite of the
		 * coincidence.
		 *
		 * @return {@code true} if this value <b>is</b> the setting's default value, as distinct
		 *         from whether the returned value is <b>equal to</b> the established default value
		 */
		public boolean isDefault();

		public Setting getSetting();

		public String asString();

		public Double asDouble();

		public Float asFloat();

		public Long asLong();

		public Integer asInteger();

		public Object asObject();

		public Boolean asBoolean();

		/**
		 * Return the value as a {@link ByteBuffer} if it's a {@code Base-64} decodeable value, an
		 * instance of {@code byte[]}, or an subclass of {@link ByteBuffer}
		 *
		 * @return the value as a {@link ByteBuffer}, assuming it's a {@code Base-64} decodeable
		 *         value.
		 * @throws DecoderException
		 *             if the value is not a {@code Base-64} decodeable value
		 */
		public ByteBuffer asBinary() throws DecoderException;
	}

	private final Map<String, Value> values;

	public Configuration(Map<String, Object> values) {
		this.values = null;
	}

	protected String asString(Object value) {
		return Tools.toString(value);
	}

	protected Double asDouble(Object value) {
		return Tools.decodeDouble(value);
	}

	protected Float asFloat(Object value) {
		return Tools.decodeFloat(value);
	}

	protected Long asLong(Object value) {
		return Tools.decodeLong(value);
	}

	protected Integer asInteger(Object value) {
		return Tools.decodeInteger(value);
	}

	protected Boolean asBoolean(Object value) {
		return Tools.decodeBoolean(value);
	}

	protected byte[] asBinary(Object value) throws DecoderException {
		String str = asString(value);
		if (str == null) { return null; }
		if (!Base64.isBase64(str)) { throw new DecoderException("The given string is not a valid Base64 string"); }
		return Base64.decodeBase64(str);
	}

	public Value getValue(Setting setting) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		return getValue(setting.getLabel(), setting.getDefault());
	}

	public Value getValue(Setting setting, String overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		String def = overrideDefault;
		if (def == null) {
			def = asString(setting.getDefault());
		}
		return getValue(setting.getLabel(), def);
	}

	public Value getValue(Setting setting, Double overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		Double def = overrideDefault;
		if (def == null) {
			def = asDouble(setting.getDefault());
		}
		return getValue(setting.getLabel(), def);
	}

	public Value getValue(Setting setting, Float overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		Float def = overrideDefault;
		if (def == null) {
			def = asFloat(setting.getDefault());
		}
		return getValue(setting.getLabel(), def);
	}

	public Value getValue(Setting setting, Long overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		Long def = overrideDefault;
		if (def == null) {
			def = asLong(setting.getDefault());
		}
		return getValue(setting.getLabel(), def);
	}

	public Value getValue(Setting setting, Integer overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		Integer def = overrideDefault;
		if (def == null) {
			def = asInteger(setting.getDefault());
		}
		return getValue(setting.getLabel(), def);
	}

	public Value getValue(Setting setting, Boolean overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		Boolean def = overrideDefault;
		if (def == null) {
			def = asBoolean(setting.getDefault());
		}
		return getValue(setting.getLabel(), def);
	}

	public Value getValue(Setting setting, byte[] overrideDefault) throws DecoderException {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		byte[] def = overrideDefault;
		if (def == null) {
			def = asBinary(setting.getDefault());
		}
		return getValue(setting.getLabel(), def);
	}

	public Value getValue(Setting setting, ByteBuffer overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		return null;
	}

	public Value getValue(Setting setting, Object overrideDefault) {
		Objects.requireNonNull(setting, "Must provide a setting to retrieve");
		return null;
	}

	public Value getValue(String label) {
		return getValue(label, (String) null);
	}

	public Value getValue(String label, String def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, Double def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, Float def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, Long def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, Integer def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, Boolean def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, byte[] def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, ByteBuffer def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

	public Value getValue(String label, Object def) {
		Value v = this.values.get(label);
		if (v != null) { return v; }
		return null;
	}

}