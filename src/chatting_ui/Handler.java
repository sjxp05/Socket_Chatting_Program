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
    private boolean nameCheck = false;

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
            while (true) {
                userName = in.readLine();
                nicknameCheck(userName);
                if (nameCheck == true) {
                    break;
                }
            }

            System.out.println("[" + userName + " 연결됨]");
            sendAll("@" + userName + " 님이 참여했습니다.");

            while (in != null) {
                String inputMsg = in.readLine();

                // if (inputMsg.indexOf(';') == -1 && inputMsg.indexOf("@nick") >= 0) {
                // continue;
                // } else
                if (inputMsg.indexOf(';') == -1 && inputMsg.indexOf("@change") >= 0) {
                    nicknameCheck(inputMsg.substring(0, inputMsg.indexOf('@')));
                    if (nameCheck == true) {
                        Server.removeNickname(userName);
                        userName = inputMsg.substring(0, inputMsg.indexOf('@'));
                        System.out.println("[" + userName + " 으로 닉네임 변경]");
                    }
                } else {
                    sendAll(inputMsg);
                }
            }
        } catch (IOException e) {
            System.out.println("[" + userName + " 접속 끊김]");

        } finally {
            sendAll("@" + userName + " 님이 나갔습니다.");
            writerList.remove(out);
            Server.removeNickname(userName);

            try {
                socket.close();
            } catch (IOException e2) {
                System.out.println("오류 발생: " + e2.getMessage());
            }
        }
    }

    void nicknameCheck(String nickname) {
        if (Server.addNickname(nickname) == true) {
            nameCheck = true;
            out.println("OK@nick");
        } else {
            out.println("EXIST@nick");
        }
    }

    private void sendAll(String msg) {
        for (PrintWriter writer : writerList) {
            writer.println(msg);
        }
    }
}
