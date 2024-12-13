package com.accurascan.accuraocr.sample;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.accurascan.ocr.mrz.model.CardDetails;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.PDF417Data;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.docrecog.scan.RecogType;

public class OcrResultActivity extends AppCompatActivity {

    Bitmap face1;

    TableLayout mrz_table_layout, front_table_layout, back_table_layout, usdl_table_layout, pdf417_table_layout, bank_table_layout;

    ImageView ivUserProfile, iv_frontside, iv_backside;
    LinearLayout ly_back, ly_front;
    View dl_plate_lout, ly_mrz_container, ly_front_container, ly_back_container, ly_security_container, ly_pdf417_container, ly_usdl_container, ly_bank_container, ly_barcode_container, faceImageContainer;
    OcrData.MapData Frontdata;
    OcrData.MapData Backdata;
    private TextView tv_security;

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        if (getIntent().getIntExtra("app_orientation", 1) != 0) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);

        initUI();
        RecogType recogType = RecogType.detachFrom(getIntent());

        if (recogType == RecogType.OCR) {
            // RecogType.OCR
            OcrData ocrData = OcrData.getOcrResult();
            if (ocrData != null) {
                setOcrData(ocrData);
            }
        } else if (recogType == RecogType.MICR) {
            ly_back.setVisibility(View.GONE);
            faceImageContainer.setVisibility(View.GONE);

            CardDetails cardDetails = CardDetails.getCardDetails();
            setBankData(cardDetails);

            if (cardDetails.getBitmap() != null) {
                iv_frontside.setImageBitmap(cardDetails.getBitmap());
            } else {
                ly_front.setVisibility(View.GONE);
            }

        } else if (recogType == RecogType.MRZ) {
            // RecogType.MRZ
            RecogResult g_recogResult = RecogResult.getRecogResult();
            if (g_recogResult != null) {
                setMRZData(g_recogResult);

                if (g_recogResult.docFrontBitmap != null) {
                    iv_frontside.setImageBitmap(g_recogResult.docFrontBitmap);
                } else {
                    ly_front.setVisibility(View.GONE);
                }

                if (g_recogResult.docBackBitmap != null) {
                    iv_backside.setImageBitmap(g_recogResult.docBackBitmap);
                } else {
                    ly_back.setVisibility(View.GONE);
                }


                if (g_recogResult.faceBitmap != null) {
                    face1 = g_recogResult.faceBitmap;
                }
            }
            setData();
        } else if (recogType == RecogType.DL_PLATE) {
            findViewById(R.id.v_divider).setVisibility(View.GONE);
            dl_plate_lout.setVisibility(View.VISIBLE);
            ly_back.setVisibility(View.GONE);
            faceImageContainer.setVisibility(View.GONE);
            OcrData ocrData = OcrData.getOcrResult();

            if (ocrData != null) {
                TextView textView = findViewById(R.id.tv_value);
                textView.setText(ocrData.getFrontData().getOcr_data().get(0).getKey_data());
                if (ocrData.getFrontimage() != null) {
                    iv_frontside.setImageBitmap(ocrData.getFrontimage());
                } else {
                    iv_frontside.setVisibility(View.GONE);
                }
            }

        } else if (recogType == RecogType.PDF417 || (recogType == RecogType.BARCODE && PDF417Data.getPDF417Result() != null)) {
            // RecogType.PDF417
            PDF417Data pdf417Data = PDF417Data.getPDF417Result();

            if (pdf417Data == null) return;
            setBarcodeData(pdf417Data);

            if (pdf417Data.docFrontBitmap != null) {
                iv_frontside.setImageBitmap(pdf417Data.docFrontBitmap);
            } else {
                ly_front.setVisibility(View.GONE);
            }

            if (pdf417Data.docBackBitmap != null) {
                iv_backside.setImageBitmap(pdf417Data.docBackBitmap);
            } else {
                ly_back.setVisibility(View.GONE);
            }

            if (pdf417Data.faceBitmap != null) {
                face1 = pdf417Data.faceBitmap;
            }
            setData();
        } else if (recogType == RecogType.BARCODE) {
            // RecogType.BARCODE
            ly_barcode_container.setVisibility(View.VISIBLE);
            ly_back.setVisibility(View.GONE);
            faceImageContainer.setVisibility(View.GONE);

            OcrData ocrData = OcrData.getOcrResult();

            if (ocrData != null) {
                TextView textView = findViewById(R.id.tv_barcode_data);
                textView.setText(ocrData.getFrontData().getOcr_data().get(0).getKey_data());

                final Bitmap frontBitmap = ocrData.getFrontimage();
                if (frontBitmap != null && !frontBitmap.isRecycled()) iv_frontside.setImageBitmap(frontBitmap);
                else ly_front.setVisibility(View.GONE);
            }
        }
    }

    private void initUI() {
        //initialize the UI
        ivUserProfile = findViewById(R.id.ivUserProfile);
        faceImageContainer = findViewById(R.id.lyt_face_image_container);
        faceImageContainer.setVisibility(View.GONE);

        ly_back = findViewById(R.id.ly_back);
        ly_front = findViewById(R.id.ly_front);
        iv_frontside = findViewById(R.id.iv_frontside);
        iv_backside = findViewById(R.id.iv_backside);

        tv_security = findViewById(R.id.tv_security);

        mrz_table_layout = findViewById(R.id.mrz_table_layout);
        front_table_layout = findViewById(R.id.front_table_layout);
        back_table_layout = findViewById(R.id.back_table_layout);
        pdf417_table_layout = findViewById(R.id.pdf417_table_layout);
        usdl_table_layout = findViewById(R.id.usdl_table_layout);
        bank_table_layout = findViewById(R.id.bank_table_layout);

        dl_plate_lout = findViewById(R.id.dl_plate_lout);
        ly_mrz_container = findViewById(R.id.ly_mrz_container);
        ly_front_container = findViewById(R.id.ly_front_container);
        ly_back_container = findViewById(R.id.ly_back_container);
        ly_security_container = findViewById(R.id.ly_security_container);
        ly_pdf417_container = findViewById(R.id.ly_pdf417_container);
        ly_usdl_container = findViewById(R.id.ly_usdl_container);
        ly_bank_container = findViewById(R.id.ly_bank_container);
        ly_barcode_container = findViewById(R.id.ly_barcode_container);

        ly_security_container.setVisibility(View.GONE);
        ly_front_container.setVisibility(View.GONE);
        ly_back_container.setVisibility(View.GONE);
        ly_mrz_container.setVisibility(View.GONE);
        ly_pdf417_container.setVisibility(View.GONE);
        ly_usdl_container.setVisibility(View.GONE);
        dl_plate_lout.setVisibility(View.GONE);
        ly_bank_container.setVisibility(View.GONE);
        ly_barcode_container.setVisibility(View.GONE);
    }

    private void setOcrData(OcrData ocrData) {

        Frontdata = ocrData.getFrontData();
        Backdata = ocrData.getBackData();

        if (face1 == null && ocrData.getFaceImage() != null && !ocrData.getFaceImage().isRecycled()) {
            face1 = ocrData.getFaceImage();
        }

        if (Frontdata != null) {
            ly_front_container.setVisibility(View.VISIBLE);
            for (int i = 0; i < Frontdata.getOcr_data().size(); i++) {
                final OcrData.MapData.ScannedData array = Frontdata.getOcr_data().get(i);
                if (array != null) {
                    final int data_type = array.getType();
                    final String key = array.getKey();
                    final String value = array.getKey_data();
                    final View layout = LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
                    final TextView tv_key = layout.findViewById(R.id.tv_key);
                    final TextView tv_value = layout.findViewById(R.id.tv_value);
                    final ImageView imageView = layout.findViewById(R.id.iv_image);
                    if (data_type == 1) {
                        if (!key.toLowerCase().contains("mrz")) {
                            if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                                tv_key.setText(key + ":");
                                tv_value.setText(value);
                                imageView.setVisibility(View.GONE);
                                front_table_layout.addView(layout);
                            }
                        } else if (key.toLowerCase().contains("mrz")) {
                            setMRZData(ocrData.getMrzData());
                        }
                    } else if (data_type == 2) {
                        if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                            try {
                                if (key.toLowerCase().contains("face")) {
//                                    if (face1 == null) {
//                                        face1 = array.getImage();
//                                    }
                                } else {
                                    tv_key.setText(key + ":");
                                    Bitmap myBitmap = array.getImage();
                                    if (myBitmap != null) {
                                        imageView.setImageBitmap(myBitmap);
                                        tv_value.setVisibility(View.GONE);
                                        front_table_layout.addView(layout);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            tv_value.setText(value);
                            imageView.setVisibility(View.GONE);
                            front_table_layout.addView(layout);
                        }
                    } else if (data_type == 3) {
                        updateSecurityLayout(value);
                    }
                }
            }
//            Glide.with(this).load(Base64.decode(ocrData.getFrontDocument(), Base64.DEFAULT)).into(iv_frontside);
            final Bitmap frontBitmap = ocrData.getFrontimage();
            if (frontBitmap != null && !frontBitmap.isRecycled()) {
                iv_frontside.setImageBitmap(frontBitmap);
            }
        } else {
            ly_front.setVisibility(View.GONE);
            ly_front_container.setVisibility(View.GONE);
        }
        if (Backdata != null) {
            boolean isBackVisible = true;
            ly_back_container.setVisibility(View.VISIBLE);
            for (int i = 0; i < Backdata.getOcr_data().size(); i++) {
                View layout = LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
                TextView tv_key = layout.findViewById(R.id.tv_key);
                TextView tv_value = layout.findViewById(R.id.tv_value);
                ImageView imageView = layout.findViewById(R.id.iv_image);
                final OcrData.MapData.ScannedData array = Backdata.getOcr_data().get(i);

                if (array != null) {
                    int data_type = array.getType();
                    String key = array.getKey();
                    final String value = array.getKey_data();
                    if (data_type == 1) {
                        if (!key.equalsIgnoreCase("mrz")) {
                            if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                                tv_key.setText(key + ":");
                                tv_value.setText(value);
                                imageView.setVisibility(View.GONE);
                                back_table_layout.addView(layout);
                                isBackVisible = true;
                            }
                        } else {
                            if (Backdata.getOcr_data().size()==1) isBackVisible = false;
                            setMRZData(ocrData.getMrzData());
                        }
                    } else if (data_type == 2) {
                        isBackVisible = true;
                        if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                            try {
                                tv_key.setText(key + ":");
                                Bitmap myBitmap = array.getImage();
                                if (myBitmap != null) {
                                    imageView.setImageBitmap(myBitmap);
                                    tv_value.setVisibility(View.GONE);
                                    back_table_layout.addView(layout);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            tv_value.setText(value);
                            imageView.setVisibility(View.GONE);
                            back_table_layout.addView(layout);

                        }
                    } else if (data_type == 3) {
                        updateSecurityLayout(value);
                    }

                }
            }
//            Glide.with(this).load(Base64.decode(ocrData.getBackDocument(), Base64.DEFAULT)).into(iv_backside);
            final Bitmap BackImage = ocrData.getBackimage();
            if (BackImage != null && !BackImage.isRecycled()) {
                iv_backside.setImageBitmap(BackImage);
            }
            if (!isBackVisible) {
                // hide OCR back container if Back side contains only MRZ data
                ly_back_container.setVisibility(View.GONE);
            }
        } else {
            ly_back.setVisibility(View.GONE);
            ly_back_container.setVisibility(View.GONE);
        }
        setData();
    }

    private void updateSecurityLayout(String s) {
        boolean isVerified = Boolean.parseBoolean(s);
        if (isVerified) {
            tv_security.setText("YES");
            tv_security.setTextColor(getResources().getColor(R.color.security_true));
        } else {
            tv_security.setTextColor(getResources().getColor(R.color.security_false));
            tv_security.setText("NO");
        }
        ly_security_container.setVisibility(View.VISIBLE);
    }

    private void setBankData(CardDetails bankData){
        if (bankData == null) return;
        ly_bank_container.setVisibility(View.VISIBLE);
        addBankLayout("Cheque Number", bankData.getNumber());
        addBankLayout("MICR Code", bankData.getCardType());
        addBankLayout("Completed Data", bankData.getOwner());
        addBankLayout("Expiry Month", bankData.getExpirationMonth());
        addBankLayout("Expiry Year", bankData.getExpirationYear());
    }

    private void addBankLayout(String key, String s) {
        if (TextUtils.isEmpty(s)) return;
        View layout1 = LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
        TextView tv_key1 = layout1.findViewById(R.id.tv_key);
        TextView tv_value1 = layout1.findViewById(R.id.tv_value);
        tv_key1.setText(key);
        tv_value1.setText(s);
        bank_table_layout.addView(layout1);
    }

    private void setMRZData(RecogResult recogResult) {
        if (recogResult == null) {
            return;
        }
        ly_mrz_container.setVisibility(View.VISIBLE);
        addLayout("MRZ", recogResult.lines);
        addLayout("Document Type", recogResult.docType);
        addLayout("First Name", recogResult.givenname);
        addLayout("Last Name", recogResult.surname);
        addLayout("Document No.", recogResult.docnumber);
        addLayout("Document check No.", recogResult.docchecksum);
        addLayout("Correct Document check No.", recogResult.correctdocchecksum);
        addLayout("Country", recogResult.country);
        addLayout("Nationality", recogResult.nationality);
        String s = (recogResult.sex.equals("M")) ? "Male" : ((recogResult.sex.equals("F")) ? "Female" : recogResult.sex);
        addLayout("Sex", s);
        addLayout("Date of Birth", recogResult.birth);
        addLayout("Birth Check No.", recogResult.birthchecksum);
        addLayout("Correct Birth Check No.", recogResult.correctbirthchecksum);
        addLayout("Date of Expiry", recogResult.expirationdate);
        addLayout("Expiration Check No.", recogResult.expirationchecksum);
        addLayout("Correct Expiration Check No.", recogResult.correctexpirationchecksum);
        addLayout("Date Of Issue", recogResult.issuedate);
        addLayout("Department No.", recogResult.departmentnumber);
        addLayout("Other ID", recogResult.otherid);
        addLayout("Other ID2", recogResult.otherid2);
        addLayout("Other ID Check", recogResult.otheridchecksum);
        addLayout("Second Row Check No.", recogResult.secondrowchecksum);
        addLayout("Correct Second Row Check No.", recogResult.correctsecondrowchecksum);
    }

    private void addLayout(String key, String s) {
        if (TextUtils.isEmpty(s)) return;
        View layout1 = LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
        TextView tv_key1 = layout1.findViewById(R.id.tv_key);
        TextView tv_value1 = layout1.findViewById(R.id.tv_value);
        tv_key1.setText(key);
        tv_value1.setText(s);
        mrz_table_layout.addView(layout1);
    }

    private void setBarcodeData(PDF417Data barcodeData) {
        if (barcodeData != null) {
            ly_usdl_container.setVisibility(View.VISIBLE);
        } else return;
        if (!TextUtils.isEmpty(barcodeData.wholeDataString)) {
            View layout = (View) LayoutInflater.from(this).inflate(R.layout.table_row, null);
            TextView tv_key417 = layout.findViewById(R.id.tv_key);
            TextView tv_value417 = layout.findViewById(R.id.tv_value);
            tv_key417.setText("PDF417");
            tv_value417.setGravity(View.TEXT_ALIGNMENT_TEXT_START);
            tv_value417.setText(barcodeData.wholeDataString);
            pdf417_table_layout.addView(layout);
            ly_pdf417_container.setVisibility(View.VISIBLE);
        }
        addBarcodeLayout(getString(R.string.firstName), barcodeData.fname);
        addBarcodeLayout(getString(R.string.firstName), barcodeData.firstName);
        addBarcodeLayout(getString(R.string.firstName), barcodeData.firstName1);
        addBarcodeLayout(getString(R.string.lastName), barcodeData.lname);
        addBarcodeLayout(getString(R.string.lastName), barcodeData.lastName);
        addBarcodeLayout(getString(R.string.lastName), barcodeData.lastName1);
        addBarcodeLayout(getString(R.string.middle_name), barcodeData.mname);
        addBarcodeLayout(getString(R.string.middle_name), barcodeData.middleName);
        addBarcodeLayout(getString(R.string.addressLine1), barcodeData.address1);
        addBarcodeLayout(getString(R.string.addressLine2), barcodeData.address2);
        addBarcodeLayout(getString(R.string.ResidenceStreetAddress1), barcodeData.ResidenceAddress1);
        addBarcodeLayout(getString(R.string.ResidenceStreetAddress2), barcodeData.ResidenceAddress2);
        addBarcodeLayout(getString(R.string.city), barcodeData.city);
        addBarcodeLayout(getString(R.string.zipcode), barcodeData.zipcode);
        addBarcodeLayout(getString(R.string.birth_date), barcodeData.birthday);
        addBarcodeLayout(getString(R.string.birth_date), barcodeData.birthday1);
        addBarcodeLayout(getString(R.string.license_number), barcodeData.licence_number);
        addBarcodeLayout(getString(R.string.license_expiry_date), barcodeData.licence_expire_date);
        addBarcodeLayout(getString(R.string.sex), barcodeData.sex);
        addBarcodeLayout(getString(R.string.jurisdiction_code), barcodeData.jurisdiction);
        addBarcodeLayout(getString(R.string.license_classification), barcodeData.licenseClassification);
        addBarcodeLayout(getString(R.string.license_restriction), barcodeData.licenseRestriction);
        addBarcodeLayout(getString(R.string.license_endorsement), barcodeData.licenseEndorsement);
        addBarcodeLayout(getString(R.string.issue_date), barcodeData.issueDate);
        addBarcodeLayout(getString(R.string.organ_donor), barcodeData.organDonor);
        addBarcodeLayout(getString(R.string.height_in_ft), barcodeData.heightinFT);
        addBarcodeLayout(getString(R.string.height_in_cm), barcodeData.heightCM);
        addBarcodeLayout(getString(R.string.full_name), barcodeData.fullName);
        addBarcodeLayout(getString(R.string.full_name), barcodeData.fullName1);
        addBarcodeLayout(getString(R.string.weight_in_lbs), barcodeData.weightLBS);
        addBarcodeLayout(getString(R.string.weight_in_kg), barcodeData.weightKG);
        addBarcodeLayout(getString(R.string.name_prefix), barcodeData.namePrefix);
        addBarcodeLayout(getString(R.string.name_suffix), barcodeData.nameSuffix);
        addBarcodeLayout(getString(R.string.prefix), barcodeData.Prefix);
        addBarcodeLayout(getString(R.string.suffix), barcodeData.Suffix);
        addBarcodeLayout(getString(R.string.suffix), barcodeData.Suffix1);
        addBarcodeLayout(getString(R.string.eye_color), barcodeData.eyeColor);
        addBarcodeLayout(getString(R.string.hair_color), barcodeData.hairColor);
        addBarcodeLayout(getString(R.string.issue_time), barcodeData.issueTime);
        addBarcodeLayout(getString(R.string.number_of_duplicate), barcodeData.numberDuplicate);
        addBarcodeLayout(getString(R.string.unique_customer_id), barcodeData.uniqueCustomerId);
        addBarcodeLayout(getString(R.string.social_security_number), barcodeData.socialSecurityNo);
        addBarcodeLayout(getString(R.string.social_security_number), barcodeData.socialSecurityNo1);
        addBarcodeLayout(getString(R.string.under_18), barcodeData.under18);
        addBarcodeLayout(getString(R.string.under_19), barcodeData.under19);
        addBarcodeLayout(getString(R.string.under_21), barcodeData.under21);
        addBarcodeLayout(getString(R.string.permit_classification_code), barcodeData.permitClassification);
        addBarcodeLayout(getString(R.string.veteran_indicator), barcodeData.veteranIndicator);
        addBarcodeLayout(getString(R.string.permit_issue), barcodeData.permitIssue);
        addBarcodeLayout(getString(R.string.permit_expire), barcodeData.permitExpire);
        addBarcodeLayout(getString(R.string.permit_restriction), barcodeData.permitRestriction);
        addBarcodeLayout(getString(R.string.permit_endorsement), barcodeData.permitEndorsement);
        addBarcodeLayout(getString(R.string.court_restriction), barcodeData.courtRestriction);
        addBarcodeLayout(getString(R.string.inventory_control_no), barcodeData.inventoryNo);
        addBarcodeLayout(getString(R.string.race_ethnicity), barcodeData.raceEthnicity);
        addBarcodeLayout(getString(R.string.standard_vehicle_class), barcodeData.standardVehicleClass);
        addBarcodeLayout(getString(R.string.document_discriminator), barcodeData.documentDiscriminator);
        addBarcodeLayout(getString(R.string.ResidenceCity), barcodeData.ResidenceCity);
        addBarcodeLayout(getString(R.string.ResidenceJurisdictionCode), barcodeData.ResidenceJurisdictionCode);
        addBarcodeLayout(getString(R.string.ResidencePostalCode), barcodeData.ResidencePostalCode);
        addBarcodeLayout(getString(R.string.MedicalIndicatorCodes), barcodeData.MedicalIndicatorCodes);
        addBarcodeLayout(getString(R.string.NonResidentIndicator), barcodeData.NonResidentIndicator);
        addBarcodeLayout(getString(R.string.VirginiaSpecificClass), barcodeData.VirginiaSpecificClass);
        addBarcodeLayout(getString(R.string.VirginiaSpecificRestrictions), barcodeData.VirginiaSpecificRestrictions);
        addBarcodeLayout(getString(R.string.VirginiaSpecificEndorsements), barcodeData.VirginiaSpecificEndorsements);
        addBarcodeLayout(getString(R.string.PhysicalDescriptionWeight), barcodeData.PhysicalDescriptionWeight);
        addBarcodeLayout(getString(R.string.CountryTerritoryOfIssuance), barcodeData.CountryTerritoryOfIssuance);
        addBarcodeLayout(getString(R.string.FederalCommercialVehicleCodes), barcodeData.FederalCommercialVehicleCodes);
        addBarcodeLayout(getString(R.string.PlaceOfBirth), barcodeData.PlaceOfBirth);
        addBarcodeLayout(getString(R.string.StandardEndorsementCode), barcodeData.StandardEndorsementCode);
        addBarcodeLayout(getString(R.string.StandardRestrictionCode), barcodeData.StandardRestrictionCode);
        addBarcodeLayout(getString(R.string.JuriSpeciVehiClassiDescri), barcodeData.JuriSpeciVehiClassiDescri);
        addBarcodeLayout(getString(R.string.JuriSpeciRestriCodeDescri), barcodeData.JuriSpeciRestriCodeDescri);
        addBarcodeLayout(getString(R.string.ComplianceType), barcodeData.ComplianceType);
        addBarcodeLayout(getString(R.string.CardRevisionDate), barcodeData.CardRevisionDate);
        addBarcodeLayout(getString(R.string.HazMatEndorsementExpiryDate), barcodeData.HazMatEndorsementExpiryDate);
        addBarcodeLayout(getString(R.string.LimitedDurationDocumentIndicator), barcodeData.LimitedDurationDocumentIndicator);
        addBarcodeLayout(getString(R.string.FamilyNameTruncation), barcodeData.FamilyNameTruncation);
        addBarcodeLayout(getString(R.string.FirstNamesTruncation), barcodeData.FirstNamesTruncation);
        addBarcodeLayout(getString(R.string.MiddleNamesTruncation), barcodeData.MiddleNamesTruncation);
        addBarcodeLayout(getString(R.string.organ_donor_indicator), barcodeData.OrganDonorIndicator);
        addBarcodeLayout(getString(R.string.PermitIdentifier), barcodeData.PermitIdentifier);
        addBarcodeLayout(getString(R.string.AuditInformation), barcodeData.AuditInformation);
        addBarcodeLayout(getString(R.string.JurisdictionSpecific), barcodeData.JurisdictionSpecific);

    }

    private void addBarcodeLayout(String key, String s) {
        if (TextUtils.isEmpty(s)) return;
        View layout1 = LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
        TextView tv_key1 = layout1.findViewById(R.id.tv_key);
        TextView tv_value1 = layout1.findViewById(R.id.tv_value);
        tv_key1.setText(key);
        tv_value1.setText(s);
        usdl_table_layout.addView(layout1);

    }

    private void setData() {
        if (face1 != null) {
            ivUserProfile.setImageBitmap(face1);
            faceImageContainer.setVisibility(View.VISIBLE);
        } else {
            faceImageContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().gc();
    }

    @Override
    public void onBackPressed() {

        //<editor-fold desc="To resolve memory leak">
        if ((RecogType.detachFrom(getIntent()) == RecogType.OCR || RecogType.detachFrom(getIntent()) == RecogType.DL_PLATE) && OcrData.getOcrResult() != null) {
            try {
                OcrData.getOcrResult().getFrontimage().recycle();
            } catch (Exception e) {
            }
            try {
                OcrData.getOcrResult().getBackimage().recycle();
            } catch (Exception e) {
            }
            try {
                OcrData.getOcrResult().getFaceImage().recycle();
            } catch (Exception e) {
            }
        }else if (RecogType.detachFrom(getIntent()) == RecogType.MRZ && RecogResult.getRecogResult() != null) {
            try {
                RecogResult.getRecogResult().docFrontBitmap.recycle();
                RecogResult.getRecogResult().faceBitmap.recycle();
                RecogResult.getRecogResult().docBackBitmap.recycle();
            } catch (Exception e) {
            }
        }else if (RecogType.detachFrom(getIntent()) == RecogType.PDF417 && PDF417Data.getPDF417Result() != null) {
            PDF417Data.getPDF417Result().faceBitmap.recycle();
            PDF417Data.getPDF417Result().docFrontBitmap.recycle();
            PDF417Data.getPDF417Result().docBackBitmap.recycle();
        }
        OcrData.setOcrResult(null);
        CardDetails.setCardDetails(null);
        RecogResult.setRecogResult(null);
        PDF417Data.setPDF417Result(null);
        //</editor-fold>

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setResult(RESULT_OK);
        finish();
    }

}
