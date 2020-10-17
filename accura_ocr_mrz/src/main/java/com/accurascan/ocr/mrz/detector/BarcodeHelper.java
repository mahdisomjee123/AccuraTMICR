package com.accurascan.ocr.mrz.detector;

import android.text.TextUtils;

import com.accurascan.ocr.mrz.model.PDF417Data;

import java.util.ArrayList;
import java.util.HashMap;

public class BarcodeHelper {

    public static boolean extractScanResult(String output, PDF417Data pdf417Data) {
//        PDF417Data pdf417Data = new PDF417Data();
        String Customer_Family_Name = "DCS", Customer_Given_Name = "DCT", Name_Suffix = "DCU",
                Street_Address_1 = "DAG", City = "DAI", Jurisdction_Code = "DAJ", Postal_Code = "DAK",
                Customer_Id_Number = "DAQ", Expiration_Date = "DBA", Sex = "DBC",
                Customer_First_Name = "DAC", Customer_Middle_Name = "DAD", Street_Address_2 = "DAH",
                Street_Address_1_optional = "DAL", Street_Address_2_optional = "DAM", Date_Of_Birth = "DBB",
                NameSuff = "DAE", NamePref = "DAF", LicenseClassification = "DAR", LicenseRestriction = "DAS",
                LicenseEndorsement = "DAT", IssueDate = "DBD", OrganDonor = "DBH", HeightFT = "DAU",
                FullName = "DAA", HeightCM = "DAV", WeightLBS = "DAW", WeightKG = "DAX",
                EyeColor = "DAY", HairColor = "DAZ", IssueTime = "DBE", NumberDuplicate = "DBF",
                UniqueCustomerId = "DBJ", SocialSecurityNo = "DBM", Under18 = "DDH", Under19 = "DDI",
                Under21 = "DDJ", PermitClassification = "PAA", VeteranIndicator = "DDL",
                PermitIssue = "PAD", PermitExpire = "PAB", PermitRestriction = "PAE", PermitEndorsement = "PAF",
                CourtRestriction = "ZVA", InventoryControlNo = "DCK", RaceEthnicity = "DCL", StandardVehicleClass = "DCM", DocumentDiscriminator = "DCF",
                Customer_Last_Name = "DAB", ResidenceCity = "DAN", ResidenceJurisdictionCode = "DAO", ResidencePostalCode = "DAP", MedicalIndicatorCodes = "DBG",
                NonResidentIndicator = "DBI", SocialSecurityNumber = "DBK", DateOfBirth = "DBL", FullName1 = "DBN", LastName = "DBO", FirstName = "DBP", MiddelName = "DBQ",
                Suffix = "DBR", Prefix = "DBS", VirginiaSpecificClass = "DCA", VirginiaSpecificRestrictions = "DCB", VirginiaSpecificEndorsements = "DCD", PhysicalDescriptionWeight = "DCE", CountryTerritoryOfIssuance = "DCG",
                FederalCommercialVehicleCodes = "DCH", PlaceOfBirth = "DCI", AuditInformation = "DCJ", StandardEndorsementCode = "DCN", StandardRestrictionCode = "DCO", JuriSpeciVehiClassiDescri = "DCP",
                JurisdictionSpecific = "DCQ", JuriSpeciRestriCodeDescri = "DCR", ComplianceType = "DDA", CardRevisionDate = "DDB", HazMatEndorsementExpiryDate = "DDC", LimitedDurationDocumentIndicator = "DDD",
                FamilyNameTruncation = "DDE", FirstNamesTruncation = "DDF", MiddleNamesTruncation = "DDG", OrganDonorIndicator = "DDK", PermitIdentifier = "PAC";

        HashMap<String, String> dataHashMap = new HashMap<>();
        ArrayList<String> allElements = new ArrayList<>();

        allElements.add(PermitIdentifier);
        allElements.add(OrganDonorIndicator);
        allElements.add(MiddleNamesTruncation);
        allElements.add(FirstNamesTruncation);
        allElements.add(FamilyNameTruncation);
        allElements.add(LimitedDurationDocumentIndicator);
        allElements.add(HazMatEndorsementExpiryDate);
        allElements.add(CardRevisionDate);
        allElements.add(ComplianceType);
        allElements.add(JuriSpeciRestriCodeDescri);
        allElements.add(JurisdictionSpecific);
        allElements.add(JuriSpeciVehiClassiDescri);
        allElements.add(StandardRestrictionCode);
        allElements.add(StandardEndorsementCode);
        allElements.add(AuditInformation);
        allElements.add(PlaceOfBirth);
        allElements.add(FederalCommercialVehicleCodes);
        allElements.add(CountryTerritoryOfIssuance);
        allElements.add(PhysicalDescriptionWeight);
        allElements.add(VirginiaSpecificRestrictions);
        allElements.add(VirginiaSpecificClass);
        allElements.add(Prefix);
        allElements.add(Suffix);
        allElements.add(MiddelName);
        allElements.add(FirstName);
        allElements.add(LastName);
        allElements.add(FullName1);
        allElements.add(DateOfBirth);
        allElements.add(SocialSecurityNumber);
        allElements.add(NonResidentIndicator);
        allElements.add(MedicalIndicatorCodes);
        allElements.add(ResidencePostalCode);
        allElements.add(ResidenceJurisdictionCode);
        allElements.add(ResidenceCity);
        allElements.add(Customer_Last_Name);
        allElements.add(Street_Address_1);
        allElements.add(City);
        allElements.add(Jurisdction_Code);
        allElements.add(Postal_Code);
        allElements.add(Expiration_Date);
        allElements.add(Sex);
        allElements.add(Street_Address_2);
        allElements.add(Street_Address_1_optional);
        allElements.add(Street_Address_2_optional);
        allElements.add(Date_Of_Birth);
        allElements.add(Customer_Family_Name);
        allElements.add(Customer_First_Name);
        allElements.add(Customer_Given_Name);
        allElements.add(Customer_Id_Number);
        allElements.add(Customer_Middle_Name);
        allElements.add(Name_Suffix);
        allElements.add(NameSuff);
        allElements.add(NamePref);
        allElements.add(LicenseClassification);
        allElements.add(LicenseRestriction);
        allElements.add(LicenseEndorsement);
        allElements.add(IssueDate);
        allElements.add(OrganDonor);
        allElements.add(HeightFT);
        allElements.add(FullName);
        allElements.add(HeightCM);
        allElements.add(WeightLBS);
        allElements.add(WeightKG);
        allElements.add(EyeColor);
        allElements.add(HairColor);
        allElements.add(IssueTime);
        allElements.add(NumberDuplicate);
        allElements.add(UniqueCustomerId);
        allElements.add(SocialSecurityNo);
        allElements.add(Under18);
        allElements.add(Under19);
        allElements.add(Under21);
        allElements.add(PermitClassification);
        allElements.add(VeteranIndicator);
        allElements.add(PermitIssue);
        allElements.add(PermitExpire);
        allElements.add(PermitRestriction);
        allElements.add(PermitEndorsement);
        allElements.add(CourtRestriction);
        allElements.add(InventoryControlNo);
        allElements.add(RaceEthnicity);
        allElements.add(StandardVehicleClass);
        allElements.add(DocumentDiscriminator);

        if (!TextUtils.isEmpty(output)) {
            String lines[] = output.split("\\r?\\n");
            if (lines.length > 0) {
                for (String line : lines) {
                    String str = line;
                    if (str.contains("ANSI") && str.contains("DL")) {
                        str = str.substring(str.indexOf("DL"));
                        String str1[] = str.split("DL");
                        if (str1.length > 1) {
                            str = str1[str1.length - 1];
                        }
                    }
                    if (str.length() > 3) {
                        String key = str.substring(0, 3);
                        String value = str.substring(3);
                        if (allElements.contains(key)) {
                            if (!value.equalsIgnoreCase("None")) {
                                /*Add key value in hashmap*/
                                dataHashMap.put(allElements.get(allElements.indexOf(key)), value);
                            }
                        }
                    }
                }
            }
        }

        pdf417Data.wholeDataString = output;

        /*check keys and value and set data in pdf417Data model class*/
        if (dataHashMap.containsKey(Customer_Family_Name) && !TextUtils.isEmpty(Customer_Family_Name)) {
            pdf417Data.lastName1 = dataHashMap.get(Customer_Family_Name).trim();
        }
        if (dataHashMap.containsKey(Customer_Last_Name) && !TextUtils.isEmpty(Customer_Last_Name)) {
            pdf417Data.lname = dataHashMap.get(Customer_Last_Name).trim();
        }

        if (dataHashMap.containsKey(LastName)) {
            pdf417Data.lastName = dataHashMap.get(LastName).trim();
        }
        if (dataHashMap.containsKey(ResidenceCity)) {
            pdf417Data.ResidenceCity = dataHashMap.get(ResidenceCity).trim();
        }
        if (dataHashMap.containsKey(ResidenceJurisdictionCode)) {
            pdf417Data.ResidenceJurisdictionCode = dataHashMap.get(ResidenceJurisdictionCode).trim();
        }
        if (dataHashMap.containsKey(ResidencePostalCode)) {
            pdf417Data.ResidencePostalCode = dataHashMap.get(ResidencePostalCode).trim();
        }
        if (dataHashMap.containsKey(MedicalIndicatorCodes)) {
            pdf417Data.MedicalIndicatorCodes = dataHashMap.get(MedicalIndicatorCodes).trim();
        }
        if (dataHashMap.containsKey(NonResidentIndicator)) {
            pdf417Data.NonResidentIndicator = dataHashMap.get(NonResidentIndicator).trim();
        }
        if (dataHashMap.containsKey(VirginiaSpecificClass)) {
            pdf417Data.VirginiaSpecificClass = dataHashMap.get(VirginiaSpecificClass).trim();
        }
        if (dataHashMap.containsKey(VirginiaSpecificRestrictions)) {
            pdf417Data.VirginiaSpecificRestrictions = dataHashMap.get(VirginiaSpecificRestrictions).trim();
        }
        if (dataHashMap.containsKey(PhysicalDescriptionWeight)) {
            pdf417Data.PhysicalDescriptionWeight = dataHashMap.get(PhysicalDescriptionWeight).trim();
        }
        if (dataHashMap.containsKey(CountryTerritoryOfIssuance)) {
            pdf417Data.CountryTerritoryOfIssuance = dataHashMap.get(CountryTerritoryOfIssuance).trim();
        }
        if (dataHashMap.containsKey(FederalCommercialVehicleCodes)) {
            pdf417Data.FederalCommercialVehicleCodes = dataHashMap.get(FederalCommercialVehicleCodes).trim();
        }
        if (dataHashMap.containsKey(PlaceOfBirth)) {
            pdf417Data.PlaceOfBirth = dataHashMap.get(PlaceOfBirth).trim();
        }
        if (dataHashMap.containsKey(StandardEndorsementCode)) {
            pdf417Data.StandardEndorsementCode = dataHashMap.get(StandardEndorsementCode).trim();
        }
        if (dataHashMap.containsKey(StandardRestrictionCode)) {
            pdf417Data.StandardRestrictionCode = dataHashMap.get(StandardRestrictionCode).trim();
        }
        if (dataHashMap.containsKey(JuriSpeciVehiClassiDescri)) {
            pdf417Data.JuriSpeciVehiClassiDescri = dataHashMap.get(JuriSpeciVehiClassiDescri).trim();
        }
        if (dataHashMap.containsKey(JurisdictionSpecific)) {
            pdf417Data.JurisdictionSpecific = dataHashMap.get(JurisdictionSpecific).trim();
        }
        if (dataHashMap.containsKey(JuriSpeciRestriCodeDescri)) {
            pdf417Data.JuriSpeciRestriCodeDescri = dataHashMap.get(JuriSpeciRestriCodeDescri).trim();
        }
        if (dataHashMap.containsKey(ComplianceType)) {
            pdf417Data.ComplianceType = dataHashMap.get(ComplianceType).trim();
        }
        if (dataHashMap.containsKey(CardRevisionDate)) {
            pdf417Data.CardRevisionDate = dataHashMap.get(CardRevisionDate).trim();
        }
        if (dataHashMap.containsKey(HazMatEndorsementExpiryDate)) {
            pdf417Data.HazMatEndorsementExpiryDate = dataHashMap.get(HazMatEndorsementExpiryDate).trim();
        }
        if (dataHashMap.containsKey(LimitedDurationDocumentIndicator)) {
            pdf417Data.LimitedDurationDocumentIndicator = dataHashMap.get(LimitedDurationDocumentIndicator).trim();
        }
        if (dataHashMap.containsKey(FamilyNameTruncation)) {
            pdf417Data.FamilyNameTruncation = dataHashMap.get(FamilyNameTruncation).trim();
        }
        if (dataHashMap.containsKey(FirstNamesTruncation)) {
            pdf417Data.FirstNamesTruncation = dataHashMap.get(FirstNamesTruncation).trim();
        }
        if (dataHashMap.containsKey(MiddleNamesTruncation)) {
            pdf417Data.MiddleNamesTruncation = dataHashMap.get(MiddleNamesTruncation).trim();
        }
        if (dataHashMap.containsKey(OrganDonorIndicator)) {
            pdf417Data.OrganDonorIndicator = dataHashMap.get(OrganDonorIndicator).trim();
        }
        if (dataHashMap.containsKey(PermitIdentifier)) {
            pdf417Data.PermitIdentifier = dataHashMap.get(PermitIdentifier).trim();
        }
        if (dataHashMap.containsKey(AuditInformation)) {
            pdf417Data.AuditInformation = dataHashMap.get(AuditInformation).trim();
        }

        if (dataHashMap.containsKey(Customer_Given_Name)) {
            try {
                String[] CustomerName = dataHashMap.get(Customer_Given_Name).split(" ");
                if (CustomerName.length >= 1)
                    pdf417Data.firstName1 = CustomerName[0].trim();
//                pdf417Data.mname = CustomerName[1].substring(0, 1).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(FirstName)) {
            pdf417Data.firstName = dataHashMap.get(FirstName);
        }
        if (dataHashMap.containsKey(Name_Suffix)) {
            pdf417Data.nameSuffix = dataHashMap.get(Name_Suffix);
        }
        if (dataHashMap.containsKey(Suffix)) {
            pdf417Data.Suffix = dataHashMap.get(Suffix);
        }
        if (dataHashMap.containsKey(Street_Address_1)) {
            try {
                pdf417Data.address1 = dataHashMap.get(Street_Address_1).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (dataHashMap.containsKey(Street_Address_2)) {
            try {
                pdf417Data.address2 = dataHashMap.get(Street_Address_2).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(Street_Address_1_optional)) {
            pdf417Data.ResidenceAddress1 = dataHashMap.get(Street_Address_1_optional).trim();
        }

        if (dataHashMap.containsKey(Street_Address_2_optional)) {
            pdf417Data.ResidenceAddress2 = dataHashMap.get(Street_Address_2_optional).trim();
        }


        if (dataHashMap.containsKey(City)) {
            pdf417Data.city = dataHashMap.get(City).trim();
        }
        if (dataHashMap.containsKey(Postal_Code)) {
            pdf417Data.zipcode = dataHashMap.get(Postal_Code);
        }
        if (dataHashMap.containsKey(Date_Of_Birth)) {
            if (dataHashMap.get(Date_Of_Birth).length() > 4) {
//                pdf417Data.birthday = dataHashMap.get(Date_Of_Birth).substring(0, 2) + "/"
//                        + dataHashMap.get(Date_Of_Birth).substring(2, 4) + "/" + dataHashMap.get(Date_Of_Birth).substring(4);

                pdf417Data.birthday = dataHashMap.get(Date_Of_Birth);
            }
        }
        if (dataHashMap.containsKey(DateOfBirth)) {
            if (dataHashMap.get(DateOfBirth).length() > 4) {
//                pdf417Data.birthday1 = dataHashMap.get(DateOfBirth).substring(0, 2) + "/"
//                        + dataHashMap.get(DateOfBirth).substring(2, 4) + "/" + dataHashMap.get(DateOfBirth).substring(4);

                pdf417Data.birthday = dataHashMap.get(DateOfBirth);
            }
        }
        if (dataHashMap.containsKey(Sex)) {
//            pdf417Data.sex = dataHashMap.get(Sex).trim().equals("1") ? "Male" : "Female";
            pdf417Data.sex = dataHashMap.get(Sex).trim();
        }
        if (dataHashMap.containsKey(FullName)) {
            String cName = dataHashMap.get(FullName);
            int startIndexOfComma;
            int endIndexOfComma;
            startIndexOfComma = cName.indexOf(",");
            endIndexOfComma = cName.lastIndexOf(",");
            if (startIndexOfComma != endIndexOfComma) {
                String CustomerName[] = dataHashMap.get(FullName).split(",");
                if (CustomerName.length >= 1)
                    pdf417Data.lname = CustomerName[0].replace(",", "").trim();
                if (CustomerName.length >= 2)
                    pdf417Data.fname = CustomerName[1].trim();
//                pdf417Data.mname = CustomerName[2].substring(0, 1).trim();
            } else {
                String CustomerName[] = dataHashMap.get(FullName).split(" ");
                if (CustomerName.length >= 1)
                    pdf417Data.lname = CustomerName[0].replace(",", "").trim();
                if (CustomerName.length >= 2)
                    pdf417Data.fname = CustomerName[1].trim();
//                pdf417Data.mname = CustomerName[2].substring(0, 1).trim();
            }
        }
        if (dataHashMap.containsKey(Customer_First_Name)) {
            pdf417Data.fname = dataHashMap.get(Customer_First_Name).trim();
        }
        if (dataHashMap.containsKey(Customer_Middle_Name)) {
            pdf417Data.mname = dataHashMap.get(Customer_Middle_Name).trim();
        }
        if (dataHashMap.containsKey(MiddelName)) {
            pdf417Data.middleName = dataHashMap.get(MiddelName);
        }

        if (dataHashMap.containsKey(Customer_Id_Number)) {
            pdf417Data.licence_number = dataHashMap.get(Customer_Id_Number).trim();
        }
        if (dataHashMap.containsKey(Expiration_Date) && dataHashMap.get(Expiration_Date).length() > 4) {
//            pdf417Data.licence_expire_date = dataHashMap.get(Expiration_Date).trim();
//            pdf417Data.licence_expire_date = dataHashMap.get(Expiration_Date).substring(0, 2) + "/"
//                    + dataHashMap.get(Expiration_Date).substring(2, 4) + "/" + dataHashMap.get(Expiration_Date).substring(4);
            pdf417Data.licence_expire_date = dataHashMap.get(Expiration_Date);
        }

        if (dataHashMap.containsKey(Jurisdction_Code)) {
            try {
                pdf417Data.jurisdiction = dataHashMap.get(Jurisdction_Code).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(NameSuff)) {
            try {
                pdf417Data.nameSuffix = dataHashMap.get(NameSuff).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(NamePref)) {
            try {
                pdf417Data.namePrefix = dataHashMap.get(NamePref).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!TextUtils.isEmpty(pdf417Data.namePrefix)) {
            pdf417Data.namePrefix = dataHashMap.get(Prefix);
        }

        if (dataHashMap.containsKey(LicenseClassification)) {
            try {
                pdf417Data.licenseClassification = dataHashMap.get(LicenseClassification).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(LicenseRestriction)) {
            try {
                pdf417Data.licenseRestriction = dataHashMap.get(LicenseRestriction).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(LicenseEndorsement)) {
            try {
                pdf417Data.licenseEndorsement = dataHashMap.get(LicenseEndorsement).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(IssueDate) && dataHashMap.get(IssueDate).length() > 4) {
//            try {
//                pdf417Data.issueDate = dataHashMap.get(IssueDate).substring(0, 2) + "/"
//                        + dataHashMap.get(IssueDate).substring(2, 4) + "/" + dataHashMap.get(IssueDate).substring(4);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            pdf417Data.issueDate = dataHashMap.get(IssueDate);
        }

        if (dataHashMap.containsKey(OrganDonor)) {
            try {
                pdf417Data.organDonor = dataHashMap.get(OrganDonor).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(HeightFT)) {
            try {
                pdf417Data.heightinFT = dataHashMap.get(HeightFT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(WeightLBS)) {
            try {
                pdf417Data.weightLBS = dataHashMap.get(WeightLBS).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(WeightKG)) {
            try {
                pdf417Data.weightKG = dataHashMap.get(WeightKG).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(EyeColor)) {
            try {
                pdf417Data.eyeColor = dataHashMap.get(EyeColor).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(HairColor)) {
            try {
                pdf417Data.hairColor = dataHashMap.get(HairColor).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(IssueTime)) {
            try {
                pdf417Data.issueTime = dataHashMap.get(IssueTime).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(PermitIssue)) {
            try {
                pdf417Data.permitIssue = dataHashMap.get(PermitIssue).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(PermitExpire)) {
            try {
                pdf417Data.permitExpire = dataHashMap.get(PermitExpire).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(PermitRestriction)) {
            try {
                pdf417Data.permitRestriction = dataHashMap.get(PermitRestriction).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(PermitEndorsement)) {
            try {
                pdf417Data.permitEndorsement = dataHashMap.get(PermitEndorsement).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(FullName) && !TextUtils.isEmpty(FullName)) {
            try {
                pdf417Data.fullName = dataHashMap.get(FullName).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (dataHashMap.containsKey(FullName1)) {
            try {
                pdf417Data.fullName1 = dataHashMap.get(FullName1).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(HeightCM)) {
            try {
                pdf417Data.heightCM = dataHashMap.get(HeightCM).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(NumberDuplicate)) {
            try {
                pdf417Data.numberDuplicate = dataHashMap.get(NumberDuplicate).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(UniqueCustomerId)) {
            try {
                pdf417Data.uniqueCustomerId = dataHashMap.get(UniqueCustomerId).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(SocialSecurityNo)) {
            try {
                pdf417Data.socialSecurityNo = dataHashMap.get(SocialSecurityNo).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (dataHashMap.containsKey(SocialSecurityNumber)) {
            try {
                pdf417Data.socialSecurityNo = dataHashMap.get(SocialSecurityNumber).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(Under18) && dataHashMap.get(Under18).length() > 4) {
            try {
                pdf417Data.under18 = dataHashMap.get(Under18).substring(0, 2) + "/"
                        + dataHashMap.get(Under18).substring(2, 4) + "/" + dataHashMap.get(Under18).substring(4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(Under19) && dataHashMap.get(Under19).length() > 4) {
            try {
                pdf417Data.under19 = dataHashMap.get(Under19).substring(0, 2) + "/"
                        + dataHashMap.get(Under19).substring(2, 4) + "/" + dataHashMap.get(Under19).substring(4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(Under21) && dataHashMap.get(Under21).length() > 4) {
            try {
                pdf417Data.under21 = dataHashMap.get(Under21).substring(0, 2) + "/"
                        + dataHashMap.get(Under21).substring(2, 4) + "/" + dataHashMap.get(Under21).substring(4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(InventoryControlNo)) {
            try {
                pdf417Data.inventoryNo = dataHashMap.get(InventoryControlNo).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(RaceEthnicity)) {
            try {
                pdf417Data.raceEthnicity = dataHashMap.get(RaceEthnicity).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(StandardVehicleClass)) {
            try {
                pdf417Data.standardVehicleClass = dataHashMap.get(StandardVehicleClass).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(DocumentDiscriminator)) {
            try {
                pdf417Data.documentDiscriminator = dataHashMap.get(DocumentDiscriminator).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(PermitClassification)) {
            try {
                pdf417Data.permitClassification = dataHashMap.get(PermitClassification).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(VeteranIndicator)) {
            try {
                pdf417Data.veteranIndicator = dataHashMap.get(VeteranIndicator).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataHashMap.containsKey(CourtRestriction)) {
            try {
                pdf417Data.courtRestriction = dataHashMap.get(CourtRestriction).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return pdf417Data.fname != null || pdf417Data.mname != null || pdf417Data.lname != null || pdf417Data.address1 != null || pdf417Data.city != null
                || pdf417Data.state != null || pdf417Data.zipcode != null || pdf417Data.licence_expire_date != null;
    }

}
