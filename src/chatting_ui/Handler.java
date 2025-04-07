package chatting_ui;

import java.io.*;
import java.net.*;

public class Handler extends Thread {
    Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public Handler(Socket clientSocket) {
        try {
            this.socket = clientSocket;
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }
}
