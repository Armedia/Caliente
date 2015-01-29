/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.cmf.storage.StoredValueEncoderException;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;

/**
 * @author diego
 *
 */
public final class ShptTranslator extends ObjectStorageTranslator<ShptObject<?>, StoredValue> {

	private static class Codec implements StoredValueCodec<StoredValue> {

		private final StoredDataType type;

		private Codec(StoredDataType type) {
			this.type = type;
		}

		@Override
		public String encodeValue(StoredValue value) throws StoredValueEncoderException {
			if (value == null) { return null; }
			return value.asString();
		}

		@Override
		public StoredValue decodeValue(String value) throws StoredValueDecoderException {
			try {
				return new StoredValue(this.type, value);
			} catch (ParseException e) {
				throw new StoredValueDecoderException(String.format("Failed to decode the %s value contained in [%s]",
					this.type, value), e);
			}
		}

	}

	private static final StoredValueCodec<StoredValue> BOOLEAN = new Codec(StoredDataType.BOOLEAN);
	private static final StoredValueCodec<StoredValue> INTEGER = new Codec(StoredDataType.INTEGER);
	private static final StoredValueCodec<StoredValue> DOUBLE = new Codec(StoredDataType.DOUBLE);
	private static final StoredValueCodec<StoredValue> STRING = new Codec(StoredDataType.STRING);
	private static final StoredValueCodec<StoredValue> ID = new Codec(StoredDataType.ID);
	private static final StoredValueCodec<StoredValue> TIME = new Codec(StoredDataType.TIME) {
		private final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

		private Date parse(String value) throws ParseException {
			return new SimpleDateFormat(this.DATE_FORMAT).parse(value);
		}

		@Override
		public String encodeValue(StoredValue value) throws StoredValueEncoderException {
			if (value == null) { return null; }
			try {
				return DateFormatUtils.format(value.asTime(), this.DATE_FORMAT);
			} catch (ParseException e) {
				throw new StoredValueEncoderException(String.format("Failed to encode the date contained in [%s]",
					value), e);
			}
		}

		@Override
		public StoredValue decodeValue(String value) throws StoredValueDecoderException {
			if (value == null) { return null; }
			try {
				return new StoredValue(parse(value));
			} catch (ParseException e) {
				throw new StoredValueDecoderException(
					String.format("Failed to parse the date contained in [%s]", value), e);
			}
		}
	};

	private static final Map<StoredDataType, StoredValueCodec<StoredValue>> CODECS;

	static {
		Map<StoredDataType, StoredValueCodec<StoredValue>> codecs = new EnumMap<StoredDataType, StoredValueCodec<StoredValue>>(
			StoredDataType.class);
		codecs.put(StoredDataType.BOOLEAN, ShptTranslator.BOOLEAN);
		codecs.put(StoredDataType.INTEGER, ShptTranslator.INTEGER);
		codecs.put(StoredDataType.DOUBLE, ShptTranslator.DOUBLE);
		codecs.put(StoredDataType.STRING, ShptTranslator.STRING);
		codecs.put(StoredDataType.ID, ShptTranslator.ID);
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
	public StoredValueCodec<StoredValue> getCodec(StoredDataType type) {
		return ShptTranslator.CODECS.get(type);
	}
}