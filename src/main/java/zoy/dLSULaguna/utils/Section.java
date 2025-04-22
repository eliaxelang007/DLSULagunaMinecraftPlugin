package zoy.dLSULaguna.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

enum Strand {
    Stem,
    // Extra strands for future proofing
    Abm,
    Humss,
    Adt,
    Spt;

    private static final Map<String, Strand> stringToStrand = new HashMap<>();

    static {
        for (final var strand : Strand.values()) {
            stringToStrand.put(strand.toString(), strand);
        }
    }

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }

    public static Optional<Strand> fromString(String strandString) {
        return Optional.ofNullable(stringToStrand.get(strandString.toUpperCase()));
    }
}

enum Grade {
    Eleven,
    // Extra grade 12 for future proofing
    Twelve;

    private static final Map<String, Grade> stringToGrade = new HashMap<>();

    @Override
    public String toString() {
        return switch (this) {
            case Eleven -> "11";
            case Twelve -> "12";
        };
    }

    static {
        for (final var grade : Grade.values()) {
            stringToGrade.put(grade.toString(), grade);
        }
    }

    public static Optional<Grade> fromString(String gradeString) {
        return Optional.ofNullable(stringToGrade.get(gradeString));
    }
}

enum Letter {
    A,
    B,
    C,
    D,
    E;

    private static final Map<String, Letter> stringToLetter = new HashMap<>();

    static {
        for (final var letter : Letter.values()) {
            stringToLetter.put(letter.toString(), letter);
        }
    }

    public static Optional<Letter> fromString(String letterString) {
        return Optional.ofNullable(stringToLetter.get(letterString));
    }
}

public record Section(Strand strand, Grade grade, Letter letter) {

    private final static Pattern sectionPattern = Pattern.compile("(\\w{3,5})(\\d{2})-(\\w)", Pattern.CASE_INSENSITIVE);

    public static Optional<Section> fromString(String sectionString) {
        final Optional<Section> empty = Optional.empty();
        final var matcher = sectionPattern.matcher(sectionString);

        if (!matcher.find()) {
            return empty;
        }

        final var maybeStrand = Strand.fromString(matcher.group(1));
        if (maybeStrand.isEmpty())
            return empty;

        final var maybeGrade = Grade.fromString(matcher.group(2));
        if (maybeGrade.isEmpty())
            return empty;

        final var maybeLetter = Letter.fromString(matcher.group(3));
        if (maybeLetter.isEmpty())
            return empty;

        return Optional.of(new Section(maybeStrand.get(), maybeGrade.get(), maybeLetter.get()));
    }

    public static String toStringOptional(Optional<Section> maybeSection) {
        return maybeSection.map((section) -> section.toString()).orElse("Unknown Section");
    }

    @Override
    public final String toString() {
        return "" + strand + grade + "-" + letter;
    }
}
