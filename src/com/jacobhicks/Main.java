/*********************************************************************
 *                             MEMORY MAP                            *
 *            EMULATOR TILESET           0x000-0x1FF                 *
 *            FONT TILESET               0x050-0x0A0                 *
 *            ROM AND WORK RAM           0x200-0xFFF                 *
 *********************************************************************/





package com.jacobhicks;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Stack;

import static java.awt.event.KeyEvent.*;

public class Main {

    private static int testing = 0;
    private static Stack<Character> stack = new Stack<>();
    private static char[] memory = new char[4096];
    private static boolean[][] graphics = new boolean[32][64];
    private static char[] V = new char[16];
    private static volatile boolean[] keypad = new boolean[16];
    private static final char[] font = new char[]{
        0xF0, 0x90, 0x90, 0x90, 0xF0,
            0x20, 0x60, 0x20, 0x20, 0x70,        // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0,        // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0,        // 3
            0x90, 0x90, 0xF0, 0x10, 0x10,        // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0,        // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0,        // 6
            0xF0, 0x10, 0x20, 0x40, 0x40,        // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0,        // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0,        // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90,        // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0,        // B
            0xF0, 0x80, 0x80, 0x80, 0xF0,        // C
            0xE0, 0x90, 0x90, 0x90, 0xE0,        // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0,        // E
            0xF0, 0x80, 0xF0, 0x80, 0x80        // F
    };
    private static int I;
    private static char pc;
    private static volatile char sound_timer;
    private static volatile char delay_timer;
    private static volatile Screen screen = new Screen();

