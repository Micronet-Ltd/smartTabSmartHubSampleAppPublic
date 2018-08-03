/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class CanFramesViewModel extends ViewModel {
    public MutableLiveData<Integer> can1FramesRx = new MutableLiveData<>();
    public MutableLiveData<Integer> can1BytesRx = new MutableLiveData<>();
    public MutableLiveData<Integer> can2FramesRx = new MutableLiveData<>();
    public MutableLiveData<Integer> can2BytesRx = new MutableLiveData<>();

    public MutableLiveData<String> can1FramesRxStr = new MutableLiveData<>();
    public MutableLiveData<String> can2FramesRxStr = new MutableLiveData<>();

    public MutableLiveData<Integer> can1FramesTx = new MutableLiveData<>();
    public MutableLiveData<Integer> can1BytesTx = new MutableLiveData<>();
    public MutableLiveData<Integer> can2FramesTx = new MutableLiveData<>();
    public MutableLiveData<Integer> can2BytesTx = new MutableLiveData<>();

}