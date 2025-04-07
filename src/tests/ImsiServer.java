package tests;

import java.io.*;
import java.net.*;
import java.util.*;

public class ImsiServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("서버를 시작합니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트에 연결되었습니다.");

                Scanner sc = new Scanner(System.in);
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String msg;
                System.out.print("메시지 입력: ");

                while ((msg = sc.nextLine()) != null) {
                    if (msg.strip().length() != 0) {
                        writer.println("User1:" + msg);
                    }
                    System.out.print("메시지 입력: ");
                }

                sc.close();
            }
        } catch (Exception e) {
            String errMsg = e.getMessage();
            if (errMsg.equals("Connection reset")) {
                System.out.println("서버를 종료합니다.");
                System.exit(0);
            } else {
                System.out.println("오류 발생: " + errMsg);
            }
        }
    }
}
