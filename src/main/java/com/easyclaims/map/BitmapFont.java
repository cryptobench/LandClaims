package com.easyclaims.map;

/**
 * Bitmap fonts for rendering text on map tiles.
 * Includes standard 5x7 font and micro 3x5 font for compact rendering.
 * Each character is stored as an array of rows, where each row is a bitmask.
 */
public class BitmapFont {

    // Standard font: 5x7 pixels
    public static final int CHAR_WIDTH = 5;
    public static final int CHAR_HEIGHT = 7;
    public static final int CHAR_SPACING = 1;

    // Micro font: 3x5 pixels (more compact)
    public static final int MICRO_CHAR_WIDTH = 3;
    public static final int MICRO_CHAR_HEIGHT = 5;
    public static final int MICRO_CHAR_SPACING = 1;

    // Micro font data: 3x5 pixels per character
    private static final int[][] MICRO_GLYPHS = new int[128][];

    // Target text height as a fraction of image height (e.g., 0.15 = 15% of image height)
    private static final float TARGET_TEXT_HEIGHT_RATIO = 0.18f;
    // Minimum and maximum scale factors
    private static final int MIN_SCALE = 1;
    private static final int MAX_SCALE = 4;

    // Font data: 5x7 pixels per character
    private static final int[][] GLYPHS = new int[128][];

