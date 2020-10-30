package com.bonitasoft.searcher.json

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

import org.bonitasoft.engine.bdm.serialization.CustomLocalDateDeserializer
import org.bonitasoft.engine.bdm.serialization.CustomLocalDateSerializer
import org.bonitasoft.engine.bdm.serialization.CustomLocalDateTimeDeserializer
import org.bonitasoft.engine.bdm.serialization.CustomLocalDateTimeSerializer
import org.bonitasoft.engine.bdm.serialization.CustomOffsetDateTimeDeserializer
import org.bonitasoft.engine.bdm.serialization.CustomOffsetDateTimeSerializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule

class BusinessDataObjectMapper {

	private static ObjectMapper objectMapper = null;

	public static ObjectMapper getBusinessDataObjectMapper() {
		if (objectMapper!=null) {
			return this.objectMapper;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		// avoid to fail when serializing proxy (proxy will be recreated client side) see BS-16031
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

		SimpleModule module = new SimpleModule();
		module.addSerializer(LocalDate.class, new CustomLocalDateSerializer());
		module.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
		module.addSerializer(OffsetDateTime.class, new CustomOffsetDateTimeSerializer());
		module.addDeserializer(LocalDate.class, new CustomLocalDateDeserializer());
		module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
		module.addDeserializer(OffsetDateTime.class, new CustomOffsetDateTimeDeserializer());
		objectMapper.registerModule(module);
		this.objectMapper = objectMapper;
		return this.objectMapper
	}
	
	public static String toJson(Object o) {
		return getBusinessDataObjectMapper().writeValueAsString(o);
	}
}
