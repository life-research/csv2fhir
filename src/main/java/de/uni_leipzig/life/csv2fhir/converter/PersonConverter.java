package de.uni_leipzig.life.csv2fhir.converter;

import static de.uni_leipzig.life.csv2fhir.TableIdentifier.Person;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.Person_Columns.Anschrift;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.Person_Columns.Geburtsdatum;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.Person_Columns.Geschlecht;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.Person_Columns.Krankenkasse;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.Person_Columns.Nachname;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.Person_Columns.Vorname;
import static org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.FEMALE;
import static org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE;
import static org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.OTHER;
import static org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.UNKNOWN;

import java.util.Collections;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Address.AddressType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

import com.google.common.base.Strings;

import de.uni_leipzig.imise.FHIRValidator;
import de.uni_leipzig.life.csv2fhir.Converter;
import de.uni_leipzig.life.csv2fhir.ConverterResult;

public class PersonConverter extends Converter {

    /**  */
    String PROFILE = "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Patient";
    // @see https://simplifier.net/MedizininformatikInitiative-ModulPerson/PatientIn

    /**
     * @param record
     * @param result
     * @param validator
     * @throws Exception
     */
    public PersonConverter(CSVRecord record, ConverterResult result, FHIRValidator validator) throws Exception {
        super(record, result, validator);
    }

    /**
     * Resets the static index counter
     */
    public static void reset() {
        //no static counter to reset at the moment
    }

    @Override
    public List<Resource> convert() throws Exception {
        Patient patient = new Patient();
        patient.setMeta(new Meta().addProfile(PROFILE));
        patient.setId(getPatientId());
        patient.setIdentifier(parseIdentifier());
        patient.addName(parseName());
        patient.setGender(parseGender());
        patient.setBirthDateElement(parseDate(Geburtsdatum));
        patient.addAddress(parseAddress());
        patient.addGeneralPractitioner(parseHealthProvider());
        return Collections.singletonList(patient);
    }

    @Override
    protected Enum<?> getPatientIDColumnIdentifier() {
        return Person.getPIDColumnIdentifier();
    }

    /**
     * @return
     * @throws Exception
     */
    private List<Identifier> parseIdentifier() throws Exception {
        Identifier identifier = new Identifier()
                .setSystem("https://" + getDIZId() + ".de/pid")
                .setValue(getPatientId())
                .setUse(IdentifierUse.USUAL)
                .setType(createCodeableConcept("http://terminology.hl7.org/CodeSystem/v2-0203", "MR"));
        return Collections.singletonList(identifier);
    }

    /**
     * @return
     */
    private HumanName parseName() {
        String forename = get(Vorname);
        String surname = get(Nachname);
        HumanName humanName = new HumanName();
        if (Strings.isNullOrEmpty(surname)) {
            warning("Empty " + Nachname + " -> Create Data Absent Reason \"unknown\"");
            StringType familyElement = humanName.getFamilyElement();
            familyElement.addExtension(getUnknownDataAbsentReason());
        } else {
            humanName.setFamily(surname).setUse(NameUse.OFFICIAL);
        }
        if (Strings.isNullOrEmpty(forename)) {
            warning("Empty " + Vorname + " -> Create Data Absent Reason \"unknown\"");
            StringType givenElement = humanName.addGivenElement();
            givenElement.addExtension(getUnknownDataAbsentReason());
        } else {
            for (String name : forename.split(" ")) {
                humanName.addGiven(name);
            }
        }
        return humanName;
    }

    /**
     * @return
     * @throws Exception
     */
    private AdministrativeGender parseGender() throws Exception {
        String gender = get(Geschlecht);
        if (gender != null) {
            if (gender.length() != 0) {
                switch (gender) {
                case "m":
                case "male":
                case "männlich":
                    return MALE;
                case "w":
                case "weiblich":
                case "female":
                case "f":
                    return FEMALE;
                case "d":
                case "divers":
                case "other":
                case "unbestimmt":
                    return OTHER;
                case "x":
                case "unbekannt":
                case "unknown":
                    return UNKNOWN;
                default:
                    throw new Exception("Error on " + Person + ": " + Geschlecht + " <" + gender + "> not parsable for Record: " + this);
                }
            }
            warning("Geschlecht empty for Record");
            return UNKNOWN;
        }
        warning("Geschlecht not found");
        return UNKNOWN;
    }

    /**
     * @return
     */
    private Address parseAddress() {
        String address = get(Anschrift);
        Address a;
        if (address != null) {
            a = new Address();
            String[] addressSplitByComma = address.split(",");
            if (addressSplitByComma.length == 2) {
                String[] addressPlzAndCity = addressSplitByComma[1].split(" ");
                String plz = addressPlzAndCity[1];
                StringBuilder city = new StringBuilder();
                for (int i = 2; i < addressPlzAndCity.length; i++) {
                    city.append(addressPlzAndCity[i]);
                }
                List<StringType> l = Collections.singletonList(new StringType(addressSplitByComma[0]));
                a.setCity(city.toString()).setPostalCode(plz).setLine(l);
            } else {
                // "12345 ORT"
                String[] addressPlzAndCity = address.split(" ");
                if (addressPlzAndCity.length == 2) {
                    String plz = addressPlzAndCity[0];
                    String city = addressPlzAndCity[1];
                    a.setCity(city).setPostalCode(plz).setText(address);
                } else {
                    a.setText(address);
                }
            }
            return a.setType(AddressType.BOTH).setCountry("DE");
        }
        warning("On " + Person + ": " + Anschrift + " empty. Creating dummy address. " + this);
        return getDummyAddress(); //KDS-Validator needs an Address
    }

    /**
     * @return a dummy address
     */
    public Address getDummyAddress() {
        return new Address()
                .setCity("Dummy City")
                .setPostalCode("00000")
                .setLine(List.of(new StringType("Dummy Street 1")));
    }

    /**
     * @return
     * @throws Exception
     */
    private Reference parseHealthProvider() throws Exception {
        String practitioner = get(Krankenkasse);
        if (!Strings.isNullOrEmpty(practitioner)) {
            return new Reference().setDisplay(practitioner);
        }
        info(Krankenkasse + " empty for Record");
        return null;
    }
}
