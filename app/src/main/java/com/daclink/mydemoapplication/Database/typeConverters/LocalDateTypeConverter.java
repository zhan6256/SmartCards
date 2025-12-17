package com.daclink.mydemoapplication.Database.typeConverters;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/*
 * Author: France Zhang
 * Created on: 12/17/2025
 * Description: LocalDateTypeConverter class
 */
public class LocalDateTypeConverter {
    @TypeConverter
    public long convertDateToLong(LocalDateTime date){
        ZonedDateTime zdt = ZonedDateTime.of(date, ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }
    @TypeConverter
    public LocalDateTime convertLongToDate(Long epochMilli){
        Instant instant  = Instant.ofEpochMilli(epochMilli);
        return LocalDateTime.ofInstant(instant,ZoneId.systemDefault());

    }
}
