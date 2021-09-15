package duke;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import duke.task.Deadline;
import duke.task.Event;
import duke.task.Task;
import duke.task.Todo;

/**
 * A personal assistant chatbot.
 */
public class Duke {
    private static final String INDENTED_HORIZONTAL_LINE = " ".repeat(4) + "_".repeat(60);
    /** Platform independent line separator */
    private static final String LS = System.lineSeparator();

    private static final Scanner SCANNER = new Scanner(System.in);

    private static final Path DATA_DIRECTORY_PATH = Paths.get("data");
    private static final String DATA_FILE_NAME = "duke.txt";
    private static final Path DATA_FILE_PATH = DATA_DIRECTORY_PATH.resolve(DATA_FILE_NAME);
    public static final String DATA_FILE_SEPARATOR = " ∥ ";

    private static final String MESSAGE_DATA_DIRECTORY_CREATED = "Created new directory: '" + DATA_DIRECTORY_PATH + "'";
    private static final String MESSAGE_DATA_FILE_CREATED = "No data file found. Created new file: '"
            + DATA_FILE_PATH + "'";
    private static final String MESSAGE_DATA_FILE_FOUND = "Data file found. Using data from '" + DATA_FILE_PATH + "'";
    private static final String MESSAGE_DATA_FILE_ACCESS_ERROR = "There was an error accessing the data file: '"
            + DATA_FILE_PATH + "'";
    private static final String MESSAGE_DATA_FILE_PARSE_ERROR = "There was an error parsing the data file:";

    private static final String MESSAGE_GREETING = "Hello! I'm Duke" + LS + "What can I do for you?";
    private static final String MESSAGE_FAREWELL = "Bye. Hope to see you again soon!";
    private static final String MESSAGE_ERROR = "☹ OOPS!!! %1$s";
    private static final String MESSAGE_TASK_ADDED = "Got it. I've added this task:" + LS + "  %1$s" + LS
            + "Now you have %2$s task(s) in the list";
    private static final String MESSAGE_TASK_DELETED = "Noted. I've removed this task:" + LS + "  %1$s" + LS
            + "Now you have %2$s task(s) in the list";
    private static final String MESSAGE_TASK_LIST = "Here are the tasks in your list:" + LS + "%1$s";
    private static final String MESSAGE_TASK_MARKED_AS_DONE = "Nice! I've marked this task as done:" + LS + "  %1$s";

    private static final String MESSAGE_TODO_DESCRIPTION_EMPTY = "The description of a todo cannot be empty.";
    private static final String MESSAGE_UNRECOGNISED_COMMAND = "I'm sorry, but I don't know what that means :-(";
    private static final String MESSAGE_UNRECOGNISED_TASK_TYPE_ICON = "Unrecognised task type icon: '%1$s'";
    private static final String MESSAGE_UNRECOGNISED_TASK_STATUS_ICON = "Unrecognised task status icon: '%1$s'";

    private static final String DEADLINE_PREFIX_BY = "/by";
    private static final String EVENT_PREFIX_AT = "/at";

    private static final String COMMAND_EXIT = "bye";
    private static final String COMMAND_ADD_TODO = "todo";
    private static final String COMMAND_ADD_DEADLINE = "deadline";
    private static final String COMMAND_ADD_EVENT = "event";
    private static final String COMMAND_DELETE_TASK = "delete";
    private static final String COMMAND_LIST_TASKS = "list";
    private static final String COMMAND_MARK_TASK_AS_DONE = "done";

    /** Array of Task objects */
    private static final ArrayList<Task> tasks = new ArrayList<>();

    /**
     * Main entry point of Duke.
     */
    public static void main(String[] args) {
        loadData();
        printGreeting();
        while (true) {
            final String userInput = getUserInput();
            final String feedback = executeCommand(userInput);
            printResponseBlock(feedback);
        }
    }

    private static void printGreeting() {
        printResponseBlock(MESSAGE_GREETING);
    }

    /**
     * Prints out the specified text formatted as a response block.
     * Horizontal lines will be printed before and after the specified text, and the text will be indented.
     *
     * @param text Text to be printed out.
     */
    private static void printResponseBlock(String text) {
        System.out.println(INDENTED_HORIZONTAL_LINE);
        System.out.println(indent(text));
        System.out.println(INDENTED_HORIZONTAL_LINE);
        System.out.println();
    }

