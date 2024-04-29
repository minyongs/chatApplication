package chatMini;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatServerMini {
    private static Map<String, Map<String, PrintWriter>> rooms = new HashMap<>();
    private static Set<String> userList = new HashSet<>();
    private static Map<String, Set<String>> blockLists = new HashMap<>();


    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("서버 on");

            while (true) {
                Socket socket = serverSocket.accept();
                new ChatThread(socket).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void createRoom(String roomName) {
        if (!rooms.containsKey(roomName)) { //rooms map 의 key 에 roomName 이 없으면 map 에 방 번호와 client 를 추가
            rooms.put(roomName, new HashMap<>());


        } else {
            System.out.println("방이 이미 존재합니다: " + roomName);
        }
    }



    public static synchronized void joinRoom(String roomName, String nickName, PrintWriter writer) {
        rooms.computeIfAbsent(roomName, k -> new HashMap<>()).put(nickName, writer);

    }


    public static synchronized void leaveRoom(String roomName, String nickName) {
        Map<String, PrintWriter> room = rooms.get(roomName);
        if (room != null) {
            room.remove(nickName);
            broadcast(nickName + " 님이 " + roomName + " 방에서 나갔습니다.", roomName);

        }
    }
    public static void blockUser(String requester, String toBlock) {
        blockLists.computeIfAbsent(requester, k -> new HashSet<>()).add(toBlock);
    }

    public static void unblockUser(String requester, String toUnblock) {
        Set<String> blocks = blockLists.getOrDefault(requester, new HashSet<>());
        blocks.remove(toUnblock);
        if (blocks.isEmpty()) {
            blockLists.remove(requester);
        }
    }


    public static void broadcast(String msg, String roomName) {
        Map<String, PrintWriter> room = rooms.get(roomName);//방번호 키로 방 찾기
        if (room != null) {
            for (PrintWriter writer : room.values()) {
                writer.println(msg);
            }
        }
    }

    static class ChatThread extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickName;
        private String currentRoom = "";// 로비

        public ChatThread(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream() ,true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                requestNickname();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("/create ")) {
                        String roomName = inputLine.substring(8).trim();
                        ChatServerMini.createRoom(roomName);
                        chattingRoomMade(roomName);


                    } else if (inputLine.startsWith("/join ")) {
                        String roomName = inputLine.substring(6).trim();
                        currentRoom = roomName;
                        ChatServerMini.joinRoom(roomName, nickName, out);
                        broadcast(nickName +"님이 입장하셨습니다.",roomName);
                    } else if (inputLine.equalsIgnoreCase("/list")) {
                        printList();
                    }else if(inputLine.equalsIgnoreCase("/users")){
                        printAllUsers();
                    }else if (inputLine.trim().equalsIgnoreCase("/roomusers")) {
                        if (!currentRoom.isEmpty()) {  // 문자열 비교를 위해 equals() 사용
                            out.println("현재 방의 유저 목록:");
                            printUsers(currentRoom);
                        } else {
                            out.println("로비에선 전체 목록 조회만 가능합니다. /users");
                        }
                    }else if (inputLine.startsWith("/block ")) {
                        String toBlock = inputLine.substring(7).trim();
                        blockUser(nickName, toBlock);
                        out.println("사용자 " + toBlock + "을(를) 차단했습니다.");
                    }
                    else if (inputLine.startsWith("/unblock ")) {
                        String toUnblock = inputLine.substring(9).trim();
                        unblockUser(nickName, toUnblock);
                        out.println("사용자 " + toUnblock + "의 차단을 해제했습니다.");
                    }

                    else if (inputLine.equalsIgnoreCase("/exit")) {
                        String roomName="";
                        if (!currentRoom.isEmpty()) {
                            ChatServerMini.leaveRoom(currentRoom, nickName);

                            roomName = currentRoom;
                            currentRoom = "";
                            out.println("로비로 돌아갑니다.");


                        }
                        Map<String,PrintWriter> room = rooms.get(roomName);
                        if(room.isEmpty()){
                            rooms.remove(roomName);
                            noOneInRoom();



                        }

                    } else if (inputLine.equalsIgnoreCase("/bye")) {
                        out.println("프로그램 종료");

                        break;

                    } else if(inputLine.startsWith("/w ")){
                        whisper(inputLine);
                    } else {
                        if (!currentRoom.isEmpty()) {
                            ChatServerMini.broadcast(nickName + ": " + inputLine, currentRoom);
                        } else {
                            out.println("채팅에 참여하시려면 방에 입장해 주세요.");

                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (!currentRoom.isEmpty()) {
                    ChatServerMini.leaveRoom(currentRoom, nickName);
                }
                try {
                    socket.close();
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void printList() {
            out.println("생성된 방 목록:");
            for (String key : ChatServerMini.rooms.keySet()) {
                out.println(key);
            }
        }

        private void chattingRoomMade(String roomName){
            out.println("채팅방 " + roomName +"이 생성되었습니다.");
        }

        private void noOneInRoom(){
            out.println("채팅방에 아무도 남아있지 않아 채팅방을 삭제합니다.");
        }

        private void printUsers(String roomName) {
            Map<String, PrintWriter> room = rooms.get(roomName);
            if (room != null) {
                for (String s : room.keySet()) {
                    out.println(s);
                }
        }
    }
        private void requestNickname() throws IOException {
            while (true) {

                nickName = in.readLine().trim();
                synchronized (userList) {
                    if (!userList.contains(nickName)) {
                        userList.add(nickName);
                        out.println("닉네임이 설정되었습니다. 채팅을 시작하세요.");
                        break;
                    } else {
                        out.println("이미 사용중인 닉네임입니다. 다른 닉네임을 입력하세요.");
                    }
                }
            }
        }

        private void printAllUsers(){
            out.println("유저 목록:");
            for (String s : userList) {
                out.println(s);

            }
        }

        public void whisper(String inputLine){
            int firstSpace = inputLine.indexOf(" ");
            int secondSpace = inputLine.indexOf(" ", firstSpace + 1);

            if (secondSpace != -1) {
                String toNickName = inputLine.substring(firstSpace + 1, secondSpace);
                String message = inputLine.substring(secondSpace + 1);

                if (isBlocked(toNickName, nickName)) {
                    out.println("메시지를 보낼 수 없습니다. " + toNickName + "님이 당신을 차단했습니다.");
                    return;
                }

                PrintWriter receiver = findUser(toNickName);
                if (receiver != null) {
                    receiver.println(nickName + " 님으로부터의 귓속말: " + message);
                } else {
                    out.println(toNickName + " 사용자를 찾을 수 없습니다.");
                }
            } else {
                out.println("'/w 닉네임 메시지' 형식으로 입력해주세요.");
            }
        }
        private PrintWriter findUser(String nickName) {
            for (Map<String, PrintWriter> room : rooms.values()) {
                if (room.containsKey(nickName)) {
                    return room.get(nickName);
                }
            }
            return null;
        }
        private boolean isBlocked(String toNickName, String fromNickName) {
            Set<String> blockedByToUser = blockLists.getOrDefault(toNickName, new HashSet<>());
            return blockedByToUser.contains(fromNickName);
        }

    }}
