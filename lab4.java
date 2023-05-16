import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

public class lab4 {

    public static int pc = 0;
    public static int branch = 0;
    public static int load = 0;

    public static int checkStall = 0;
    public static int cycles = 0;
    public static Boolean quit = false;

    public static int execute = 0;
    public static int pipelinePC = 0;
    public static HashMap<String, Integer> registers = new HashMap<String, Integer>();
    public static int[] data_memory = new int[8192];
    public static HashMap<String, String> instructType = new HashMap<String, String>();
    public static ArrayList<String> instructions = new ArrayList<String>();
    public static HashMap<String, Integer> labels = new HashMap<String, Integer>();
    public static ArrayList<Instruction> instructionObject = new ArrayList<Instruction>();

    public static LinkedList<String> pipeline = new LinkedList<String>();


    public static String[][] registerNames = { { "$0", "$v0", "$v1", "$a0" },
            { "$a1", "$a2", "$a3", "$t0" },
            { "$t1", "$t2", "$t3", "$t4" },
            { "$t5", "$t6", "$t7", "$s0" },
            { "$s1", "$s2", "$s3", "$s4" },
            { "$s5", "$s6", "$s7", "$t8" },
            { "$t9", "$sp", "$ra" } };

    public static void printRegisters() {
        System.out.println("pc = " + pc);

        for (int i = 0; i < registerNames.length; i++) {
            String line = "";
            for (int j = 0; j < registerNames[i].length; j++) {
                String reg_str = registerNames[i][j] + " = " + registers.get(registerNames[i][j]);
                line += String.format("%-15s", reg_str);
            }
            System.out.println(line);
        }
    }

    public static void fillRegister(HashMap<String, Integer> inputMap) {
        inputMap.put("$zero", 0);
        inputMap.put("$0", 0);
        inputMap.put("$v0", 0);
        inputMap.put("$v1", 0);
        inputMap.put("$a0", 0);
        inputMap.put("$a1", 0);
        inputMap.put("$a2", 0);
        inputMap.put("$a3", 0);
        inputMap.put("$t0", 0);
        inputMap.put("$t1", 0);
        inputMap.put("$t2", 0);
        inputMap.put("$t3", 0);
        inputMap.put("$t4", 0);
        inputMap.put("$t5", 0);
        inputMap.put("$t6", 0);
        inputMap.put("$t7", 0);
        inputMap.put("$t8", 0);
        inputMap.put("$t9", 0);
        inputMap.put("$s0", 0);
        inputMap.put("$s1", 0);
        inputMap.put("$s2", 0);
        inputMap.put("$s3", 0);
        inputMap.put("$s4", 0);
        inputMap.put("$s5", 0);
        inputMap.put("$s6", 0);
        inputMap.put("$s7", 0);
        inputMap.put("$ra", 0);
        inputMap.put("$sp", 0);

    }

    public static void fillType(HashMap<String, String> inputMap) {
        inputMap.put("and", "r");
        inputMap.put("or", "r");
        inputMap.put("add", "r");
        inputMap.put("addi", "i");
        inputMap.put("sll", "r");
        inputMap.put("sub", "r");
        inputMap.put("slt", "r");
        inputMap.put("beq", "i");
        inputMap.put("bne", "i");
        inputMap.put("lw", "i");
        inputMap.put("sw", "i");
        inputMap.put("j", "j");
        inputMap.put("jr", "r");
        inputMap.put("jal", "j");

    }

