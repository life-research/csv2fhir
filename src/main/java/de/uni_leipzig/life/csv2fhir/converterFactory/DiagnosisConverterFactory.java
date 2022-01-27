package de.uni_leipzig.life.csv2fhir.converterFactory;

import org.apache.commons.csv.CSVRecord;

import de.uni_leipzig.imise.FHIRValidator;
import de.uni_leipzig.life.csv2fhir.Converter;
import de.uni_leipzig.life.csv2fhir.ConverterFactory;
import de.uni_leipzig.life.csv2fhir.ConverterResult;
import de.uni_leipzig.life.csv2fhir.TableColumnIdentifier;
import de.uni_leipzig.life.csv2fhir.converter.DiagnosisConverter;

public class DiagnosisConverterFactory implements ConverterFactory {

    public static enum Diagnosis_Columns implements TableColumnIdentifier {
        Patient_ID {
            @Override
            public String toString() {
                return "Patient-ID";
            }
        },
        Bezeichner,
        ICD,
        Dokumentationsdatum,
        Typ
    }

    @Override
    public Converter create(CSVRecord record, ConverterResult result, FHIRValidator validator) throws Exception {
        return new DiagnosisConverter(record, result, validator);
    }

}