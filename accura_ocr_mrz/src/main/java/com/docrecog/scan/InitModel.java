package com.docrecog.scan;

public class InitModel {

    private InitData data;
    private Integer responseCode;
    private String responseMessage;

    public InitData getInitData() {
        return data;
    }

    public void setInitData(InitData data) {
        this.data = data;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public class InitData {
        private Integer cameraHeight;
        private Integer cameraWidth;
        private String cardName;
        private Boolean isbothavailable;

        public Integer getCameraHeight() {
            return cameraHeight;
        }

        public void setCameraHeight(Integer cameraHeight) {
            this.cameraHeight = cameraHeight;
        }

        public Integer getCameraWidth() {
            return cameraWidth;
        }

        public void setCameraWidth(Integer cameraWidth) {
            this.cameraWidth = cameraWidth;
        }

        public String getCardName() {
            return cardName;
        }

        public void setCardName(String cardName) {
            this.cardName = cardName;
        }

        public Boolean getIsbothavailable() {
            return isbothavailable;
        }

        public void setIsbothavailable(Boolean isbothavailable) {
            this.isbothavailable = isbothavailable;
        }

    }
}