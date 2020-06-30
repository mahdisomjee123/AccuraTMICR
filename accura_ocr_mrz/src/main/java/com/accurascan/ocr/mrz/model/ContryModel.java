package com.accurascan.ocr.mrz.model;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ContryModel {
    public int country_id;
    public String country_name;
    public List<CardModel> cards;

    public int getCountry_id() {
        return country_id;
    }

    public void setCountry_id(int country_id) {
        this.country_id = country_id;
    }

    public String getCountry_name() {
        return country_name;
    }

    public void setCountry_name(String country_name) {
        this.country_name = country_name;
    }

    public List<CardModel> getCards() {
        return cards != null ? cards : new ArrayList<>();
    }

    public void setCards(List<CardModel> cards) {
        this.cards = cards;
    }

    public class CardModel {
        public int card_id;
        public int card_type; // 0 - OCR , 1 - PDF417
        public String card_name;

        public int getCard_id() {
            return card_id;
        }

        public void setCard_id(int card_id) {
            this.card_id = card_id;
        }

        public int getCard_type() {
            return card_type;
        }

        public void setCard_type(int card_type) {
            this.card_type = card_type;
        }

        public String getCard_name() {
            return card_name;
        }

        public void setCard_name(String card_name) {
            this.card_name = card_name;
        }

    }

}