    public static void main(String[] args) throws Exception{
        Arrays.fill(V, (char) 0);
        int tmp = 0x050;
        int tmp2 = 0x200;
        for(int i : font){
            memory[tmp++] = (char)((i & 0xFF00) >> 8);
            memory[tmp++] = (char)(i & 0x00FF);
        }
        FileInputStream in = new FileInputStream("BRIX");
        char opcode;
        do {
            opcode = (char)((in.read() << 8) | (in.read()));
            memory[tmp2++] = (char)((opcode & 0xFF00) >> 8);
            memory[tmp2++] = (char)(opcode & 0x00FF);
        } while(opcode != 0xFFFF);
        new Thread(() -> {
            while(true){
                if(sound_timer > 0) sound_timer--;
                if(delay_timer > 0) delay_timer--;
                try {
                    Thread.sleep(17);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                screen.update();
            }
        }).start();
        pc = 0x200;
        while (true) {
            opcode = (char)((memory[pc] << 8) | (memory[pc+1]));
            if (opcode == 0x0000);
            else if (opcode == 0x00E0) {    //Return from subroutine
                for(boolean[] ia : graphics) {
                    Arrays.fill(ia, false);
                }
            }
            else if (opcode == 0x00EE) {    //Return from subroutine
                pc = stack.pop();
            }
            else if (opcode == 0x00FF) {
                graphics = new boolean[64][128];
            }
            else if ((opcode & 0xF000) == 0x1000) { //
                pc = (char)((opcode & 0x0FFF)-2);
            }
            else if ((opcode & 0xF000) == 0x2000) {
                stack.push(pc);
                pc = (char)((opcode & 0x0FFF)-2);
            }
            else if ((opcode & 0xF000) == 0x3000) {
                if(V[(opcode & 0x0F00)>>8] == (opcode & 0x00FF)) pc+=2;
            }
            else if ((opcode & 0xF000) == 0x4000) {
                if(V[(opcode & 0x0F00)>>8] != (opcode & 0x00FF)) pc+=2;
            }
            else if ((opcode & 0xF000) == 0x6000) V[(opcode & 0x0F00) >> 8] = (char) (opcode & 0x00FF);
            else if ((opcode & 0xF000) == 0x7000) {
                V[(opcode & 0x0F00)>>8] += opcode & 0x00FF;
                V[(opcode & 0x0F00)>>8] &= 0xFF;
            }
            else if ((opcode & 0xF00F) == 0x8000) V[(opcode & 0x0F00)>>8] = V[(opcode & 0x00F0)>>4];
            else if ((opcode & 0xF00F) == 0x8001) V[(opcode & 0x0F00)>>8] |= V[(opcode & 0x00F0)>>4];
            else if ((opcode & 0xF00F) == 0x8002) V[(opcode & 0x0F00)>>8] &= V[(opcode & 0x00F0)>>4];
            else if ((opcode & 0xF00F) == 0x8003) V[(opcode & 0x0F00)>>8] ^= V[(opcode & 0x00F0)>>4];
            else if ((opcode & 0xF00F) == 0x8004) {
                V[(opcode & 0x0F00)>>8] += V[(opcode & 0x00F0)>>4];
                if(V[(opcode & 0x0F00)>>8] > 0xFF) V[0xF] = 1;
                else V[0xF] = 0;
                V[(opcode & 0x0F00)>>8] &= 0xFF;
            }
            else if ((opcode & 0xF00F) == 0x8005) {
                if(V[(opcode & 0x0F00)>>8] > V[(opcode & 0x00F0)>>4]) V[0xF] = 1;
                else V[0xF] = 0;
                V[(opcode & 0x0F00)>>8] -= V[(opcode & 0x00F0)>>4];
                V[(opcode & 0x0F00)>>8] &= 0xFF;
            }
            else if ((opcode & 0xF00F) == 0x8006) {
                if(V[(opcode & 0x0F00)>>8] % 2 == 1) V[0xF] = 1;
                else V[0xF] = 0;
                V[(opcode & 0x0F00)>>8] >>= 1;
            }
            else if ((opcode & 0xF00F) == 0x800E) {
                if((V[(opcode & 0x0F00)>>8] << 1) > 0xFF) V[0xF] = 1;
                else V[0xF] = 0;
                V[(opcode & 0x0F00)>>8] <<= 1;
            }
            else if ((opcode & 0xF00F) == 0x9000){
                if(V[(opcode & 0x0F00)>>8] != V[(opcode & 0x00F0)>>4]) pc+=2;
            }
            else if ((opcode & 0xF000) == 0xA000) I = (opcode & 0x0FFF);
            else if ((opcode & 0xF000) == 0xC000) {
                V[((opcode & 0x0F00)>>8)] = (char) (((char)(Math.random()*256)) & (opcode & 0x00FF));
            }
            else if ((opcode & 0xF000) == 0xD000) {
                V[0xF] = 0;
                for (int i = 0; i < (opcode & 0x000F); i++) {
                    Screen.drawByte(V[(opcode & 0x0F00)>>8], V[(opcode & 0x00F0) >> 4] + i, memory[I + i]);
                }
                long start = System.nanoTime();
                while(System.nanoTime() - start < 5500000);
                if(sound_timer > 0) Toolkit.getDefaultToolkit().beep();
            }
            else if ((opcode & 0xF000) == 0xE000 && ((opcode & 0x00FF) == 0x009E)) {
                if(keypad[((opcode & 0x0F00)>>8)]) pc+=2;
            }
            else if ((opcode & 0xF000) == 0xE000 && ((opcode & 0x00FF) == 0x00A1)) {
                if(!keypad[((opcode & 0x0F00)>>8)]) pc+=2;
            }
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0007)) V[((opcode & 0x0F00)>>8)] = delay_timer;
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0015)) delay_timer = V[((opcode & 0x0F00)>>8)];
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0018)) sound_timer = V[((opcode & 0x0F00)>>8)];
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x001E)) I += V[((opcode & 0x0F00)>>8)];
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0029)) {
                I = V[((opcode & 0x0F00)>>8)];
            }
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0033)) {
                memory[I] = (char) (V[((opcode & 0x0F00)>>8)] / 100);
                memory[I+1] = (char) (V[((opcode & 0x0F00)>>8)] % 100 / 10);
                memory[I+2] = (char) (V[(opcode & 0x0F00)>>8] % 10);
            }
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0055)) {
                int t = I;
                for(int i = 0; i <= ((opcode & 0x0F00)>>8); i++){
                    memory[t++] = V[i];
                }
            }
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0065)) {
                int t = I;
                for(int i = 0; i <= ((opcode & 0x0F00)>>8); i++){
                    V[i] = memory[t++];
                }
            }
            else {
                System.out.println(String.format(testing++ + ": Unsupported opcode at 0x%04X: 0x%04X", (int)pc, (int)opcode));
                break;
            }
            //System.out.println(String.format("PC: 0x%04X OPCODE: 0x%04X", (int)pc, (int)opcode));
            pc+=2;
        }
    }

    static class Keypad implements KeyListener {
        public void keyTyped(KeyEvent keyEvent) {}
        public void keyPressed(KeyEvent keyEvent) {
            switch (keyEvent.getKeyCode()) {
                case (VK_1) : keypad[0x0] = true; break;
                case (VK_2) : keypad[0x1] = true; break;
                case (VK_3) : keypad[0x2] = true; break;
                case (VK_4) : keypad[0x3] = true; break;
                case (VK_Q) : keypad[0x4] = true; break;
                case (VK_W) : keypad[0x5] = true; break;
                case (VK_E) : keypad[0x6] = true; break;
                case (VK_R) : keypad[0x7] = true; break;
                case (VK_A) : keypad[0x8] = true; break;
                case (VK_S) : keypad[0x9] = true; break;
                case (VK_D) : keypad[0xA] = true; break;
                case (VK_F) : keypad[0xB] = true; break;
                case (VK_Z) : keypad[0xC] = true; break;
                case (VK_X) : keypad[0xD] = true; break;
                case (VK_C) : keypad[0xE] = true; break;
                case (VK_V) : keypad[0xF] = true; break;
            }
        }
        public void keyReleased(KeyEvent keyEvent) {
            switch (keyEvent.getKeyCode()) {
                case (VK_1) : keypad[0x0] = false; break;
                case (VK_2) : keypad[0x1] = false; break;
                case (VK_3) : keypad[0x2] = false; break;
                case (VK_4) : keypad[0x3] = false; break;
                case (VK_Q) : keypad[0x4] = false; break;
                case (VK_W) : keypad[0x5] = false; break;
                case (VK_E) : keypad[0x6] = false; break;
                case (VK_R) : keypad[0x7] = false; break;
                case (VK_A) : keypad[0x8] = false; break;
                case (VK_S) : keypad[0x9] = false; break;
                case (VK_D) : keypad[0xA] = false; break;
                case (VK_F) : keypad[0xB] = false; break;
                case (VK_Z) : keypad[0xC] = false; break;
                case (VK_X) : keypad[0xD] = false; break;
                case (VK_C) : keypad[0xE] = false; break;
                case (VK_V) : keypad[0xF] = false; break;
            }
        }
    }

    static class Screen extends JFrame {
        Graphics g;
        Screen(){
            setAlwaysOnTop(true);
            setSize(128, 64);
            setLayout(null);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            g = getGraphics();
            setVisible(true);
            addKeyListener(new Keypad());
        }
        static void drawByte(int x, int y, char byt){
            String bitfield = String.format("%8s", Integer.toBinaryString(byt)).replace(' ', '0');
            for (char c : bitfield.toCharArray()){
                x %= graphics[0].length;
                y %= graphics.length;
                if (graphics[y][x] && c=='1') V[0xF] = 1;
                graphics[y][x++] ^= c=='1';
            }
        }
        public void update(){
            Image buff = createImage(getWidth(), getHeight());
            g = buff.getGraphics();
            for(int i = 0; i < graphics.length; i++){
                for(int x = 0; x < graphics[0].length; x++){
                    g.setColor(graphics[i][x] ? Color.WHITE : Color.BLACK);
                    g.fillRect(x*(getWidth()/64), i*(getHeight()/graphics.length), (getWidth()/graphics[0].length), (getHeight()/graphics.length));
                }
            }
            getGraphics().drawImage(buff, 0, 0, null);
        }
    }
    
}