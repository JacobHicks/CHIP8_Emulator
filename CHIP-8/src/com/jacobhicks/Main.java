/*********************************************************************
 *                             MEMORY MAP                            *
 *            EMULATOR TILESET           0x000-0x1FF                 *
 *            FONT TILESET               0x050-0x0A0                 *
 *            ROM AND WORK RAM           0x200-0xFFF                 *
 *********************************************************************/





package com.jacobhicks;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Stack;

public class Main {

    static Stack<Character> stack = new Stack<>();
    static char[] memory = new char[4096];
    static boolean[][] graphics = new boolean[32][64];
    static char[] V = new char[16];
    static char[] keypad = new char[16];
    static final char[] font = new char[]{
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
    static int I;
    static char pc;
    static volatile char sound_timer;
    static volatile char delay_timer;

    public static void main(String[] args) throws Exception{
        int tmp = 0x050;
        int tmp2 = 0x200;
        //Arrays.fill(memory, 0x00, 0x1ff, (byte) 0x1);
        for(int i : font){
            memory[tmp++] = (char)((i & 0xFF00) >> 8);
            memory[tmp++] = (char)(i & 0x00FF);
        }
        FileInputStream in = new FileInputStream("PONG");
        char opcode;
        do {
            opcode = (char)((in.read() << 8) | (in.read()));
            memory[tmp2++] = (char)((opcode & 0xFF00) >> 8);
            memory[tmp2++] = (char)(opcode & 0x00FF);
        } while(opcode != 0xFFFF);
        Screen screen = new Screen();
        new Thread(() -> {
            while(true){
                if(sound_timer > 0) sound_timer--;
                if(delay_timer > 0) delay_timer--;
                try {
                    Thread.sleep(17);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        pc = 0x200;
        while (true) {
            opcode = (char)((memory[pc] << 8) | (memory[pc+1]));
            if (false) ;
            else if (opcode == 0x00EE) {
                pc = stack.pop();
            }
            else if ((opcode & 0xF000) == 0x1000) {
                pc = (char)((opcode & 0x0FFF)-2);
            }
            else if ((opcode & 0xF000) == 0x2000) {
                stack.push(pc);
                pc = (char)((opcode & 0x0FFF)-2);
            }
            else if ((opcode & 0xF000) == 0x3000) {
                if(V[(opcode & 0x0F00)>>8] == (opcode & 0x00FF)) pc+=2;
            }
            else if ((opcode & 0xF000) == 0x6000) V[(opcode & 0x0F00) >> 8] = (char) (opcode & 0x00FF);
            else if ((opcode & 0xF000) == 0x7000) V[(opcode & 0x0F00)>>8] += opcode & 0x00FF;
            else if ((opcode & 0xF000) == 0xA000) I = (opcode & 0x0FFF);
            else if ((opcode & 0xF000) == 0xC000) {
                V[((opcode & 0x0F00)>>8)] = (char) (((char)(Math.random()*256)) & (opcode & 0x00FF));
            }
            else if ((opcode & 0xF000) == 0xD000) {
                for (int i = 0; i < (opcode & 0x000F); i++) {
                    Screen.drawByte(((opcode & 0x0F00)>>8) * 16 + 16, ((opcode & 0x00F0) >> 4) + i, memory[I + i]);
                }
            }
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0007)) V[((opcode & 0x0F00)>>8)] = delay_timer;
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0015)) delay_timer = V[((opcode & 0x0F00)>>8)];
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0029)) {
                I = V[((opcode & 0x0F00)>>8)];
            }
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0033)) {
                memory[I] = V[((opcode & 0x0F00)>>8)/100];
                memory[I+1] = V[(((opcode & 0x0F00)>>8)%100)/10];
                memory[I+2] = V[((opcode & 0x0F00)>>8)%10];
            }
            else if ((opcode & 0xF000) == 0xF000 && ((opcode & 0x00FF) == 0x0065)) {
                int t = I;
                for(int i = 0; i < ((opcode & 0x0F00)>>8); i++){
                    V[i] = memory[t++];
                }
            }
            else {
                System.out.println(String.format("Unsupported opcode at 0x%04X: 0x%04X", (int)pc, (int)opcode));
                break;
            }
            screen.update();
            pc+=2;
        }
    }

    static class Screen extends JFrame {
        Graphics g;
        public Screen(){
            setResizable(false);
            setSize(128, 64);
            setLayout(null);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            g = getGraphics();
            setVisible(true);
        }
        static void drawByte(int x, int y, char byt){
            String bitfield = String.format("%8s", Integer.toBinaryString(byt)).replace(' ', '0');
            for (char c : bitfield.toCharArray()){
                x %= 64;
                y %= 32;
                if(graphics[y][x] && c=='1') V[0xF] = 1;
                graphics[y][x++] ^= c=='1';
            }
        }
        public void update(){
            g = getGraphics();
            for(int i = 0; i < 32; i++){
                for(int x = 0; x < 64; x++){
                    g.setColor(graphics[i][x] ? Color.WHITE : Color.BLACK);
                    g.fillRect(x*2, i*2, 2, 2);
                }
            }
        }
    }
    
}

