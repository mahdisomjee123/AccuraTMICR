package com.accurascan.ocr.mrz.model;

import android.graphics.Bitmap;

import com.docrecog.scan.RecogEngine;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecogResult {
    public String lines = "";//
    public String docType = "";//
    public String country = "";//
    public String surname = "";//
    public String givenname = "";//
    public String docnumber = "";//
    public String docchecksum = "";//
    public String nationality = "";//
    public String birth = "";//
    public String birthchecksum = "";//
    public String sex = "";//
    public String expirationdate = "";//
    public String expirationchecksum = "";//
    public String issuedate = "";//
    public String otherid = "";//
    public String otheridchecksum = "";//
    public String departmentnumber = "";//
    public String secondrowchecksum = "";
    public int ret = 0;
    public Bitmap faceBitmap = null;
    public Bitmap docBackBitmap = null;
    public Bitmap docFrontBitmap = null;
    public RecogEngine.RecType recType = RecogEngine.RecType.INIT;
    public boolean bRecDone = false;
    public boolean bFaceReplaced = false;
    DateFormat date = new SimpleDateFormat("yymmdd", Locale.getDefault());
    SimpleDateFormat newDateFormat = new SimpleDateFormat("dd-mm-yy", Locale.getDefault());

    public void SetResult(int[] intData) {
        int i, k = 0, len;
        byte[] tmp = new byte[100];
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; lines = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; docType = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; country = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; surname = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; givenname = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; docnumber = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; docchecksum = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; nationality = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; birth = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; birthchecksum = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; sex = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; expirationdate = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; expirationchecksum = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; issuedate = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; otherid = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; otheridchecksum = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; departmentnumber = convchar2string(tmp);
		len = intData[k++]; for(i=0;i<len;++i) tmp[i] = (byte)intData[k++]; tmp[i] = 0; secondrowchecksum = convchar2string(tmp);

        if (!sex.isEmpty()) {
            if (sex.equalsIgnoreCase("F")) {
                sex = "Female";
            } else {
                sex = "Male";
            }
        }
        try {
            if (!birth.isEmpty()) {
                Date parse = date.parse(birth.replace("<", ""));
				if (parse != null) {
					birth = "";
					birth = newDateFormat.format(parse);
				}
			}
        } catch (ParseException e) {
        }
        try {
            if (!expirationdate.isEmpty()) {
                Date parse = date.parse(expirationdate.replace("<", ""));
				if (parse != null) {
					expirationdate = "";
					expirationdate = newDateFormat.format(parse);
				}
			}
        } catch (ParseException e) {
        }
        try {
            if (!issuedate.isEmpty()) {
                Date parse = date.parse(issuedate.replace("<", ""));
                if (parse != null) {
                    issuedate = "";
                    issuedate = newDateFormat.format(parse);
                }
            }
        } catch (ParseException e) {
        }

    }

    public String GetResultString() {
        String str = "";
//        if (ret == 1) {
//            str = "correct doc\n";
//        } else {
//            str = "incorrect doc\n";
//        }
		/*str = str + lines + "\n"
			 + docType + "\n"
			 + country + "\n"
			 + surname + "\n"
			 + givenname + "\n"
			 + docnumber + "\n"
			 + docchecksum + "\n"
			 + nationality + "\n"
			 + birth + "\n"
			 + birthchecksum + "\n"
			 + sex + "\n"
			 + expirationdate + "\n"
			 + expirationchecksum + "\n"
			 + otherid + "\n"
			 + otheridchecksum + "\n"
			 + secondrowchecksum + "\n";*/

        str += "Lines : " + lines + "\n"
                + "Document Type : " + docType + "\n"
                + "Country : " + country + "\n"
                + "Surname : " + surname + "\n"
                + "Given Names : " + givenname + "\n"
                + "Document No. : " + docnumber + "\n"
                + "Document Check Number: " + docchecksum + "\n"
                + "Nationaltiy : " + nationality + "\n"
                + "Birth Date : " + birth + "\n"
                + "Birth Check Number: " + birthchecksum + "\n"
                + "Sex : " + sex + "\n"
                + "ExpirationDate : " + expirationdate + "\n"
                + "Expiration Check Number: " + expirationchecksum + "\n";

		if (!issuedate.isEmpty()) {
			str += "Issue Date: " + issuedate + "\n";
		}

		if (otherid.length() > 0) {
			str += "Other ID : " + otherid + "\n"
					+ "Other ID Check: " + otheridchecksum + "\n";
		}

		if (!departmentnumber.isEmpty()) {
			str += "Department Number: " + departmentnumber + "\n";
		}

//        str += "Flag : " + Integer.toString(ret) + "\n";

        return str;
    }

    public static int getByteLength(byte[] str, int maxLen) {
        int i, len = 0;
        for (i = 0; i < maxLen; ++i) {
            if (str[i] == 0) {
                break;
            }
        }
        len = i;
        return len;
    }

    public static String convchar2string(byte[] chstr) {
        int len = getByteLength(chstr, 2000);
        String outStr = null;
        try {
            outStr = new String(chstr, 0, len, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return outStr;
    }


    private static RecogResult recogResult;

    public static RecogResult getRecogResult() {
        RecogResult ocrData = recogResult;
        recogResult = null;
        return ocrData;
    }

    public static void setRecogResult(RecogResult ocrResult) {
        RecogResult.recogResult = ocrResult;
    }
}
