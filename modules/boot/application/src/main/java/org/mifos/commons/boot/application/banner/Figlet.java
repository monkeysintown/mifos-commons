///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.application.banner;

import com.google.errorprone.annotations.Var;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/// A small, self-contained FIGlet renderer that turns a line of text into ASCII-art using a FIGlet
/// `.flf` font.
///
/// This is a clean-room implementation written against the public _FIGfont Version 2 FIGfont and
/// FIGdriver Standard_ (the `.flf` format specification). It exists to give this library a
/// permissively-licensed banner renderer, and only implements the subset needed here: parsing a font and
/// rendering a single line of text with horizontal fitting/smushing. Vertical smushing, right-to-left
/// layout and code-tagged characters beyond the required set are intentionally not implemented.
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"java:S3776", "java:S2677", "java:S6218", "java:S135"})
final class Figlet {

    // Horizontal layout / smushing rule bits, as defined by the FIGfont standard.
    private static final int SM_EQUAL = 1;
    private static final int SM_LOWLINE = 2;
    private static final int SM_HIERARCHY = 4;
    private static final int SM_PAIR = 8;
    private static final int SM_BIGX = 16;
    private static final int SM_HARDBLANK = 32;
    private static final int SM_KERN = 64;
    private static final int SM_SMUSH = 128;

    /// Character codes stored, in order, immediately after the 95 required ASCII characters (32..126).
    private static final int[] EXTRA_CODES = {196, 214, 220, 228, 246, 252, 223};

    private final char hardBlank;
    private final int height;
    private final int smushMode;
    private final Map<Integer, String[]> glyphs;

    /// Parses a `.flf` font stream and renders `text` as a single-line banner.
    ///
    /// @param fontStream the `.flf` font resource (not closed by this method)
    /// @param text the text to render
    /// @return the rendered banner: `height` rows, each terminated by `'\n'`
    /// @throws IOException if the stream cannot be read or is not a valid `.flf` font
    static String convertOneLine(InputStream fontStream, String text) throws IOException {
        return parse(fontStream).render(text);
    }

    /// Renders `text` and also reports, for every output cell, which input character's ink occupies it. Used to
    /// colour the banner per cell so colours follow each character's actual ink.
    ///
    /// @param fontStream the `.flf` font resource (not closed by this method)
    /// @param text the text to render
    /// @return the rendered banner together with the per-cell ink ownership
    /// @throws IOException if the stream cannot be read or is not a valid `.flf` font
    static RenderResult renderTracked(InputStream fontStream, String text) throws IOException {
        return parse(fontStream).renderTracked(text);
    }

    /// A rendered banner together with per-cell ink ownership: `owner[row][col]` is the index of the input
    /// character whose ink occupies that cell, or `-1` for a blank cell.
    ///
    /// @param banner the rendered banner rows, each terminated by `'\n'`
    /// @param owner per-cell index of the owning input character, or `-1` for a blank cell
    record RenderResult(String banner, int[][] owner) {}

    /// Parses a `.flf` font stream and returns a [Figlet] object that represents the font.
    private static Figlet parse(InputStream fontStream) throws IOException {
        // Read with ISO-8859-1 so every byte maps to a char and parsing never fails on non-UTF-8 comment
        // bytes; the required ASCII glyphs decode identically either way.
        var reader = new BufferedReader(new InputStreamReader(fontStream, StandardCharsets.ISO_8859_1));

        String header = reader.readLine();
        if (header == null || !header.startsWith("flf2a") || header.length() < 6) {
            throw new IOException("Not a FIGlet .flf font (bad signature)");
        }
        var hardBlank = header.charAt(5);
        String[] params = header.substring(6).trim().split("\\s+");
        if (params.length < 5) {
            throw new IOException("Malformed .flf header: " + header);
        }
        var height = Integer.parseInt(params[0]);
        var oldLayout = Integer.parseInt(params[3]);
        var commentLines = Integer.parseInt(params[4]);
        // Full_Layout (index 6) is optional; when present it takes precedence over Old_Layout.
        Integer fullLayout = params.length > 6 ? Integer.parseInt(params[6]) : null;
        var smushMode = smushMode(oldLayout, fullLayout);

        for (var i = 0; i < commentLines; i++) {
            if (reader.readLine() == null) {
                throw new IOException("Truncated .flf header comments");
            }
        }

        var glyphs = new HashMap<Integer, String[]>();
        // Required characters: ASCII 32..126, then the seven additional Deutsch characters.
        for (var code = 32; code <= 126; code++) {
            String[] glyph = readGlyph(reader, height);
            if (glyph.length == 0) {
                throw new IOException("Truncated .flf: missing glyph for code " + code);
            }
            glyphs.put(code, glyph);
        }
        for (int code : EXTRA_CODES) {
            String[] glyph = readGlyph(reader, height);
            if (glyph.length == 0) {
                break; // some fonts omit the Deutsch characters; that is tolerated
            }
            glyphs.put(code, glyph);
        }
        return new Figlet(hardBlank, height, smushMode, glyphs);
    }