    public static void parseASM(Scanner scanner) {
        fillRegister(registers);
        fillType(instructType);

        /* first pass to find labels and get instructions */
        int count = 0;
        while (scanner.hasNext()) {
            String line = scanner.nextLine().trim();

            // skip if empty line or comment
            if (line.length() == 0 || line.charAt(0) == '#')
                continue;

            // remove comment from line
            if (line.contains("#")) {
                line = line.substring(0, line.indexOf("#")).trim();
            }

            // add to label and instruction list
            if (line.contains(":")) {
                String[] split_label = line.split(":");
                labels.put(split_label[0], count);
                if (split_label.length == 1)
                    continue;
                else
                    instructions.add(split_label[1].trim());
            } else {
                instructions.add(line);
            }
            count++;
        }
        scanner.close();

        for (int i = 0; i < instructions.size(); i++) {

            String first = instructions.get(i).replaceAll("[()]", "");
            String before = first.replace("$", " $");
            String splity = before.replace(",", " ");
            String after = splity.trim().replaceAll(" +", " ");
            String[] split_instruct = after.split(" ");

            String op = split_instruct[0].replaceAll("\\s", "");

            String val1 = split_instruct[1];

            String type = instructType.get(op);

            if (type == "r") {
                if (op.equals("jr")) {
                    instructionObject.add(new Instruction("r", op, val1, "n/a", "n/a"));
                } else if (op.equals("sll")) {
                    String val2 = split_instruct[2];
                    String val3 = split_instruct[3];
                    instructionObject.add(new Instruction("r", op, val2, val3, val1));
                } else {
                    String val2 = split_instruct[2];
                    String val3 = split_instruct[3];
                    instructionObject.add(new Instruction("r", op, val2, val3, val1));
                }

            } else if (type == "i") {

                String val2 = split_instruct[2];
                String val3 = split_instruct[3];

                if (op.equals("lw") || op.equals("sw")) {
                    int imd = Integer.parseInt(val2);
                    instructionObject.add(
                            new Instruction("i", op, val3, val1, imd));

                } else if (op.equals("bne") || op.equals("beq")) {
                    int imd = labels.get(val3);
                    instructionObject.add(
                            new Instruction("i", op, val2, val1, imd));
                } else {
                    int imd = Integer.parseInt(val3);
                    instructionObject.add(
                            new Instruction("i", op, val2, val1, imd));
                }
            } else if (type == "j") {
                int imd = Integer.parseInt(Integer.toString(labels.get(val1)));
                instructionObject.add(new Instruction("j", op, imd));
            } else {
                instructionObject.add(new Instruction("invalid", op));
                break;
            }
        }

    }

