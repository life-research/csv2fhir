package de.uni_leipzig.life.csv2fhir.converterFactory;

import org.apache.commons.csv.CSVRecord;

import de.uni_leipzig.imise.FHIRValidator;
import de.uni_leipzig.life.csv2fhir.Converter;
import de.uni_leipzig.life.csv2fhir.ConverterFactory;
import de.uni_leipzig.life.csv2fhir.ConverterResult;
import de.uni_leipzig.life.csv2fhir.TableColumnIdentifier;
import de.uni_leipzig.life.csv2fhir.converter.PersonConverter;

public class PersonConverterFactory implements ConverterFactory {

    public static enum Person_Columns implements TableColumnIdentifier {
        Patient_ID {
            @Override
            public String toString() {
                return "Patient-ID";
            }
        },
        Vorname,
        Nachname,
        Anschrift,
        Geburtsdatum,
        Geschlecht,
        Krankenkasse
    }

    @Override
    public Converter create(CSVRecord record, ConverterResult result, FHIRValidator validator) throws Exception {
        return new PersonConverter(record, result, validator);
    }

}
