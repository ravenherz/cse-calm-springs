package com.ravenherz.rhzwe.util.helpers;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Lazy
@Service
@Scope(value = "singleton")
public class HttpErrorHelper {

    public class HttpErrorDescription {

        private int code;
        private String errorClass;
        private String name;
        private String description;

        public int getCode() {
            return code;
        }

        public String getErrorClass() {
            return errorClass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public HttpErrorDescription(int code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
            switch ((int) Math.floor(code / 100.f)) {
                case 4:
                    this.errorClass = "Client error";
                    break;
                case 5:
                    this.errorClass = "Server error";
                    break;
                default:
                    this.errorClass = "Unknown";
                    break;
            }
        }
    }

    private HashMap<Integer, HttpErrorDescription> errorDescriptions;

    public HttpErrorDescription getHttpErrorDescByCode(int code) {
        return errorDescriptions.getOrDefault(code,
                new HttpErrorDescription(666, "Void", "Totally shit on serverside"));
    }

    public HttpErrorHelper() {
        errorDescriptions = new HashMap<>();
        errorDescriptions.put(400, new HttpErrorDescription(
                400,
                "Bad request",
                ""
        ));
        errorDescriptions.put(401, new HttpErrorDescription(
                401,
                "Unauthorized",
                "Authentication is required and has failed or has not yet been provided"
        ));
        errorDescriptions.put(403, new HttpErrorDescription(
                403,
                "Forbidden",
                "Authentication is required and has failed (not sufficient privilegies)"
        ));
        errorDescriptions.put(404, new HttpErrorDescription(
                404,
                "Page not found",
                "It has been moved or deleted, or even didn't ever exist"
        ));
        errorDescriptions.put(405, new HttpErrorDescription(
                405,
                "Method not allowed",
                "A request method is not supported for the requested resource"
        ));
        errorDescriptions.put(406, new HttpErrorDescription(
                406,
                "Not acceptable",
                "The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request"
        ));
        errorDescriptions.put(407, new HttpErrorDescription(
                407,
                "Proxy authentication tequired",
                "The client must first authenticate itself with the proxy"
        ));
        errorDescriptions.put(408, new HttpErrorDescription(
                408,
                "Request timeout",
                "The server timed out waiting for the request"
        ));
        errorDescriptions.put(409, new HttpErrorDescription(
                409,
                "Conflict",
                "Request could not be processed because of conflict in the request"
        ));
        errorDescriptions.put(410, new HttpErrorDescription(
                410,
                "Gone",
                "The resource requested is no longer available and will not be available again"
        ));
        errorDescriptions.put(411, new HttpErrorDescription(
                411,
                "Length required",
                "The request did not specify the length of its content, which is required by the requested resource"
        ));
        errorDescriptions.put(412, new HttpErrorDescription(
                412,
                "Precondition failed",
                "The server does not meet one of the preconditions that the requester put on the request"
        ));
        errorDescriptions.put(413, new HttpErrorDescription(
                413,
                "Payload too large",
                "The request is larger than the server is willing or able to process"
        ));
        errorDescriptions.put(414, new HttpErrorDescription(
                414,
                "URI too long",
                "The URI provided was too long for the server to process"
        ));
        errorDescriptions.put(415, new HttpErrorDescription(
                415,
                "Unsupported media type",
                "The URI provided was too long for the server to process"
        ));
        errorDescriptions.put(416, new HttpErrorDescription(
                416,
                "Range not satisfiable",
                "The server cannot supply that portion of the file for some reason"
        ));
        errorDescriptions.put(417, new HttpErrorDescription(
                417,
                "Expectation failed",
                "The server cannot meet the requirements of the Expect request-header field"
        ));
        errorDescriptions.put(418, new HttpErrorDescription(
                418,
                "I'm a teapot",
                "No, seriously"
        ));
        errorDescriptions.put(419, new HttpErrorDescription(
                419,
                "Expectation failed",
                "The server cannot meet the requirements of the Expect request-header field"
        ));
        errorDescriptions.put(421, new HttpErrorDescription(
                421,
                "Misdirected request",
                "The request was directed at a server that is not able to produce a response"
        ));
        errorDescriptions.put(422, new HttpErrorDescription(
                422,
                "Unprocessable entity",
                "The request was well-formed but was unable to be followed due to semantic errors"
        ));
        errorDescriptions.put(423, new HttpErrorDescription(
                423,
                "Locked",
                "The resource that is being accessed is locked"
        ));
        errorDescriptions.put(429, new HttpErrorDescription(
                429,
                "Locked",
                "The user has sent too many requests in a given amount of time"
        ));
        errorDescriptions.put(431, new HttpErrorDescription(
                431,
                "Request header dields too large",
                "The server is unwilling to process the request because either an individual header field, or all the header fields collectively, are too large"
        ));
        errorDescriptions.put(451, new HttpErrorDescription(
                451,
                "Unavailable for legal reasons",
                "A server operator has received a legal demand to deny access to a resource or to a set of resources that includes the requested resource"
        ));
    }
}