package de.uni_leipzig.life.csv2fhir;

import static de.uni_leipzig.life.csv2fhir.PrintExceptionMessageHandler.printException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Resource;

import com.google.common.base.Strings;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.uni_leipzig.imise.utils.Alphabetical;
import de.uni_leipzig.imise.utils.Sys;
import de.uni_leipzig.life.csv2fhir.converterFactory.AbteilungsfallConverterFactory;
import de.uni_leipzig.life.csv2fhir.converterFactory.DiagnoseConverterFactory;
import de.uni_leipzig.life.csv2fhir.converterFactory.KlinischeDokumentationConverterFactory;
import de.uni_leipzig.life.csv2fhir.converterFactory.LaborbefundConverterFactory;
import de.uni_leipzig.life.csv2fhir.converterFactory.MedikationConverterFactory;
import de.uni_leipzig.life.csv2fhir.converterFactory.PersonConverterFactory;
import de.uni_leipzig.life.csv2fhir.converterFactory.ProzedurConverterFactory;
import de.uni_leipzig.life.csv2fhir.converterFactory.VersorgungsfallConverterFactory;

/**
 * @author fheuschkel (02.11.2020)
 */
public class Csv2Fhir {

    /**
     * @author AXS (07.11.2021)
     */
    public static enum OutputFileType {

        JSON {
            @Override
            public IParser getParser() {
                return fhirContext.newJsonParser();
            }
        },

        XML {
            @Override
            public IParser getParser() {
                return fhirContext.newXmlParser();
            }
        };

        public String getFileExtension() {
            return "." + name().toLowerCase();
        }

        /** The context to generate the parser */
        private static final FhirContext fhirContext = FhirContext.forR4();

        /**
         * @return the parser to write the bundles
         */
        public abstract IParser getParser();

    }

    /**  */
    private final File inputDirectory;

    /**  */
    private final File outputDirectory;

    /**  */
    private final String outputFileNameBase;

    /**  */
    private final Map<String, ConverterFactory> converterFactorys;

    /**  */
    private final CSVFormat csvFormat;

    /**
     * @param inputDirectory
     * @param outputFileNameBase
     */
    public Csv2Fhir(File inputDirectory, String outputFileNameBase) {
        this(inputDirectory, inputDirectory, outputFileNameBase);
    }

    /**
     * @param inputDirectory
     * @param outputFileNameBase
     */
    @SuppressWarnings("serial")
    public Csv2Fhir(File inputDirectory, File outputDirectory, String outputFileNameBase) {
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.outputFileNameBase = outputFileNameBase;
        converterFactorys = new HashMap<>() {
            {
                put("Person.csv", new PersonConverterFactory());
                put("Versorgungsfall.csv", new VersorgungsfallConverterFactory());
                put("Abteilungsfall.csv", new AbteilungsfallConverterFactory());
                put("Laborbefund.csv", new LaborbefundConverterFactory());
                put("Diagnose.csv", new DiagnoseConverterFactory());
                put("Prozedur.csv", new ProzedurConverterFactory());
                put("Medikation.csv", new MedikationConverterFactory());
                put("Klinische Dokumentation.csv", new KlinischeDokumentationConverterFactory());
            }
        };
        csvFormat = CSVFormat.DEFAULT.withNullString("").withIgnoreSurroundingSpaces().withTrim(true)
                .withAllowMissingColumnNames(true).withFirstRecordAsHeader();
    }

    /**
     * @param csvFileName
     * @param columnName
     * @param distinct
     * @param alphabetical
     * @return
     * @throws IOException
     */
    public Collection<String> getValues(String csvFileName, String columnName, boolean distinct, boolean alphabetical)
            throws IOException {
        Collection<String> values = distinct ? new HashSet<>() : new ArrayList<>();
        File file = new File(inputDirectory, csvFileName);

        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        try (CSVParser records = csvFormat.parse(new FileReader(file))) {
            for (CSVRecord record : records) {
                String pid = record.get(columnName);
                if (pid != null) {
                    values.add(pid.toUpperCase());
                    Sys.out1("found pid=" + pid);
                }
            }
            if (alphabetical) {
                if (distinct) {
                    values = new ArrayList<>(values);
                }
                Alphabetical.sort((List<String>) values);
            }
            return values;
        }
    }

    /**
     * @param convertFilesPerPatient
     * @throws Exception
     */
    public void convertFiles(OutputFileType outputFileType, boolean convertFilesPerPatient) throws Exception {
        if (convertFilesPerPatient) {
            convertFilesPerPatient(outputFileType);
        } else {
            convertFiles(outputFileType);
        }
    }

    /**
     * @param outputFileType
     * @throws Exception
     */
    public void convertFiles(OutputFileType outputFileType) throws Exception {
        convertFiles(outputFileType, null);
    }

