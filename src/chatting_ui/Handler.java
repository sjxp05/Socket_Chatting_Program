package chatting_ui;

import java.io.*;
import java.net.*;
import java.util.*;

public class Handler {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public Handler(Socket clientSocket) {
        try {
            this.socket = clientSocket;
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }

    static synchronized void broadcast() {

    }
}
