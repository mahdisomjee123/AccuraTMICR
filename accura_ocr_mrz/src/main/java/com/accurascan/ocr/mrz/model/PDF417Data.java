package com.accurascan.ocr.mrz.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class PDF417Data implements Parcelable {

    public String wholeDataString, fname, mname, lname, address1, address2, ResidenceAddress1, ResidenceAddress2, city, state, zipcode, birthday, birthday1,
            licence_number, licence_expire_date, sex, jurisdiction,
            Suffix, Suffix1, Prefix, nameSuffix, namePrefix, licenseClassification, licenseRestriction,
            licenseEndorsement, issueDate, organDonor, heightinFT, fullName, fullName1, lastName, firstName, firstName1, middleName, lastName1,
            givenName, heightCM, weightLBS, weightKG, eyeColor, hairColor,
            issueTime, numberDuplicate, uniqueCustomerId, socialSecurityNo, socialSecurityNo1, under18, under19,
            under21, permitClassification, veteranIndicator, permitIssue, permitExpire,
            permitRestriction, permitEndorsement, courtRestriction,
            inventoryNo, raceEthnicity, standardVehicleClass, documentDiscriminator,
            ResidenceCity, ResidenceJurisdictionCode, ResidencePostalCode, MedicalIndicatorCodes, NonResidentIndicator, VirginiaSpecificClass, VirginiaSpecificRestrictions, VirginiaSpecificEndorsements,
            PhysicalDescriptionWeight, CountryTerritoryOfIssuance, FederalCommercialVehicleCodes, PlaceOfBirth, StandardEndorsementCode, StandardRestrictionCode, JuriSpeciVehiClassiDescri,
            JurisdictionSpecific, JuriSpeciRestriCodeDescri, ComplianceType, CardRevisionDate, HazMatEndorsementExpiryDate, LimitedDurationDocumentIndicator, FamilyNameTruncation, FirstNamesTruncation, MiddleNamesTruncation, OrganDonorIndicator,
            PermitIdentifier, AuditInformation;

    public Bitmap faceBitmap = null;
    public Bitmap docBackBitmap = null;
    public Bitmap docFrontBitmap = null;

    public PDF417Data(){

    }

    protected PDF417Data(Parcel in) {
        wholeDataString = in.readString();
        fname = in.readString();
        mname = in.readString();
        lname = in.readString();
        address1 = in.readString();
        address2 = in.readString();
        ResidenceAddress1 = in.readString();
        ResidenceAddress2 = in.readString();
        city = in.readString();
        state = in.readString();
        zipcode = in.readString();
        birthday = in.readString();
        birthday1 = in.readString();
        licence_number = in.readString();
        licence_expire_date = in.readString();
        sex = in.readString();
        jurisdiction = in.readString();
        Suffix = in.readString();
        Suffix1 = in.readString();
        Prefix = in.readString();
        nameSuffix = in.readString();
        namePrefix = in.readString();
        licenseClassification = in.readString();
        licenseRestriction = in.readString();
        licenseEndorsement = in.readString();
        issueDate = in.readString();
        organDonor = in.readString();
        heightinFT = in.readString();
        fullName = in.readString();
        fullName1 = in.readString();
        lastName = in.readString();
        firstName = in.readString();
        middleName = in.readString();
        lastName1 = in.readString();
        givenName = in.readString();
        heightCM = in.readString();
        weightLBS = in.readString();
        weightKG = in.readString();
        eyeColor = in.readString();
        hairColor = in.readString();
        issueTime = in.readString();
        numberDuplicate = in.readString();
        uniqueCustomerId = in.readString();
        socialSecurityNo = in.readString();
        socialSecurityNo1 = in.readString();
        under18 = in.readString();
        under19 = in.readString();
        under21 = in.readString();
        permitClassification = in.readString();
        veteranIndicator = in.readString();
        permitIssue = in.readString();
        permitExpire = in.readString();
        permitRestriction = in.readString();
        permitEndorsement = in.readString();
        courtRestriction = in.readString();
        inventoryNo = in.readString();
        raceEthnicity = in.readString();
        standardVehicleClass = in.readString();
        documentDiscriminator = in.readString();
        ResidenceCity = in.readString();
        ResidenceJurisdictionCode = in.readString();
        ResidencePostalCode = in.readString();
        MedicalIndicatorCodes = in.readString();
        NonResidentIndicator = in.readString();
        VirginiaSpecificClass = in.readString();
        VirginiaSpecificRestrictions = in.readString();
        VirginiaSpecificEndorsements = in.readString();
        PhysicalDescriptionWeight = in.readString();
        CountryTerritoryOfIssuance = in.readString();
        FederalCommercialVehicleCodes = in.readString();
        PlaceOfBirth = in.readString();
        StandardEndorsementCode = in.readString();
        StandardRestrictionCode = in.readString();
        JuriSpeciVehiClassiDescri = in.readString();
        JurisdictionSpecific = in.readString();
        JuriSpeciRestriCodeDescri = in.readString();
        ComplianceType = in.readString();
        CardRevisionDate = in.readString();
        HazMatEndorsementExpiryDate = in.readString();
        LimitedDurationDocumentIndicator = in.readString();
        FamilyNameTruncation = in.readString();
        FirstNamesTruncation = in.readString();
        MiddleNamesTruncation = in.readString();
        OrganDonorIndicator = in.readString();
        PermitIdentifier = in.readString();
        AuditInformation = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(wholeDataString);
        dest.writeString(fname);
        dest.writeString(mname);
        dest.writeString(lname);
        dest.writeString(address1);
        dest.writeString(address2);
        dest.writeString(ResidenceAddress1);
        dest.writeString(ResidenceAddress2);
        dest.writeString(city);
        dest.writeString(state);
        dest.writeString(zipcode);
        dest.writeString(birthday);
        dest.writeString(birthday1);
        dest.writeString(licence_number);
        dest.writeString(licence_expire_date);
        dest.writeString(sex);
        dest.writeString(jurisdiction);
        dest.writeString(Suffix);
        dest.writeString(Suffix1);
        dest.writeString(Prefix);
        dest.writeString(nameSuffix);
        dest.writeString(namePrefix);
        dest.writeString(licenseClassification);
        dest.writeString(licenseRestriction);
        dest.writeString(licenseEndorsement);
        dest.writeString(issueDate);
        dest.writeString(organDonor);
        dest.writeString(heightinFT);
        dest.writeString(fullName);
        dest.writeString(fullName1);
        dest.writeString(lastName);
        dest.writeString(firstName);
        dest.writeString(middleName);
        dest.writeString(lastName1);
        dest.writeString(givenName);
        dest.writeString(heightCM);
        dest.writeString(weightLBS);
        dest.writeString(weightKG);
        dest.writeString(eyeColor);
        dest.writeString(hairColor);
        dest.writeString(issueTime);
        dest.writeString(numberDuplicate);
        dest.writeString(uniqueCustomerId);
        dest.writeString(socialSecurityNo);
        dest.writeString(socialSecurityNo1);
        dest.writeString(under18);
        dest.writeString(under19);
        dest.writeString(under21);
        dest.writeString(permitClassification);
        dest.writeString(veteranIndicator);
        dest.writeString(permitIssue);
        dest.writeString(permitExpire);
        dest.writeString(permitRestriction);
        dest.writeString(permitEndorsement);
        dest.writeString(courtRestriction);
        dest.writeString(inventoryNo);
        dest.writeString(raceEthnicity);
        dest.writeString(standardVehicleClass);
        dest.writeString(documentDiscriminator);
        dest.writeString(ResidenceCity);
        dest.writeString(ResidenceJurisdictionCode);
        dest.writeString(ResidencePostalCode);
        dest.writeString(MedicalIndicatorCodes);
        dest.writeString(NonResidentIndicator);
        dest.writeString(VirginiaSpecificClass);
        dest.writeString(VirginiaSpecificRestrictions);
        dest.writeString(VirginiaSpecificEndorsements);
        dest.writeString(PhysicalDescriptionWeight);
        dest.writeString(CountryTerritoryOfIssuance);
        dest.writeString(FederalCommercialVehicleCodes);
        dest.writeString(PlaceOfBirth);
        dest.writeString(StandardEndorsementCode);
        dest.writeString(StandardRestrictionCode);
        dest.writeString(JuriSpeciVehiClassiDescri);
        dest.writeString(JurisdictionSpecific);
        dest.writeString(JuriSpeciRestriCodeDescri);
        dest.writeString(ComplianceType);
        dest.writeString(CardRevisionDate);
        dest.writeString(HazMatEndorsementExpiryDate);
        dest.writeString(LimitedDurationDocumentIndicator);
        dest.writeString(FamilyNameTruncation);
        dest.writeString(FirstNamesTruncation);
        dest.writeString(MiddleNamesTruncation);
        dest.writeString(OrganDonorIndicator);
        dest.writeString(PermitIdentifier);
        dest.writeString(AuditInformation);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PDF417Data> CREATOR = new Creator<PDF417Data>() {
        @Override
        public PDF417Data createFromParcel(Parcel in) {
            return new PDF417Data(in);
        }

        @Override
        public PDF417Data[] newArray(int size) {
            return new PDF417Data[size];
        }
    };


    private static PDF417Data pdf417Data;

    public static PDF417Data getPDF417Result() {
        PDF417Data pdf417data = pdf417Data;
        pdf417Data = null;
        return pdf417data;
    }

    public static void setPDF417Result(PDF417Data ocrResult) {
        PDF417Data.pdf417Data = ocrResult;
    }
}
