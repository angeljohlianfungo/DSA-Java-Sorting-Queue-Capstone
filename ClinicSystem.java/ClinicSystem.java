import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/*
 * =====================================================================
 *  CLINIC QUEUE AND PATIENT MANAGEMENT SYSTEM
 * =====================================================================
 *  A console program that manages patient records and a consultation
 *  queue at a small clinic.
 *
 *  DESIGN (procedural, NO object-oriented programming):
 *   - One public class that holds only the main method and static methods.
 *   - Patient records are stored in PARALLEL ArrayLists: index i in every
 *     list belongs to the same patient.
 *   - The consultation queue (java.util.Queue backed by LinkedList) stores
 *     patient IDs in First-In, First-Out order.
 *   - Sorting is done with hand-written Insertion, Selection, and Bubble
 *     Sort. They all use swapRecords() so the lists never fall out of sync.
 *   - All input is read with Scanner.nextLine() and validated, so the
 *     program never crashes on bad input.
 * =====================================================================
 */
public class ClinicSystem {

    // ------------------------------------------------------------------
    // SHARED SCANNER: one Scanner for the whole program. Every input goes
    // through readLine(), which uses nextLine() only.
    // ------------------------------------------------------------------
    static Scanner sc = new Scanner(System.in);

    // ------------------------------------------------------------------
    // PARALLEL ARRAYLISTS: index i in EVERY list is the same patient.
    // Any add, remove, update, or swap must touch ALL seven lists.
    // ------------------------------------------------------------------
    static ArrayList<Integer> patientIds = new ArrayList<>();       // unique, auto-generated
    static ArrayList<String> patientNames = new ArrayList<>();
    static ArrayList<Integer> patientAges = new ArrayList<>();
    static ArrayList<String> patientGenders = new ArrayList<>();
    static ArrayList<String> complaints = new ArrayList<>();        // reason for visit
    static ArrayList<String> appointmentTimes = new ArrayList<>();  // "HH:MM" 24-hour
    static ArrayList<String> statuses = new ArrayList<>();          // Registered / Waiting / Done

    // ------------------------------------------------------------------
    // CONSULTATION QUEUE: stores patient IDs (NOT list indexes), because
    // sorting moves records to new indexes but an ID never changes.
    // ------------------------------------------------------------------
    static Queue<Integer> consultationQueue = new LinkedList<>();

    // Next ID to hand out. Starts at 1001 and only ever goes up, so an ID
    // is never reused even after a patient is removed.
    static int nextId = 1001;

    // Status values (plain String constants, not an enum).
    static final String STATUS_REGISTERED = "Registered";
    static final String STATUS_WAITING = "Waiting";
    static final String STATUS_DONE = "Done";

    // =====================================================================
    //  MAIN METHOD: startup prompt, then the main menu loop
    // =====================================================================
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   CLINIC QUEUE AND PATIENT MANAGEMENT SYSTEM");
        System.out.println("==================================================");

        // Startup: sample data or manual input. readYesNo() re-prompts
        // until the user types y, Y, n, or N.
        boolean loadSamples = readYesNo("Load 5 sample patients for demonstration? (Y/N): ");
        if (loadSamples) {
            loadSampleData();
            System.out.println("5 sample patients loaded (IDs 1001-1005).");
        } else {
            System.out.println("Starting with an empty patient list. Use option 1 to register patients.");
        }

        // Main menu loop: keeps running until the user chooses 0 (Exit).
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice (0-10): ", 0, 10);
            System.out.println();