    static {
        // Letters A-Z (5x7)
        GLYPHS['A'] = new int[]{0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
        GLYPHS['B'] = new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110};
        GLYPHS['C'] = new int[]{0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110};
        GLYPHS['D'] = new int[]{0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110};
        GLYPHS['E'] = new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111};
        GLYPHS['F'] = new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000};
        GLYPHS['G'] = new int[]{0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110};
        GLYPHS['H'] = new int[]{0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
        GLYPHS['I'] = new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b11111};
        GLYPHS['J'] = new int[]{0b00111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100};
        GLYPHS['K'] = new int[]{0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001};
        GLYPHS['L'] = new int[]{0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111};
        GLYPHS['M'] = new int[]{0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001};
        GLYPHS['N'] = new int[]{0b10001, 0b11001, 0b10101, 0b10101, 0b10011, 0b10001, 0b10001};
        GLYPHS['O'] = new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
        GLYPHS['P'] = new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000};
        GLYPHS['Q'] = new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101};
        GLYPHS['R'] = new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001};
        GLYPHS['S'] = new int[]{0b01110, 0b10001, 0b10000, 0b01110, 0b00001, 0b10001, 0b01110};
        GLYPHS['T'] = new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100};
        GLYPHS['U'] = new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
        GLYPHS['V'] = new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100};
        GLYPHS['W'] = new int[]{0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001};
        GLYPHS['X'] = new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001};
        GLYPHS['Y'] = new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100};
        GLYPHS['Z'] = new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111};

        // Lowercase maps to uppercase
        for (char c = 'a'; c <= 'z'; c++) {
            GLYPHS[c] = GLYPHS[Character.toUpperCase(c)];
        }

        // Numbers 0-9
        GLYPHS['0'] = new int[]{0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110};
        GLYPHS['1'] = new int[]{0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110};
        GLYPHS['2'] = new int[]{0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111};
        GLYPHS['3'] = new int[]{0b01110, 0b10001, 0b00001, 0b00110, 0b00001, 0b10001, 0b01110};
        GLYPHS['4'] = new int[]{0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010};
        GLYPHS['5'] = new int[]{0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110};
        GLYPHS['6'] = new int[]{0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110};
        GLYPHS['7'] = new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000};
        GLYPHS['8'] = new int[]{0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110};
        GLYPHS['9'] = new int[]{0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100};

        // Special characters
        GLYPHS[' '] = new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000};
        GLYPHS['.'] = new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b01100};
        GLYPHS[','] = new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b00100, 0b01000};
        GLYPHS[':'] = new int[]{0b00000, 0b01100, 0b01100, 0b00000, 0b01100, 0b01100, 0b00000};
        GLYPHS['-'] = new int[]{0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000};
        GLYPHS['_'] = new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b11111};
        GLYPHS['+'] = new int[]{0b00000, 0b00100, 0b00100, 0b11111, 0b00100, 0b00100, 0b00000};
        GLYPHS['\''] = new int[]{0b00100, 0b00100, 0b01000, 0b00000, 0b00000, 0b00000, 0b00000};
        GLYPHS['!'] = new int[]{0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00000, 0b00100};
        GLYPHS['?'] = new int[]{0b01110, 0b10001, 0b00010, 0b00100, 0b00100, 0b00000, 0b00100};
        GLYPHS['['] = new int[]{0b01110, 0b01000, 0b01000, 0b01000, 0b01000, 0b01000, 0b01110};
        GLYPHS[']'] = new int[]{0b01110, 0b00010, 0b00010, 0b00010, 0b00010, 0b00010, 0b01110};
        GLYPHS['('] = new int[]{0b00010, 0b00100, 0b01000, 0b01000, 0b01000, 0b00100, 0b00010};
        GLYPHS[')'] = new int[]{0b01000, 0b00100, 0b00010, 0b00010, 0b00010, 0b00100, 0b01000};
        GLYPHS['/'] = new int[]{0b00001, 0b00010, 0b00010, 0b00100, 0b01000, 0b01000, 0b10000};

        // ===== MICRO FONT (3x5 pixels) =====
        // Letters A-Z
        MICRO_GLYPHS['A'] = new int[]{0b010, 0b101, 0b111, 0b101, 0b101};
        MICRO_GLYPHS['B'] = new int[]{0b110, 0b101, 0b110, 0b101, 0b110};
        MICRO_GLYPHS['C'] = new int[]{0b011, 0b100, 0b100, 0b100, 0b011};
        MICRO_GLYPHS['D'] = new int[]{0b110, 0b101, 0b101, 0b101, 0b110};
        MICRO_GLYPHS['E'] = new int[]{0b111, 0b100, 0b110, 0b100, 0b111};
        MICRO_GLYPHS['F'] = new int[]{0b111, 0b100, 0b110, 0b100, 0b100};
        MICRO_GLYPHS['G'] = new int[]{0b011, 0b100, 0b101, 0b101, 0b011};
        MICRO_GLYPHS['H'] = new int[]{0b101, 0b101, 0b111, 0b101, 0b101};
        MICRO_GLYPHS['I'] = new int[]{0b111, 0b010, 0b010, 0b010, 0b111};
        MICRO_GLYPHS['J'] = new int[]{0b001, 0b001, 0b001, 0b101, 0b010};
        MICRO_GLYPHS['K'] = new int[]{0b101, 0b110, 0b100, 0b110, 0b101};
        MICRO_GLYPHS['L'] = new int[]{0b100, 0b100, 0b100, 0b100, 0b111};
        MICRO_GLYPHS['M'] = new int[]{0b101, 0b111, 0b101, 0b101, 0b101};
        MICRO_GLYPHS['N'] = new int[]{0b101, 0b111, 0b111, 0b101, 0b101};
        MICRO_GLYPHS['O'] = new int[]{0b010, 0b101, 0b101, 0b101, 0b010};
        MICRO_GLYPHS['P'] = new int[]{0b110, 0b101, 0b110, 0b100, 0b100};
        MICRO_GLYPHS['Q'] = new int[]{0b010, 0b101, 0b101, 0b110, 0b011};
        MICRO_GLYPHS['R'] = new int[]{0b110, 0b101, 0b110, 0b101, 0b101};
        MICRO_GLYPHS['S'] = new int[]{0b011, 0b100, 0b010, 0b001, 0b110};
        MICRO_GLYPHS['T'] = new int[]{0b111, 0b010, 0b010, 0b010, 0b010};
        MICRO_GLYPHS['U'] = new int[]{0b101, 0b101, 0b101, 0b101, 0b010};
        MICRO_GLYPHS['V'] = new int[]{0b101, 0b101, 0b101, 0b010, 0b010};
        MICRO_GLYPHS['W'] = new int[]{0b101, 0b101, 0b101, 0b111, 0b101};
        MICRO_GLYPHS['X'] = new int[]{0b101, 0b101, 0b010, 0b101, 0b101};
        MICRO_GLYPHS['Y'] = new int[]{0b101, 0b101, 0b010, 0b010, 0b010};
        MICRO_GLYPHS['Z'] = new int[]{0b111, 0b001, 0b010, 0b100, 0b111};

        // Lowercase maps to uppercase
        for (char c = 'a'; c <= 'z'; c++) {
            MICRO_GLYPHS[c] = MICRO_GLYPHS[Character.toUpperCase(c)];
        }

        // Numbers 0-9
        MICRO_GLYPHS['0'] = new int[]{0b010, 0b101, 0b101, 0b101, 0b010};
        MICRO_GLYPHS['1'] = new int[]{0b010, 0b110, 0b010, 0b010, 0b111};
        MICRO_GLYPHS['2'] = new int[]{0b110, 0b001, 0b010, 0b100, 0b111};
        MICRO_GLYPHS['3'] = new int[]{0b110, 0b001, 0b010, 0b001, 0b110};
        MICRO_GLYPHS['4'] = new int[]{0b101, 0b101, 0b111, 0b001, 0b001};
        MICRO_GLYPHS['5'] = new int[]{0b111, 0b100, 0b110, 0b001, 0b110};
        MICRO_GLYPHS['6'] = new int[]{0b011, 0b100, 0b110, 0b101, 0b010};
        MICRO_GLYPHS['7'] = new int[]{0b111, 0b001, 0b010, 0b010, 0b010};
        MICRO_GLYPHS['8'] = new int[]{0b010, 0b101, 0b010, 0b101, 0b010};
        MICRO_GLYPHS['9'] = new int[]{0b010, 0b101, 0b011, 0b001, 0b110};

        // Special characters
        MICRO_GLYPHS[' '] = new int[]{0b000, 0b000, 0b000, 0b000, 0b000};
        MICRO_GLYPHS['.'] = new int[]{0b000, 0b000, 0b000, 0b000, 0b010};
        MICRO_GLYPHS['-'] = new int[]{0b000, 0b000, 0b111, 0b000, 0b000};
        MICRO_GLYPHS['_'] = new int[]{0b000, 0b000, 0b000, 0b000, 0b111};
    }

    /**
     * Calculate the width in pixels needed to render a micro string.
     */
    public static int getMicroTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() * MICRO_CHAR_WIDTH + (text.length() - 1) * MICRO_CHAR_SPACING;
    }

    /**
     * Calculate the width in pixels needed to render a string.
     */
    public static int getTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() * CHAR_WIDTH + (text.length() - 1) * CHAR_SPACING;
    }

    /**
     * Calculate the width in pixels needed to render a string at a given scale.
     */
    public static int getTextWidth(String text, int scale) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() * (CHAR_WIDTH * scale) + (text.length() - 1) * (CHAR_SPACING * scale);
    }

    /**
     * Calculate the optimal scale factor for text based on image dimensions.
     * Returns 0 if the image is too small for readable text.
     *
     * @param imageWidth  The width of the image
     * @param imageHeight The height of the image
     * @return The scale factor to use (0 = skip text, 1-4 for rendering)
     */
    public static int calculateScale(int imageWidth, int imageHeight) {
        // Use the smaller dimension to determine scale
        int minDimension = Math.min(imageWidth, imageHeight);

        // Images too small for readable text - skip rendering
        if (minDimension < 12) {
            return 0;
        }

        // Scale breakpoints - keep text small for typical tile sizes (16-48px)
        // Scale 1 = 7px text height, fits well in small tiles
        int scale;
        if (minDimension < 128) {
            scale = 1;
        } else if (minDimension < 384) {
            scale = 2;
        } else {
            scale = 3;
        }

        // Clamp to valid range
        return Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale));
    }

    /**
     * Check if text rendering should be skipped for this image size.
     */
    public static boolean shouldSkipText(int imageWidth, int imageHeight) {
        return calculateScale(imageWidth, imageHeight) == 0;
    }

    /**
     * Get the scaled character height.
     */
    public static int getScaledCharHeight(int scale) {
        return CHAR_HEIGHT * scale;
    }

    /**
     * Get the scaled line height (character height + spacing).
     */
    public static int getScaledLineHeight(int scale) {
        return (CHAR_HEIGHT + 2) * scale;
    }

    /**
     * Splits text into multiple lines to fit within a given width.
     *
     * @param text       The text to potentially split
     * @param maxWidth   Maximum width in pixels per line
     * @param scale      The scale factor being used
     * @return Array of lines (may be single element if text fits)
     */
    public static String[] splitToFit(String text, int maxWidth, int scale) {
        if (text == null || text.isEmpty()) return new String[]{""};

        int textWidth = getTextWidth(text, scale);
        if (textWidth <= maxWidth) {
            return new String[]{text};
        }

        // Calculate max characters per line
        int charWidth = CHAR_WIDTH * scale + CHAR_SPACING * scale;
        int maxCharsPerLine = Math.max(1, maxWidth / charWidth);

        // Split into lines
        java.util.List<String> lines = new java.util.ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxCharsPerLine, text.length());
            lines.add(text.substring(start, end));
            start = end;
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Calculates how many lines are needed to display text at the given width.
     */
    public static int getLinesNeeded(String text, int maxWidth, int scale) {
        if (text == null || text.isEmpty()) return 1;

        int textWidth = getTextWidth(text, scale);
        if (textWidth <= maxWidth) {
            return 1;
        }

        int charWidth = CHAR_WIDTH * scale + CHAR_SPACING * scale;
        int maxCharsPerLine = Math.max(1, maxWidth / charWidth);
        return (int) Math.ceil((double) text.length() / maxCharsPerLine);
    }

    /**
     * Draw text onto an image data array.
     */
    public static void drawText(int[] imageData, int imageWidth, int imageHeight,
                                String text, int startX, int startY, int color) {
        if (text == null) return;

        int x = startX;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            drawChar(imageData, imageWidth, imageHeight, c, x, startY, color);
            x += CHAR_WIDTH + CHAR_SPACING;
        }
    }

    /**
     * Draw text centered horizontally within the image.
     */
    public static void drawTextCentered(int[] imageData, int imageWidth, int imageHeight,
                                        String text, int centerY, int color) {
        int textWidth = getTextWidth(text);
        int startX = (imageWidth - textWidth) / 2;
        drawText(imageData, imageWidth, imageHeight, text, startX, centerY, color);
    }

    /**
     * Draw text with an outline for better visibility.
     */
    public static void drawTextWithOutline(int[] imageData, int imageWidth, int imageHeight,
                                           String text, int startX, int startY,
                                           int textColor, int outlineColor) {
        // Draw outline in all 8 directions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    drawText(imageData, imageWidth, imageHeight, text, startX + dx, startY + dy, outlineColor);
                }
            }
        }
        // Draw main text on top
        drawText(imageData, imageWidth, imageHeight, text, startX, startY, textColor);
    }

    /**
     * Draw text with a shadow/outline for better visibility.
     */
    public static void drawTextWithShadow(int[] imageData, int imageWidth, int imageHeight,
                                          String text, int startX, int startY,
                                          int textColor, int shadowColor) {
        // Draw shadow offset by 1 pixel
        drawText(imageData, imageWidth, imageHeight, text, startX + 1, startY + 1, shadowColor);
        // Draw main text on top
        drawText(imageData, imageWidth, imageHeight, text, startX, startY, textColor);
    }

    /**
     * Draw text centered with outline.
     */
    public static void drawTextCenteredWithOutline(int[] imageData, int imageWidth, int imageHeight,
                                                   String text, int centerY,
                                                   int textColor, int outlineColor) {
        int textWidth = getTextWidth(text);
        int startX = (imageWidth - textWidth) / 2;
        drawTextWithOutline(imageData, imageWidth, imageHeight, text, startX, centerY, textColor, outlineColor);
    }

    /**
     * Draw text centered with shadow.
     */
    public static void drawTextCenteredWithShadow(int[] imageData, int imageWidth, int imageHeight,
                                                   String text, int centerY,
                                                   int textColor, int shadowColor) {
        int textWidth = getTextWidth(text);
        int startX = (imageWidth - textWidth) / 2;
        drawTextWithShadow(imageData, imageWidth, imageHeight, text, startX, centerY, textColor, shadowColor);
    }

    // ==================== SCALED DRAWING METHODS ====================

    /**
     * Draw text at a given scale.
     */
    public static void drawTextScaled(int[] imageData, int imageWidth, int imageHeight,
                                      String text, int startX, int startY, int color, int scale) {
        if (text == null || scale < 1) return;

        int x = startX;
        int scaledCharWidth = CHAR_WIDTH * scale;
        int scaledSpacing = CHAR_SPACING * scale;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            drawCharScaled(imageData, imageWidth, imageHeight, c, x, startY, color, scale);
            x += scaledCharWidth + scaledSpacing;
        }
    }

    /**
     * Draw text centered horizontally at a given scale.
     */
    public static void drawTextCenteredScaled(int[] imageData, int imageWidth, int imageHeight,
                                              String text, int centerY, int color, int scale) {
        int textWidth = getTextWidth(text, scale);
        int startX = (imageWidth - textWidth) / 2;
        drawTextScaled(imageData, imageWidth, imageHeight, text, startX, centerY, color, scale);
    }

    /**
     * Draw text with outline at a given scale.
     */
    public static void drawTextWithOutlineScaled(int[] imageData, int imageWidth, int imageHeight,
                                                 String text, int startX, int startY,
                                                 int textColor, int outlineColor, int scale) {
        // Draw outline in all 8 directions (scaled offset)
        int outlineOffset = Math.max(1, scale / 2);
        for (int dx = -outlineOffset; dx <= outlineOffset; dx++) {
            for (int dy = -outlineOffset; dy <= outlineOffset; dy++) {
                if (dx != 0 || dy != 0) {
                    drawTextScaled(imageData, imageWidth, imageHeight, text,
                            startX + dx, startY + dy, outlineColor, scale);
                }
            }
        }
        // Draw main text on top
        drawTextScaled(imageData, imageWidth, imageHeight, text, startX, startY, textColor, scale);
    }

    /**
     * Draw text centered with outline at a given scale.
     */
    public static void drawTextCenteredWithOutlineScaled(int[] imageData, int imageWidth, int imageHeight,
                                                         String text, int centerY,
                                                         int textColor, int outlineColor, int scale) {
        int textWidth = getTextWidth(text, scale);
        int startX = (imageWidth - textWidth) / 2;
        drawTextWithOutlineScaled(imageData, imageWidth, imageHeight, text, startX, centerY,
                textColor, outlineColor, scale);
    }

    /**
     * Draw a single character at a given scale.
     */
    private static void drawCharScaled(int[] imageData, int imageWidth, int imageHeight,
                                       char c, int x, int y, int color, int scale) {
        int[] glyph = (c < GLYPHS.length) ? GLYPHS[c] : null;
        if (glyph == null) {
            glyph = GLYPHS[' ']; // Default to space for unknown chars
        }

        for (int row = 0; row < CHAR_HEIGHT; row++) {
            int rowBits = glyph[row];
            for (int col = 0; col < CHAR_WIDTH; col++) {
                // Check if this pixel should be drawn
                boolean pixelOn = (rowBits & (1 << (CHAR_WIDTH - 1 - col))) != 0;
                if (pixelOn) {
                    // Draw a scale x scale block for this pixel
                    for (int sy = 0; sy < scale; sy++) {
                        for (int sx = 0; sx < scale; sx++) {
                            int px = x + (col * scale) + sx;
                            int py = y + (row * scale) + sy;
                            if (px >= 0 && px < imageWidth && py >= 0 && py < imageHeight) {
                                imageData[py * imageWidth + px] = color;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Draw a single character.
     */
    private static void drawChar(int[] imageData, int imageWidth, int imageHeight,
                                 char c, int x, int y, int color) {
        int[] glyph = (c < GLYPHS.length) ? GLYPHS[c] : null;
        if (glyph == null) {
            glyph = GLYPHS[' ']; // Default to space for unknown chars
        }

        for (int row = 0; row < CHAR_HEIGHT; row++) {
            int rowBits = glyph[row];
            for (int col = 0; col < CHAR_WIDTH; col++) {
                // Check if this pixel should be drawn
                boolean pixelOn = (rowBits & (1 << (CHAR_WIDTH - 1 - col))) != 0;
                if (pixelOn) {
                    int px = x + col;
                    int py = y + row;
                    if (px >= 0 && px < imageWidth && py >= 0 && py < imageHeight) {
                        imageData[py * imageWidth + px] = color;
                    }
                }
            }
        }
    }

    /**
     * Pack RGBA values into an int.
     */
    public static int packColor(int r, int g, int b, int a) {
        return (r & 255) << 24 | (g & 255) << 16 | (b & 255) << 8 | (a & 255);
    }

    /**
     * Common colors for map text.
     */
    public static final int WHITE = packColor(255, 255, 255, 255);
    public static final int BLACK = packColor(0, 0, 0, 255);
    public static final int SHADOW = packColor(30, 30, 30, 255);
    public static final int YELLOW = packColor(255, 255, 100, 255);
    public static final int GREEN = packColor(100, 255, 100, 255);
    public static final int RED = packColor(255, 100, 100, 255);

    // ==================== MICRO FONT METHODS (3x5) ====================

    /**
     * Draw micro text onto an image data array.
     */
    public static void drawMicroText(int[] imageData, int imageWidth, int imageHeight,
                                     String text, int startX, int startY, int color) {
        if (text == null) return;

        int x = startX;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            drawMicroChar(imageData, imageWidth, imageHeight, c, x, startY, color);
            x += MICRO_CHAR_WIDTH + MICRO_CHAR_SPACING;
        }
    }

    /**
     * Draw micro text centered horizontally within the image.
     */
    public static void drawMicroTextCentered(int[] imageData, int imageWidth, int imageHeight,
                                             String text, int centerY, int color) {
        int textWidth = getMicroTextWidth(text);
        int startX = (imageWidth - textWidth) / 2;
        drawMicroText(imageData, imageWidth, imageHeight, text, startX, centerY, color);
    }

    /**
     * Draw micro text with outline for visibility.
     */
    public static void drawMicroTextWithOutline(int[] imageData, int imageWidth, int imageHeight,
                                                String text, int startX, int startY,
                                                int textColor, int outlineColor) {
        // Draw outline in all 8 directions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    drawMicroText(imageData, imageWidth, imageHeight, text, startX + dx, startY + dy, outlineColor);
                }
            }
        }
        // Draw main text on top
        drawMicroText(imageData, imageWidth, imageHeight, text, startX, startY, textColor);
    }

    /**
     * Draw micro text centered with outline.
     */
    public static void drawMicroTextCenteredWithOutline(int[] imageData, int imageWidth, int imageHeight,
                                                        String text, int centerY,
                                                        int textColor, int outlineColor) {
        int textWidth = getMicroTextWidth(text);
        int startX = (imageWidth - textWidth) / 2;
        drawMicroTextWithOutline(imageData, imageWidth, imageHeight, text, startX, centerY, textColor, outlineColor);
    }

    /**
     * Draw a single micro character.
     */
    private static void drawMicroChar(int[] imageData, int imageWidth, int imageHeight,
                                      char c, int x, int y, int color) {
        int[] glyph = (c < MICRO_GLYPHS.length) ? MICRO_GLYPHS[c] : null;
        if (glyph == null) {
            glyph = MICRO_GLYPHS[' ']; // Default to space for unknown chars
            if (glyph == null) return; // Space not defined
        }

        for (int row = 0; row < MICRO_CHAR_HEIGHT; row++) {
            int rowBits = glyph[row];
            for (int col = 0; col < MICRO_CHAR_WIDTH; col++) {
                // Check if this pixel should be drawn
                boolean pixelOn = (rowBits & (1 << (MICRO_CHAR_WIDTH - 1 - col))) != 0;
                if (pixelOn) {
                    int px = x + col;
                    int py = y + row;
                    if (px >= 0 && px < imageWidth && py >= 0 && py < imageHeight) {
                        imageData[py * imageWidth + px] = color;
                    }
                }
            }
        }
    }

    // ==================== BALANCED TEXT SPLITTING ====================

    /**
     * Split text into multiple lines with balanced (even) character distribution.
     * For example "ABCDEF" with 2 lines becomes ["ABC", "DEF"] not ["ABCD", "EF"].
     *
     * @param text The text to split
     * @param numLines Number of lines to split into
     * @return Array of lines with balanced lengths
     */
    public static String[] splitBalanced(String text, int numLines) {
        if (text == null || text.isEmpty() || numLines <= 0) {
            return new String[]{""};
        }
        if (numLines == 1 || text.length() <= numLines) {
            return new String[]{text};
        }

        String[] lines = new String[numLines];
        int totalLength = text.length();
        int baseLength = totalLength / numLines;
        int remainder = totalLength % numLines;

        int pos = 0;
        for (int i = 0; i < numLines; i++) {
            // Distribute remainder across first few lines
            int lineLength = baseLength + (i < remainder ? 1 : 0);
            int end = Math.min(pos + lineLength, totalLength);
            lines[i] = text.substring(pos, end);
            pos = end;
        }

        return lines;
    }

    /**
     * Calculate optimal number of lines needed to fit text in given width.
     * Returns the minimum lines needed while keeping lines balanced.
     *
     * @param text The text to measure
     * @param maxWidth Maximum width in pixels
     * @param charWidth Width of each character including spacing
     * @return Number of lines needed (1 if fits on single line)
     */
    public static int calculateLinesNeeded(String text, int maxWidth, int charWidth) {
        if (text == null || text.isEmpty()) return 1;
        
        int textLength = text.length();
        int maxCharsPerLine = Math.max(1, maxWidth / charWidth);
        
        if (textLength <= maxCharsPerLine) {
            return 1;
        }
        
        return (int) Math.ceil((double) textLength / maxCharsPerLine);
    }

    // ==================== GLYPH ACCESSORS ====================

    /**
     * Get the glyph data for a character (standard 5x7 font).
     * Used for external rendering with clipping support.
     *
     * @param c The character to get glyph for
     * @return int array of row bitmasks, or null if not defined
     */
    public static int[] getGlyph(char c) {
        if (c >= 0 && c < GLYPHS.length && GLYPHS[c] != null) {
            return GLYPHS[c];
        }
        // Return space glyph as fallback
        return GLYPHS[' '];
    }

    /**
     * Get the glyph data for a character (micro 3x5 font).
     * Used for external rendering with clipping support.
     *
     * @param c The character to get glyph for
     * @return int array of row bitmasks, or null if not defined
     */
    public static int[] getMicroGlyph(char c) {
        if (c >= 0 && c < MICRO_GLYPHS.length && MICRO_GLYPHS[c] != null) {
            return MICRO_GLYPHS[c];
        }
        // Return space glyph as fallback
        return MICRO_GLYPHS[' '];
    }
}
