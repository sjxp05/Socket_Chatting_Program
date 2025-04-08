package chatting_ui;

// import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {
    static ArrayList<String> nicknameList = new ArrayList<>();

    public static void main(String[] args) {
        // try {
        // BufferedReader nameReader = new BufferedReader(new
        // FileReader("src/chatting_ui/clientsNameList.txt"));
        // String name;
        // while ((name = nameReader.readLine()) != null) {
        // nicknameList.add(name);
        // }
        // nameReader.close();
        // } catch (Exception e) {
        // System.out.println("오류 발생: " + e.getMessage());
        // }

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("서버를 시작합니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Handler handler = new Handler(clientSocket);
                handler.start();
                System.out.println("클라이언트와 연결되었습니다.");
            }
        } catch (Exception e) {
            String errMsg = e.getMessage();

            if (errMsg.equals("Connection reset")) {
                System.out.println("서버를 종료합니다.");
                System.exit(0);
            } else {
                System.out.println("오류 발생: " + errMsg);
                /* 놀라운 점: sout 말고 out만 쳐도 println이 자동으로 나온다 ㄷㄷ */
            }
        }
    }
}
