package de.uni_leipzig.life.csv2fhir.converterFactory;

import org.apache.commons.csv.CSVRecord;

import de.uni_leipzig.imise.FHIRValidator;
import de.uni_leipzig.life.csv2fhir.Converter;
import de.uni_leipzig.life.csv2fhir.ConverterFactory;
import de.uni_leipzig.life.csv2fhir.ConverterResult;
import de.uni_leipzig.life.csv2fhir.TableColumnIdentifier;
import de.uni_leipzig.life.csv2fhir.converter.ObservationVitalSignsConverter;

public class ObservationVitalSignsConverterFactory implements ConverterFactory {

    public static enum ObservationVitalSigns_Columns implements TableColumnIdentifier {
        Patient_ID {
            @Override
            public String toString() {
                return "Patient-ID";
            }
        },
        Bezeichner,
        LOINC,
        Wert,
        Einheit,
        Zeitstempel
    }

    @Override
    public Converter create(final CSVRecord record, ConverterResult result, FHIRValidator validator) throws Exception {
        return new ObservationVitalSignsConverter(record, result, validator);
    }

}