    private static String indent(String text) {
        String[] lines = text.split(LS);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = " ".repeat(5) + lines[i];
        }
        return String.join(LS, lines);
    }

    private static void loadData() {
        boolean createdDataDirectory = DATA_DIRECTORY_PATH.toFile().mkdir();

        File dataFile = DATA_FILE_PATH.toFile();
        try {
            if (dataFile.createNewFile()) {
                printResponseBlock((createdDataDirectory ? MESSAGE_DATA_DIRECTORY_CREATED + LS : "")
                        + MESSAGE_DATA_FILE_CREATED);
            } else {
                printResponseBlock(MESSAGE_DATA_FILE_FOUND);
                parseDataFromFile(dataFile);
            }
        } catch (IOException e) {
            printResponseBlock(MESSAGE_DATA_FILE_ACCESS_ERROR + LS + e.getMessage());
            System.exit(0);
        }
    }

    private static void parseDataFromFile(File dataFile) throws FileNotFoundException {
        final Scanner scanner = new Scanner(dataFile);
        try {
            while (scanner.hasNext()) {
                final String line = scanner.nextLine();
                final String[] args = line.split(DATA_FILE_SEPARATOR);
                Task task = decodeTaskFromString(args);
                tasks.add(task);
            }
        } catch (DukeException e) {
            printResponseBlock(MESSAGE_DATA_FILE_PARSE_ERROR + LS + e.getMessage());
            System.exit(0);
        }
    }

    private static Task decodeTaskFromString(String[] args) throws DukeException {
        final String taskTypeIcon = args[0];
        final String statusString = args[1];
        final String description = args[2];
        Task task;
        switch (taskTypeIcon) {
        case Todo.TASK_TYPE_ICON:
            task = new Todo(description);
            break;
        case Event.TASK_TYPE_ICON:
            final String at = args[3];
            task = new Event(description, at);
            break;
        case Deadline.TASK_TYPE_ICON:
            final String by = args[3];
            task = new Deadline(description, by);
            break;
        default:
            throw new DukeException(String.format(MESSAGE_UNRECOGNISED_TASK_TYPE_ICON, taskTypeIcon));
        }
        if (statusString.equals("1")) {
            task.setAsDone();
        } else if (!statusString.equals("0")){
            throw new DukeException(String.format(MESSAGE_UNRECOGNISED_TASK_STATUS_ICON, statusString));
        }
        return task;
    }

    private static void saveData() {
        try {
            FileWriter fw = new FileWriter(DATA_FILE_PATH.toFile());
            fw.write(formatTasksAsDataOutput());
            fw.close();
        } catch (IOException e) {
            printResponseBlock(MESSAGE_DATA_FILE_ACCESS_ERROR + LS + e.getMessage());
            System.exit(0);
        }
    }

    private static String formatTasksAsDataOutput() {
        ArrayList<String> taskDataStrings = new ArrayList<>();
        for (Task task : tasks) {
            taskDataStrings.add(task.toDataString());
        }
        return String.join(LS, taskDataStrings);
    }

    /**
     * Reads input commands from the user.
     * Ignores blank lines and trims input command.
     *
     * @return Trimmed input command.
     */
    private static String getUserInput() {
        String line = SCANNER.nextLine();
        // Ignore blank lines
        while (line.trim().isEmpty()) {
            line = SCANNER.nextLine();
        }
        return line.trim();
    }

    /**
     * Executes the command specified by the input.
     *
     * @param userInput Input command together with any arguments.
     * @return Feedback about what was executed.
     */
    private static String executeCommand(String userInput) {
        final String[] commandAndArgs = userInput.split(" ", 2);
        final String command = commandAndArgs[0];
        final String args = commandAndArgs.length > 1 ? commandAndArgs[1] : "";

        try {
            switch (command) {
            case COMMAND_EXIT:
                printFarewell();
                System.exit(0);
                // fallthrough
            case COMMAND_ADD_TODO:
                return addTodo(args);
            case COMMAND_ADD_DEADLINE:
                return addDeadline(args);
            case COMMAND_ADD_EVENT:
                return addEvent(args);
            case COMMAND_DELETE_TASK:
                return deleteTask(args);
            case COMMAND_LIST_TASKS:
                return listTasks();
            case COMMAND_MARK_TASK_AS_DONE:
                return markTaskAsDone(args);
            default:
                return handleUnrecognisedCommand();
            }
        } catch (DukeException e) {
            return String.format(MESSAGE_ERROR, e.getMessage());
        }
    }

    private static void printFarewell() {
        printResponseBlock(MESSAGE_FAREWELL);
    }

    private static String addTodo(String description) throws DukeException {
        if (description.isEmpty()) {
            throw new DukeException(MESSAGE_TODO_DESCRIPTION_EMPTY);
        }
        Task task = new Todo(description);
        return addTask(task);
    }

    private static String addDeadline(String args) {
        final String[] splitArgs = args.split(" " + DEADLINE_PREFIX_BY + " ");
        final String description = splitArgs[0];
        final String by = splitArgs[1];
        Task task = new Deadline(description, by);
        return addTask(task);
    }

    private static String addEvent(String args) {
        final String[] splitArgs = args.split(" " + EVENT_PREFIX_AT + " ");
        final String description = splitArgs[0];
        final String at = splitArgs[1];
        Task task = new Event(description, at);
        return addTask(task);
    }

    private static String addTask(Task task) {
        tasks.add(task);
        saveData();
        return String.format(MESSAGE_TASK_ADDED, task, tasks.size());
    }

    private static String deleteTask(String args) {
        Task task = getTaskFromStringId(args);
        tasks.remove(task);
        return String.format(MESSAGE_TASK_DELETED, task, tasks.size());
    }

    /** Returns the list of tasks (numbered) together with their status icons */
    private static String listTasks() {
        String[] formattedTasks = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            formattedTasks[i] = String.format("%d.%s", i + 1, tasks.get(i));
        }
        String taskListOutput = String.join(LS, formattedTasks);
        return String.format(MESSAGE_TASK_LIST, taskListOutput);
    }

    private static String markTaskAsDone(String args) {
        Task task = getTaskFromStringId(args);
        task.setAsDone();
        saveData();
        return String.format(MESSAGE_TASK_MARKED_AS_DONE, task);
    }

    private static Task getTaskFromStringId(String args) {
        int taskId = Integer.parseInt(args) - 1;
        return tasks.get(taskId);
    }

    private static String handleUnrecognisedCommand() throws DukeException {
        throw new DukeException(MESSAGE_UNRECOGNISED_COMMAND);
    }
}
