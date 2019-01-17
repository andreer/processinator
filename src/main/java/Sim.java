import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Sim {

    private final int[] mem = new int[1 << 24]; // 64M
    {
        mem[0] = 0xd0ed004B; // print k
        mem[1] = 0xd0ed0041; // print a
        mem[2] = 0xd0ed004B; // print k
        mem[3] = 0xd0ed0045; // print e
        mem[4] = 0xf0ed0000; // jmp 0
    }

    private final static int ZR = 13;
    private final static int PC = 15;
    private final static int OUT = 13;
    private final static int MR = 12;
    private final static int AR = 14;
    private final static int IMM = 14;

    public static void main(String[] args) throws IOException {
        new Sim().go();
    }

    private void go() throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(System.out);

        Writer writer = new OutputStreamWriter(out);

        int[] regs = new int[16];
        int[] alu = new int[4];

        while (true) {
            execute(writer, regs, alu);
        }
    }

    private void execute(Writer writer, int[] regs, int[] alu) throws IOException {
        int ir;
        regs[ZR] = 0;
        regs[OUT] = 0;
        ir = mem[regs[PC]];
        regs[MR] = mem[regs[AR]];

        int imm = ir & 0xffff;
        regs[IMM] = imm;
        int src1 = ir >> 16 & 0xF;
        int src2 = ir >> 20 & 0xF;
        int op = ir >> 24 & 0xF;
        int dst = ir >> 28 & 0xF;

        int src1v = regs[src1];
        int src2v = regs[src2];

        alu[0] = src1v + src2v;
        alu[1] = -src1v;
        alu[2] = src1v & src2v;
        alu[3] = src1v | src2v;

        int res = alu[op];

        regs[dst] = res;

        char c = (char) (regs[OUT] & 0x7f);
        if(c != 0)
            writer.append(c);

        if(dst != PC)
            regs[PC]++;

        regs[PC] = (regs[PC]) & 0xffffff;
    }
}
