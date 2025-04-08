package chatting_ui;

import java.io.*;
import java.net.*;
import java.util.*;

public class Handler extends Thread {
    static ArrayList<PrintWriter> writerList = new ArrayList<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    String userName;

    public Handler(Socket clientSocket) {
        try {
            socket = clientSocket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writerList.add(out);
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            this.userName = in.readLine();
            System.out.println("[" + userName + " 연결됨]");
            sendAll(userName + " 님이 참여했습니다.");
            Server.printNicknames();

            while (in != null) {
                String inputMsg = in.readLine();
                sendAll(inputMsg);
            }
        } catch (IOException e) {
            System.out.println("[" + userName + " 접속 끊김]");

        } finally {
            sendAll(userName + " 님이 나갔습니다.");
            writerList.remove(out);
            Server.removeNickname(userName);

            try {
                socket.close();
            } catch (IOException e2) {
                System.out.println("오류 발생: " + e2.getMessage());
            }
        }
    }

    private void sendAll(String msg) {
        for (PrintWriter writer : writerList) {
            writer.println(msg);
        }
    }
}
