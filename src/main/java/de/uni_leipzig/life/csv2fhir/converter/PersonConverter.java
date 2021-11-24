package de.uni_leipzig.life.csv2fhir.converter;

import static de.uni_leipzig.life.csv2fhir.TableIdentifier.Person;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.NeededColumns.Anschrift;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.NeededColumns.Geburtsdatum;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.NeededColumns.Geschlecht;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.NeededColumns.Krankenkasse;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.NeededColumns.Nachname;
import static de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory.NeededColumns.Vorname;
import static org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.FEMALE;
import static org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE;
import static org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.OTHER;

import java.util.Collections;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Address.AddressType;
import org.hl7.fhir.r4.model.DateType;
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

import de.uni_leipzig.life.csv2fhir.Converter;
import de.uni_leipzig.life.csv2fhir.utils.DateUtil;

public class PersonConverter extends Converter {

    /**  */
    String PROFILE = "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Patient";
    // @see https://simplifier.net/MedizininformatikInitiative-ModulPerson/PatientIn

    /**
     * @param record
     * @throws Exception
     */
    public PersonConverter(CSVRecord record) throws Exception {
        super(record);
    }

    @Override
    public List<Resource> convert() throws Exception {
        Patient patient = new Patient();
        patient.setMeta(new Meta().addProfile(PROFILE));
        patient.setId(getPatientId());
        patient.setIdentifier(parseIdentifier());
        patient.addName(parseName());
        patient.setGender(parseSex());
        patient.setBirthDateElement(parseBirthDate());
        patient.addAddress(parseAddress());
        //        patient.addGeneralPractitioner(parseHealthProvider());
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
        String forename = record.get(Vorname);
        String surname = record.get(Nachname);

        if (forename == null) {
            forename = "Vorname-" + getPatientId();
            warning("Empty " + Vorname + " replaced by " + forename);
        }
        if (surname == null) {
            surname = "Nachname-" + getPatientId();
            warning("Empty " + Nachname + " replaced by " + surname);
        }

        HumanName humanName = new HumanName().setFamily(surname).setUse(NameUse.OFFICIAL);
        for (String name : forename.split(" ")) {
            humanName.addGiven(name);
        }
        return humanName;
    }

    /**
     * @return
     * @throws Exception
     */
    private AdministrativeGender parseSex() throws Exception {
        String sex = record.get(Geschlecht);
        if (sex != null) {
            if (sex.length() != 0) {
                switch (sex) {
                case "m":
                case "männlich":
                    return MALE;
                case "w":
                case "weiblich":
                    return FEMALE;
                case "d":
                case "divers":
                    return OTHER;
                default:
                    throw new Exception("Error on " + Person + ": " + Geschlecht + " <" + sex + ">not parsable for Record: " + record
                            .getRecordNumber() + "! " + record.toString());
                }
            }
            warning("Geschlecht empty for Record");
            return null;
        }
        warning("Geschlecht not found");
        return null;
    }

    /**
     * @return
     * @throws Exception
     */
    private DateType parseBirthDate() throws Exception {
        String birthday = record.get(Geburtsdatum);
        if (birthday != null) {
            try {
                return DateUtil.parseDateType(birthday);
            } catch (Exception e) {
                error("Can not parse " + Geburtsdatum + " for Record");
                return null;
            }
        }
        error(Geburtsdatum + " empty for Record");
        return null;
    }

    /**
     * @return
     */
    private Address parseAddress() {
        String address = record.get(Anschrift);
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
        warning("On " + Person + ": " + Anschrift + " empty. Creating dummy address. " + record);
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

    // not used yet
    /**
     * @return
     * @throws Exception
     */
    private Reference parseHealthProvider() throws Exception {
        String practitioner = record.get(Krankenkasse);
        if (!Strings.isNullOrEmpty(practitioner)) {
            return new Reference().setDisplay(practitioner);
        }
        error(Krankenkasse + " empty for Record");
        return null;
    }
}
