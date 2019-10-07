package org.belov;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class CommandProcessor {
    private static final String MSG_COMMAND_NOT_FOUND = "Command not found";
    private static final String MSG_DELIM = "====================";

    private Map<String, Command> commands;

    private String consoleEncoding;

    public CommandProcessor(String consoleEncoding) {
        commands = new TreeMap<>();
        Command cmd = new HelpCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new DirCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new ExitCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new CdCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new PwdCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new CatCommand();
        commands.put(cmd.getName(), cmd);
        this.consoleEncoding = consoleEncoding;
    }

    public void execute() {
        Context c = new Context();
        c.currentDirectory = new File(".").getAbsoluteFile();
        boolean result = true;
        Scanner scanner = new Scanner(System.in, consoleEncoding);

        do {
            System.out.print("> ");
            String fullCommand = scanner.nextLine();
            ParsedCommand pc = new ParsedCommand(fullCommand);
            if (pc.command == null || "".equals(pc.command)) {
                continue;
            }
            Command cmd = commands.get(pc.command.toUpperCase());
            if (cmd == null) {
                System.out.println(MSG_COMMAND_NOT_FOUND);
                continue;
            }
            result = cmd.execute(c, pc.args);
        } while (result);
    }

    public static void main(String[] args) {
        CommandProcessor cp = new CommandProcessor("Cp1251");
        cp.execute();
    }

    class ParsedCommand {
        String command;

        String[] args;

        public ParsedCommand(String line) {
            String parts[] = line.split(" ");
            if (parts != null) {
                command = parts[0];
                if (parts.length > 1) {
                    args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, args.length);
                }
            }
        }
    }

    interface Command {
        boolean execute(Context context, String... args);

        void printHelp();

        String getName();

        String getDescription();
    }

    class Context {
        private File currentDirectory;
    }

    class HelpCommand implements Command {

        @Override
        public boolean execute(Context context, String... args) {
            if (args == null) {
                System.out.println("Avaliable commands:\n" + MSG_DELIM);
                for (Command cmd : commands.values()) {
                    System.out.println(cmd.getName() + ": " + cmd.getDescription());
                }
                System.out.println(MSG_DELIM);
            } else {
                for (String cmd : args) {
                    System.out.println("Help for command " + cmd + ":\n" + MSG_DELIM);
                    Command command = commands.get(cmd.toUpperCase());
                    if (command == null) {
                        System.out.println(MSG_COMMAND_NOT_FOUND);
                    } else {
                        command.printHelp();
                    }
                    System.out.println(MSG_DELIM);
                }
            }
            return true;
        }

        @Override
        public void printHelp() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "HELP";
        }

        @Override
        public String getDescription() {
            return "Print list of available commands";
        }
    }

    class DirCommand implements Command {

        @Override
        public boolean execute(Context context, String... args) {
            if (args == null) {
                //print current directory content
                printDir(context.currentDirectory);
            } else  {
                //print specified directory content
                File gFile = context.currentDirectory;
                context.currentDirectory = new File(args[0]).getAbsoluteFile();
                printDir(context.currentDirectory);
                //вернем положение курсора
                context.currentDirectory = gFile;
            }
            return true;
        }

        @Override
        public void printHelp() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "DIR";
        }

        private void printDir(File dir) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    System.out.println(f.getName());
                }
            }
        }

        @Override
        public String getDescription() {
            return "Print directory content";
        }
    }

    class ExitCommand implements Command {
        @Override
        public boolean execute(Context context, String... args) {
            System.out.println("Finishing command processor... done.");
            return false;
        }

        @Override
        public void printHelp() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "EXIT";
        }

        @Override
        public String getDescription() {
            return "Exist from command processor";
        }
    }

    class CdCommand implements Command {

        @Override
        public boolean execute(Context context, String... args) {
            if (args != null) {
                if (new File(args[0]).isDirectory()) {
                    //go to directory
                    context.currentDirectory = new File(args[ 0 ]).getAbsoluteFile();
                } else {
                    System.out.println("Is not directory...");
                }
            }
            return true;
        }

        @Override
        public void printHelp() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "CD";
        }

        @Override
        public String getDescription() {
            return "Go to directory";
        }
    }

    class PwdCommand implements Command {
        @Override
        public boolean execute(Context context, String... args) {
            if (args == null) {
                //print absolute path
                System.out.println(context.currentDirectory);
            } else  {
                //print absolute path directory
                context.currentDirectory = new File(args[0]).getAbsoluteFile();
                System.out.println(context.currentDirectory);
            }
            return true;
        }

        @Override
        public void printHelp() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "PWD";
        }

        @Override
        public String getDescription() {
            return "Absolute path to current directory";
        }
    }

    class CatCommand implements Command {

        @Override
        public boolean execute(Context context, String... args) {
            if (args == null) {
                System.out.println("You did not enter a file name!");
            } else {
                //check file existence
                String nameFile = context.currentDirectory + "\\" + args[0];
                if (new File(nameFile).exists()) {
                    // читаем файл в строку с помощью класса Files
                    try {
                        readFileNew(nameFile, StandardCharsets.US_ASCII);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("File not found!");
                }
            }
            return true;
        }

        private void readFileNew(String path, Charset encoding) throws IOException {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            String contents =  new String(encoded, encoding);
            System.out.println(contents);
        }

        @Override
        public void printHelp() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "CAT";
        }

        @Override
        public String getDescription() {
            return "displays the contents of a text file";
        }
    }
}
