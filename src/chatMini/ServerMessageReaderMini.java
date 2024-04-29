package chatMini;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerMessageReaderMini implements Runnable {
    private BufferedReader in;

    public ServerMessageReaderMini(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String serverLine;
            while ((serverLine = in.readLine()) != null) {
                System.out.println(serverLine); // 서버로부터 받은 메시지를 출력
            }
        } catch (IOException e) {
            System.out.println("Server connection was closed.");
        }
    }
}

