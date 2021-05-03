package com.docrecog.scan;

import android.content.Intent;

@androidx.annotation.Keep
public enum RecogType {
        OCR, MRZ, BARCODE, PDF417, DL_PLATE, BANKCARD;

        private static final String recogType = RecogType.class.getName();

        public void attachTo(Intent intent) {
            intent.putExtra(recogType, ordinal());
        }

        public static RecogType detachFrom(Intent intent) {
            if (!intent.hasExtra(recogType)) throw new IllegalStateException();
            return values()[intent.getIntExtra(recogType, -1)];
        }
    }