import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class AssEmbler {

    private LinkedHashMap<String, Short> labels = new LinkedHashMap<>();

    public static void main(String[] args) {

        AssEmbler assEmbler = new AssEmbler();

        Scanner s = new Scanner(System.in);

        ArrayList<String> lines = new ArrayList<>();
        while (s.hasNextLine()) {
            lines.add(s.nextLine());
        }

        assEmbler.toHexImg(sugarCoat(lines), System.out);
    }

    private static List<String> sugarCoat(List<String> lines) {

        List<String> output = new ArrayList<>();

        for (String line : lines) {
            String s = " " + line;

            s = s.replaceFirst(";.*", ""); // remove comments

            s = s.replaceAll("(?i) JMP ", " MOV IMM PC ");

            s = s.replaceAll("(?i) PUT ", " ADD 0 IMM OUT ");

            s = s.replaceAll("(?i) MOV ", " ADD 0 ");

            s = s.replaceAll(" 0 ", " ZERO ");
            s = s.replaceAll(" 0 ", " ZERO ");

            s += "\n";

            output.add(s);
        }

        return output;
    }

    private void toHexImg(List<String> in, PrintStream out) {

        String inputString = in.stream().collect(Collectors.joining(""));


        out.println("v2.0 raw");

        // first pass, just find labels
        Scanner sc = new Scanner(inputString);
        int i = 0;
        while (sc.hasNext()) {
            String next = sc.next();
            Instruction instruction = new Instruction(sc, (short) i, next).parse(0);
            if (instruction.label) continue;
            i++;
        }

        sc = new Scanner(inputString);
        i = 0;

        while (sc.hasNext()) {
            String next = sc.next();

            Instruction instruction = new Instruction(sc, (short) i, next).parse(1);
            if (instruction.label) continue;

            out.print(Integer.toHexString(instruction.dst.ordinal()));
            out.print(Integer.toHexString(instruction.op.ordinal()));
            out.print(Integer.toHexString(instruction.src2.ordinal()));
            out.print(Integer.toHexString(instruction.src1.ordinal()));
            out.print(String.format("%04X", instruction.imm));

            if (++i % 8 == 0) {
                out.println();
            } else {
                out.print(" ");
            }
            out.flush();
        }
    }

    @SuppressWarnings("unused")
    enum Op {
        ADD, NEG, AND, OR, SEQ, SGT, RESERVED1, RESERVED2,
        CADD, CNEG, CAND, COR, CSEQ, CSGT, CRESERVED1, CRESERVED2 // C* ops only writes to DST if (Src A == 0)
    }

    @SuppressWarnings("unused")
    enum Src {
        A, B, C, D,
        E, F, G, H,
        RESERVED1, RESERVED2, RESERVED3, RESERVED4,
        MEM, ZERO, IMM, PC
    }

    @SuppressWarnings("unused")
    enum Dst {
        A, B, C, D,
        E, F, G, H,
        RESERVED1, RESERVED2, RESERVED3, RESERVED4,
        MEM, OUT, ADDR, PC
    }

    private class Instruction {
        private boolean label;
        private Scanner sc;
        private short i;
        private String next;
        private Op op;
        private Src src1;
        private Src src2;
        private Dst dst;
        private short imm;

        Instruction(Scanner sc, short i, String next) {
            this.sc = sc;
            this.i = i;
            this.next = next;
        }

        Instruction parse(int pass) {
            if (next.endsWith(":")) {
                String label = next.substring(0, next.length() - 1);
                if (labels.containsKey(label) && labels.get(label) != i)
                    throw new RuntimeException("Duplicate label " + label);
                labels.put(label, i);
                this.label = true;
                return this;
            } else {
                op = Op.valueOf(next.toUpperCase());
            }

            next = sc.next();
            src1 = Src.valueOf(next.toUpperCase());

            next = sc.next();
            src2 = Src.valueOf(next.toUpperCase());

            next = sc.next();
            dst = Dst.valueOf(next.toUpperCase());

            imm = 0;
            if (src1 == Src.IMM || src2 == Src.IMM) {
                next = sc.next();
                if (next.endsWith(":")) {
                    String label = next.substring(0, next.length() - 1);
                    imm = labels.getOrDefault(label, (short) -1);
                    if (pass > 0 && imm < 0) {
                        throw new RuntimeException("Unknown label " + next);
                    }
                } else if (next.startsWith("0x")) {
                    imm = Short.parseShort(next.replaceFirst("0x", ""), 16);
                } else if (next.length() == 1 && !Character.isDigit(next.charAt(0))) {
                    imm = (short) next.charAt(0);
                } else {
                    imm = Short.parseShort(next);
                }
            }
            label = false;
            return this;
        }
    }
}