    /// Derives the effective horizontal smush mode from the Old_Layout and optional Full_Layout header fields.
    private static int smushMode(int oldLayout, Integer fullLayout) {
        if (fullLayout != null) {
            return fullLayout;
        }
        if (oldLayout < 0) {
            return 0; // full width: neither kerning nor smushing
        }
        if (oldLayout == 0) {
            return SM_KERN;
        }
        return (oldLayout & 63) | SM_SMUSH;
    }

    /// Reads one glyph (`height` sub-lines), stripping the trailing end-mark character(s) from each. Returns an
    /// empty array if the stream ends before a full glyph can be read.
    private static String[] readGlyph(BufferedReader reader, int height) throws IOException {
        String[] rows = new String[height];
        var width = 0;
        for (var i = 0; i < height; i++) {
            String line = reader.readLine();
            if (line == null) {
                return new String[0]; // no more glyph data in the stream
            }
            // Drop any trailing carriage return, then strip all trailing end-mark characters. The end-mark
            // is whatever the final character of the sub-line is (typically '@'); the last sub-line carries
            // two of them.
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (!line.isEmpty()) {
                var endMark = line.charAt(line.length() - 1);
                var end = line.length();
                while (end > 0 && line.charAt(end - 1) == endMark) {
                    end--;
                }
                line = line.substring(0, end);
            }
            rows[i] = line;
            width = Math.max(width, line.length());
        }
        // Pad every sub-line to the glyph's width so all rows are rectangular.
        for (var i = 0; i < height; i++) {
            if (rows[i].length() < width) {
                rows[i] = rows[i] + " ".repeat(width - rows[i].length());
            }
        }
        return rows;
    }

    /// Renders the input text using the FIGlet font represented by the current instance. The text is transformed into a
    /// visual representation based on the font glyphs and smushing configuration.
    private String render(String text) {
        return renderTracked(text).banner();
    }

    private RenderResult renderTracked(String text) {
        StringBuilder[] out = new StringBuilder[height];
        // owner[r][c] is the index of the input character whose visible ink occupies cell (r, c), or -1. It
        // lets the banner be coloured per cell, which is what slanted/overlapping fonts need: a colour change
        // follows each character's actual ink rather than a single vertical boundary.
        int[][] owner = new int[height][0];
        for (@Var var i = 0; i < height; i++) {
            out[i] = new StringBuilder();
        }
        var outLen = 0;
        var prevWidth = 0; // width of the previously placed glyph, for the narrow-character smush guard

        for (var i = 0; i < text.length(); i++) {
            String[] glyph = glyphs.get((int) text.charAt(i));
            if (glyph == null) {
                continue; // characters with no glyph in this font are skipped, as FIGlet drivers do
            }
            var charWidth = glyph[0].length();
            if (charWidth == 0) {
                continue; // empty glyph contributes nothing
            }
            var smush = smushAmount(out, outLen, glyph, charWidth, prevWidth);
            // The glyph is overlaid onto the accumulated output starting at this column. It is negative for
            // the first glyph, which slides it against the left margin and clips its leading blank columns.
            var offset = outLen - smush;
            var newLen = Math.max(outLen, offset + charWidth);

            for (var r = 0; r < height; r++) {
                StringBuilder row = out[r];
                while (row.length() < newLen) {
                    row.append(' ');
                }
                if (owner[r].length < newLen) {
                    var from = owner[r].length;
                    owner[r] = java.util.Arrays.copyOf(owner[r], newLen);
                    java.util.Arrays.fill(owner[r], from, newLen, -1);
                }
                String gr = glyph[r];
                for (var k = 0; k < charWidth; k++) {
                    var x = offset + k;
                    if (x < 0) {
                        continue; // clipped off the left edge
                    }
                    var glyphChar = gr.charAt(k);
                    var merged = merge(row.charAt(x), glyphChar);
                    row.setCharAt(x, merged != 0 ? merged : row.charAt(x));
                    // This character owns the cell where its glyph contributes visible ink; a later,
                    // overlapping character takes ownership of any cell it smushes into.
                    if (glyphChar != ' ' && glyphChar != hardBlank) {
                        owner[r][x] = i;
                    }
                }
            }
            outLen = newLen;
            prevWidth = charWidth;
        }

        var result = new StringBuilder();
        for (@Var var r = 0; r < height; r++) {
            String row = out[r].toString();
            if (hardBlank != ' ') {
                row = row.replace(hardBlank, ' ');
            }
            result.append(row).append('\n');
        }
        return new RenderResult(result.toString(), owner);
    }

