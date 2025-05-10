## 프로토콜만들기

(구분자: @)

상황:

1. 들어올때 닉네임 체크 - CONFIRM@userID\
   신규멤버 들어올때 요청 - NEW@userName\
   기존멤버 다시들어올때 요청 - REJOIN@userID@userName\

2. 바꿀때 요청 - CHANGE@newName\
   사용자 닉네임이 바뀌었을때 - HASCHANGED@userID

3. 입장 퇴장 - NOTICE@username님이 참여했습니다/나갔습니다.

4. 사용자 메시지 MSG@userID@userName@msg

5. 사용자 목록 보기 요청 VIEWNICKNAME@\
   사용자 목록 전송하기 VIEW@otherUserName\
   목록 끝 VIEWEND@