            switch (choice) {
                case 1:
                    registerPatient();
                    break;
                case 2:
                    displayAllPatients();
                    break;
                case 3:
                    searchPatient();
                    break;
                case 4:
                    updatePatient();
                    break;
                case 5:
                    removePatient();
                    break;
                case 6:
                    enqueuePatient();
                    break;
                case 7:
                    dequeuePatient();
                    break;
                case 8:
                    peekNextPatient();
                    break;
                case 9:
                    displayQueue();
                    break;
                case 10:
                    sortMenu();
                    break;
                case 0:
                    running = false;
                    break;
            }
        }

        System.out.println("Thank you for using the Clinic Queue and Patient Management System. Goodbye!");
        sc.close();
    }

    // Prints the main menu with a quick summary of the clinic's current state.
    static void printMainMenu() {
        System.out.println();
        System.out.println("==================== MAIN MENU ====================");
        System.out.println(" Registered patients: " + patientIds.size()
                + "   |   Waiting in queue: " + consultationQueue.size());
        System.out.println("---------------------------------------------------");
        System.out.println("  1. Register Patient");
        System.out.println("  2. Display All Patients");
        System.out.println("  3. Search Patient");
        System.out.println("  4. Update Patient");
        System.out.println("  5. Remove Patient");
        System.out.println("  6. Add Patient to Queue");
        System.out.println("  7. Call Next Patient");
        System.out.println("  8. View Next Patient (Peek)");
        System.out.println("  9. Display Waiting Queue");
        System.out.println(" 10. Sort Patient Records");
        System.out.println("  0. Exit");
        System.out.println("===================================================");
    }

    // =====================================================================
    //  INPUT VALIDATION HELPERS
    //  Every input in the program goes through these methods. They loop
    //  and re-prompt until the input is valid, so bad input never crashes
    //  the program.
    // =====================================================================

    // Reads one raw line. If the input stream is closed (e.g. Ctrl+Z or
    // Ctrl+D), nextLine() would throw an exception, so we exit cleanly
    // instead of crashing.
    static String readLine(String prompt) {
        System.out.print(prompt);
        if (!sc.hasNextLine()) {
            System.out.println();
            System.out.println("Input ended. Closing the program safely.");
            System.exit(0);
        }
        return sc.nextLine();
    }

    // Reads a whole number between min and max (inclusive).
    // Integer.parseInt() throws NumberFormatException for text like "abc"
    // or "12.5"; we catch it and ask again.
    static int readInt(String prompt, int min, int max) {
        while (true) {
            String input = readLine(prompt).trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  [!] Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input. Please enter a whole number.");
            }
        }
    }

    // Reads text that is not empty or only spaces.
    static String readNonEmptyString(String prompt) {
        while (true) {
            String input = readLine(prompt).trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("  [!] This field cannot be empty. Please try again.");
        }
    }

    // Reads a patient name: not empty, and only letters, spaces, periods,
    // hyphens, or apostrophes (e.g. "Ma. Clara O'Neil-Cruz").
    static String readName(String prompt) {
        while (true) {
            String name = readNonEmptyString(prompt);
            boolean valid = true;
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (!Character.isLetter(c) && c != ' ' && c != '.' && c != '-' && c != '\'') {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return name;
            }
            System.out.println("  [!] Name may only contain letters, spaces, periods, hyphens, and apostrophes.");
        }
    }

    // Reads gender as M/F/O (or the full word) and returns a standard value.
    static String readGender(String prompt) {
        while (true) {
            String input = readLine(prompt).trim();
            if (input.equalsIgnoreCase("M") || input.equalsIgnoreCase("Male")) {
                return "Male";
            }
            if (input.equalsIgnoreCase("F") || input.equalsIgnoreCase("Female")) {
                return "Female";
            }
            if (input.equalsIgnoreCase("O") || input.equalsIgnoreCase("Other")) {
                return "Other";
            }
            System.out.println("  [!] Please enter M (Male), F (Female), or O (Other).");
        }
    }

    // Reads an appointment time in strict 24-hour "HH:MM" format.
    static String readTime(String prompt) {
        while (true) {
            String input = readLine(prompt).trim();
            if (isValidTime(input)) {
                return input;
            }
            System.out.println("  [!] Invalid time. Use 24-hour HH:MM format from 00:00 to 23:59 (e.g. 09:30, 14:05).");
        }
    }

    // Returns true only if the text looks exactly like "HH:MM" with
    // hours 00-23 and minutes 00-59.
    static boolean isValidTime(String time) {
        // Must be exactly 5 characters with a colon in the middle: H H : M M
        if (time.length() != 5 || time.charAt(2) != ':') {
            return false;
        }
        // The other 4 characters must all be digits (this also rejects "+1:30").
        int[] digitPositions = {0, 1, 3, 4};
        for (int pos : digitPositions) {
            if (!Character.isDigit(time.charAt(pos))) {
                return false;
            }
        }
        try {
            int hours = Integer.parseInt(time.substring(0, 2));
            int minutes = Integer.parseInt(time.substring(3, 5));
            return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Reads a Y/N answer. Accepts only y, Y, n, or N; re-prompts otherwise.
    static boolean readYesNo(String prompt) {
        while (true) {
            String input = readLine(prompt).trim();
            if (input.equalsIgnoreCase("Y")) {
                return true;
            }
            if (input.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("  [!] Please enter Y or N only.");
        }
    }

    // Reads a patient ID that must exist in the records. Re-prompts on an
    // unknown ID. Returns -1 if the user enters 0 to cancel.
    static int readExistingId(String prompt) {
        while (true) {
            int id = readInt(prompt + " (0 to cancel): ", 0, Integer.MAX_VALUE);
            if (id == 0) {
                return -1;
            }
            if (findIndexById(id) != -1) {
                return id;
            }
            System.out.println("  [!] No patient found with ID " + id + ". Please try again.");
        }
    }

    // =====================================================================
    //  PARALLEL LIST HELPERS
    //  These are the ONLY methods that change the structure of the lists,
    //  so keeping them correct keeps every list in sync.
    // =====================================================================

    // Adds one patient by appending to the END of all seven lists, so the
    // new values all land on the same index. Returns the generated ID.
    static int addPatient(String name, int age, String gender, String complaint, String time) {
        int id = nextId;
        nextId++;
        patientIds.add(id);
        patientNames.add(name);
        patientAges.add(age);
        patientGenders.add(gender);
        complaints.add(complaint);
        appointmentTimes.add(time);
        statuses.add(STATUS_REGISTERED);
        return id;
    }

    // Removes the patient at the given index from ALL seven lists.
    // Every list shifts the later elements left by one in the same way,
    // so the remaining patients stay aligned.
    static void removeRecordAt(int index) {
        patientIds.remove(index);
        patientNames.remove(index);
        patientAges.remove(index);
        patientGenders.remove(index);
        complaints.remove(index);
        appointmentTimes.remove(index);
        statuses.remove(index);
    }

    // Swaps the patients at positions i and j in ALL seven lists at once.
    // Every sorting algorithm uses this, so a patient's name, age, time,
    // etc. always move together and never get mixed with someone else's.
    static void swapRecords(int i, int j) {
        if (i == j) {
            return;
        }
        Integer tempId = patientIds.get(i);
        patientIds.set(i, patientIds.get(j));
        patientIds.set(j, tempId);

        String tempName = patientNames.get(i);
        patientNames.set(i, patientNames.get(j));
        patientNames.set(j, tempName);

        Integer tempAge = patientAges.get(i);
        patientAges.set(i, patientAges.get(j));
        patientAges.set(j, tempAge);

        String tempGender = patientGenders.get(i);
        patientGenders.set(i, patientGenders.get(j));
        patientGenders.set(j, tempGender);

        String tempComplaint = complaints.get(i);
        complaints.set(i, complaints.get(j));
        complaints.set(j, tempComplaint);

        String tempTime = appointmentTimes.get(i);
        appointmentTimes.set(i, appointmentTimes.get(j));
        appointmentTimes.set(j, tempTime);

        String tempStatus = statuses.get(i);
        statuses.set(i, statuses.get(j));
        statuses.set(j, tempStatus);
    }

    // Linear search for a patient's current index by ID.
    // Returns -1 if no patient has that ID.
    // (Needed because sorting changes indexes, but IDs never change.)
    static int findIndexById(int id) {
        for (int i = 0; i < patientIds.size(); i++) {
            if (patientIds.get(i) == id) { // unboxes Integer to int, so == compares values
                return i;
            }
        }
        return -1;
    }

    // Converts a valid "HH:MM" string to minutes since midnight, e.g.
    // "09:45" -> 585. Used to compare appointment times as numbers.
    static int timeToMinutes(String time) {
        int hours = Integer.parseInt(time.substring(0, 2));
        int minutes = Integer.parseInt(time.substring(3, 5));
        return hours * 60 + minutes;
    }

    // Preloads 5 sample patients. They are deliberately NOT in order by
    // name, age, or time, so every sort visibly changes the table.
    static void loadSampleData() {
        addPatient("Maria Santos", 34, "Female", "Fever and cough", "10:30");
        addPatient("Juan Dela Cruz", 67, "Male", "High blood pressure", "08:15");
        addPatient("Angela Reyes", 8, "Female", "Ear pain", "14:00");
        addPatient("Carlo Mendoza", 22, "Male", "Sprained ankle", "09:45");
        addPatient("Bea Villanueva", 45, "Female", "Migraine", "11:20");
    }

    // =====================================================================
    //  TABLE DISPLAY HELPERS (aligned with printf)
    // =====================================================================

    // Shortens text that is too long for its column so the table stays aligned.
    static String fit(String text, int width) {
        if (text.length() <= width) {
            return text;
        }
        return text.substring(0, width - 3) + "...";
    }

    // Builds a string of the same character repeated n times.
    static String repeat(char c, int n) {
        String result = "";
        for (int i = 0; i < n; i++) {
            result += c;
        }
        return result;
    }

    // Horizontal border line that matches the column widths below.
    static void printDivider() {
        System.out.println("+" + repeat('-', 8) + "+" + repeat('-', 22) + "+" + repeat('-', 5)
                + "+" + repeat('-', 8) + "+" + repeat('-', 24) + "+" + repeat('-', 7)
                + "+" + repeat('-', 12) + "+");
    }

    static void printTableHeader() {
        printDivider();
        System.out.printf("| %-6s | %-20s | %3s | %-6s | %-22s | %-5s | %-10s |%n",
                "ID", "Name", "Age", "Gender", "Complaint", "Time", "Status");
        printDivider();
    }

    // Prints one patient row. Reading index i from every list gives one
    // complete patient record.
    static void printPatientRow(int i) {
        System.out.printf("| %-6d | %-20s | %3d | %-6s | %-22s | %-5s | %-10s |%n",
                patientIds.get(i),
                fit(patientNames.get(i), 20),
                patientAges.get(i),
                patientGenders.get(i),
                fit(complaints.get(i), 22),
                appointmentTimes.get(i),
                statuses.get(i));
    }

    // Prints every patient in current list order (used by Display and Sort).
    static void printAllRecordsTable() {
        printTableHeader();
        for (int i = 0; i < patientIds.size(); i++) {
            printPatientRow(i);
        }
        printDivider();
    }

    // Prints one patient's full details vertically (used by Dequeue/Peek).
    static void printPatientDetails(int i) {
        System.out.println("  Patient ID   : " + patientIds.get(i));
        System.out.println("  Name         : " + patientNames.get(i));
        System.out.println("  Age          : " + patientAges.get(i));
        System.out.println("  Gender       : " + patientGenders.get(i));
        System.out.println("  Complaint    : " + complaints.get(i));
        System.out.println("  Appointment  : " + appointmentTimes.get(i));
        System.out.println("  Status       : " + statuses.get(i));
    }

    // =====================================================================
    //  RECORD MANAGEMENT
    // =====================================================================

    // 1. Register a new patient. The user types every field except the ID,
    //    which is generated automatically by addPatient().
    static void registerPatient() {
        System.out.println("--- REGISTER NEW PATIENT ---");
        String name = readName("Full name: ");
        int age = readInt("Age (0-120): ", 0, 120);
        String gender = readGender("Gender (M/F/O): ");
        String complaint = readNonEmptyString("Complaint / reason for visit: ");
        String time = readTime("Appointment time (HH:MM, 24-hour): ");

        int id = addPatient(name, age, gender, complaint, time);
        System.out.println("Patient registered successfully! Assigned ID: " + id);
        System.out.println("Tip: use option 6 to add this patient to the waiting queue.");
    }

    // 2. Display all patients in an aligned table.
    static void displayAllPatients() {
        if (patientIds.isEmpty()) {
            System.out.println("No patients registered.");
            return;
        }
        System.out.println("--- ALL PATIENT RECORDS (" + patientIds.size() + ") ---");
        printAllRecordsTable();
    }

    // 3. Search sub-menu: by exact ID or by partial name.
    static void searchPatient() {
        if (patientIds.isEmpty()) {
            System.out.println("No patients registered.");
            return;
        }
        System.out.println("--- SEARCH PATIENT ---");
        System.out.println("  1. Search by ID");
        System.out.println("  2. Search by Name (partial, case-insensitive)");
        System.out.println("  0. Back");
        int choice = readInt("Enter your choice (0-2): ", 0, 2);

        if (choice == 1) {
            searchById();
        } else if (choice == 2) {
            searchByName();
        }
    }

    // Finds exactly one patient by ID using findIndexById().
    static void searchById() {
        int id = readInt("Enter patient ID: ", 0, Integer.MAX_VALUE);
        int index = findIndexById(id);
        if (index == -1) {
            System.out.println("No patient found with ID " + id + ".");
            return;
        }
        printTableHeader();
        printPatientRow(index);
        printDivider();
    }

    // Shows every patient whose name CONTAINS the keyword, ignoring case.
    // Example: "rey" matches "Angela Reyes".
    static void searchByName() {
        String keyword = readNonEmptyString("Enter name or part of name: ").toLowerCase();
        int matches = 0;
        for (int i = 0; i < patientNames.size(); i++) {
            if (patientNames.get(i).toLowerCase().contains(keyword)) {
                if (matches == 0) {
                    printTableHeader(); // print the header only once, before the first match
                }
                printPatientRow(i);
                matches++;
            }
        }
        if (matches == 0) {
            System.out.println("No patients found matching \"" + keyword + "\".");
        } else {
            printDivider();
            System.out.println(matches + " patient(s) found.");
        }
    }

    // 4. Update a patient's complaint or appointment time.
    //    Only ONE list changes, at the patient's index, so nothing shifts
    //    and the lists stay in sync.
    static void updatePatient() {
        if (patientIds.isEmpty()) {
            System.out.println("No patients registered.");
            return;
        }
        System.out.println("--- UPDATE PATIENT ---");
        int id = readExistingId("Enter the ID of the patient to update");
        if (id == -1) {
            System.out.println("Update cancelled.");
            return;
        }
        int index = findIndexById(id);
        printTableHeader();
        printPatientRow(index);
        printDivider();

        System.out.println("  1. Update complaint");
        System.out.println("  2. Update appointment time");
        System.out.println("  0. Cancel");
        int choice = readInt("What do you want to update? (0-2): ", 0, 2);

        if (choice == 1) {
            String newComplaint = readNonEmptyString("New complaint: ");
            complaints.set(index, newComplaint);
            System.out.println("Complaint updated for patient " + id + ".");
        } else if (choice == 2) {
            String newTime = readTime("New appointment time (HH:MM, 24-hour): ");
            appointmentTimes.set(index, newTime);
            System.out.println("Appointment time updated for patient " + id + ".");
        } else {
            System.out.println("Update cancelled.");
        }
    }

    // 5. Remove a patient from the records AND from the queue if waiting.
    //    If we left the ID in the queue, calling that patient later would
    //    point to a record that no longer exists.
    static void removePatient() {
        if (patientIds.isEmpty()) {
            System.out.println("No patients registered.");
            return;
        }
        System.out.println("--- REMOVE PATIENT ---");
        int id = readExistingId("Enter the ID of the patient to remove");
        if (id == -1) {
            System.out.println("Removal cancelled.");
            return;
        }
        int index = findIndexById(id);
        printTableHeader();
        printPatientRow(index);
        printDivider();

        if (!readYesNo("Are you sure you want to remove this patient? (Y/N): ")) {
            System.out.println("Removal cancelled.");
            return;
        }

        // Integer.valueOf(id) makes remove() delete the ID VALUE from the
        // queue. It returns true if the ID was found and removed.
        boolean wasInQueue = consultationQueue.remove(Integer.valueOf(id));
        removeRecordAt(index);

        System.out.println("Patient " + id + " has been removed from the records.");
        if (wasInQueue) {
            System.out.println("Patient " + id + " was also removed from the waiting queue.");
        }
    }

    // =====================================================================
    //  QUEUE OPERATIONS (FIFO: first patient added is the first called)
    // =====================================================================

    // isEmpty check used by every queue operation that needs a patient.
    // Prints a clear message when the queue is empty so the caller can
    // simply stop.
    static boolean isQueueEmpty() {
        if (consultationQueue.isEmpty()) {
            System.out.println("No patients are waiting.");
            return true;
        }
        return false;
    }

    // 6. ENQUEUE: add a registered patient's ID to the BACK of the queue.
    static void enqueuePatient() {
        if (patientIds.isEmpty()) {
            System.out.println("No patients registered. Please register a patient first.");
            return;
        }
        System.out.println("--- ADD PATIENT TO QUEUE ---");

        // Show only the patients who can be queued (not already waiting).
        int eligible = 0;
        for (int i = 0; i < patientIds.size(); i++) {
            if (!statuses.get(i).equals(STATUS_WAITING)) {
                if (eligible == 0) {
                    System.out.println("Patients not yet in the queue:");
                    printTableHeader();
                }
                printPatientRow(i);
                eligible++;
            }
        }
        if (eligible == 0) {
            System.out.println("All registered patients are already waiting in the queue.");
            return;
        }
        printDivider();

        // readExistingId() rejects IDs that don't exist and asks again.
        int id = readExistingId("Enter the ID of the patient to add to the queue");
        if (id == -1) {
            System.out.println("Cancelled. No patient was added to the queue.");
            return;
        }

        // Reject duplicates: the same patient cannot wait twice.
        if (consultationQueue.contains(id)) {
            System.out.println("Rejected: patient " + id + " is already waiting in the queue.");
            return;
        }

        consultationQueue.offer(id);                       // add to the back of the queue
        statuses.set(findIndexById(id), STATUS_WAITING);   // update the record's status
        System.out.println("Patient " + id + " added to the queue at position "
                + consultationQueue.size() + ".");
    }

    // 7. DEQUEUE: remove the patient at the FRONT of the queue, show their
    //    details, and mark them as Done.
    static void dequeuePatient() {
        System.out.println("--- CALL NEXT PATIENT ---");
        if (isQueueEmpty()) {
            return;
        }
        int id = consultationQueue.poll();   // removes and returns the front ID
        int index = findIndexById(id);       // find where the record is NOW
        if (index == -1) {
            // Should never happen, because removePatient() also cleans the queue.
            System.out.println("Record for patient " + id + " was not found. Skipped.");
            return;
        }
        statuses.set(index, STATUS_DONE);
        System.out.println(">>> NOW CALLING: " + patientNames.get(index) + " (ID " + id + ")");
        System.out.println("    Please proceed to the consultation room.");
        printPatientDetails(index);
        System.out.println("Patients still waiting: " + consultationQueue.size());
    }

    // 8. PEEK: show who is next WITHOUT removing them from the queue.
    static void peekNextPatient() {
        System.out.println("--- NEXT PATIENT (PEEK) ---");
        if (isQueueEmpty()) {
            return;
        }
        int id = consultationQueue.peek();   // looks at the front ID, does not remove it
        int index = findIndexById(id);
        if (index == -1) {
            System.out.println("Record for patient " + id + " was not found.");
            return;
        }
        System.out.println("Next in line:");
        printPatientDetails(index);
        System.out.println("Total waiting: " + consultationQueue.size());
    }

    // 9. DISPLAY QUEUE: list all waiting patients from front (position 1)
    //    to back. The for-each loop reads the queue without removing anything.
    static void displayQueue() {
        System.out.println("--- WAITING QUEUE (front to back) ---");
        if (isQueueEmpty()) {
            return;
        }
        String line = "+" + repeat('-', 10) + "+" + repeat('-', 8) + "+" + repeat('-', 22)
                + "+" + repeat('-', 5) + "+" + repeat('-', 24) + "+" + repeat('-', 7) + "+";
        System.out.println(line);
        System.out.printf("| %-8s | %-6s | %-20s | %3s | %-22s | %-5s |%n",
                "Position", "ID", "Name", "Age", "Complaint", "Time");
        System.out.println(line);

        int position = 1;
        for (int id : consultationQueue) {
            int index = findIndexById(id);
            if (index != -1) {
                System.out.printf("| %-8d | %-6d | %-20s | %3d | %-22s | %-5s |%n",
                        position, id,
                        fit(patientNames.get(index), 20),
                        patientAges.get(index),
                        fit(complaints.get(index), 22),
                        appointmentTimes.get(index));
            }
            position++;
        }
        System.out.println(line);
        System.out.println("Total waiting: " + consultationQueue.size());
    }

    // =====================================================================
    //  SORTING
    // =====================================================================

    // 10. Sorting sub-menu. Loops so the user can try several sorts, then
    //     returns to the main menu on 0.
    static void sortMenu() {
        boolean inSortMenu = true;
        while (inSortMenu) {
            System.out.println();
            System.out.println("------------- SORT PATIENT RECORDS -------------");
            System.out.println("  1. Insertion Sort - by Name (A-Z)");
            System.out.println("  2. Selection Sort - by Age (youngest to oldest)");
            System.out.println("  3. Bubble Sort    - by Appointment Time (earliest first)");
            System.out.println("  0. Back to Main Menu");
            System.out.println("------------------------------------------------");
            int choice = readInt("Enter your choice (0-3): ", 0, 3);
            System.out.println();

            if (choice == 0) {
                inSortMenu = false;
            } else {
                runSort(choice);
            }
        }
    }

    // Shows the table BEFORE, runs the chosen sort, then shows it AFTER.
    static void runSort(int choice) {
        // A list with 0 or 1 patients is already "sorted".
        if (patientIds.size() < 2) {
            System.out.println("There is nothing to sort. At least 2 patients are needed (currently "
                    + patientIds.size() + ").");
            return;
        }

        String description;
        if (choice == 1) {
            description = "INSERTION SORT by Name (A-Z)";
        } else if (choice == 2) {
            description = "SELECTION SORT by Age (youngest to oldest)";
        } else {
            description = "BUBBLE SORT by Appointment Time (earliest first)";
        }

        System.out.println("=== " + description + " ===");
        System.out.println("BEFORE sorting:");
        printAllRecordsTable();

        if (choice == 1) {
            insertionSortByName();
        } else if (choice == 2) {
            selectionSortByAge();
        } else {
            bubbleSortByTime();
        }

        System.out.println("AFTER sorting:");
        printAllRecordsTable();
        System.out.println("Note: the waiting queue order is unchanged, because it stores patient IDs, not indexes.");
    }

    /*
     * INSERTION SORT - by patient name (A-Z, case-insensitive)
     * ---------------------------------------------------------
     * How it works:
     *   The list is split into a SORTED left part and an UNSORTED right
     *   part. At the start, only index 0 is "sorted". On each pass we take
     *   the first unsorted record (index i) and slide it LEFT, swapping it
     *   with its left neighbour, as long as that neighbour's name comes
     *   later in the alphabet. When it stops, it is in its correct place
     *   inside the sorted part, which has grown by one.
     *
     * Time complexity:
     *   Best case    O(n)   - list already sorted: each record is compared
     *                         once and never moves.
     *   Average/Worst O(n^2) - list in random or reverse order.
     *   Extra space  O(1)   - sorts in place. It is also STABLE: patients
     *                         with the same name keep their relative order.
     *
     * Why it suits this use case:
     *   A small clinic's daily roster is short, and new patients are added
     *   at the end of a list that is usually already mostly sorted. Insertion
     *   sort is fast for small, nearly sorted lists because the new records
     *   only slide a few positions to the left.
     *
     * compareToIgnoreCase() returns a positive number when the first name
     * comes AFTER the second alphabetically (ignoring upper/lower case).
     */
    static void insertionSortByName() {
        int n = patientNames.size();
        for (int i = 1; i < n; i++) {
            int j = i; // position of the record we are inserting
            while (j > 0 && patientNames.get(j - 1).compareToIgnoreCase(patientNames.get(j)) > 0) {
                swapRecords(j - 1, j); // move the WHOLE record one step left
                j--;
            }
        }
    }

    /*
     * SELECTION SORT - by age (youngest to oldest)
     * ---------------------------------------------
     * How it works:
     *   For each position i from left to right, scan the UNSORTED part
     *   (i to the end) to find the youngest patient, then swap that record
     *   into position i. After pass i, positions 0..i hold the youngest
     *   patients in order and never change again.
     *
     * Time complexity:
     *   Best/Average/Worst O(n^2) comparisons - it always scans the whole
     *   unsorted part, even if the list is already sorted.
     *   Swaps: at most n - 1 (one per pass), the fewest of the three sorts.
     *   Extra space O(1). It is NOT stable (equal ages may change order).
     *
     * Why it suits this use case:
     *   Each swap here moves a record across SEVEN parallel lists, so swaps
     *   are the "expensive" step. Selection sort makes at most one swap per
     *   pass, which keeps that work low. With a small number of patients,
     *   the O(n^2) comparisons are not a problem, and the logic is simple
     *   to verify (useful for prioritizing children or seniors by age).
     */
    static void selectionSortByAge() {
        int n = patientAges.size();
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i; // assume the first unsorted patient is the youngest
            for (int j = i + 1; j < n; j++) {
                if (patientAges.get(j) < patientAges.get(minIndex)) {
                    minIndex = j; // found a younger patient
                }
            }
            if (minIndex != i) {
                swapRecords(i, minIndex); // one swap per pass
            }
        }
    }

    /*
     * BUBBLE SORT - by appointment time (earliest first)  [BONUS]
     * ------------------------------------------------------------
     * How it works:
     *   Walk through the list comparing each pair of NEIGHBOURS. If the
     *   left one has a later time than the right one, swap them. After one
     *   full pass, the latest appointment has "bubbled" to the end. Each
     *   next pass can stop one position earlier. If a whole pass makes no
     *   swaps, the list is already sorted, so we stop early.
     *
     * Time complexity:
     *   Best case    O(n)   - already sorted: one pass, no swaps, stop.
     *   Average/Worst O(n^2).
     *   Extra space  O(1). It is STABLE.
     *
     * Why it suits this use case:
     *   Appointment times are usually entered roughly in order, so the
     *   early-exit check often finishes in one or two passes. It is the
     *   easiest algorithm to trace by hand during a demonstration.
     *
     * Times are converted to minutes since midnight (timeToMinutes) so they
     * are compared as numbers, e.g. "09:45" = 585 and "10:30" = 630.
     */
    static void bubbleSortByTime() {
        int n = appointmentTimes.size();
        for (int pass = 0; pass < n - 1; pass++) {
            boolean swapped = false;
            // The last "pass" positions are already in their final place.
            for (int j = 0; j < n - 1 - pass; j++) {
                if (timeToMinutes(appointmentTimes.get(j)) > timeToMinutes(appointmentTimes.get(j + 1))) {
                    swapRecords(j, j + 1);
                    swapped = true;
                }
            }
            if (!swapped) {
                break; // no swaps means the list is sorted
            }
        }
    }
}
