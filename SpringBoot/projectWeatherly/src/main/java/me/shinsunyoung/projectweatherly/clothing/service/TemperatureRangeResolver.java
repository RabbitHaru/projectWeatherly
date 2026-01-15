package me.shinsunyoung.projectweatherly.clothing.service;

import me.shinsunyoung.projectweatherly.clothing.enums.TemperatureRange;

public class TemperatureRangeResolver {
    public static TemperatureRange resolve(double temp){
        if(temp >= 28) return TemperatureRange.HOT;
        if(temp >= 23) return TemperatureRange.WARM;
        if(temp >= 20) return TemperatureRange.MILD_WARM;
        if(temp >= 17) return TemperatureRange.MILD;
        if(temp >= 12) return TemperatureRange.COOL;
        if(temp >= 9) return TemperatureRange.COLD_COOL;
        if(temp >= 5) return TemperatureRange.COLD;

        return TemperatureRange.FREEZING;
    }
}
