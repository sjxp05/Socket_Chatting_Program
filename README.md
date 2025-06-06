# 자바 소켓을 활용한 채팅 프로그램

### 구조 소개 (`src/chatting_ui`)

1.  **`Server.java`**

    -   간단한 채팅 서버 클래스

    -   사실 하는 일은 별거 없음 소켓 연결 요청이 들어오면 그냥 핸들러에 넣어주기만 하면 됨ㅋㅋ

    -   로컬호스트 말고 외부에서도 연결할 수 있도록 Ngrok 등 사용해볼 예정

2.  **`Handler.java`**

    -   클라이언트 객체가 생성될 때마다 그에 대한 스레드를 만들어 주기 위한 클래스

    -   서버와 직접 상호작용할 수 있어 서버에 있는 닉네임 리스트 확인 등 서버에 접근하는 작업에 많이 사용됨

3.  **`client` 패키지**

    (1) **`Main.java`**

    -   클라이언트가 실행하여 서버에 접속할 수 있게 한 클래스

    -   서버 통신, 사용자 정보 저장 및 기본 로직 담당

    -   메시지 전송 및 수신, 닉네임 지정 및 변경, 사용자 목록 보기 등 다양한 기능 탑재

    (2) **`client/ChatUI.java`**

    -   메인 클래스에서 호출되는 ui 생성/변경 클래스

    -   Swing으로 이쁘게 GUI 구현 (사실 아직 개ㅐㅐㅐㅐ 이쁘지는 않음.. 나중에 더 손볼예정)

    -   이모지 입력 쪼오오금.. 가능 but 아직 문제가 있음

        -   이모지 두개 이상을 입력한 뒤 전송하면 한글 출력에 약간 문제가 생김;;; 내용은 잘 전달되는데 보이는게 좀..
        -   하트 이모지 등 일부 이모지 출력이 조금 이상함(특히 마지막으로 입력했을때)

    -   의도하진 않았지만 메시지 복붙도 됨ㅋㅋ

    .

    \* `Main.java`에는 함수나 객체들 기능을 쉽게 구별할 수 있도록 자세한 주석을 달아놓았습니당

    \* `client2` 패키지: 이 패키지는 `client/Main.java` 파일을 두 번 이상 동시에 돌릴 수가 없어서 복붙해서 만들어 놓은 임시파일임!

---

-   `src/terminal_message_echo`

    -   연습용으로 단순한 에코서버를 구현함

    -   원래 이걸로 소켓 공부를 가볍게 시작해보려 했으나.. 막상 이것만 만들어보니 개 재미없어서 멀티채팅을 만들기로 결심함ㅋㅋㅋㅋ

-   `src/tests`

    -   각종 기능을 테스트했던 파일을 모아놓음

    -   ui/전송 기능/자동 읽기 및 업데이트 기능 등 단순한 기능 한두개씩만 테스트함

---

### 구현한 기능

-   동시 채팅

    -   사용자가 보낸 메시지나 입장/퇴장 안내 메시지가 실시간으로 각 클라이언트의 채팅창에 반영됨

-   닉네임 정하기

    -   처음 접속해서 닉네임이 없을 경우 만들 수 있음
    -   이미 접속 경력이 있으면 원래 있던 닉네임으로 사용
    -   닉네임 중간에 변경 가능
    -   닉 글자수 및 기호 제한 기능
    -   고유 ID만 다르면 닉네임 중복 가능

-   방에 있는 모든 멤버들 목록 표시

    -   클라이언트에서 멤버 목록 보기 버튼을 누르면 서버로 목록 제공 요청
    -   서버에 있는 멤버 목록(자동 동기화되는 `ConcurrentHashMap` 구조)에서 for문 돌려서 하나씩 해당 클라이언트에게 전송
    -   클라이언트 ui에 전송받은 멤버 목록 반영됨

-   목록표시로 되어 있을 때 (채팅창이 보이지 않을 때)

    1. 사용자가 보낸 새로운 메시지가 있을 경우:\
       목록을 지우고 채팅창을 바로 보여줌

    2. 사용자 입/퇴장 메시지일 경우:\
       목록이 새로고침되어 입장/퇴장한 사용자가 반영됨, 채팅창에도 'ㅇㅇㅇ님이 참여했습니다.' 식으로 업데이트됨

-   사용자 정보 저장 방식 (간단함주의)

    -   클라이언트: 컴퓨터의 사용자 폴더에 텍스트 파일을 만들어 해당 사용자의 id와 닉네임을 저장함

    -   서버: 대화 내용 등의 데이터가 저장되는 시스템은 아니어서 신규 사용자가 들어올 경우 부여할 '다음 id'만 텍스트 파일로 저장

### 추가 구현할 것

-   시간 표시? 할말?

-   나중에 데베 쓰고 싶당
