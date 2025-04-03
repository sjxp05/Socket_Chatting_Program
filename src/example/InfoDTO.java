package example;

import java.io.*;

enum Info {
    JOIN, EXIT, SEND
}

class InfoDTO implements Serializable {
    private String nickName;
    private String message;
    private Info command;

    public String getNickName() {
        return nickName;
    }

    public Info getCommand() {
        return command;
    }

    public String getMessage() {
        return message;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setCommand(Info command) {
        this.command = command;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

/*
 * [출처] [JAVA] 채팅 프로그램 만들기(네트워크) | 작성자 Ohsanrim
 */