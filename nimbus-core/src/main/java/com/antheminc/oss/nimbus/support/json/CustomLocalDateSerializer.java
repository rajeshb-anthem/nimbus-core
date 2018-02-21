/**
 *  Copyright 2016-2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.antheminc.oss.nimbus.support.json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @Author Mayur Mehta, Tony Lopez, Soham Chakravarti
 */
public class CustomLocalDateSerializer extends StdSerializer<LocalDate> {
    
	private static final long serialVersionUID = 1L;
	
	private final CustomLocalDateTimeSerializer serializer;

    public CustomLocalDateSerializer() {
        this(null);
    }

    public CustomLocalDateSerializer(Class<LocalDate> t) {
        super(t);
        this.serializer = new CustomLocalDateTimeSerializer();
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializerProvider) {
    	if(value == null)
    		return;
    	
    	this.serializer.serialize(LocalDateTime.of(value, LocalTime.MIN), gen, serializerProvider);
    }

}
