/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp;

public enum ADCs{
    ADC0("kADC_ANALOG_IN1"),
    ADC1("kADC_GPIO_IN1"),
    ADC2("kADC_GPIO_IN2"),
    ADC3("kADC_GPIO_IN3"),
    ADC4("kADC_GPIO_IN4"),
    ADC5("kADC_GPIO_IN5"),
    ADC6("kADC_GPIO_IN6"),
    ADC7("kADC_GPIO_IN7"),
    ADC8("kADC_POWER_IN"),
    ADC9("kADC_POWER_VCAP"),
    ADC10("kADC_TEMPERATURE"),
    ADC11("kADC_CABLE_TYPE");

    private String adcString;

    ADCs(String adcString){
        this.adcString = adcString;
    }

    public String getString(){
        return adcString;
    }
}
