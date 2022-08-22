package de.uni_leipzig.life.csv2fhir;

import static de.uni_leipzig.life.csv2fhir.ConverterOptions.IntOption.PID_LAST_NUMBER_INCREASE_INITIAL_OFFSET;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import de.uni_leipzig.life.csv2fhir.utils.ResourceMapper;

/**
 * @author AXS (24.06.2022)
 */
public class ConverterOptions {

    /**
     * Default file extension for files with converter options. If an option
     * file could not be found with the given name it will
     */
    public static final String CONVERTER_OPTIONS_FILE_EXTENSION = ".config";

    /** Map with all default options for the converting process */
    private final ResourceMapper options = ResourceMapper.of("Converter_Options.config");

    /** Cache for the boolean values */
    private final Map<BooleanOption, Boolean> booleanValues = new HashMap<>();

    /** Cache for the int values */
    private final Map<IntOption, Integer> intValues = new HashMap<>();

    /** Cache for the string values */
    private final Map<StringOption, String> stringValues = new HashMap<>();

    /**
     * @param optionsAbsoluteFileName options file to load
     */
    public ConverterOptions(String optionsAbsoluteFileName) {
        putValues(optionsAbsoluteFileName);
    }

    /**
     * @param optionsAbsoluteFileName options file to load
     */
    private void putValues(String optionsAbsoluteFileName) {
        //add or overwrite the defaults with the project specific option values
        if (!options.load(optionsAbsoluteFileName)) {
            options.load(optionsAbsoluteFileName + CONVERTER_OPTIONS_FILE_EXTENSION);
        }
    }

    /**
     * @param booleanOption
     * @return
     */
    public boolean is(BooleanOption booleanOption) {
        Boolean value = booleanValues.get(booleanOption);
        if (value == null) {
            Object mapValueContent = options.get(booleanOption.toString());
            if (mapValueContent == null) {
                value = booleanOption.getDefault();
            } else {
                String v = mapValueContent.toString().trim().toLowerCase();
                value = BooleanOption.trueValues.contains(v);
            }
            booleanValues.put(booleanOption, value);
        }
        return value;
    }

    /**
     * @param intOption
     * @return the value of this option
     * @throws NumberFormatException if the found string in the properties
     *             cannot be parsed as an integer.
     */
    public int getValue(IntOption intOption) {
        Integer value = intValues.get(intOption);
        if (value == null) {
            Object mapValueContent = options.get(intOption.toString());
            if (mapValueContent == null) {
                value = intOption.getDefault();
            } else {
                value = Integer.valueOf(mapValueContent.toString().trim());
            }
            intValues.put(intOption, value);
        }
        return value;
    }

    /**
     * @param stringOption
     * @return the value of this option
     * @throws NumberFormatException if the found string in the properties
     *             cannot be parsed as an integer.
     */
    public String getValue(StringOption stringOption) {
        String value = stringValues.get(stringOption);
        if (value == null) {
            Object mapValueContent = options.get(stringOption.toString());
            if (mapValueContent == null) {
                value = stringOption.getDefault();
            } else {
                value = String.valueOf(mapValueContent.toString());
            }
            stringValues.put(stringOption, value);
        }
        return value;
    }

    /**
     * Boolean Options
     */
    public enum BooleanOption {
        /**
         * Enable to set a the optional reference from diagnoses (conditions) to
         * encounters. </br>
         * If <code>true</code> then circle references in the data are possible,
         * if the encounters have a reference to all diagnoses (conditions).
         * Some FHIR-Servers don't accept such circle references. In this case
         * the corresponding option
         * {@link BooleanOption#SET_REFERENCE_FROM_ENCOUNTER_TO_DIAGNOSIS_CONDITION}
         * must be set to <code>false</code>.</br>
         * The Default is <code>false</code>.
         */
        SET_REFERENCE_FROM_DIAGNOSIS_CONDITION_TO_ENCOUNTER,
        /**
         * Enable to set the references from the encounters to the diagnoses
         * (conditions). </br>
         * If <code>true</code> then circle references in the data are possible,
         * if the diagnoses (conditions) have a reference to their encounter.
         * Some FHIR-Servers don't accept such circle references. In this case
         * the corresponding option
         * {@link BooleanOption#SET_REFERENCE_FROM_DIAGNOSIS_CONDITION_TO_ENCOUNTER}
         * must be set to <code>false</code>.</br>
         * The Default is <code>true</code>.
         */
        SET_REFERENCE_FROM_ENCOUNTER_TO_DIAGNOSIS_CONDITION,

        /**
         * Enable to set a the optional reference from procedures (conditions)
         * to encounters. </br>
         * If <code>true</code> then circle references in the data are possible,
         * if the encounters have a reference to all procedures (conditions).
         * Some FHIR-Servers don't accept such circle references. In this case
         * the corresponding option
         * {@link BooleanOption#SET_REFERENCE_FROM_ENCOUNTER_TO_PROCEDURE_CONDITION}
         * must be set to <code>false</code>.</br>
         * The Default is <code>false</code>.
         */
        SET_REFERENCE_FROM_PROCEDURE_CONDITION_TO_ENCOUNTER,
        /**
         * Enable to set the references from the encounters to the procedures
         * (conditions). </br>
         * If <code>true</code> then circle references in the data are possible,
         * if the procedures (conditions) have a reference to their encounters.
         * Some FHIR-Servers don't accept such circle references. In this case
         * the corresponding option
         * {@link BooleanOption#SET_REFERENCE_FROM_PROCEDURE_CONDITION_TO_ENCOUNTER}
         * must be set to <code>false</code>.</br>
         * The Default is <code>true</code>.
         */
        SET_REFERENCE_FROM_ENCOUNTER_TO_PROCEDURE_CONDITION,

