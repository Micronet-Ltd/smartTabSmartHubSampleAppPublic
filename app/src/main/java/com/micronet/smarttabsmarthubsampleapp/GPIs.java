/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp;

public enum GPIs {
    ANALOG_IN1("kADC_ANALOG_IN1"),
    GPI1("kADC_GPIO_IN1"),
    GPI2("kADC_GPIO_IN2"),
    GPI3("kADC_GPIO_IN3"),
    GPI4("kADC_GPIO_IN4"),
    GPI5("kADC_GPIO_IN5"),
    GPI6("kADC_GPIO_IN6"),
    GPI7("kADC_GPIO_IN7");

    private String gpiString;

    GPIs(String gpiString){
        this.gpiString = gpiString;
    }

    public String getString(){
        return gpiString;
    }
}