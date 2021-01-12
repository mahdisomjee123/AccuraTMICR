package com.accurascan.ocr.mrz.model;

import android.graphics.Bitmap;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class CardDetails {
    public String owner;
    public String number;
    public String cardType;
    public String expirationDate;
    public String expirationMonth;
    public String expirationYear;
    public Bitmap bitmap;

    public static CardDetails cardDetails;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getExpirationMonth() {
        return expirationMonth;
    }

    public void setExpirationMonth(String expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    public String getExpirationYear() {
        return expirationYear;
    }

    public void setExpirationYear(String expirationYear) {
        this.expirationYear = expirationYear;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public static CardDetails getCardDetails() {
        return cardDetails;
    }

    public static void setCardDetails(CardDetails cardDetails) {
        CardDetails.cardDetails = cardDetails;
    }

    @NonNull
    public String toString(){
        return "CardDetails(owner="+owner+", number="+number+", type="+cardType+", expirationDate="+expirationDate+", expirationMonth="+expirationMonth+", expirationYear="+expirationYear+")";
    }
}
