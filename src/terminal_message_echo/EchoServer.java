package terminal_message_echo;

import java.io.*;
import java.net.*;

/*
 * 작동시키는 법
 *   1. EchoServer 먼저 작동시키기
 *   2. 다른 터미널에서 EchoClient 작동시키기
 *   3. EchoClient 터미널에 문자 입력하고 결과 확인하기
 */

public class EchoServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("서버 가동을 시작합니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트와 연결되었습니다.");

                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String input;

                while ((input = reader.readLine()) != null) {
                    System.out.println("클라이언트로부터 받은 메시지: " + input);

                    if (input.equals("종료") || input.toLowerCase().equals("close")) {
                        System.out.println("서버를 종료합니다.");
                        writer.println("서버를 종료합니다.");
                    }

                    writer.println(input);
                }
            }
        } catch (Exception e) {
            if (e.getMessage().equals("Connection reset")) {
                System.exit(0);
            } else {
                System.out.println("오류 발생: " + e.getMessage());
            }
        }
    }
}