    /// Returns how many columns the incoming glyph may overlap the accumulated output (per the standard).
    private int smushAmount(StringBuilder[] out, int outLen, String[] glyph, int charWidth, int prevWidth) {
        if ((smushMode & (SM_SMUSH | SM_KERN)) == 0) {
            return 0; // full width
        }
        // Smushing (overlapping two visible characters into one column) is only attempted when both the
        // previous and current characters are at least two columns wide; otherwise only fitting applies.
        var allowSmush = prevWidth >= 2 && charWidth >= 2;
        var maxSmush = charWidth;
        for (var r = 0; r < height; r++) {
            StringBuilder row = out[r];
            var trailing = 0; // blank columns at the right of the accumulated row
            var idx = outLen - 1;
            while (idx >= 0 && row.charAt(idx) == ' ') {
                trailing++;
                idx--;
            }
            char left = idx >= 0 ? row.charAt(idx) : 0;

            String gr = glyph[r];
            var leading = 0; // blank columns at the left of the incoming glyph row
            while (leading < charWidth && gr.charAt(leading) == ' ') {
                leading++;
            }
            char right = leading < charWidth ? gr.charAt(leading) : 0;

            var amt = trailing + leading;
            // If both touching characters are visible and can be smushed into one, one more column overlaps.
            if (allowSmush && left != 0 && right != 0 && merge(left, right) != 0) {
                amt++;
            }
            if (amt < maxSmush) {
                maxSmush = amt;
            }
        }
        return Math.max(0, maxSmush);
    }

    /// Merges two overlapping sub-characters, returning the resulting character, or `0` if the two cannot be
    /// smushed together.
    private char merge(char left, char right) {
        if (left == ' ') {
            return right;
        }
        if (right == ' ') {
            return left;
        }
        if ((smushMode & SM_SMUSH) == 0) {
            return 0; // kerning only: visible characters must not overlap
        }
        if ((smushMode & 63) == 0) {
            // Universal smushing: a hardblank yields to a visible character, otherwise the later
            // (right-hand) character wins.
            if (left == hardBlank) {
                return right;
            }
            if (right == hardBlank) {
                return left;
            }
            return right;
        }
        if (left == hardBlank || right == hardBlank) {
            // Hardblanks are asymmetric under controlled smushing: a hardblank already in the output (on
            // the left) yields to the incoming character, but a hardblank at the start of the incoming
            // glyph (on the right) is protected and blocks smushing.
            if ((smushMode & SM_HARDBLANK) != 0 && left == hardBlank && right == hardBlank) {
                return hardBlank; // rule 6: two hardblanks
            }
            if ((smushMode & SM_HARDBLANK) != 0 && left == hardBlank) {
                return right;
            }
            return 0;
        }
        if ((smushMode & SM_EQUAL) != 0 && left == right) {
            return left;
        }
        if ((smushMode & SM_LOWLINE) != 0) {
            var replacers = "|/\\[]{}()<>";
            if (left == '_' && replacers.indexOf(right) >= 0) {
                return right;
            }
            if (right == '_' && replacers.indexOf(left) >= 0) {
                return left;
            }
        }
        if ((smushMode & SM_HIERARCHY) != 0) {
            var l = hierarchyClass(left);
            var r = hierarchyClass(right);
            if (l > 0 && r > 0 && l != r) {
                return l > r ? left : right;
            }
        }
        if ((smushMode & SM_PAIR) != 0) {
            if ((left == '[' && right == ']') || (left == ']' && right == '[')) {
                return '|';
            }
            if ((left == '{' && right == '}') || (left == '}' && right == '{')) {
                return '|';
            }
            if ((left == '(' && right == ')') || (left == ')' && right == '(')) {
                return '|';
            }
        }
        if ((smushMode & SM_BIGX) != 0) {
            if (left == '/' && right == '\\') {
                return '|';
            }
            if (left == '\\' && right == '/') {
                return 'Y';
            }
            if (left == '>' && right == '<') {
                return 'X';
            }
        }
        return 0;
    }

    /// The FIGfont hierarchy class (1..6) of a smushing character, or 0 if it is not part of the hierarchy.
    private static int hierarchyClass(char c) {
        return switch (c) {
            case '|' -> 1;
            case '/', '\\' -> 2;
            case '[', ']' -> 3;
            case '{', '}' -> 4;
            case '(', ')' -> 5;
            case '<', '>' -> 6;
            default -> 0;
        };
    }
}
