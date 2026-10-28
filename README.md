postman기준으로 작성

로그인
http://127.0.0.1:5173/api/auth/login   POST
{"email":"alice@class.test","password":"Classroom!2026"}
{"email":"bob@class.test","password":"Classroom!2026"}

내정보
http://127.0.0.1:5173/api/me   GET
Authorization   Bearer token에 토큰 필요

글목록
http://127.0.0.1:5173/api/posts   GET

글상세
http://127.0.0.1:5173/api/posts/숫자(id)   GET

글 등록
http://127.0.0.1:5173/api/posts   POST
{"title":"제목","content":"내용"}
토큰필요

{"title":"forged","content":"forged","authorId":1}

글 수정
http://127.0.0.1:5173/api/posts/숫자(id)   PUT
{"title":"제목","content":"내용"}
토큰필요

글 삭제
http://127.0.0.1:5173/api/posts/숫자(id)   DELETE
토큰필요