    /**
     * @param outputFileType
     * @throws Exception
     */
    private void convertFilesPerPatient(OutputFileType outputFileType) throws Exception {
        Collection<String> pids = getValues("Person.csv", "Patient-ID", true, false);
        for (String pid : pids) {
            convertFiles(outputFileType, pid);
        }
    }

    /**
     * @param outputFileType
     * @param pid
     * @throws Exception
     */
    private void convertFiles(OutputFileType outputFileType, String pid) throws Exception {
        String filter = pid == null ? null : pid.toUpperCase();
        String[] files = inputDirectory.list();
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        if (files != null) {
            convertFiles(bundle, filter, files);
        }
        EncounterReferenceReplacer.convert(bundle);
        writeOutputFile(bundle, pid == null ? "" : "-" + pid, outputFileType);
    }

    /**
     * @param bundle
     * @param fileNameExtension
     * @param outputFileType
     * @throws IOException
     */
    private void writeOutputFile(Bundle bundle, String fileNameExtension, OutputFileType outputFileType)
            throws IOException {
        String fileName = outputFileNameBase + (Strings.isNullOrEmpty(fileNameExtension) ? "" : fileNameExtension)
                + outputFileType.getFileExtension();
        File outputFile = new File(outputDirectory, fileName);
        Sys.out1("writing file " + fileName);
        try (FileWriter fw = new FileWriter(outputFile)) {
            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                outputFileType.getParser().setPrettyPrint(true).encodeResourceToWriter(bundle, fileWriter);
            }
        }
    }

    /**
     * @param bundle
     * @param filterID
     * @param files
     * @throws Exception
     */
    private void convertFiles(Bundle bundle, String filterID, String[] files) throws Exception {
        for (String fileName : files) {
            ConverterFactory factory = converterFactorys.get(fileName);
            if (factory == null) {
                continue;
            }
            File file = new File(inputDirectory, fileName);
            if (!file.exists() || file.isDirectory()) {
                continue;
            }
            try (Reader in = new FileReader(file)) {
                CSVParser records = csvFormat.parse(in);
                Sys.out1("Start parsing File:" + fileName);
                Map<String, Integer> headerMap = records.getHeaderMap();
                String[] columnNames = factory.getNeededColumnNames();
                if (isColumnMissing(headerMap, columnNames)) {
                    records.close();
                    throw new Exception("Error - File: " + fileName + " not convertable!");
                }
                for (CSVRecord record : records) {
                    try {
                        if (!Strings.isNullOrEmpty(filterID)) {
                            String idColumnName = factory.getIdColumnName();
                            String p = record.get(idColumnName);
                            if (!p.toUpperCase().matches(filterID)) {
                                continue;
                            }
                        }
                        List<Resource> list = factory.create(record).convert();
                        if (list != null) {
                            for (Resource resource : list) {
                                if (resource != null) {
                                    bundle.addEntry().setResource(resource).setRequest(getRequestComponent(resource));
                                }
                            }
                        }
                    } catch (Exception e) {
                        printException(e);
                    }
                }
                records.close();
            }
        }
    }

    /**
     * @param map
     * @return
     */
    private static Set<String> getTrimmedKeys(Map<String, Integer> map) {
        Set<String> keySet = map.keySet();
        Stream<String> keySetStream = keySet.stream();
        keySetStream = keySetStream.map(String::trim);
        keySet = keySetStream.collect(Collectors.toSet());
        return keySet;
    }

    /**
     * @param map
     * @param neededColls
     * @return
     */
    private static boolean isColumnMissing(Map<String, Integer> map, String[] neededColls) {
        Set<String> columns = getTrimmedKeys(map);
        List<String> neededColumns = Arrays.asList(neededColls);
        if (!columns.containsAll(neededColumns)) {//Error message
            for (String s : neededColls) {
                if (!columns.contains(s)) {
                    Sys.out1("Column " + s + " missing");
                }
            }
            //            System.out.println();
            //            Sys.out1(columns);
            return true;
        }
        return false;
    }

    /**
     * @param resource
     * @return
     */
    private static Bundle.BundleEntryRequestComponent getRequestComponent(Resource resource) {
        String resourceID = resource.getId();
        Bundle.HTTPVerb method = resourceID == null ? Bundle.HTTPVerb.POST : Bundle.HTTPVerb.PUT;

        String url = resource.getResourceType().toString();
        if (resourceID != null) {
            url += "/" + resourceID;
        }
        BundleEntryRequestComponent requestComponent = new Bundle.BundleEntryRequestComponent();
        requestComponent = requestComponent.setMethod(method);
        requestComponent = requestComponent.setUrl(url);
        return requestComponent;
    }
}
