package io.pacworx.atp.config;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Converter(autoApply = true)
public class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    @Override
    public java.sql.Timestamp convertToDatabaseColumn(ZonedDateTime entityValue) {
        if(entityValue != null) {
            return Timestamp.from(entityValue.toInstant());
        } else {
            return null;
        }
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(java.sql.Timestamp databaseValue) {
        if(databaseValue != null) {
            LocalDateTime localDateTime = databaseValue.toLocalDateTime();
            return localDateTime.atZone(ZoneId.systemDefault());
        } else {
            return null;
        }
    }

}
