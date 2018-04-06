package project.model.units;

import java.util.ArrayList;

/**
 * Representation of a measure in MusicXML.
 */
public class Measure {
    private int number;
    private String attributes;
    private String XML;
    private boolean empty;

    /**
     * Create a measure.
     * @param number The measure number
     */
    public Measure(int number) {
        this.number = number;
        attributes = "";
        XML = "";
        empty = true;
    }

    /**
     * Add attributes to the measure.
     * @param fifths The parameter related to the key signature
     * @param beats The number of beats in the measure
     * @param beatType The beat type (the bottom number of the metre)
     * @param clefs The clefs
     * @param staves The number of staves
     */
    public void addAttributes(int fifths, int beats, int beatType, ArrayList<String> clefs, int staves) {
        attributes += "\r\n\t\t\t" + "<attributes>";
        attributes += "\r\n\t\t\t\t" + "<divisions>" + 4 + "</divisions>";

        attributes += "\r\n\t\t\t\t" + "<key>";
        attributes += "\r\n\t\t\t\t\t" + "<fifths>" + fifths + "</fifths>";
        attributes += "\r\n\t\t\t\t\t" + "<mode>" + "major" + "</mode>";
        attributes += "\r\n\t\t\t\t" + "</key>";

        attributes += "\r\n\t\t\t\t" + "<time>";
        attributes += "\r\n\t\t\t\t\t" + "<beats>" + beats + "</beats>";
        attributes += "\r\n\t\t\t\t\t" + "<beat-type>" + beatType + "</beat-type>";
        attributes += "\r\n\t\t\t\t" + "</time>";

        attributes += "\r\n\t\t\t\t" + "<staves>" + staves + "</staves>";

        for(int i=0; i<clefs.size(); i++) {
            char sign = '0';
            int line = 0;
            switch(clefs.get(i)) {
                case "treble":
                    sign = 'G';
                    line = 2;
                    break;
                case "bass":
                    sign = 'F';
                    line = 4;
                    break;
            }
            attributes += "\r\n\t\t\t\t" + "<clef number=\"" + (i+1) + "\">";
            attributes += "\r\n\t\t\t\t\t" + "<sign>" + sign + "</sign>";
            attributes += "\r\n\t\t\t\t\t" + "<line>" + line + "</line>";
            attributes += "\r\n\t\t\t\t" + "</clef>";
        }

        attributes += "\r\n\t\t\t" + "</attributes>";
    }

    /**
     * Add a note to the measure.
     * @param note The note to be added
     */
    public void addNote(Note note) {
        empty = false;
        XML += "\r\n\t\t\t" + "<note>";

        if(note.isChord()) {
            XML += "\r\n\t\t\t\t" + "<chord/>";
        }

        if(!note.isRest()) {
            XML += "\r\n\t\t\t\t" + "<pitch>";
            XML += "\r\n\t\t\t\t\t" + "<step>" + note.getStep() + "</step>";
            if(note.getAlter() != 0) {
                XML += "\r\n\t\t\t\t\t" + "<alter>" + note.getAlter() + "</alter>";
            }
            XML += "\r\n\t\t\t\t\t" + "<octave>" + note.getOctave() + "</octave>";
            XML += "\r\n\t\t\t\t" + "</pitch>";
        } else {
            XML += "\r\n\t\t\t\t" + "<rest/>";
        }

        int duration = 0;
        switch(note.getType()) {
            case "16th":
                duration = 1;
                break;
            case "eighth":
                duration = 2;
                break;
            case "quarter":
                duration = 4;
                break;
            case "half":
                duration = 8;
                break;
            case "whole":
                duration = 16;
                break;
        }
        if(note.isDotted()) {
            duration *= 1.5;
        }
        XML += "\r\n\t\t\t\t" + "<duration>" + duration + "</duration>";

        if(note.getTieType() != null) {
            XML += "\r\n\t\t\t\t" + "<tie type=\"" + note.getTieType() + "\"/>";
        }

        XML += "\r\n\t\t\t\t" + "<voice>" + note.getVoice() + "</voice>";

        XML += "\r\n\t\t\t\t" + "<type>" + note.getType() + "</type>";

        if(note.isDotted()) {
            XML += "\r\n\t\t\t\t" + "<dot/>";
        }

        if(note.getAccidental() != null) {
            XML += "\r\n\t\t\t\t" + "<accidental>" + note.getAccidental() + "</accidental>";
        }

        XML += "\r\n\t\t\t\t" + "<staff>" + note.getStaff() + "</staff>";

        if(note.getBeams() != null) {
            if(note.getBeams().size() > 0) {
                XML += "\r\n\t\t\t\t" + "<beam number=\"1\">" + note.getBeams().get(0) + "</beam>";
            }
            if(note.getBeams().size() > 1) {
                XML += "\r\n\t\t\t\t" + "<beam number=\"2\">" + note.getBeams().get(1) + "</beam>";
            }
        }

        if(note.getTieType() != null) {
            XML += "\r\n\t\t\t\t" + "<notations>";
            XML += "\r\n\t\t\t\t\t" + "<tied type=\"" + note.getTieType() + "\"/>";
            XML += "\r\n\t\t\t\t" + "</notations>";
        }

        XML += "\r\n\t\t\t" + "</note>";
    }

    /**
     * Make this measure the first one in the system.
     */
    public void setNewSystem() {
        XML += "\r\n\t\t\t" + "<print new-system=\"yes\"/>";
    }

    /**
     * Make this measure the last one in the music piece.
     */
    public void setLast() {
        XML += "\r\n\t\t\t" + "<barline location=\"right\">";
        XML += "\r\n\t\t\t\t" + "<bar-style>light-heavy</bar-style>";
        XML += "\r\n\t\t\t" + "</barline>";
    }

    /**
     * Go back to add notes on the next stave.
     */
    public void addBackup() {
        XML += "\r\n\t\t\t" + "<backup>";
        XML += "\r\n\t\t\t\t" + "<duration>" + 16 + "</duration>";
        XML += "\r\n\t\t\t" + "</backup>";
    }

    /**
     * Get the MusicXML representation.
     * @return The MusicXML representation
     */
    public String getXML() {
        return "\r\n\r\n\t\t" + "<measure number=\"" + number + "\">" +
                attributes + XML +
                "\r\n\t\t" + "</measure>";
    }

    /**
     * Check if the measure is empty
     * @return True if the measure is empty
     */
    public boolean isEmpty() {
        return empty;
    }
}
