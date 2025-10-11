IntelliJ IDEA (Windows):

- Column Selection (best for fixed columns)
    - Toggle column mode: `Alt`+`Shift`+`Insert`.
    - Select the same columns across multiple lines (mouse drag or `Shift`+arrows), press `Delete`/`Backspace`.
    - Tip: You can also `Alt`+drag with the mouse to make a rectangular selection without toggling mode.

- Multi‑caret (best for uneven positions)
    - Place carets with `Alt`+click on each line, then press `Delete`/`Backspace`.
    - For repeated text: select one occurrence, press `Alt`+`J` to add next, `Ctrl`+`Alt`+`Shift`+`J` to select all, then delete.

- Regex replace (best for patterns)
    - Press `Ctrl`+`R`, enable regex (`.\*` button), select the lines, and use patterns:
        - Remove first N chars: find `^.{N}` and replace with empty.
        - Remove last N chars: find `.{N}$` and replace with empty.
    - Use “In Selection” to limit to highlighted lines.

# i18n

Internationalization (i18n) is the process of designing your software so it can easily be translated and adapted for users in different:

Languages (English, Spanish, French, etc.)

Regions/locales (date/time formats, currency symbols, measurement units)

Cultural conventions (right-to-left text, plural rules, etc.)

It’s the engineering work that enables localization later.