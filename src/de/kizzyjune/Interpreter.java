package de.kizzyjune;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

final class Interpreter {
    private static final Path PROG_FILE = Path.of("program.junescript");
    private static long VAR1 = 0;
    private static long VAR2 = 0;
    private static long OUT = 0;
    private static double OUT_DOUBLE = 0.0D;
    private static final JFrame window = new JFrame("junescript interpreter");
    private static final JTextArea text = new JTextArea("meow");
    private static boolean WAIT_FOR_PLAYBACK = false;
    private static Clip clip = null;
    private static AudioInputStream audioInput = null;

    static void main() {
        if (!Files.exists(PROG_FILE)) handleThrowable(new ScriptEngineError("Script file not found!"));
        System.setProperty("interpreter.version", "1.0");
        prepareWindow();
        try (final Stream<String> lines = Files.lines(PROG_FILE)) {
            lines.forEach(Interpreter::handleInstruction);
        } catch (final Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleInstruction(final String instruction) {
        final int spaceIndex = instruction.indexOf(' ');
        final String instWithoutArgs = (spaceIndex != -1)
                ? instruction.substring(0, spaceIndex)
                : instruction;
        try {
            switch (instWithoutArgs) {
                case "PAINT":
                    IO.println("\uD83D\uDE33");
                    break;
                case "WRITEVAR1":
                    VAR1 = Long.parseLong(instruction.substring(spaceIndex + 1));
                    break;
                case "WRITEVAR2":
                    VAR2 = Long.parseLong(instruction.substring(spaceIndex + 1));
                    break;
                case "ADD":
                    OUT = VAR1 + VAR2;
                    break;
                case "SUB":
                    OUT = VAR1 - VAR2;
                    break;
                case "MUL":
                    OUT = VAR1 * VAR2;
                    break;
                case "DIV":
                    OUT = VAR1 / VAR2;
                    break;
                case "SQRT":
                    // Planned instruction, coming soon!
                    break;
                case "RES":
                    IO.println(OUT);
                    break;
                case "ERR":
                    throw new ScriptError(instruction.substring(spaceIndex + 1));
                case "EX":
                    throw new ScriptException(instruction.substring(spaceIndex + 1));
                case "EXIT":
                    System.exit(Integer.parseInt(instruction.substring(spaceIndex + 1)));
                    break;
                case "GETPROP":
                    IO.println(System.getProperty(instruction.substring(spaceIndex + 1)));
                    break;
                case "PRINT":
                    IO.println(instruction.substring(spaceIndex + 1));
                    break;
                case "PRINTERR":
                    System.err.println(instruction.substring(spaceIndex + 1));
                    break;
                case "SHOWWIN":
                    window.setVisible(true);
                    break;
                case "HIDEWIN":
                    window.setVisible(false);
                    break;
                case "ALLOWEDIT":
                    text.setEditable(true);
                    break;
                case "NOEDIT":
                    text.setEditable(false);
                    break;
                case "TITLE":
                    window.setTitle(instruction.substring(spaceIndex + 1));
                    break;
                case "TEXT":
                    if (instruction.substring(spaceIndex + 1).equals("OUT")) {
                        text.setText(String.valueOf(OUT));
                        break;
                    }
                    text.setText(instruction.substring(spaceIndex + 1));
                    break;
                case "WAIT":
                    Thread.sleep(Integer.parseInt(instruction.substring(spaceIndex + 1)));
                    break;
                case "COLOUR":
                    text.setBackground(Color.decode(instruction.substring(spaceIndex + 1)));
                    text.repaint();
                    break;
                case "WAITFORAUDIO":
                    WAIT_FOR_PLAYBACK = true;
                    break;
                case "NOWAITFORAUDIO":
                    WAIT_FOR_PLAYBACK = false;
                    break;
                case "PLAYSOUND":
                    playFile(instruction.substring(spaceIndex + 1));
                    break;
                case "STOPAUDIO":
                    if (clip == null || audioInput == null) throw new ScriptEngineError("Can't stop audio when audio isn't playing.");
                    clip.stop();
                    clip.close();
                    audioInput.close();
                    break;
                default:
                    throw new ScriptEngineError("Invalid instruction! " + instruction);
            }
        } catch (final Throwable t) {
          handleThrowable(t);
        }
    }

    private static void handleThrowable(final Throwable t) {
        final boolean isError = t instanceof Error;
        final boolean isException = t instanceof Exception;
        final boolean justThrowable = !isError && !isException;
        final StringBuilder sb = new StringBuilder();
        sb.append("An ");
        if (!justThrowable) sb.append(isError ? "error" : "exception");
        else sb.append("throwable");
        sb.append(" has occurred.");
        final boolean isEngineError = t.getClass().equals(ScriptEngineError.class);
        if (t.getClass().equals(ScriptError.class) || t.getClass().equals(ScriptException.class)) sb.append("\nThis throwable has been thrown from inside the executed script.\n");
        else if (isEngineError) System.err.println("An error has occurred in the script execution engine.");
        else {
            sb.append("\n");
        }
        if (!isEngineError) System.err.println(sb);
        if (!isEngineError && !t.getMessage().contains("Invalid instruction!")) {
            t.printStackTrace();
        }
        else {
            System.err.println(t.getMessage());
        }
        System.exit(1);
    }

    public static final class ScriptException extends RuntimeException {
        private ScriptException(final String in) {
            super(in);
        }
    }

    public static final class ScriptError extends Error {
        private ScriptError(final String in) {
            super(in);
        }
    }

    public static final class ScriptEngineError extends Error {
        private ScriptEngineError(final String in) {
            super(in);
        }
    }

    private static void prepareWindow() {
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        window.setSize(1920 / 2, 1080 / 2);
        window.setLocationRelativeTo(null);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setVisible(true);
        final InputStream fontIs = Interpreter.class.getResourceAsStream("/resources/Halogen.otf");
        if (fontIs == null) handleThrowable(new FileNotFoundException("Font file in JAR not found!"));
        try {
            final Font getFontFromIs = Font.createFont(Font.TRUETYPE_FONT,fontIs);
            final Font resizeFont = getFontFromIs.deriveFont(43.0F);
            text.setFont(resizeFont);
        } catch (final Throwable t) {
            handleThrowable(t);
        }
        window.add(text);
    }

    private static void playFile(final String file) throws Throwable {
      final File soundFile = new File(file);
      audioInput = AudioSystem.getAudioInputStream(soundFile);
      clip = AudioSystem.getClip();
      clip.open(audioInput);
      clip.start();
      if (WAIT_FOR_PLAYBACK) Thread.sleep(clip.getMicrosecondLength() / 1000);
    }
}