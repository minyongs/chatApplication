package chatMini;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClientMini {
    public static void main(String[] args) {
        String hostName = "192.168.219.147"; // 서버가 실행 중인 호스트의 이름 또는 IP 주소
        int portNumber = 12345; // 서버와 동일한 포트 번호 사용

        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        try{
            socket = new Socket(hostName, portNumber);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner stdIn = new Scanner(System.in);

            System.out.print("닉네임을 입력하세요: ");
            String nickname = stdIn.nextLine();
            out.println(nickname); // 서버에 닉네임을 전송

            // 서버로부터 메시지를 읽어 화면에 출력하는 별도의 스레드
            Thread readThread = new Thread(new ServerMessageReaderMini(in));
            readThread.start(); // 메시지 읽기 스레드 시작
            printMenu();

            // 사용자 입력 처리
            String userInput;
            while (true) {
                userInput = stdIn.nextLine();

                switch (userInput.toLowerCase()) {
                    case "/bye":
                        out.println(userInput);
                        return; // 클라이언트 종료
                    case "/list":
                    case "/create":
                    case "/exit":
                    case"/users":
                    case"/roomusers":

                        out.println(userInput);
                        break;
                    default:
                        if (userInput.startsWith("/join ")) {
                            out.println(userInput);
                        } else {
                            out.println(userInput); // 일반 채팅 메시지
                        }
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("예외 발생 : " + hostName + " on port " + portNumber);
            e.printStackTrace();
        }
    }


    private static void printMenu() {
        System.out.println("\n명령어:");
        System.out.println("/list - 생성된 방");
        System.out.println("/create - 새로운 채팅방 만들기");
        System.out.println("/join [방번호] - 채팅방 입장");
        System.out.println("/exit - 채팅방 나가기");
        System.out.println("/bye - 채팅 어플리케이션 종료");
        System.out.println("==========추가기능==========");
        System.out.println("/users - 모든 유저 목록");
        System.out.println("/roomusers - 현재 입장한 방 유저 목록");
        System.out.println("/w[닉네임] - 귓속말");
        System.out.println("/b[닉네임] - 사용자 차단");
    }
}

