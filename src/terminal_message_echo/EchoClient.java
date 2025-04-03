package terminal_message_echo;

import java.io.*;
import java.net.*;

public class EchoClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("서버에 연결되었습니다.");

            String userInput;
            System.out.print("메시지를 입력하세요: ");

            while ((userInput = stdIn.readLine()) != null) {
                writer.println(userInput);

                if (userInput.equals("종료") || userInput.toLowerCase().equals("close")) {
                    System.out.println(reader.readLine());
                    System.exit(0);
                }

                System.out.println("서버로부터 받은 응답: " + reader.readLine());
                System.out.print("메시지를 입력하세요: ");
            }
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }
}
