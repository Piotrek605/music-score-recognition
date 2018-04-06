package project.model.units;

import java.util.ArrayList;

/**
 * Represents a note.
 */
public class Note {
    private char step;
    private int alter;
    private int octave;
    private String tieType;
    private int voice;
    private String type;
    private boolean dotted;
    private int staff;
    private ArrayList<String> beams;
    private String accidental;
    private boolean chord;
    private boolean isRest;

    /**
     * Create a new note.
     * @param step The pitch
     * @param alter The pitch alteration
     * @param octave The octave of the pitch
     * @param tieType The tie
     * @param voice The voice number
     * @param type The duration
     * @param dotted True if dotted
     * @param staff The staff number
     * @param beams The beams
     * @param accidental The accidental
     * @param chord True if chord
     */
    public Note(char step, int alter, int octave, String tieType, int voice, String type,
                boolean dotted, int staff, ArrayList<String> beams, String accidental,
                boolean chord) {
        this.step = step;
        this.alter = alter;
        this.octave = octave;
        this.tieType = tieType;
        this.voice = voice;
        this.type = type;
        this.dotted = dotted;
        this.staff = staff;
        this.beams = beams;
        this.accidental = accidental;
        this.chord = chord;
        isRest = false;
    }

    /**
     * Create a new rest
     * @param voice The voice number
     * @param type The duration
     * @param dotted True if dotted
     * @param staff The staff number
     */
    public Note(int voice, String type, boolean dotted, int staff) {
        tieType = null;
        this.voice = voice;
        this.type = type;
        this.dotted = dotted;
        this.staff = staff;
        beams = null;
        accidental = null;
        chord = false;
        isRest = true;
    }

    /**
     * Get the pitch
     * @return The pitch
     */
    public char getStep() {
        return step;
    }

    /**
     * Get the pitch alteration
     * @return The pitch alteration
     */
    public int getAlter() {
        return alter;
    }

    /**
     * Get the octave of the pitch
     * @return The octave
     */
    public int getOctave() {
        return octave;
    }

    /**
     * Get the tie type
     * @return The tie type
     */
    public String getTieType() {
        return tieType;
    }

    /**
     * Get the voice number
     * @return The voice number
     */
    public int getVoice() {
        return voice;
    }

    /**
     * Get the duration
     * @return The duration
     */
    public String getType() {
        return type;
    }

    /**
     * Check if the note is dotted
     * @return True if dotted
     */
    public boolean isDotted() {
        return dotted;
    }

    /**
     * Get the staff number
     * @return The staff number
     */
    public int getStaff() {
        return staff;
    }

    /**
     * Get the beams
     * @return The beams
     */
    public ArrayList<String> getBeams() {
        return beams;
    }

    /**
     * Get the accidental
     * @return The accidental
     */
    public String getAccidental() {
        return accidental;
    }

    /**
     * Check if part of a chord
     * @return True if part of a chord
     */
    public boolean isChord() {
        return chord;
    }

    /**
     * Check if a rest
     * @return True if a rest
     */
    public boolean isRest() {
        return isRest;
    }
}
