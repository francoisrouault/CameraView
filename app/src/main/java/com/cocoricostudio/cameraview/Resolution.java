/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cocoricostudio.cameraview;

import android.annotation.SuppressLint;

enum Resolution {
    S_1920x1080_r1_77(1920, 1080),
    S_1440x1080_r1_33(1440, 1080),
    S_1088x1088_r1_00(1088, 1088),
    S_1280x720_r1_77(1280, 720),
    S_1056x704_r1_50(1056, 704),
    S_1024x768_r1_33(1024, 768),
    S_960x720_r1_33(960, 720),
    S_800x450_r1_77(800, 450),
    S_720x720_r1_00(720, 720),
    S_720x480_r1_50(720, 480),
    S_640x480_r1_33(640, 480),
    S_352x288_r1_22(352, 288),
    S_320x240_r1_33(320, 240),
    S_256x144_r1_77(256, 144),
    S_176x144_r1_22(176, 144);

    final int width;
    final int height;

    Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return width + "x" + height + " r:" + String.format("%.2f", getRatio());
    }

    private double getRatio() {
        return (double) width / height;
    }
}

