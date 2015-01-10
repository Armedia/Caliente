/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.cmf.storage.StoredValueEncoderException;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public final class ShptTranslator extends ObjectStorageTranslator<ShptObject<?>, Object> {

	private static final StoredValueCodec<Object> BOOLEAN = new StoredValueCodec<Object>() {

		@Override
		public String encodeValue(Object value) throws StoredValueEncoderException {
			if (value == null) { return null; }
			return Tools.decodeBoolean(value).toString();
		}

		@Override
		public Object decodeValue(String value) throws StoredValueDecoderException {
			return Tools.decodeBoolean(value);
		}
	};

	private static final StoredValueCodec<Object> INTEGER = new StoredValueCodec<Object>() {
		@Override
		public String encodeValue(Object value) throws StoredValueEncoderException {
			if (value == null) { return null; }
			return Tools.decodeInteger(value).toString();
		}

		@Override
		public Object decodeValue(String value) throws StoredValueDecoderException {
			return Tools.decodeInteger(value);
		}
	};

	private static final StoredValueCodec<Object> DOUBLE = new StoredValueCodec<Object>() {

		@Override
		public String encodeValue(Object value) throws StoredValueEncoderException {
			if (value == null) { return null; }
			return Tools.decodeDouble(value).toString();
		}

		@Override
		public Object decodeValue(String value) throws StoredValueDecoderException {
			return Tools.decodeDouble(value);
		}
	};

	private static final StoredValueCodec<Object> STRING = new StoredValueCodec<Object>() {

		@Override
		public String encodeValue(Object value) throws StoredValueEncoderException {
			return Tools.toString(value);
		}

		@Override
		public Object decodeValue(String value) throws StoredValueDecoderException {
			return value;
		}
	};

	private static final StoredValueCodec<Object> TIME = new StoredValueCodec<Object>() {
		private final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

		private Date parse(String value) throws ParseException {
			return new SimpleDateFormat(this.DATE_FORMAT).parse(value);
		}

		@Override
		public String encodeValue(Object value) throws StoredValueEncoderException {
			if (value == null) { return null; }

			final Date date;
			if (value instanceof Date) {
				date = Date.class.cast(value);
			} else {
				if (value instanceof Calendar) {
					date = Calendar.class.cast(value).getTime();
				} else {
					// Parse it as a string...no choice, really
					final String str = value.toString();
					try {
						date = parse(str);
					} catch (ParseException e) {
						throw new StoredValueEncoderException(
							String.format("Failed to decode the date from [%s]", str), e);
					}
				}
			}
			return DateFormatUtils.format(date, this.DATE_FORMAT);
		}

		@Override
		public Object decodeValue(String value) throws StoredValueDecoderException {
			if (value == null) { return null; }
			try {
				return parse(value);
			} catch (ParseException e) {
				throw new StoredValueDecoderException(
					String.format("Failed to parse the date contained in [%s]", value), e);
			}
		}
	};

	private static final Map<StoredDataType, StoredValueCodec<Object>> CODECS;

	static {
		Map<StoredDataType, StoredValueCodec<Object>> codecs = new EnumMap<StoredDataType, StoredValueCodec<Object>>(
			StoredDataType.class);
		codecs.put(StoredDataType.BOOLEAN, ShptTranslator.BOOLEAN);
		codecs.put(StoredDataType.INTEGER, ShptTranslator.INTEGER);
		codecs.put(StoredDataType.DOUBLE, ShptTranslator.DOUBLE);
		codecs.put(StoredDataType.STRING, ShptTranslator.STRING);
		codecs.put(StoredDataType.ID, ShptTranslator.STRING);
		codecs.put(StoredDataType.TIME, ShptTranslator.TIME);
		CODECS = Collections.unmodifiableMap(codecs);
	}

	public static ShptTranslator INSTANCE = new ShptTranslator();

	private ShptTranslator() {
		// Avoid instantiation
	}

	@Override
	protected StoredObjectType doDecodeObjectType(ShptObject<?> object) throws UnsupportedObjectTypeException {
		return object.getStoredType();
	}

	@Override
	protected Class<? extends ShptObject<?>> doDecodeObjectType(StoredObjectType type)
		throws UnsupportedObjectTypeException {
		switch (type) {
			case DOCUMENT:
				return ShptFile.class;
			case FOLDER:
				return ShptFolder.class;
			case USER:
				return ShptUser.class;
			case GROUP:
				return ShptGroup.class;
			default:
				return null;
		}
	}

	@Override
	protected String doGetObjectId(ShptObject<?> object) throws Exception {
		return object.getId();
	}

	@Override
	public StoredValueCodec<Object> getCodec(StoredDataType type) {
		return ShptTranslator.CODECS.get(type);
	}
}