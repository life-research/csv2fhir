package de.uni_leipzig.life.csv2fhir.converter;

import static de.uni_leipzig.life.csv2fhir.ConverterOptions.IntOption.START_ID_DOCUMENT_REFERENCE;
import static de.uni_leipzig.life.csv2fhir.TableIdentifier.DocumentReference;
import static de.uni_leipzig.life.csv2fhir.converter.DocumentReferenceConverter.DocumentReference_Columns.URI;
import static java.util.Collections.singletonList;
import static org.apache.logging.log4j.util.Strings.isBlank;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.Resource;

import de.uni_leipzig.imise.validate.FHIRValidator;
import de.uni_leipzig.life.csv2fhir.Converter;
import de.uni_leipzig.life.csv2fhir.ConverterOptions;
import de.uni_leipzig.life.csv2fhir.ConverterResult;
import de.uni_leipzig.life.csv2fhir.TableColumnIdentifier;


public class DocumentReferenceConverter extends Converter {

    /**
     *
     */
    public static enum DocumentReference_Columns implements TableColumnIdentifier {
        URI,
    }
    
    /**  */
//    String PROFILE = "https://www.medizininformatik-initiative.de/fhir/core/modul-prozedur/StructureDefinition/Procedure";
    // https://simplifier.net/medizininformatikinitiative-modulprozeduren/prozedur

    /**
     * @param record
     * @param result
     * @param validator
     * @param options
     * @throws Exception
     */
    public DocumentReferenceConverter(CSVRecord record, ConverterResult result, FHIRValidator validator, ConverterOptions options) throws Exception {
        super(record, result, validator, options);
    }

    @Override
    protected List<Resource> convertInternal() throws Exception {
        DocumentReference documentReference = new DocumentReference();
        int nextId = result.getNextId(DocumentReference, DocumentReference.class, START_ID_DOCUMENT_REFERENCE);
        String encounterId = getEncounterId();
        String id = (isBlank(encounterId) ? getPatientId() : encounterId) + "-D-" + nextId;
        documentReference.setId(id);
        //documentReference.setMeta(new Meta().addProfile(PROFILE));
        documentReference.setSubject(getPatientReference());

        DocumentReferenceContextComponent c = new DocumentReferenceContextComponent();
        c.setEncounter(Collections.singletonList(getEncounterReference()));             
        documentReference.setContext(c);
       
		Attachment a = createAttachment(Paths.get(get(URI)),true);
        documentReference.setContent(Collections.singletonList(new DocumentReferenceContentComponent(a)));

		return singletonList(documentReference);
    }
	/**
	 * create Attachment from file
	 * @param file
	 * @param embed if true embed file as binary else just add URL 
	 * @return new FHIR Attachment
	 * @throws IOException
	 */
	public static Attachment createAttachment(Path file, boolean embed) throws IOException {
		Attachment att = new Attachment();

		if (embed) {
			// Read unlimited
			byte[] bytes = Files.readAllBytes(file);		
			att.setData(bytes);
			// optional
			att.setSize(bytes.length);
		} else {
			att.setUrl(file.toUri().toURL().toExternalForm());
			att.setSize((int) Files.size(file));
		}
		att.setContentType(getContentType(file.toFile()));
		// optional
		att.setTitle(file.getFileName().toString());
		return att;		
	}
	public static String getContentType(File file) {
		return URLConnection.guessContentTypeFromName(file.getName());
	}

 }
