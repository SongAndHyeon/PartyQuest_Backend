package com.partyquest.backend.config.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    NOT_FOUND(404,"COMMON-ERR-404","PAGE NOT FOUND"),
    INTER_SERVER_ERROR(500,"COMMON-ERR-500","INTERNAL SERVER ERROR"),
    EMAIL_DUPLICATION(409,"MEMBER-ERR-409", "EMAIL DUPLICATED"),
    EMAIL_NOT_FOUND(400,"MEMBER-NOT_FOUND-ERR-400", "MEMBER NOT FOUND"),
    OAUTH2_ERROR(400,"MEMBER-OAUTH2LOGIN-ERR-400", "OAUTH2 BAD RESPONSE"),
    PASSWORD_ERROR(400, "MEMBER-PASSWORD-ERR-400", "BAD REQUEST PASSWORD"),
    PARTY_NOT_FOUND(400,"PARTY-NOT_FOUND-ERR-400","PARTY NOT FOUND"),
    PARTY_APPLICATION_DUPLICATED(409,"PARTY-APPLICATION-ERR-409","PARTY APPLICATION DUPLICATE"),
    JWT_TOKEN_MALFORMED(401,"AUTH-JWT-TOKEN-MALFORMED-401","MALFORMED JWT TOKEN"),
    JWT_TOKEN_EXPIRED(401,"AUTH-JWT-TOKEN-EXPIRED-401","EXPIRED JWT TOKEN"),
    JWT_TOKEN_WRONG_TYPE(401,"AUTH-JWT-TOKEN-WRONG_TYPE-401","WRONG TYPE JWT TOKEN"),
    JWT_TOKEN_UNSUPPORTED(401,"AUTH-JWT-TOKEN-UNSUPPORTED-401","NOT SUPPORTED JWT TOKEN TYPE"),
    ACCESS_DENIED(401,"SECURITY-ACCESS-DENIED-401","SECURITY ACCESS DENIED"),
    NOT_PARTY_MEMBER(401,"PARTY-MEMBER-401","NOT PARTY MEMBER"),
    PARTY_MEMBER_ERROR(400,"PARTY-MEMBER-400","PARTY MEMBER ERROR"),
    ;

    private int status;
    private String errorCode;
    private String message;
}
