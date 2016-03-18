/*
 * Copyright 2016 Dhatim.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dhatim.fastexcel;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Font definition. For details, check
 * <a href="https://msdn.microsoft.com/en-us/library/documentformat.openxml.spreadsheet.font(v=office.14).aspx">this
 * page</a>.
 */
class Font {

    /**
     * Default font.
     */
    protected static final Font DEFAULT = build(false, false, "000000");

    /**
     * Bold flag.
     */
    private final boolean bold;
    /**
     * Italic flag.
     */
    private final boolean italic;
    /**
     * Font name.
     */
    private final String name;
    /**
     * Font family.
     */
    private final int family;
    /**
     * Font size.
     */
    private final BigDecimal size;
    /**
     * RGB font color.
     */
    private final String rgbColor;

    /**
     * Constructor.
     *
     * @param bold Bold flag.
     * @param italic Italic flag.
     * @param name Font name.
     * @param family Font family numbering.
     * @param size Font size, in points.
     * @param rgbColor RGB font color.
     */
    Font(boolean bold, boolean italic, String name, int family, BigDecimal size, String rgbColor) {
        this.bold = bold;
        this.italic = italic;
        this.name = name;
        this.family = family;
        this.size = size;
        this.rgbColor = rgbColor;
    }

    /**
     * Helper to create a new "Calibri" font, family 2.
     *
     * @param bold Bold flag.
     * @param italic Italic flag.
     * @param rgbColor RGB font color.
     * @return New font object.
     */
    static Font build(boolean bold, boolean italic, String rgbColor) {
        return new Font(bold, italic, "Calibri", 2, BigDecimal.valueOf(11.0), rgbColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, name, family, size, rgbColor);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Font other = (Font) obj;
        if (this.bold != other.bold) {
            return false;
        }
        if (this.italic != other.italic) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.family != other.family) {
            return false;
        }
        if (this.size.compareTo(other.size) != 0) {
            return false;
        }
        if (!Objects.equals(this.rgbColor, other.rgbColor)) {
            return false;
        }
        return true;
    }

    /**
     * Write this font as an XML element.
     *
     * @param w Output writer
     * @throws IOException If an I/O error occurs.
     */
    void write(Writer w) throws IOException {
        w.append("<font>").append(bold ? "<b/>" : "").append(italic ? "<i/>" : "").append("<sz val=\"").append(size.toString()).append("\"/>");
        if (rgbColor != null) {
            w.append("<color rgb=\"").append(rgbColor).append("\"/>");
        }
        w.append("<name val=\"").appendEscaped(name).append("\"/></font>");
    }
}
