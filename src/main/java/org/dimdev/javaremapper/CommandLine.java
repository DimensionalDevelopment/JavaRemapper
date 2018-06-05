package org.dimdev.javaremapper;

import org.dimdev.srg2jam.Srg2Jam;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public final class CommandLine {
    public static void main(String... args) throws IOException {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            System.out.println("JavaRemapper - Tool for remapping obfuscated jar files");
            System.out.println("JAM format specification: https://github.com/caseif/JAM");
            System.out.println();
            System.out.println("Subcommands:");
            System.out.println(" remap <jar> <target> <mappings> - Remaps a jar file using a JAM mapping file");
            System.out.println(" srg2jam <path to MCP config folder> - Converts a MCP config folder to a JAM file");
            System.out.println(" help - Displays this help message");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "remap": {
                File inputFile = new File(args[1]);
                File remapTarget = new File(args[2]);
                File mappingFile = new File(args[3]);

                if (remapTarget.exists()) remapTarget.delete();

                Mapping mapping = new Mapping();
                try (Reader reader = new FileReader(mappingFile)) {
                    mapping.readFromJAM(reader);
                }

                new JavaRemapper(mapping).remapJar(inputFile, remapTarget);
                break;
            }

            case "srg2jam": {
                Srg2Jam.convert(new File(args[1]));
                break;
            }

            default: {
                System.err.println("Unknown subcommand, use \"help\" for a list of subcommands.");
                break;
            }
        }
    }
}