        /**
         * If <code>true</code>, then Sub Encounters will have a diagnosis of
         * the Super Encounter attached instead of a Data Absent Reason. If the
         * Super Encounter has a main diagnosis (chief complaint), it is
         * preferred.</br>
         * If <code>false</code>, the non-existing diagnoses are supplemented by
         * an "unknown" Data Absent Reason.
         */
        ADD_MISSING_DIAGNOSES_FROM_SUPER_ENCOUNTER,
        /**
         * If <code>true</code>, then Sub Encounters will have the same class
         * coding like the Super Encounter attached instead of a Data Absent
         * Reason.</br>
         * If <code>false</code>, the non-existing class codings are
         * supplemented by an "unknown" Data Absent Reason.</br>
         * Every Encounter needs at least one class coding to be valid.
         */
        ADD_MISSING_CLASS_FROM_SUPER_ENCOUNTER;

        /**
         * Set of String values which can be interpreted as booleans with value
         * <code>true</code>.
         */
        private static final Set<String> trueValues = ImmutableSet.of("true", "t", "wahr", "w", "yes", "y", "ja", "j", "1");

        /** All BooleanOptions whose default value is <code>true</code>. */
        private static final Set<BooleanOption> DEFAULT_TRUE_PROERTIES = ImmutableSet.of(SET_REFERENCE_FROM_ENCOUNTER_TO_DIAGNOSIS_CONDITION, SET_REFERENCE_FROM_ENCOUNTER_TO_PROCEDURE_CONDITION);

        /**
         * @return Default-Wert dieser Property
         */
        private boolean getDefault() {
            return DEFAULT_TRUE_PROERTIES.contains(this);
        }

    }

    /**
     * Integer Options
     */
    public static enum IntOption {
        /**
         * Start index counter for the number that will be added on the first
         * element of this type. The only resource type that will not get such
         * an index counter is Person. If the value is missing in this map then
         * the default is 1.
         */
        START_ID_CONSENT,
        START_ID_DIAGNOSIS,
        START_ID_ENCOUNTER_LEVEL_2,
        START_ID_MEDICATION_ADMINISTRATION,
        START_ID_MEDICATION_STATEMENT,
        START_ID_OBSERVATION_LABORATORY,
        START_ID_OBSERVATION_VITAL_SIGNS,
        START_ID_PROCEDURE,

        /**
         * The last number in all patient IDs of an data set will be increased
         * by this value. At the beginning this number will be added to all
         * patient IDs.</br>
         * Default value is 0.
         */
        PID_LAST_NUMBER_INCREASE_INITIAL_OFFSET(0);

        /** Default value of the int option */
        private final int defaultValue;

        /**
         * Creates an int option with the default value 1
         */
        private IntOption() {
            this(1);
        }

        /**
         * Creates an int option with the passed value.
         *
         * @param defaultValue
         */
        private IntOption(int defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * @return the default value of this option
         */
        private int getDefault() {
            return defaultValue;
        }

    }

    /**
     * String options
     */
    public static enum StringOption {

        /**
         * This prefix will be added to all patient IDs.</br>
         * The default is an empty string.
         */
        PID_PREFIX,
        /**
         * This suffix will be added to all patient IDs.</br>
         * The default is an empty string.
         */
        PID_SUFFIX;

        private final String defaultValue;

        private String getDefault() {
            return defaultValue;
        }

        private StringOption() {
            this("");
        }

        private StringOption(String defaultValue) {
            this.defaultValue = defaultValue;
        }

    }

    /**
     * @param s
     * @return the substring positions of the last integer number in this string
     */
    private static Range<Integer> getLastNumberStringBounds(String s) {
        // this is probably still faster than using a RegExp
        int start = -1;
        int end = -1;
        for (int i = s.length() - 1; i > 0; i--) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                if (end == -1) {
                    end = i + 1;
                }
                start = i;
            } else if (end != -1) {
                break;
            }
        }
        return Range.closedOpen(start, end);
    }

    /**
     * @param pid
     * @return
     */
    public String getIncreasedLastPidNumber(String pid, int value) {
        Range<Integer> lastNumberStringBounds = getLastNumberStringBounds(pid);
        int start = lastNumberStringBounds.lowerEndpoint();
        int end = lastNumberStringBounds.upperEndpoint();
        String preNumberString = pid.substring(0, start);
        String numberSubString = pid.substring(start, end);
        String afterNumberString = pid.substring(end);
        int numberLength = numberSubString.length();
        Integer number = Integer.valueOf(numberSubString);
        number += value;
        numberSubString = number.toString();
        if (numberSubString.length() < numberLength) {
            numberSubString = StringUtils.leftPad(numberSubString, numberLength, "0");
        }
        return preNumberString + numberSubString + afterNumberString;
    }

    /**
     * @param pid
     * @return
     */
    public String getFullPID(String pid) {
        int value = getValue(PID_LAST_NUMBER_INCREASE_INITIAL_OFFSET);
        if (value > 0) {
            pid = getIncreasedLastPidNumber(pid, value);
        }
        pid = getValue(StringOption.PID_PREFIX) + pid + getValue(StringOption.PID_SUFFIX);
        return pid.replace('_', '-'); //AXS: (Some) FHIR Server will not accept IDs with an underscore!
    }

}