    public static void handleInstruct(Instruction instruct) {

        String op = instruct.opcode;
        int temp;
        switch (op) {
            case "and":
                temp = (registers.get(instruct.rs) & registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "or":
                temp = (registers.get(instruct.rs) | registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "add":
                temp = (registers.get(instruct.rs) + registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "addi":
                temp = (registers.get(instruct.rs) + instruct.immediate);
                registers.put(instruct.rt, temp);
                pc++;
                break;
            case "sll":
                temp = (registers.get(instruct.rs) << Integer.parseInt(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "sub":
                temp = (registers.get(instruct.rs) - registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "slt":
                if (registers.get(instruct.rs) < registers.get(instruct.rt)) {
                    temp = 1;
                } else {
                    temp = 0;
                }
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "beq":
                if (registers.get(instruct.rt) == registers.get(instruct.rs)) {
                    branch = 1;
                    pc = instruct.immediate;
                } else {
                    pc++;
                }
                break;
            case "bne":
                if (registers.get(instruct.rt) != registers.get(instruct.rs)) {
                    branch = 1;
                    pc = instruct.immediate;
                } else {
                    pc++;
                }
                break;
            case "lw":
                int mem = data_memory[instruct.immediate + registers.get(instruct.rs)];
                load = 1;
                registers.put(instruct.rt, mem);
                pc++;
                break;
            case "sw":
                data_memory[instruct.immediate + registers.get(instruct.rs)] = registers.get(instruct.rt);
                pc++;
                break;
            case "j":
                pc = instruct.address;
                break;
            case "jal":
                registers.put("$ra", pc + 1);
                pc = instruct.address;
                break;
            case "jr":
                pc = registers.get(instruct.rs);
                break;
            default:
                System.exit(0);

        }

    }

    public static void completeOperation() {

        Instruction instruct;

        while (pc < instructionObject.size()) {

            //pipelinePC ++;
            System.out.println("\nPC: " + pc + " SIZE: " + instructionObject.size() + " " + instructionObject.get(pc).opcode + "\n");

            addToPipeline(instructionObject.get(pc).opcode);

            handleInstruct(instructionObject.get(pc));

            System.out.println("HERE");

//            if(pipeline.get(1).equals("lw")){
//                if(!(instructionObject.get(pc - 1).opcode.equals("lw")) && instructionObject.get(pc).rt.equals(instructionObject.get(pc - 1).rs)
//                        || !(instructionObject.get(pc - 1).opcode.equals("lw")) && instructionObject.get(pc).rt.equals(instructionObject.get(pc - 1).rt)
//                        || (instructionObject.get(pc - 1).opcode.equals("lw") && instructionObject.get(pc).rt.equals(instructionObject.get(pc - 1).rs))){
//                    execute = 1;
//                }
//            }

            String str = "";
            str += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
            str += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
            System.out.println(str);

            if(pipeline.get(1).equals("lw")) {
                System.out.println("PC -2: " + instructionObject.get(pc - 2).opcode);
                System.out.println("PC -1: " + instructionObject.get(pc - 1).opcode);


                if (!(instructionObject.get(pc - 1).opcode.equals("lw")) && instructionObject.get(pc - 1).rt.equals(instructionObject.get(pc - 2).rs)
                        || !(instructionObject.get(pc - 1).opcode.equals("lw")) && instructionObject.get(pc - 1).rt.equals(instructionObject.get(pc - 2).rt)
                        || (instructionObject.get(pc - 2).opcode.equals("lw") && instructionObject.get(pc - 1).rt.equals(instructionObject.get(pc - 2).rs))) {
                    execute = 1;
                    System.out.println("\nI Stalled!\n");
                }

                if ((instructionObject.get(pc - 2).opcode.equals("lw") && (instructionObject.get(pc - 2).rt.equals("$0")))) {
                    System.out.println("I got here");
                    execute = 0;
                }
            }

            if(load == 1 && execute == 1){

                pipeline.add(1, "stall");
                pipeline.removeLast();
                cycles ++;
                checkStall ++;
                load = 0;
                execute = 0;

                String str2 = "";
                str2 += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
                str2 += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
                System.out.println(str2);

            }

            System.out.println("HERE I AM  " + pc);
            if(pc == instructionObject.size()){
                System.out.println("HERE I AM ");
                String str3 = "";
                str3 += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
                str3 += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
                System.out.println(str3);
                addToPipeline("empty");
                addToPipeline("empty");
                addToPipeline("empty");
                addToPipeline("empty");
                //cycles += 3;

                str3 = "";
                str3 += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
                str3 += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
                System.out.println(str3);
            }

//            String str = "";
//            str += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
//            str += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
//            System.out.println(str);

        }

        String str = "";
        str += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
        str += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
        System.out.println(str);

        System.out.println("Program Complete");
        System.out.println("CHECK STALL: " + checkStall);
        System.out.println("CPI = " + ((double)cycles/instructionObject.size()) + " Cycles = " + cycles + " Instructions = " + instructionObject.size());

        //quit = true;

    }

    public static void addToPipeline(String str){
        pipeline.addFirst(str);
        pipelinePC ++;
        cycles ++;
        if(pipeline.size() > 4 ){
            pipeline.removeLast();
        }
    }

    public static void handleCommand(String[] input) {
        String command = input[0];
        if (command.equals("q")) {
            System.exit(0);

        } else if (command.equals("h")) {
            String help_msg = "h = show help\n";
            help_msg += "d = dump register state\n";
            help_msg += "s = single step through the program (i.e. execute 1 instruction and stop)\n";
            help_msg += "s num = step through num instructions of the program\n";
            help_msg += "r = run until the program ends\n";
            help_msg += "m num1 num2 = display data memory from location num1 to num2\n";
            help_msg += "c = clear all registers, memory, and the program counter to 0\n";
            help_msg += "q = exit the program";
            System.out.println(help_msg);

        } else if (command.equals("d")) {
            System.out.println("");
            printRegisters();

        } else if (command.equals("s")) {
            if (input.length == 2) {
                String count = input[1];
                //System.out.printf("%9s instruction(s) executed\n", count);
                for (int i = 0; i < Integer.parseInt(count); i++) {
                    handleInstruct(instructionObject.get(pc));
                    addToPipeline(instructionObject.get(pc).opcode);
                    //System.out.println(pipeline);
                    //System.out.println("BRANCH: " + pipeline.getLast());
                    if(branch == 1 && pipeline.getLast().equals("bne")){
                        pipeline.set(0, "squash");
                        pipeline.set(1, "squash");
                        pipeline.set(2, "squash");
                        System.out.println(pipeline);
                    }
                }
            } else if (input.length == 1) {

                addToPipeline(instructionObject.get(pc).opcode);
                handleInstruct(instructionObject.get(pc));

                if(branch == 1 && pipeline.getLast().equals("bne")){
                    pipeline.set(0, "squash");
                    pipeline.set(1, "squash");
                    pipeline.set(2, "squash");

                }



                if(pipeline.get(1).equals("lw")) {
                    System.out.println("PC -2: " + instructionObject.get(pc - 2).opcode);
                    System.out.println("PC -1: " + instructionObject.get(pc - 1).opcode);


                    if (!(instructionObject.get(pc - 1).opcode.equals("lw")) && instructionObject.get(pc - 1).rt.equals(instructionObject.get(pc - 2).rs)
                            || !(instructionObject.get(pc - 1).opcode.equals("lw")) && instructionObject.get(pc - 1).rt.equals(instructionObject.get(pc - 2).rt)
                            || (instructionObject.get(pc - 2).opcode.equals("lw") && instructionObject.get(pc - 1).rt.equals(instructionObject.get(pc - 2).rs))) {
                        execute = 1;
                        System.out.println("\nI Stalled!\n");
                    }

                    if ((instructionObject.get(pc - 2).opcode.equals("lw") && (instructionObject.get(pc - 2).rt.equals("$0")))) {
                        System.out.println("I got here");
                        execute = 0;
                    }
                }

                if(load == 1 && execute == 1){

                    pipeline.add(1, "stall");
                    pipeline.removeLast();
                    cycles ++;
                    checkStall ++;
                    load = 0;
                    execute = 0;

                    String str2 = "";
                    str2 += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
                    str2 += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
                    System.out.println(str2);

                }

                System.out.print("\nmips> s");
                String str = "";
                str += "PC   " + "if/id   " +"id/exe   "+"exe/mem   "+"mem/wb   \n";
                str += pipelinePC + "    " + pipeline.get(0) + "    " + pipeline.get(1) + "    " + pipeline.get(2) + "    " + pipeline.get(3);
                System.out.println(str);




            } else {
                System.out.println("        Incorrect number of arguments for command s");
            }

        } else if (command.equals("r")) {
            completeOperation();

        } else if (command.equals("m")) {
            if (input.length != 3) {
                System.out.println("        Incorrect number of arguments for command m");
                return;
            }
            System.out.println("");
            int num1 = Integer.parseInt(input[1]);
            int num2 = Integer.parseInt(input[2]);
            for (int i = num1; i <= num2; i++) {
                System.out.println("[" + i + "] = " + data_memory[i]);
            }

        } else if (command.equals("c")) {
            System.out.println("        Simulator reset");
            registers.replaceAll((k, v) -> 0);
            pc = 0;
            Arrays.fill(data_memory, 0);

        } else {
            System.out.println("Incorrect command.\n");

        }
    }

    public static void main(String[] args) throws FileNotFoundException {

        File file = new File(args[0]);
        //File file = new File("test1.asm");
        Scanner scanner = new Scanner(file);

        parseASM(scanner);

//        for (Instruction instruction : instructionObject) {
//            System.out.println(instruction.opcode);
//        }


        for(int i = 0; i < 4; i++){
            pipeline.add("empty");
        }

        String mode = "";
        switch (args.length) {
            case 0:
                mode = "interactive";
                break;
            case 2:
                mode = "script";
                break;
            default:
                System.out.println("Incorrect number of arguments");
                System.exit(0);
        }

        String[] input;
        if (mode == "interactive") {

            Scanner scanner2 = new Scanner(System.in);
            //Boolean quit = false;
//            do {
//                if(quit){
//                    break;
//                }
//                System.out.print("mips> ");
//                input = scanner2.nextLine().split("\\s");
//                System.out.println(input);
//                handleCommand(input);
//                System.out.println("");
//            } while (!quit);
//            scanner2.close();

            while(!quit){
                System.out.print("mips> ");
                input = scanner2.nextLine().split("\\s");
                handleCommand(input);
                System.out.println("");
            }
            scanner2.close();


        }
        if (mode == "script") {
            Scanner scanner2 = new Scanner(new File(args[1]));
            while (scanner2.hasNext()) {
                String line = scanner2.nextLine();
                input = line.split("\\s");
                System.out.println("mips> " + line);
                handleCommand(input);
                System.out.println("");
            }
            scanner2.close();
        }
        scanner.close();
    }
}
